package br.com.raizesdahistoria.catalogo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.raizesdahistoria.catalogo.internal.dominio.Planta;
import br.com.raizesdahistoria.catalogo.internal.dominio.PlantaRepository;

/**
 * API pública do catálogo para outros módulos: consulta de plantas, reserva e devolução de estoque.
 * Nunca expõe entidades.
 */
@Service
public class CatalogoService {

    private final PlantaRepository plantaRepository;

    public CatalogoService(PlantaRepository plantaRepository) {
        this.plantaRepository = plantaRepository;
    }

    /** Consulta sem bloqueio (validação de carrinho). Ids inexistentes simplesmente não aparecem no resultado. */
    @Transactional(readOnly = true)
    public List<PlantaResumo> buscarResumos(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return plantaRepository.findByIdIn(ids).stream().map(CatalogoService::toResumo).toList();
    }

    /**
     * Bloqueia as plantas ({@code SELECT ... FOR UPDATE}, em ordem de id) até o fim da transação do chamador.
     * Exige transação ativa: o lock só faz sentido dentro do checkout.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<PlantaResumo> bloquearParaReserva(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return plantaRepository.bloquearPorIds(ids).stream().map(CatalogoService::toResumo).toList();
    }

    /**
     * Decrementa {@code quantidade_pronta_venda}. Chamar após {@link #bloquearParaReserva} na mesma transação.
     *
     * @throws IllegalStateException se alguma planta não existir ou não tiver estoque (o chamador deve validar antes)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void reservarEstoque(Map<UUID, Integer> quantidadesPorPlanta) {
        aplicar(quantidadesPorPlanta, true);
    }

    /** Devolve estoque (pedido expirado ou cancelado). Bloqueia as linhas em ordem de id. */
    @Transactional
    public void devolverEstoque(Map<UUID, Integer> quantidadesPorPlanta) {
        aplicar(quantidadesPorPlanta, false);
    }

    private void aplicar(Map<UUID, Integer> quantidades, boolean reservar) {
        if (quantidades.isEmpty()) {
            return;
        }
        var ordenadas = new TreeMap<>(quantidades);
        Map<UUID, Planta> plantas = new TreeMap<>();
        plantaRepository.bloquearPorIds(ordenadas.keySet()).forEach(p -> plantas.put(p.getId(), p));
        ordenadas.forEach((id, qtd) -> {
            Planta planta = plantas.get(id);
            if (planta == null) {
                throw new IllegalStateException("Planta " + id + " não encontrada");
            }
            if (reservar) {
                planta.reservar(qtd);
            } else {
                planta.devolver(qtd);
            }
        });
    }

    private static PlantaResumo toResumo(Planta p) {
        return new PlantaResumo(p.getId(), p.getNomePopular(), p.getPreco(), p.getQuantidadeProntaVenda(), p.isAtivo());
    }
}
