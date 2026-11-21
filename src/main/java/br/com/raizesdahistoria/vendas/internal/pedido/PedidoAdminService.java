package br.com.raizesdahistoria.vendas.internal.pedido;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.raizesdahistoria.shared.exception.RecursoNaoEncontradoException;

@Service
public class PedidoAdminService {

    /** O período informado pelo admin é interpretado no fuso da loja. */
    public static final ZoneId FUSO_LOJA = ZoneId.of("America/Sao_Paulo");

    private final PedidoRepository pedidoRepository;

    public PedidoAdminService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional(readOnly = true)
    public Page<Pedido> listar(StatusPagamento status, LocalDate de, LocalDate ate, Pageable pageable) {
        List<Specification<Pedido>> specs = new ArrayList<>();
        if (status != null) {
            specs.add((r, q, cb) -> cb.equal(r.get("statusPagamento"), status));
        }
        if (de != null) {
            var inicio = de.atStartOfDay(FUSO_LOJA).toInstant();
            specs.add((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("criadoEm"), inicio));
        }
        if (ate != null) {
            var fim = ate.plusDays(1).atStartOfDay(FUSO_LOJA).toInstant();
            specs.add((r, q, cb) -> cb.lessThan(r.get("criadoEm"), fim));
        }
        return pedidoRepository.findAll(Specification.allOf(specs), pageable);
    }

    @Transactional(readOnly = true)
    public Pedido buscar(UUID id) {
        return pedidoRepository.findWithItensById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));
    }

    @Transactional
    public Pedido atualizarStatusEntrega(UUID pedidoId, UUID itemId, StatusEntrega novo) {
        var pedido = buscar(pedidoId);
        var item = pedido.item(itemId);
        if (item == null) {
            throw new RecursoNaoEncontradoException("Item não encontrado neste pedido.");
        }
        item.atualizarStatusEntrega(novo);
        return pedido;
    }
}
