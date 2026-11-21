package br.com.raizesdahistoria.vendas.internal.web;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.raizesdahistoria.shared.exception.RecursoNaoEncontradoException;
import br.com.raizesdahistoria.vendas.internal.pedido.CarrinhoService;
import br.com.raizesdahistoria.vendas.internal.pedido.CarrinhoService.CarrinhoValidado;
import br.com.raizesdahistoria.vendas.internal.pedido.CheckoutService;
import br.com.raizesdahistoria.vendas.internal.pedido.ItensSolicitados;
import br.com.raizesdahistoria.vendas.internal.pedido.Pedido;
import br.com.raizesdahistoria.vendas.internal.pedido.PedidoRepository;
import br.com.raizesdahistoria.vendas.internal.pedido.StatusPagamento;
import br.com.raizesdahistoria.vendas.internal.pix.PixProperties;
import br.com.raizesdahistoria.vendas.internal.pix.QrCodeService;
import br.com.raizesdahistoria.vendas.internal.web.VendasDtos.CheckoutRequest;
import br.com.raizesdahistoria.vendas.internal.web.VendasDtos.ItemCarrinhoRequest;
import br.com.raizesdahistoria.vendas.internal.web.VendasDtos.ItemResponse;
import br.com.raizesdahistoria.vendas.internal.web.VendasDtos.PedidoCriadoResponse;
import br.com.raizesdahistoria.vendas.internal.web.VendasDtos.PedidoPublicoResponse;
import br.com.raizesdahistoria.vendas.internal.web.VendasDtos.PixResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1")
@Validated
@Tag(name = "Carrinho e Pedidos (público)")
class PedidoPublicoController {

    private final CarrinhoService carrinhoService;
    private final CheckoutService checkoutService;
    private final PedidoRepository pedidoRepository;
    private final QrCodeService qrCodeService;
    private final NomesPlantas nomesPlantas;
    private final PixProperties pixProperties;

    PedidoPublicoController(CarrinhoService carrinhoService, CheckoutService checkoutService,
            PedidoRepository pedidoRepository, QrCodeService qrCodeService, NomesPlantas nomesPlantas,
            PixProperties pixProperties) {
        this.carrinhoService = carrinhoService;
        this.checkoutService = checkoutService;
        this.pedidoRepository = pedidoRepository;
        this.qrCodeService = qrCodeService;
        this.nomesPlantas = nomesPlantas;
        this.pixProperties = pixProperties;
    }

    @PostMapping("/carrinho/validar")
    @Operation(summary = "Revalida o carrinho do front-end",
            description = "Devolve preços atuais, subtotais, total e avisos (planta inativa, estoque insuficiente, "
                    + "preço alterado). Itens repetidos são somados.")
    CarrinhoValidado validarCarrinho(
            @RequestBody @NotEmpty(message = "O carrinho está vazio.") @Size(max = 50, message = "Máximo de 50 itens.")
            List<@Valid ItemCarrinhoRequest> itens) {
        var quantidades = ItensSolicitados.consolidar(itens, ItemCarrinhoRequest::plantaId, ItemCarrinhoRequest::quantidade);
        Map<UUID, BigDecimal> referencias = new java.util.HashMap<>();
        itens.stream().filter(i -> i.precoReferencia() != null)
                .forEach(i -> referencias.put(i.plantaId(), i.precoReferencia()));
        return carrinhoService.validar(quantidades, referencias);
    }

    @PostMapping("/pedidos")
    @Operation(summary = "Fecha a compra (checkout rápido) e gera a cobrança Pix",
            description = "Cria cliente, pedido e itens, reserva o estoque e devolve o Pix copia-e-cola e o QR Code "
                    + "(PNG em Base64). Preço e total são sempre calculados no servidor.")
    ResponseEntity<PedidoCriadoResponse> finalizar(@Valid @RequestBody CheckoutRequest request) {
        var quantidades = ItensSolicitados.consolidar(request.itens(),
                VendasDtos.ItemCheckoutRequest::plantaId, VendasDtos.ItemCheckoutRequest::quantidade);
        var criado = checkoutService.finalizar(request.cliente(), quantidades);
        Pedido p = criado.pedido();
        var pix = new PixResponse(p.getPixTxid(), p.getPixCopiaCola(),
                qrCodeService.gerarPngBase64(p.getPixCopiaCola()), criado.pixExpiraEm());
        var itens = p.getItens().stream()
                .map(i -> new ItemResponse(i.getPlantaId(), criado.nomesPlantas().get(i.getPlantaId()),
                        i.getQuantidade(), i.getPrecoUnitario()))
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new PedidoCriadoResponse(p.getId(), p.getValorTotal(), p.getStatusPagamento(), pix, itens));
    }

    @GetMapping("/pedidos/{id}")
    @Operation(summary = "Status do pedido e dados do Pix (polling do front)",
            description = "Não retorna dados pessoais do cliente. O QR Code só é enviado enquanto o pedido está pendente.")
    ResponseEntity<PedidoPublicoResponse> consultar(@PathVariable UUID id) {
        Pedido p = pedidoRepository.findWithItensById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));
        var nomes = nomesPlantas.de(p.getItens().stream().map(i -> i.getPlantaId()).toList());
        var itens = p.getItens().stream()
                .map(i -> new ItemResponse(i.getPlantaId(), nomes.get(i.getPlantaId()), i.getQuantidade(),
                        i.getPrecoUnitario()))
                .toList();
        PixResponse pix = null;
        if (p.getPixTxid() != null) {
            boolean pendente = p.getStatusPagamento() == StatusPagamento.PENDENTE;
            pix = new PixResponse(p.getPixTxid(),
                    pendente ? p.getPixCopiaCola() : null,
                    pendente ? qrCodeService.gerarPngBase64(p.getPixCopiaCola()) : null,
                    p.getCriadoEm().plus(Duration.ofMinutes(pixProperties.expiracaoMinutos())));
        }
        var body = new PedidoPublicoResponse(p.getId(), p.getStatusPagamento(), p.getValorTotal(), p.getCriadoEm(),
                p.getPagoEm(), pix, itens);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}
