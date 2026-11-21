package br.com.raizesdahistoria.vendas.internal.pedido;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/** Consolida itens repetidos (mesmo {@code plantaId}) somando as quantidades; ordenado por id. */
public final class ItensSolicitados {

    private ItensSolicitados() {
    }

    public static <T> Map<UUID, Integer> consolidar(Collection<T> itens, Function<T, UUID> plantaId,
            ToIntFunction<T> quantidade) {
        Map<UUID, Integer> mapa = new TreeMap<>();
        itens.forEach(i -> mapa.merge(plantaId.apply(i), quantidade.applyAsInt(i), Integer::sum));
        return mapa;
    }
}
