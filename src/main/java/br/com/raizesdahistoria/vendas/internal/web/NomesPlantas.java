package br.com.raizesdahistoria.vendas.internal.web;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import br.com.raizesdahistoria.catalogo.CatalogoService;
import br.com.raizesdahistoria.catalogo.PlantaResumo;

/** Resolve nomes das plantas dos itens via API pública do catálogo (uma consulta por pedido). */
@Component
class NomesPlantas {

    private final CatalogoService catalogoService;

    NomesPlantas(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    Map<UUID, String> de(Collection<UUID> ids) {
        return catalogoService.buscarResumos(ids).stream()
                .collect(Collectors.toMap(PlantaResumo::id, PlantaResumo::nomePopular));
    }
}
