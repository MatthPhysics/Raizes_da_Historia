package br.com.raizesdahistoria.vendas.internal.web;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.raizesdahistoria.shared.exception.RecursoNaoEncontradoException;
import br.com.raizesdahistoria.vendas.internal.pedido.PagamentoService;
import br.com.raizesdahistoria.vendas.internal.pix.MockPixGateway;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Somente no profile {@code dev}: simula o pagamento de uma cobrança do PSP mock e dispara o fluxo do webhook. */
@RestController
@Profile("dev")
@RequestMapping("/api/v1/dev/pix")
@Tag(name = "Dev · Simulação Pix")
class DevPixController {

    private final MockPixGateway mockPixGateway;
    private final PagamentoService pagamentoService;

    DevPixController(MockPixGateway mockPixGateway, PagamentoService pagamentoService) {
        this.mockPixGateway = mockPixGateway;
        this.pagamentoService = pagamentoService;
    }

    @PostMapping("/{txid}/simular-pagamento")
    @Operation(summary = "Simula o pagamento do Pix (apenas dev)")
    ResponseEntity<Void> simular(@PathVariable String txid) {
        if (!mockPixGateway.simularPagamento(txid)) {
            throw new RecursoNaoEncontradoException("Cobrança não encontrada ou não está ativa.");
        }
        pagamentoService.confirmarPagamento(txid);
        return ResponseEntity.noContent().build();
    }
}
