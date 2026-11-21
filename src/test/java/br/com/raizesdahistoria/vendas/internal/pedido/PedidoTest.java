package br.com.raizesdahistoria.vendas.internal.pedido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PedidoTest {

    private final UUID plantaA = UUID.randomUUID();

    private Pedido novoPedido() {
        var p = new Pedido(UUID.randomUUID(), new BigDecimal("79.80"), Instant.now());
        p.adicionarItem(plantaA, 2, new BigDecimal("39.90"));
        return p;
    }

    @Test
    void pagamentoSoAPartirDePendenteEhIdempotente() {
        var p = novoPedido();
        Instant quando = Instant.now();

        assertThat(p.marcarPago(quando)).isTrue();
        assertThat(p.getStatusPagamento()).isEqualTo(StatusPagamento.PAGO);
        assertThat(p.getPagoEm()).isEqualTo(quando);

        assertThat(p.marcarPago(Instant.now())).isFalse();
        assertThat(p.expirar()).isFalse();
        assertThat(p.cancelar()).isFalse();
        assertThat(p.getPagoEm()).isEqualTo(quando);
    }

    @Test
    void expirarCancelaItens() {
        var p = novoPedido();
        assertThat(p.expirar()).isTrue();
        assertThat(p.getStatusPagamento()).isEqualTo(StatusPagamento.EXPIRADO);
        assertThat(p.getItens()).allMatch(i -> i.getStatusEntrega() == StatusEntrega.CANCELADA);
        assertThat(p.marcarPago(Instant.now())).isFalse();
    }

    @Test
    void statusDeEntregaSoEmPedidoPago() {
        var p = novoPedido();
        var item = p.getItens().get(0);
        assertThatThrownBy(() -> item.atualizarStatusEntrega(StatusEntrega.PRONTA))
                .isInstanceOf(TransicaoStatusInvalidaException.class);

        p.marcarPago(Instant.now());
        item.atualizarStatusEntrega(StatusEntrega.PRONTA);
        assertThat(item.getStatusEntrega()).isEqualTo(StatusEntrega.PRONTA);
        assertThatThrownBy(() -> item.atualizarStatusEntrega(StatusEntrega.CANCELADA))
                .isInstanceOf(TransicaoStatusInvalidaException.class);
    }

    @Test
    void quantidadesPorPlanta() {
        assertThat(novoPedido().quantidadesPorPlanta()).containsEntry(plantaA, 2);
    }

    @Test
    void dinheiroUsaHalfEven() {
        assertThat(Dinheiro.arredondar(new BigDecimal("10.125"))).isEqualByComparingTo("10.12");
        assertThat(Dinheiro.arredondar(new BigDecimal("10.135"))).isEqualByComparingTo("10.14");
        assertThat(Dinheiro.multiplicar(new BigDecimal("39.90"), 3)).isEqualByComparingTo("119.70");
        assertThat(Dinheiro.iguais(new BigDecimal("79.8"), new BigDecimal("79.80"))).isTrue();
    }
}
