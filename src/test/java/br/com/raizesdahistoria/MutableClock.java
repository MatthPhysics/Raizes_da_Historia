package br.com.raizesdahistoria;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Relógio controlável nos testes (expiração de pedidos, tokens). Por padrão acompanha o relógio real. */
public class MutableClock extends Clock {

    private volatile Duration deslocamento = Duration.ZERO;

    public void avancar(Duration d) {
        deslocamento = deslocamento.plus(d);
    }

    public void resetar() {
        deslocamento = Duration.ZERO;
    }

    @Override
    public Instant instant() {
        return Instant.now().plus(deslocamento);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }
}
