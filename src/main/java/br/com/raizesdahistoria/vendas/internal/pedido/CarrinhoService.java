package br.com.raizesdahistoria.vendas.internal.pedido;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import br.com.raizesdahistoria.catalogo.CatalogoService;
import br.com.raizesdahistoria.catalogo.PlantaResumo;

/** O carrinho vive no front-end; aqui ele é apenas revalidado com preços e estoque atuais. */
@Service
public class CarrinhoService {

    private final CatalogoService catalogoService;

    public CarrinhoService(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    public CarrinhoValidado validar(Map<UUID, Integer> quantidades, Map<UUID, BigDecimal> precosReferencia) {
        Map<UUID, PlantaResumo> plantas = catalogoService.buscarResumos(quantidades.keySet()).stream()
                .collect(Collectors.toMap(PlantaResumo::id, Function.identity()));
        List<ItemValidado> itens = new ArrayList<>();
        List<Aviso> avisos = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (var e : quantidades.entrySet()) {
            UUID id = e.getKey();
            int qtd = e.getValue();
            PlantaResumo p = plantas.get(id);
            if (p == null || !p.ativo()) {
                avisos.add(new Aviso(id, TipoAviso.PLANTA_INATIVA, "Esta planta não está mais disponível no catálogo."));
                continue;
            }
            BigDecimal subtotal = Dinheiro.multiplicar(p.preco(), qtd);
            itens.add(new ItemValidado(id, p.nomePopular(), qtd, p.preco(), subtotal, p.quantidadeProntaVenda()));
            total = total.add(subtotal);
            if (qtd > p.quantidadeProntaVenda()) {
                avisos.add(new Aviso(id, TipoAviso.ESTOQUE_INSUFICIENTE,
                        "'" + p.nomePopular() + "': solicitado " + qtd + ", disponível " + p.quantidadeProntaVenda() + "."));
            }
            BigDecimal referencia = precosReferencia.get(id);
            if (referencia != null && !Dinheiro.iguais(referencia, p.preco())) {
                avisos.add(new Aviso(id, TipoAviso.PRECO_ALTERADO,
                        "O preço de '" + p.nomePopular() + "' mudou para R$ " + p.preco() + "."));
            }
        }
        boolean podeFinalizar = !itens.isEmpty()
                && avisos.stream().noneMatch(a -> a.tipo() != TipoAviso.PRECO_ALTERADO);
        return new CarrinhoValidado(itens, Dinheiro.arredondar(total), avisos, podeFinalizar);
    }

    public enum TipoAviso {
        PLANTA_INATIVA, ESTOQUE_INSUFICIENTE, PRECO_ALTERADO
    }

    public record Aviso(UUID plantaId, TipoAviso tipo, String mensagem) {
    }

    public record ItemValidado(UUID plantaId, String nomePopular, int quantidade, BigDecimal precoUnitario,
            BigDecimal subtotal, int quantidadeDisponivel) {
    }

    public record CarrinhoValidado(List<ItemValidado> itens, BigDecimal total, List<Aviso> avisos,
            boolean podeFinalizar) {
    }
}
