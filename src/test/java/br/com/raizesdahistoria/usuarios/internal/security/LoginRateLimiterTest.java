package br.com.raizesdahistoria.usuarios.internal.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import br.com.raizesdahistoria.MutableClock;
import br.com.raizesdahistoria.shared.exception.MuitasTentativasException;

class LoginRateLimiterTest {

    @Test
    void limitaCincoTentativasPorMinutoPorIp() {
        var relogio = new MutableClock();
        var limiter = new LoginRateLimiter(relogio, 5);
        for (int i = 0; i < 5; i++) {
            limiter.registrarTentativa("10.0.0.1");
        }
        assertThatThrownBy(() -> limiter.registrarTentativa("10.0.0.1")).isInstanceOf(MuitasTentativasException.class);
        assertThatCode(() -> limiter.registrarTentativa("10.0.0.2")).doesNotThrowAnyException();

        relogio.avancar(Duration.ofSeconds(61));
        assertThatCode(() -> limiter.registrarTentativa("10.0.0.1")).doesNotThrowAnyException();
    }
}
