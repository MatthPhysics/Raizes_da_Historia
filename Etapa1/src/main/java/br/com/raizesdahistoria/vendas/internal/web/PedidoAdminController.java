package br.com.raizesdahistoria.vendas.internal.web;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.raizesdahistoria.shared.config.OpenApiConfig;
import br.com.raizesdahistoria.usuarios.ClienteService;
import br.com.raizesdahistoria.vendas.internal.pedido.PagamentoService;
import br.com.raizesdahistoria.vendas.internal.pedido.Pedido;
import br.com.raizesdahistoria.vendas.internal.pedido.PedidoAdminService;
import br.com.raizesdahistoria.vendas.internal.pedido.StatusPagamento;
import br.com.raizesdahistoria.vendas.internal.web.VendasDtos.ItemAdminResponse;
import br.com.raizesdahistoria.vendas.internal.web.VendasDtos.PedidoAdminResponse;
import br.com.raizesdahistoria.vendas.internal.web.VendasDtos.PedidoResumoAdmin;
import br.com.raizesdahistoria.vendas.internal.web.VendasDtos.StatusEntregaRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/pedidos")
@Tag(name = "Admin · Pedidos")
@SecurityRequirement(name = OpenApiConfig.BEARER)
class PedidoAdminController {

    private final PedidoAdminService service;
    private final PagamentoService pagamentoService;
    private final ClienteService clienteService;
    private final NomesPlantas nomesPlantas;

    PedidoAdminController(PedidoAdminService service, PagamentoService pagamentoService,
            ClienteService clienteService, NomesPlantas nomesPlantas) {
        this.service = service;
        this.pagamentoService = pagamentoService;
        this.clienteService = clienteService;
        this.nomesPlantas = nomesPlantas;
    }

    @GetMapping
    @Operation(summary = "Lista pedidos", description = "Filtros: statusPagamento e período (de/até, datas no fuso de São Paulo).")
    Page<PedidoResumoAdmin> listar(
            @RequestParam(required = false) StatusPagamento statusPagamento,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @PageableDefault(size = 20, sort = "criadoEm", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.listar(statusPagamento, de, ate, pageable)
                .map(p -> new PedidoResumoAdmin(p.getId(), p.getClienteId(), p.getValorTotal(),
                        p.getStatusPagamento(), p.getPixTxid(), p.getCriadoEm(), p.getPagoEm()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe do pedido com cliente e itens")
    PedidoAdminResponse detalhar(@PathVariable UUID id) {
        return toResponse(service.buscar(id));
    }

    @PatchMapping("/{pedidoId}/itens/{itemId}/status-entrega")
    @Operation(summary = "Atualiza o status de entrega de um item (somente pedidos pagos)")
    PedidoAdminResponse atualizarStatusEntrega(@PathVariable UUID pedidoId, @PathVariable UUID itemId,
            @Valid @RequestBody StatusEntregaRequest req) {
        return toResponse(service.atualizarStatusEntrega(pedidoId, itemId, req.statusEntrega()));
    }

    @PatchMapping("/{id}/cancelar")
    @Operation(summary = "Cancela pedido pendente e devolve o estoque")
    PedidoAdminResponse cancelar(@PathVariable UUID id) {
        pagamentoService.cancelar(id);
        return toResponse(service.buscar(id));
    }

    private PedidoAdminResponse toResponse(Pedido p) {
        var nomes = nomesPlantas.de(p.getItens().stream().map(i -> i.getPlantaId()).toList());
        var itens = p.getItens().stream()
                .map(i -> new ItemAdminResponse(i.getId(), i.getPlantaId(), nomes.get(i.getPlantaId()),
                        i.getQuantidade(), i.getPrecoUnitario(), i.getStatusEntrega()))
                .toList();
        return new PedidoAdminResponse(p.getId(), p.getStatusPagamento(), p.getValorTotal(), p.getPixTxid(),
                p.getCriadoEm(), p.getPagoEm(), clienteService.buscar(p.getClienteId()).orElse(null), itens);
    }
}
