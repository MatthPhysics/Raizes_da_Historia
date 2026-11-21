package br.com.raizesdahistoria.vendas.internal.pedido;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.raizesdahistoria.catalogo.CatalogoService;
import br.com.raizesdahistoria.catalogo.PlantaResumo;
import br.com.raizesdahistoria.shared.exception.RegraNegocioException;
import br.com.raizesdahistoria.usuarios.ClienteService;
import br.com.raizesdahistoria.usuarios.NovoCliente;
import br.com.raizesdahistoria.vendas.internal.pix.GeradorTxid;
import br.com.raizesdahistoria.vendas.internal.pix.PixGateway;
import br.com.raizesdahistoria.vendas.internal.pix.PixProperties;
import br.com.raizesdahistoria.vendas.internal.pix.PspIndisponivelException;

/**
 * Checkout (§7.1 do CLAUDE.md) — tudo em uma única transação: se o PSP falhar, o rollback devolve o estoque.
 */
@Service
public class CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);
    public static final int QUANTIDADE_MAXIMA_POR_ITEM = 99;

    private final CatalogoService catalogoService;
    private final ClienteService clienteService;
    private final PedidoRepository pedidoRepository;
    private final PixGateway pixGateway;
    private final GeradorTxid geradorTxid;
    private final PixProperties pixProperties;
    private final Clock clock;

    public CheckoutService(CatalogoService catalogoService, ClienteService clienteService, PedidoRepository pedidoRepository, PixGateway pixGateway, GeradorTxid geradorTxid, PixProperties pixProperties, Clock clock) {
        this.catalogoService = catalogoService;
        this.clienteService = clienteService;
        this.pedidoRepository = pedidoRepository;
        this.pixGateway = pixGateway;
        this.geradorTxid = geradorTxid;
        this.pixProperties = pixProperties;
        this.clock = clock;
    }

    @Transactional
    public PedidoCriado finalizar(NovoCliente dadosCliente, Map<UUID, Integer> quantidades) {
        // 1. Validação (itens já consolidados por plantaId)
        if (quantidades.isEmpty()) {
            throw new RegraNegocioException("O pedido deve ter ao menos um item.");
        }
        var excedidos = quantidades.entrySet().stream()
                .filter(e -> e.getValue() < 1 || e.getValue() > QUANTIDADE_MAXIMA_POR_ITEM)
                .map(e -> "Quantidade da planta " + e.getKey() + " deve estar entre 1 e " + QUANTIDADE_MAXIMA_POR_ITEM + ".")
                .toList();
        if (!excedidos.isEmpty()) {
            throw new RegraNegocioException("Quantidade inválida.", excedidos);
        }

        // 2. Lock pessimista das plantas, em ordem de id (evita deadlock)
        Map<UUID, PlantaResumo> plantas = catalogoService.bloquearParaReserva(quantidades.keySet()).stream()
                .collect(Collectors.toMap(PlantaResumo::id, Function.identity()));

        // 3. Disponibilidade
        List<String> problemas = new ArrayList<>();
        quantidades.forEach((id, qtd) -> {
            PlantaResumo p = plantas.get(id);
            if (p == null) {
                problemas.add("Planta " + id + " não encontrada.");
            } else if (!p.ativo()) {
                problemas.add("'" + p.nomePopular() + "' não está mais disponível no catálogo.");
            } else if (p.quantidadeProntaVenda() < qtd) {
                problemas.add("'" + p.nomePopular() + "': solicitado " + qtd + ", disponível "
                        + p.quantidadeProntaVenda() + ".");
            }
        });
        if (!problemas.isEmpty()) {
            throw new RegraNegocioException("Não foi possível finalizar a compra.", problemas);
        }

        // 4. Total calculado no servidor (preço enviado pelo cliente é ignorado)
        BigDecimal total = quantidades.entrySet().stream()
                .map(e -> Dinheiro.multiplicar(plantas.get(e.getKey()).preco(), e.getValue()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        total = Dinheiro.arredondar(total);

        // 5. Reserva de estoque
        catalogoService.reservarEstoque(quantidades);

        // 6. Cliente (checkout rápido: sempre novo registro)
        var cliente = clienteService.criar(dadosCliente);

        // Pedido + itens com preço histórico
        Instant agora = Instant.now(clock);
        var pedido = new Pedido(cliente.id(), total, agora);
        quantidades.forEach((id, qtd) -> pedido.adicionarItem(id, qtd, plantas.get(id).preco()));
        pedidoRepository.saveAndFlush(pedido);

        // 7. Cobrança Pix
        String txid = geradorTxid.gerar();
        Duration expiracao = Duration.ofMinutes(pixProperties.expiracaoMinutos());
        PixGateway.CobrancaCriada cobranca;
        try {
            cobranca = pixGateway.criarCobranca(txid, total, expiracao,
                    new PixGateway.Devedor(cliente.nome(), cliente.cpf()));
        } catch (PspIndisponivelException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Erro inesperado ao criar cobrança Pix do pedido {}", pedido.getId(), e);
            throw new PspIndisponivelException("Erro inesperado no gateway Pix", e);
        }
        pedido.registrarCobranca(cobranca.txid(), cobranca.copiaECola());

        log.info("Pedido {} criado: {} item(ns), total {}", pedido.getId(), quantidades.size(), total);
        Map<UUID, String> nomes = plantas.values().stream()
                .collect(Collectors.toMap(PlantaResumo::id, PlantaResumo::nomePopular));
        return new PedidoCriado(pedido, nomes, agora.plus(expiracao));
    }

    /** Resultado do checkout (8. o QR Code é gerado na camada web, sob demanda). */
    public record PedidoCriado(Pedido pedido, Map<UUID, String> nomesPlantas, Instant pixExpiraEm) {
    }
}
