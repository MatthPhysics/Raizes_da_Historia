package br.com.raizesdahistoria.vendas.internal.pedido;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Item de pedido. Guarda {@code plantaId} (sem associação JPA com o catálogo) e o preço histórico. */
@Entity
@Table(name = "itens_pedido")
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false, updatable = false)
    private Pedido pedido;

    @Column(name = "planta_id", nullable = false, updatable = false)
    private UUID plantaId;

    @Column(nullable = false, updatable = false)
    private int quantidade;

    @Column(name = "preco_unitario", nullable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_entrega", nullable = false, length = 20)
    private StatusEntrega statusEntrega = StatusEntrega.EM_SEPARACAO;

    protected ItemPedido() {
    }

    ItemPedido(Pedido pedido, UUID plantaId, int quantidade, BigDecimal precoUnitario) {
        this.pedido = pedido;
        this.plantaId = plantaId;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
    }

    /** Atualização manual pelo admin: só para pedidos pagos e nunca para/de {@code CANCELADA}. */
    public void atualizarStatusEntrega(StatusEntrega novo) {
        if (pedido.getStatusPagamento() != StatusPagamento.PAGO) {
            throw new TransicaoStatusInvalidaException("Só é possível atualizar a entrega de pedidos pagos.");
        }
        if (novo == StatusEntrega.CANCELADA || statusEntrega == StatusEntrega.CANCELADA) {
            throw new TransicaoStatusInvalidaException(
                    "Itens cancelados não podem ser alterados; para cancelar, cancele o pedido.");
        }
        this.statusEntrega = novo;
    }

    void cancelar() {
        this.statusEntrega = StatusEntrega.CANCELADA;
    }

    public UUID getId() {
        return id;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public UUID getPlantaId() {
        return plantaId;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public StatusEntrega getStatusEntrega() {
        return statusEntrega;
    }
}
