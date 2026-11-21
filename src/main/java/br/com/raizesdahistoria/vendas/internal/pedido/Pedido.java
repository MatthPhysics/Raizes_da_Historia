package br.com.raizesdahistoria.vendas.internal.pedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Pedido da Etapa 1. Guarda apenas o {@code clienteId} (sem associação JPA com outro módulo).
 * Transições de pagamento só a partir de {@code PENDENTE}; os métodos devolvem {@code false} quando a transição
 * não se aplica, para que webhook e job sejam idempotentes.
 */
@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "cliente_id", nullable = false, updatable = false)
    private UUID clienteId;

    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_pagamento", nullable = false, length = 20)
    private StatusPagamento statusPagamento = StatusPagamento.PENDENTE;

    @Column(name = "pix_txid", length = 35, unique = true)
    private String pixTxid;

    @Column(name = "pix_copia_cola", columnDefinition = "text")
    private String pixCopiaCola;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "pago_em")
    private Instant pagoEm;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedido> itens = new ArrayList<>();

    protected Pedido() {
    }

    public Pedido(UUID clienteId, BigDecimal valorTotal, Instant criadoEm) {
        this.clienteId = clienteId;
        this.valorTotal = valorTotal;
        this.criadoEm = criadoEm;
    }

    public void adicionarItem(UUID plantaId, int quantidade, BigDecimal precoUnitario) {
        itens.add(new ItemPedido(this, plantaId, quantidade, precoUnitario));
    }

    public List<ItemPedido> getItens() {
        return Collections.unmodifiableList(itens);
    }

    public void registrarCobranca(String txid, String copiaECola) {
        this.pixTxid = txid;
        this.pixCopiaCola = copiaECola;
    }

    public boolean isPendente() {
        return statusPagamento == StatusPagamento.PENDENTE;
    }

    public boolean marcarPago(Instant quando) {
        if (!isPendente()) {
            return false;
        }
        this.statusPagamento = StatusPagamento.PAGO;
        this.pagoEm = quando;
        return true;
    }

    public boolean expirar() {
        return encerrarSemPagamento(StatusPagamento.EXPIRADO);
    }

    public boolean cancelar() {
        return encerrarSemPagamento(StatusPagamento.CANCELADO);
    }

    public ItemPedido item(UUID itemId) {
        return itens.stream().filter(i -> i.getId().equals(itemId)).findFirst().orElse(null);
    }

    /** Quantidades por planta, usadas para devolver estoque. */
    public Map<UUID, Integer> quantidadesPorPlanta() {
        Map<UUID, Integer> mapa = new LinkedHashMap<>();
        itens.forEach(i -> mapa.merge(i.getPlantaId(), i.getQuantidade(), Integer::sum));
        return mapa;
    }

    private boolean encerrarSemPagamento(StatusPagamento novo) {
        if (!isPendente()) {
            return false;
        }
        this.statusPagamento = novo;
        itens.forEach(ItemPedido::cancelar);
        return true;
    }

    public UUID getId() {
        return id;
    }

    public UUID getClienteId() {
        return clienteId;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public StatusPagamento getStatusPagamento() {
        return statusPagamento;
    }

    public String getPixTxid() {
        return pixTxid;
    }

    public String getPixCopiaCola() {
        return pixCopiaCola;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getPagoEm() {
        return pagoEm;
    }
}
