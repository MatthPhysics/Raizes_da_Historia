package br.com.raizesdahistoria.vendas;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import br.com.raizesdahistoria.AbstractIntegrationTest;
import br.com.raizesdahistoria.Fixtures;
import br.com.raizesdahistoria.shared.exception.RegraNegocioException;
import br.com.raizesdahistoria.usuarios.NovoCliente;
import br.com.raizesdahistoria.vendas.internal.pedido.CheckoutService;

class ConcorrenciaCheckoutIT extends AbstractIntegrationTest {

    @Autowired
    CheckoutService checkoutService;

    @Test
    void doisCheckoutsSimultaneosParaAUltimaUnidadeSoUmSucede() throws Exception {
        UUID ultima = Fixtures.planta(jdbc, "Última muda", "50.00", 1);
        int threads = 2;
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<Boolean>> resultados = new ArrayList<>();
        try {
            for (int i = 0; i < threads; i++) {
                int n = i;
                resultados.add(pool.submit(() -> {
                    largada.await();
                    try {
                        checkoutService.finalizar(new NovoCliente("Cliente " + n, "c" + n + "@email.com",
                                "1199999888" + n, null), Map.of(ultima, 1));
                        return true;
                    } catch (RegraNegocioException e) {
                        return false;
                    }
                }));
            }
            largada.countDown();
            int sucessos = 0;
            for (Future<Boolean> f : resultados) {
                try {
                    if (f.get(30, TimeUnit.SECONDS)) {
                        sucessos++;
                    }
                } catch (TimeoutException e) {
                    throw new AssertionError("Possível deadlock no checkout", e);
                }
            }
            assertThat(sucessos).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
        assertThat(Fixtures.estoque(jdbc, ultima)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pedidos", Integer.class)).isEqualTo(1);
    }

    @Test
    void checkoutsCruzadosNaoGeramDeadlock() throws Exception {
        UUID a = Fixtures.planta(jdbc, "A", "10.00", 100);
        UUID b = Fixtures.planta(jdbc, "B", "10.00", 100);
        int threads = 8;
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<?>> resultados = new ArrayList<>();
        try {
            for (int i = 0; i < threads; i++) {
                int n = i;
                // Metade pede A+B, metade B+A: com lock ordenado por id não há deadlock
                Map<UUID, Integer> itens = n % 2 == 0 ? new java.util.LinkedHashMap<>(Map.of(a, 1)) : new java.util.LinkedHashMap<>(Map.of(b, 1));
                itens.put(n % 2 == 0 ? b : a, 1);
                resultados.add(pool.submit(() -> {
                    largada.await();
                    checkoutService.finalizar(new NovoCliente("C" + n, "c" + n + "@email.com", "11999998888", null),
                            new java.util.TreeMap<>(itens));
                    return null;
                }));
            }
            largada.countDown();
            for (Future<?> f : resultados) {
                f.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
        assertThat(Fixtures.estoque(jdbc, a)).isEqualTo(100 - threads);
        assertThat(Fixtures.estoque(jdbc, b)).isEqualTo(100 - threads);
    }
}
