package br.com.raizesdahistoria.usuarios.internal.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import br.com.raizesdahistoria.shared.exception.MuitasTentativasException;

/**
 * Limite de tentativas de login por IP em janela deslizante de 1 minuto (em memória).
 * Suficiente para um único deployable; com várias instâncias, trocar por um store compartilhado.
 */
@Component
class LoginRateLimiter {

    private static final Duration JANELA = Duration.ofMinutes(1);
    private static final int LIMITE_CHAVES_ANTES_DE_LIMPAR = 10_000;

    private final Map<String, Deque<Instant>> tentativas = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int maxTentativas;

    LoginRateLimiter(Clock clock, @Value("${raizes.login.max-tentativas-por-minuto:5}") int maxTentativas) {
        this.clock = clock;
        this.maxTentativas = maxTentativas;
    }

    /** Registra uma tentativa para o IP ou lança 429 se o limite da janela já foi atingido. */
    void registrarTentativa(String ip) {
        Instant agora = Instant.now(clock);
        Instant limite = agora.minus(JANELA);
        if (tentativas.size() > LIMITE_CHAVES_ANTES_DE_LIMPAR) {
            limparExpiradas(limite);
        }
        Deque<Instant> fila = tentativas.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (fila) {
            while (!fila.isEmpty() && !fila.peekFirst().isAfter(limite)) {
                fila.pollFirst();
            }
            if (fila.size() >= maxTentativas) {
                throw new MuitasTentativasException(
                        "Muitas tentativas de login. Aguarde um minuto e tente novamente.");
            }
            fila.addLast(agora);
        }
    }

    void limpar() {
        tentativas.clear();
    }

    private void limparExpiradas(Instant limite) {
        tentativas.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                return e.getValue().isEmpty() || !e.getValue().peekLast().isAfter(limite);
            }
        });
    }
}
