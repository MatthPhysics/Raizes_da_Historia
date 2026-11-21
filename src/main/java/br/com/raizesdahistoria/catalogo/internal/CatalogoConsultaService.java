package br.com.raizesdahistoria.catalogo.internal;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.raizesdahistoria.catalogo.internal.dominio.ExposicaoSol;
import br.com.raizesdahistoria.catalogo.internal.dominio.FrequenciaRega;
import br.com.raizesdahistoria.catalogo.internal.dominio.Planta;
import br.com.raizesdahistoria.catalogo.internal.dominio.PlantaRepository;
import br.com.raizesdahistoria.catalogo.internal.dominio.PlantaSpecifications;
import br.com.raizesdahistoria.catalogo.internal.dominio.TipoPlanta;
import br.com.raizesdahistoria.catalogo.internal.dominio.TipoPlantaRepository;

/** Consultas do catálogo público: somente plantas ativas. */
@Service
public class CatalogoConsultaService {

    private final TipoPlantaRepository tipoRepository;
    private final PlantaRepository plantaRepository;

    public CatalogoConsultaService(TipoPlantaRepository tipoRepository, PlantaRepository plantaRepository) {
        this.tipoRepository = tipoRepository;
        this.plantaRepository = plantaRepository;
    }

    @Transactional(readOnly = true)
    public List<TipoPlanta> listarTipos() {
        return tipoRepository.findAll(Sort.by("nome"));
    }

    @Transactional(readOnly = true)
    public Page<Planta> buscarCatalogo(String nome, Integer tipoId, FrequenciaRega rega, ExposicaoSol sol,
            Pageable pageable) {
        var spec = Specification.allOf(
                PlantaSpecifications.ativa(true),
                PlantaSpecifications.nomeContem(nome),
                PlantaSpecifications.doTipo(tipoId),
                PlantaSpecifications.comRega(rega),
                PlantaSpecifications.comSol(sol));
        return plantaRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Planta buscarAtiva(UUID id) {
        return plantaRepository.findWithTipoByIdAndAtivoTrue(id).orElseThrow(PlantaNaoEncontradaException::new);
    }
}
