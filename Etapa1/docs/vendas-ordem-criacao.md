# Módulo `vendas`: ordem lógica de criação dos arquivos

Este documento descreve a ordem em que os arquivos do módulo `vendas` devem ser criados. A ordem segue as dependências: cada arquivo só usa o que foi criado antes dele.

Pasta do módulo: [`src/main/java/br/com/raizesdahistoria/vendas/`](../src/main/java/br/com/raizesdahistoria/vendas/)

> Este é o último módulo da Etapa 1: ele depende da API pública de `catalogo` e de `usuarios`. Crie-o depois que os dois estiverem prontos.

---

## 0. Pré-requisitos fora da pasta

| Arquivo | Por que vem antes |
| :--- | :--- |
| [`V4__modulo_vendas.sql`](../src/main/resources/db/migration/V4__modulo_vendas.sql) | Cria `pedidos` e `itens_pedido`, com os `CHECK` de status, a FK para `clientes` e `plantas` e o `UNIQUE (pix_txid)`. Como `ddl-auto: validate`, as entidades precisam bater com essas tabelas, então o SQL vem antes do Java. Depende da V2 e da V3 por causa das FKs. |
| Módulo `catalogo` | `CatalogoService` (`buscarResumos`, `bloquearParaReserva`, `reservarEstoque`, `devolverEstoque`) e `PlantaResumo`. |
| Módulo `usuarios` | `ClienteService`, `ClienteDto` e `NovoCliente`. A `SecurityConfig` também precisa liberar `/carrinho/validar`, `/pedidos`, `/pedidos/*`, `/pagamentos/pix/webhook` e `/dev/**`. |
| [`application.yml`](../src/main/resources/application.yml) / [`application-dev.yml`](../src/main/resources/application-dev.yml) | Chaves `pix.*` (`PIX_PROVEDOR`, `PIX_CHAVE`, `PIX_WEBHOOK_SECRET`, `PIX_RECEBEDOR_*`, `expiracao-minutos`) e `pix.psp.*` (credenciais do PSP real). |
| [`pom.xml`](../pom.xml) | Dependências do ZXing (`com.google.zxing:core` e `javase`) para o QR Code. |
| `shared/exception` | `ApiException` (base da 502), `RegraNegocioException` (422), `RecursoNaoEncontradoException` (404), `ConflitoException` (409) e `NaoAutorizadoException` (401). |
| `shared/config` | `ClockConfig` (bean `Clock`, essencial para testar a expiração) e `OpenApiConfig` (constante `BEARER`). |

---

## 1. Declaração do módulo

1. **[`package-info.java`](../src/main/java/br/com/raizesdahistoria/vendas/package-info.java)**: marca `vendas` como módulo do Spring Modulith. A partir daí, o `ModularityTests` verifica as fronteiras dele.

## 2. API pública do módulo (evento)

2. **[`PedidoPagoEvent.java`](../src/main/java/br/com/raizesdahistoria/vendas/PedidoPagoEvent.java)**: record `(pedidoId, clienteId, valorTotal, pagoEm)`, sem dependências. É a única classe pública do módulo: na Etapa 5 o financeiro vai escutá-lo. Nesta etapa não existe nenhum listener.

## 3. Domínio de pedidos (`internal/pedido/`)

3. **[`StatusPagamento.java`](../src/main/java/br/com/raizesdahistoria/vendas/internal/pedido/StatusPagamento.java)** e **[`StatusEntrega.java`](../src/main/java/br/com/raizesdahistoria/vendas/internal/pedido/StatusEntrega.java)**: enums sem dependências. Os valores são os mesmos dos `CHECK` da V4 (`PENDENTE`/`PAGO`/`CANCELADO`/`EXPIRADO` e `EM_SEPARACAO`/`PRONTA`/`ENTREGUE`/`CANCELADA`).
4. **[`TransicaoStatusInvalidaException.java`](../src/main/java/br/com/raizesdahistoria/vendas/internal/pedido/TransicaoStatusInvalidaException.java)**: estende `ConflitoException`, que o `GlobalExceptionHandler` transforma em 409.
5. **[`Dinheiro.java`](../src/main/java/br/com/raizesdahistoria/vendas/internal/pedido/Dinheiro.java)**: utilitário monetário (`arredondar`, `multiplicar`, `iguais`) com `BigDecimal`, escala 2 e `HALF_EVEN`. Todo cálculo de valor do módulo passa por ele.
6. **[`Pedido.java`](../src/main/java/br/com/raizesdahistoria/vendas/internal/pedido/Pedido.java)** e **[`ItemPedido.java`](../src/main/java/br/com/raizesdahistoria/vendas/internal/pedido/ItemPedido.java)**: criados juntos, porque um referencia o outro (`@OneToMany` / `@ManyToOne`, permitido dentro do mesmo módulo).
   - `Pedido` guarda `clienteId` (UUID) e concentra as transições: `marcarPago`, `expirar` e `cancelar`. Todas só funcionam a partir de `PENDENTE` e devolvem `false` em vez de lançar exceção, o que torna webhook e job idempotentes. Também tem `quantidadesPorPlanta()`, usado para devolver o estoque.
   - `ItemPedido` guarda `plantaId` (UUID) e o `precoUnitario` histórico. `atualizarStatusEntrega` só aceita pedidos `PAGO` e nunca define nem altera `CANCELADA` manualmente.
7. **[`PedidoRepository.java`](../src/main/java/br/com/raizesdahistoria/vendas/internal/pedido/PedidoRepository.java)**:
   - `findWithItensById` com `@EntityGraph("itens")`, para evitar N+1;
   - `bloquearPorId` e `bloquearPorTxid` com `@Lock(PESSIMISTIC_WRITE)`, que serializam webhook, job e cancelamento do mesmo pedido;
   - `idsPorStatusCriadosAntesDe`, usado pelo job de expiração;
   - `JpaSpecificationExecutor`, para os filtros da listagem administrativa.
8. **[`ItensSolicitados.java`](../src/main/java/br/com/raizesdahistoria/vendas/internal/pedido/ItensSolicitados.java)**: `consolidar(...)` soma itens com `plantaId` repetido e devolve um `TreeMap` ordenado por id. A ordem ajuda a evitar deadlock no lock das plantas.

## 4. Integração Pix (`internal/pix/`)

Esta camada não depende do domínio de pedidos (camada 3). As duas podem ser criadas em qualquer ordem.

9. **[`PixProperties.java`](../src/main/java/br/com/raizesdahistoria/vendas/internal/pix/PixProperties.java)**: record `@ConfigurationProperties("pix")` com valores padrão (expiração de 30 min, nome e cidade do recebedor).
10. **[`PspIndisponivelException.java`](../src/main/java/br/com/raizesdahistoria/vendas/internal/pix/PspIndisponivelException.java)**: estende `ApiException` com status 502 e uma mensagem amigável. O detalhe técnico fica só na causa (nunca vai para a resposta).
11. **[`PixGateway.java`](../src/main/java/br/com/raizesdahistoria/vendas/internal/pix/PixGateway.java)**: interface com `criarCobranca`, `consultarCobranca` e `removerCobranca`, mais os records `Devedor`, `CobrancaCriada`, `CobrancaConsultada` e o enum `StatusCobranca` (mesmos nomes da API Pix do Bacen).
12. **[`GeradorTxid.java`](../src/main/java/br/com/raizesdahistoria/vendas/internal/pix/GeradorTxid.java)**: gera `txid` de 32 caracteres alfanuméricos (prefixo `RAIZES` + `SecureRandom`), dentro da faixa 26–35 exigida pelo Bacen.
13. **[`BrCode.java`](../src/main/java/br/com/raizesdahistoria/vendas/internal/pix/BrCode.java)**: monta o BR Code dinâmico (padrão EMV) com CRC16-CCITT. Só o mock usa; em produção, o copia-e-cola vem do PSP.
14. **[`MockPixGateway.java`](../src/main/java/br/com/raizesdahistoria/vendas/internal/pix/MockPixGateway.java)**: implementação ativa com `pix.provedor=mock` (padrão). Guarda as cobranças em memória e tem `simularPagamento` e `simularPagamentoComValor` para dev e testes.
15. **[`PspPixGateway.java`](../src/main/java/br/com/raizesdahistoria/vendas/internal/pix/PspPixGateway.java)**: esqueleto da integração real (`PUT/GET/PATCH /v2/cob/{txid}`, OAuth2 client-credentials, mTLS por SSL bundle, timeout de 5 s), ativo com `pix.provedor=psp`. Precisa ser revisado quando o PSP for escolhido.
16. **[`QrCodeService.java`](../src/main/java/br/com/raizesdahistoria/vendas/internal/pix/QrCodeService.java)**: gera o PNG 300×300 em Base64 com ZXing, sob demanda. A imagem nunca é persistida.

## 5. Serviços de pedido (`internal/pedido/`)

17. **[`CarrinhoService.java`](../src/main/java/br/com/raizesdahistoria/vendas/internal/pedido/CarrinhoService.java)**: `validar(...)` revalida o carrinho do front com `CatalogoService.buscarResumos` e devolve itens, total, avisos (`PLANTA_INATIVA`, `ESTOQUE_INSUFICIENTE`, `PRECO_ALTERADO`) e `podeFinalizar`. Não grava nada.
18. **[`CheckoutService.java`](../src/main/java/br/com/raizesdahistoria/vendas/internal/pedido/CheckoutService.java)**: `finalizar(...)` executa os passos da §7.1 numa única transação:
    1. valida quantidade (1 a 99);
    2. trava as plantas (`bloquearParaReserva`);
    3. rejeita com 422 plantas inativas ou sem estoque, listando quais;
    4. calcula o total no servidor;
    5. reserva o estoque;
    6. cria o cliente;
    7. grava pedido e itens e cria a cobrança no `PixGateway`.

    Se o PSP falhar, lança `PspIndisponivelException` (502) e o rollback devolve o estoque. É o arquivo com mais dependências do módulo.
19. **[`PagamentoService.java`](../src/main/java/br/com/raizesdahistoria/vendas/internal/pedido/PagamentoService.java)**: ciclo de vida do pagamento (§7.2).
    - `confirmarPagamento(txid)`: trava o pedido, consulta o PSP e só marca `PAGO` se o `txid` e o valor conferirem. Nesse caso, publica o `PedidoPagoEvent`;
    - `expirarSeVencido(id)`: antes de expirar, consulta o PSP (webhook perdido vira `PAGO`). Se o PSP estiver fora, adia a expiração. Ao expirar, devolve o estoque;
    - `cancelar(id)`: só pedidos `PENDENTE`. Devolve o estoque e remove a cobrança no PSP.
20. **[`ExpiracaoPedidosJob.java`](../src/main/java/br/com/raizesdahistoria/vendas/internal/pedido/ExpiracaoPedidosJob.java)**: `@Scheduled` a cada 1 min (`pix.expiracao-job-intervalo-ms`). Busca até 100 pedidos vencidos e chama `expirarSeVencido` para cada um, cada pedido em sua própria transação. Também liga o `@EnableScheduling`.
21. **[`PedidoAdminService.java`](../src/main/java/br/com/raizesdahistoria/vendas/internal/pedido/PedidoAdminService.java)**: `listar` (filtros `statusPagamento` e período, com as datas interpretadas em `America/Sao_Paulo`), `buscar` e `atualizarStatusEntrega`.

> As camadas 3 e 4 não dependem uma da outra. A camada 5 depende das duas.

## 6. Camada web (`internal/web/`)

22. **[`VendasDtos.java`](../src/main/java/br/com/raizesdahistoria/vendas/internal/web/VendasDtos.java)**: todos os requests e responses (records com Bean Validation): carrinho, checkout (sem nenhum campo de preço), Pix, visão pública do pedido (sem dados pessoais), webhook no formato do Bacen e visões administrativas. Os controllers nunca retornam entidades.
23. **[`NomesPlantas.java`](../src/main/java/br/com/raizesdahistoria/vendas/internal/web/NomesPlantas.java)**: resolve os nomes das plantas dos itens com uma única chamada a `CatalogoService.buscarResumos`. É reaproveitado pelos controllers público e administrativo.
24. **[`PedidoPublicoController.java`](../src/main/java/br/com/raizesdahistoria/vendas/internal/web/PedidoPublicoController.java)**: `POST /api/v1/carrinho/validar`, `POST /api/v1/pedidos` (201, com copia-e-cola e QR Code) e `GET /api/v1/pedidos/{id}` (polling, `Cache-Control: no-store`; o QR Code só é enviado enquanto o pedido está pendente).
25. **[`WebhookPixController.java`](../src/main/java/br/com/raizesdahistoria/vendas/internal/web/WebhookPixController.java)**: `POST /api/v1/pagamentos/pix/webhook`. Compara `X-Webhook-Token` (ou `?token=`) com `PIX_WEBHOOK_SECRET` em tempo constante e, para cada `txid` recebido, chama `confirmarPagamento`. O corpo recebido serve só de gatilho: quem decide é o PSP.
26. **[`PedidoAdminController.java`](../src/main/java/br/com/raizesdahistoria/vendas/internal/web/PedidoAdminController.java)**: `GET /api/v1/admin/pedidos[/{id}]`, `PATCH .../itens/{itemId}/status-entrega` e `PATCH .../{id}/cancelar`. O detalhe inclui o `ClienteDto`, buscado pelo `ClienteService`.
27. **[`DevPixController.java`](../src/main/java/br/com/raizesdahistoria/vendas/internal/web/DevPixController.java)**: só existe no profile `dev`. `POST /api/v1/dev/pix/{txid}/simular-pagamento` marca a cobrança como paga no mock e dispara o mesmo fluxo do webhook. É o que permite demonstrar o pagamento sem um PSP real.

## 7. Testes

28. **[`PedidoTest.java`](../src/test/java/br/com/raizesdahistoria/vendas/internal/pedido/PedidoTest.java)**: unitário. Cobre transições só a partir de `PENDENTE` (idempotência), expiração cancelando os itens, status de entrega só em pedido pago, `quantidadesPorPlanta` e arredondamento `HALF_EVEN`.
29. **[`BrCodeTest.java`](../src/test/java/br/com/raizesdahistoria/vendas/internal/pix/BrCodeTest.java)** e **[`GeradorTxidTest.java`](../src/test/java/br/com/raizesdahistoria/vendas/internal/pix/GeradorTxidTest.java)**: unitários do CRC16, do BR Code e do formato e unicidade do `txid`.
30. **[`CheckoutIT.java`](../src/test/java/br/com/raizesdahistoria/vendas/CheckoutIT.java)**: total calculado pelo servidor, reserva de estoque, itens repetidos somados, 422 por estoque insuficiente ou planta inativa, rollback com 502 quando o PSP falha, consulta pública sem dados pessoais e validação do carrinho.
31. **[`ConcorrenciaCheckoutIT.java`](../src/test/java/br/com/raizesdahistoria/vendas/ConcorrenciaCheckoutIT.java)**: dois checkouts simultâneos para a última unidade (só um sucede) e checkouts cruzados sem deadlock.
32. **[`PagamentoIT.java`](../src/test/java/br/com/raizesdahistoria/vendas/PagamentoIT.java)**: webhook confirma e publica o `PedidoPagoEvent` (`@RecordApplicationEvents`), webhook duplicado é idempotente, valor divergente não confirma, token inválido é rejeitado, expiração após 30 min com `Clock` controlado, job recupera webhook perdido, cancelamento pelo admin e atualização do status de entrega.

---

## Resumo do fluxo

```
SQL (V4) + módulos catalogo e usuarios prontos
  → package-info
  → PedidoPagoEvent                                   (API pública)
  → enums → TransicaoStatusInvalidaException → Dinheiro
  → Pedido + ItemPedido → PedidoRepository → ItensSolicitados
  → PixProperties → PspIndisponivelException → PixGateway
  → GeradorTxid → BrCode → MockPixGateway / PspPixGateway → QrCodeService
  → CarrinhoService → CheckoutService → PagamentoService
  → ExpiracaoPedidosJob → PedidoAdminService
  → VendasDtos → NomesPlantas → controllers
  → PedidoTest, BrCodeTest, GeradorTxidTest
  → CheckoutIT, ConcorrenciaCheckoutIT, PagamentoIT
```

## Regras de modularidade aplicadas

- `vendas` usa apenas `CatalogoService`, `PlantaResumo`, `ClienteService`, `ClienteDto` e `NovoCliente`. Nunca importa nada de `catalogo.internal` ou `usuarios.internal`.
- `Pedido` guarda `clienteId` e `ItemPedido` guarda `plantaId` (UUIDs), sem `@ManyToOne` para outros módulos. As FKs existem só no banco.
- O único elemento público de `vendas` é o `PedidoPagoEvent`. Os outros módulos não chamam nada de `vendas`.
- Preço e total são sempre calculados no servidor, e a confirmação de pagamento é sempre verificada no PSP.
- Antes de considerar o módulo pronto, rode `./mvnw verify` para executar os ITs de vendas e o `ModularityTests`.
