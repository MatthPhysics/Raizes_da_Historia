package br.com.raizesdahistoria.vendas.internal.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.raizesdahistoria.shared.exception.NaoAutorizadoException;
import br.com.raizesdahistoria.vendas.internal.pedido.PagamentoService;
import br.com.raizesdahistoria.vendas.internal.pix.PixProperties;
import br.com.raizesdahistoria.vendas.internal.web.VendasDtos.WebhookPixRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Notificação do PSP. Protegida por segredo compartilhado (cabeçalho {@code X-Webhook-Token} ou parâmetro
 * {@code token}); em produção, somar mTLS conforme o PSP. Mesmo autenticada, a notificação só serve de gatilho:
 * o pagamento é sempre conferido no PSP.
 */
@RestController
@RequestMapping("/api/v1/pagamentos/pix")
@Tag(name = "Pagamentos")
class WebhookPixController {

    private static final Logger log = LoggerFactory.getLogger(WebhookPixController.class);

    private final PagamentoService pagamentoService;
    private final PixProperties pixProperties;

    WebhookPixController(PagamentoService pagamentoService, PixProperties pixProperties) {
        this.pagamentoService = pagamentoService;
        this.pixProperties = pixProperties;
    }

    @PostMapping("/webhook")
    @Operation(summary = "Webhook do PSP (formato API Pix do Bacen)")
    ResponseEntity<Void> receber(@RequestHeader(name = "X-Webhook-Token", required = false) String tokenHeader,
            @RequestParam(name = "token", required = false) String tokenParam,
            @RequestBody(required = false) WebhookPixRequest corpo) {
        autenticar(tokenHeader != null ? tokenHeader : tokenParam);
        if (corpo == null || corpo.pix() == null) {
            return ResponseEntity.ok().build(); // PSPs costumam enviar um POST de teste ao cadastrar o webhook
        }
        for (var pix : corpo.pix()) {
            if (pix.txid() == null || pix.txid().isBlank()) {
                continue;
            }
            try {
                pagamentoService.confirmarPagamento(pix.txid());
            } catch (RuntimeException e) {
                log.error("Falha ao processar notificação Pix", e);
            }
        }
        return ResponseEntity.ok().build();
    }

    private void autenticar(String token) {
        String segredo = pixProperties.webhookSecret();
        if (segredo == null || segredo.isBlank()) {
            log.error("PIX_WEBHOOK_SECRET não configurado: webhook rejeitado");
            throw new NaoAutorizadoException("Webhook não autorizado.");
        }
        if (token == null || !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                segredo.getBytes(StandardCharsets.UTF_8))) {
            log.warn("Webhook Pix com token inválido");
            throw new NaoAutorizadoException("Webhook não autorizado.");
        }
    }
}
