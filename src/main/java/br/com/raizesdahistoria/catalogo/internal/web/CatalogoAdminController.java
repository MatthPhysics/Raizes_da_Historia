package br.com.raizesdahistoria.catalogo.internal.web;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.com.raizesdahistoria.catalogo.internal.CatalogoAdminService;
import br.com.raizesdahistoria.catalogo.internal.dominio.ExposicaoSol;
import br.com.raizesdahistoria.catalogo.internal.dominio.FrequenciaRega;
import br.com.raizesdahistoria.catalogo.internal.web.CatalogoDtos.AtivoRequest;
import br.com.raizesdahistoria.catalogo.internal.web.CatalogoDtos.EstoqueRequest;
import br.com.raizesdahistoria.catalogo.internal.web.CatalogoDtos.PlantaAdminResponse;
import br.com.raizesdahistoria.catalogo.internal.web.CatalogoDtos.PlantaRequest;
import br.com.raizesdahistoria.catalogo.internal.web.CatalogoDtos.TipoPlantaRequest;
import br.com.raizesdahistoria.catalogo.internal.web.CatalogoDtos.TipoPlantaResponse;
import br.com.raizesdahistoria.shared.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin · Catálogo")
@SecurityRequirement(name = OpenApiConfig.BEARER)
class CatalogoAdminController {

    private final CatalogoAdminService service;

    CatalogoAdminController(CatalogoAdminService service) {
        this.service = service;
    }

    // ---------- Tipos ----------

    @PostMapping("/tipos-planta")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria tipo de planta")
    TipoPlantaResponse criarTipo(@Valid @RequestBody TipoPlantaRequest req) {
        return TipoPlantaResponse.de(service.criarTipo(req.nome(), req.descricao()));
    }

    @PutMapping("/tipos-planta/{id}")
    @Operation(summary = "Atualiza tipo de planta")
    TipoPlantaResponse atualizarTipo(@PathVariable Integer id, @Valid @RequestBody TipoPlantaRequest req) {
        return TipoPlantaResponse.de(service.atualizarTipo(id, req.nome(), req.descricao()));
    }

    @DeleteMapping("/tipos-planta/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui tipo de planta (409 se houver plantas vinculadas)")
    void excluirTipo(@PathVariable Integer id) {
        service.excluirTipo(id);
    }

    // ---------- Plantas ----------

    @GetMapping("/plantas")
    @Operation(summary = "Lista todas as plantas, inclusive inativas")
    Page<PlantaAdminResponse> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) Integer tipoId,
            @RequestParam(required = false) FrequenciaRega frequenciaRega,
            @RequestParam(required = false) ExposicaoSol exposicaoSol,
            @RequestParam(required = false) Boolean ativo,
            @PageableDefault(size = 20, sort = "nomePopular", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.listar(nome, tipoId, frequenciaRega, exposicaoSol, ativo, pageable)
                .map(PlantaAdminResponse::de);
    }

    @GetMapping("/plantas/{id}")
    @Operation(summary = "Detalhe de uma planta (inclusive inativa)")
    PlantaAdminResponse detalhar(@PathVariable UUID id) {
        return PlantaAdminResponse.de(service.buscarPlanta(id));
    }

    @PostMapping("/plantas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria planta")
    PlantaAdminResponse criar(@Valid @RequestBody PlantaRequest req) {
        return PlantaAdminResponse.de(service.criarPlanta(req.tipoId(), req.nomePopular(), req.nomeCientifico(),
                req.preco(), req.frequenciaRega(), req.exposicaoSol(), req.quantidadeProntaVenda()));
    }

    @PutMapping("/plantas/{id}")
    @Operation(summary = "Edita planta (não altera estoque nem pedidos existentes)")
    PlantaAdminResponse atualizar(@PathVariable UUID id, @Valid @RequestBody PlantaRequest req) {
        return PlantaAdminResponse.de(service.atualizarPlanta(id, req.tipoId(), req.nomePopular(),
                req.nomeCientifico(), req.preco(), req.frequenciaRega(), req.exposicaoSol()));
    }

    @DeleteMapping("/plantas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove planta do catálogo (soft delete: ativo = false)")
    void remover(@PathVariable UUID id) {
        service.desativar(id);
    }

    @PatchMapping("/plantas/{id}/ativo")
    @Operation(summary = "Ativa ou desativa planta")
    PlantaAdminResponse definirAtivo(@PathVariable UUID id, @Valid @RequestBody AtivoRequest req) {
        return PlantaAdminResponse.de(service.definirAtivo(id, req.ativo()));
    }

    @PatchMapping("/plantas/{id}/estoque")
    @Operation(summary = "Define a quantidade pronta para venda")
    PlantaAdminResponse ajustarEstoque(@PathVariable UUID id, @Valid @RequestBody EstoqueRequest req) {
        return PlantaAdminResponse.de(service.ajustarEstoque(id, req.quantidadeProntaVenda()));
    }

    @PostMapping(path = "/plantas/{id}/imagem", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Envia imagem da planta (JPEG, PNG ou WEBP, até 5 MB)")
    PlantaAdminResponse enviarImagem(@PathVariable UUID id, @RequestPart("arquivo") MultipartFile arquivo) {
        return PlantaAdminResponse.de(service.atualizarImagem(id, arquivo));
    }
}
