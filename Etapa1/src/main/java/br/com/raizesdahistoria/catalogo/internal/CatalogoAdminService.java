package br.com.raizesdahistoria.catalogo.internal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.com.raizesdahistoria.catalogo.internal.dominio.ExposicaoSol;
import br.com.raizesdahistoria.catalogo.internal.dominio.FrequenciaRega;
import br.com.raizesdahistoria.catalogo.internal.dominio.Planta;
import br.com.raizesdahistoria.catalogo.internal.dominio.PlantaRepository;
import br.com.raizesdahistoria.catalogo.internal.dominio.PlantaSpecifications;
import br.com.raizesdahistoria.catalogo.internal.dominio.TipoPlanta;
import br.com.raizesdahistoria.catalogo.internal.dominio.TipoPlantaRepository;
import br.com.raizesdahistoria.shared.exception.ConflitoException;
import br.com.raizesdahistoria.shared.exception.RecursoNaoEncontradoException;
import br.com.raizesdahistoria.shared.exception.RegraNegocioException;
import br.com.raizesdahistoria.shared.storage.ArmazenamentoImagemService;
import br.com.raizesdahistoria.shared.storage.ImagemInvalidaException;

@Service
public class CatalogoAdminService {

    private static final Logger log = LoggerFactory.getLogger(CatalogoAdminService.class);

    private final TipoPlantaRepository tipoRepository;
    private final PlantaRepository plantaRepository;
    private final ArmazenamentoImagemService armazenamento;

    public CatalogoAdminService(TipoPlantaRepository tipoRepository, PlantaRepository plantaRepository, ArmazenamentoImagemService armazenamento) {
        this.tipoRepository = tipoRepository;
        this.plantaRepository = plantaRepository;
        this.armazenamento = armazenamento;
    }
    // ---------- Tipos ----------

    @Transactional
    public TipoPlanta criarTipo(String nome, String descricao) {
        if (tipoRepository.existsByNomeIgnoreCase(nome.trim())) {
            throw new ConflitoException("Já existe um tipo de planta com este nome.");
        }
        return tipoRepository.save(new TipoPlanta(nome.trim(), descricao));
    }

    @Transactional
    public TipoPlanta atualizarTipo(Integer id, String nome, String descricao) {
        var tipo = buscarTipo(id);
        if (tipoRepository.existsByNomeIgnoreCaseAndIdNot(nome.trim(), id)) {
            throw new ConflitoException("Já existe um tipo de planta com este nome.");
        }
        tipo.atualizar(nome.trim(), descricao);
        return tipo;
    }

    @Transactional
    public void excluirTipo(Integer id) {
        var tipo = buscarTipo(id);
        if (plantaRepository.existsByTipoId(id)) {
            throw new ConflitoException("Não é possível excluir: existem plantas vinculadas a este tipo.");
        }
        tipoRepository.delete(tipo);
    }

    // ---------- Plantas ----------

    @Transactional(readOnly = true)
    public Page<Planta> listar(String nome, Integer tipoId, FrequenciaRega rega, ExposicaoSol sol, Boolean ativo,
            Pageable pageable) {
        Specification<Planta> spec = Specification.allOf(
                PlantaSpecifications.ativa(ativo),
                PlantaSpecifications.nomeContem(nome),
                PlantaSpecifications.doTipo(tipoId),
                PlantaSpecifications.comRega(rega),
                PlantaSpecifications.comSol(sol));
        return plantaRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Planta buscarPlanta(UUID id) {
        return plantaRepository.findWithTipoById(id).orElseThrow(PlantaNaoEncontradaException::new);
    }

    @Transactional
    public Planta criarPlanta(Integer tipoId, String nomePopular, String nomeCientifico, BigDecimal preco,
            FrequenciaRega rega, ExposicaoSol sol, Integer quantidade) {
        var planta = new Planta(buscarTipoParaPlanta(tipoId), nomePopular.trim(), vazioParaNulo(nomeCientifico),
                preco, rega, sol, quantidade == null ? 0 : quantidade);
        planta = plantaRepository.save(planta);
        log.info("Planta {} criada", planta.getId());
        return planta;
    }

    /** Edição não altera pedidos existentes: o preço histórico fica em {@code itens_pedido.preco_unitario}. */
    @Transactional
    public Planta atualizarPlanta(UUID id, Integer tipoId, String nomePopular, String nomeCientifico,
            BigDecimal preco, FrequenciaRega rega, ExposicaoSol sol) {
        var planta = buscarPlanta(id);
        planta.atualizar(buscarTipoParaPlanta(tipoId), nomePopular.trim(), vazioParaNulo(nomeCientifico),
                preco, rega, sol);
        return planta;
    }

    /** "Remover" = soft delete: plantas são referenciadas por itens de pedido. */
    @Transactional
    public void desativar(UUID id) {
        definirAtivo(id, false);
    }

    @Transactional
    public Planta definirAtivo(UUID id, boolean ativo) {
        var planta = buscarPlanta(id);
        planta.definirAtivo(ativo);
        log.info("Planta {} {}", id, ativo ? "reativada" : "desativada");
        return planta;
    }

    @Transactional
    public Planta ajustarEstoque(UUID id, int quantidade) {
        // Bloqueia a linha para não conflitar com uma reserva de checkout em andamento
        var planta = plantaRepository.bloquearPorIds(List.of(id)).stream().findFirst()
                .orElseThrow(PlantaNaoEncontradaException::new);
        Hibernate.initialize(planta.getTipo());
        planta.ajustarEstoque(quantidade);
        log.info("Estoque da planta {} ajustado para {}", id, quantidade);
        return planta;
    }

    @Transactional
    public Planta atualizarImagem(UUID id, MultipartFile arquivo) {
        var planta = buscarPlanta(id);
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ImagemInvalidaException("Envie um arquivo de imagem.");
        }
        byte[] conteudo;
        try {
            conteudo = arquivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String anterior = planta.getUrlImagem();
        planta.definirUrlImagem(armazenamento.salvar(conteudo));
        armazenamento.remover(anterior);
        return planta;
    }

    private TipoPlanta buscarTipo(Integer id) {
        return tipoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tipo de planta não encontrado."));
    }

    private TipoPlanta buscarTipoParaPlanta(Integer id) {
        return tipoRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Tipo de planta informado não existe."));
    }

    private static String vazioParaNulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
