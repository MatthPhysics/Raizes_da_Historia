package br.com.raizesdahistoria.catalogo.internal.web;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.raizesdahistoria.catalogo.internal.CatalogoConsultaService;
import br.com.raizesdahistoria.catalogo.internal.dominio.ExposicaoSol;
import br.com.raizesdahistoria.catalogo.internal.dominio.FrequenciaRega;
import br.com.raizesdahistoria.catalogo.internal.web.CatalogoDtos.PlantaResponse;
import br.com.raizesdahistoria.catalogo.internal.web.CatalogoDtos.TipoPlantaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Catálogo (público)")
class CatalogoPublicoController {

    /** Apoia o cache offline do PWA. */
    private static final CacheControl CACHE_CATALOGO = CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic();

    private final CatalogoConsultaService service;

    CatalogoPublicoController(CatalogoConsultaService service) {
        this.service = service;
    }

    @GetMapping("/tipos-planta")
    @Operation(summary = "Lista os tipos de planta")
    ResponseEntity<List<TipoPlantaResponse>> listarTipos() {
        var tipos = service.listarTipos().stream().map(TipoPlantaResponse::de).toList();
        return ResponseEntity.ok().cacheControl(CACHE_CATALOGO).body(tipos);
    }

    @GetMapping("/plantas")
    @Operation(summary = "Catálogo de plantas ativas",
            description = "Busca por nome é parcial, sem acento e sem caixa (nome popular e científico). "
                    + "Plantas esgotadas aparecem com disponivel=false. Paginação: page, size (máx. 50), sort.")
    ResponseEntity<Page<PlantaResponse>> listarPlantas(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) Integer tipoId,
            @RequestParam(required = false) FrequenciaRega frequenciaRega,
            @RequestParam(required = false) ExposicaoSol exposicaoSol,
            @PageableDefault(size = 20, sort = "nomePopular", direction = Sort.Direction.ASC) Pageable pageable) {
        var pagina = service.buscarCatalogo(nome, tipoId, frequenciaRega, exposicaoSol, pageable)
                .map(PlantaResponse::de);
        return ResponseEntity.ok().cacheControl(CACHE_CATALOGO).body(pagina);
    }

    @GetMapping("/plantas/{id}")
    @Operation(summary = "Detalhe de uma planta ativa (404 se inativa ou inexistente)")
    PlantaResponse detalhar(@PathVariable UUID id) {
        return PlantaResponse.de(service.buscarAtiva(id));
    }
}
