package br.com.raizesdahistoria.vendas.internal.pedido;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface PedidoRepository extends JpaRepository<Pedido, UUID>, JpaSpecificationExecutor<Pedido> {

    @EntityGraph(attributePaths = "itens")
    Optional<Pedido> findWithItensById(UUID id);

    /** Bloqueia o pedido: serializa webhook, job de expiração e cancelamento do mesmo pedido. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id")
    Optional<Pedido> bloquearPorId(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.pixTxid = :txid")
    Optional<Pedido> bloquearPorTxid(@Param("txid") String txid);

    @Query("select p.id from Pedido p where p.statusPagamento = :status and p.criadoEm < :limite order by p.criadoEm")
    List<UUID> idsPorStatusCriadosAntesDe(@Param("status") StatusPagamento status, @Param("limite") Instant limite,
            Pageable pageable);
}
