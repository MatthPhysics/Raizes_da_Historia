# Raízes da História — Back-end (Etapa 1)

API REST do e-commerce de plantas: catálogo, carrinho, checkout com Pix e área administrativa.
A especificação completa (regras, rotas, modelo de dados).

## Requisitos

- **JDK 21** (o JDK completo, com `javac` — no Debian/Ubuntu: `sudo apt install openjdk-21-jdk-headless`)
- **Docker** (PostgreSQL local via `docker compose` e testes de integração via Testcontainers)
- Maven não precisa estar instalado: use o wrapper `./mvnw`

## Rodando localmente

```bash
docker compose up -d                                   # PostgreSQL 16 em localhost:5432
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev  # API em http://localhost:8080
```

No profile `dev`, o Pix é simulado e o admin inicial é `admin@raizes.dev` / `admin12345`
(sobrescreva com `ADMIN_INICIAL_EMAIL` / `ADMIN_INICIAL_SENHA`).

- Swagger: http://localhost:8080/swagger-ui.html
- Saúde: http://localhost:8080/actuator/health

## Testes

```bash
./mvnw test     # unitários + verificação de módulos (Spring Modulith)
./mvnw verify   # + testes de integração (*IT) com PostgreSQL real via Testcontainers
```

Sem Docker, os testes de integração são **pulados**. Para rodá-los contra um PostgreSQL já existente
(banco descartável — as tabelas são truncadas):

```bash
./mvnw verify -Dit.db.url=jdbc:postgresql://localhost:5432/raizes_it -Dit.db.user=raizes -Dit.db.password=raizes_dev
```

## Roteiro de demonstração (Swagger, profile dev)

1. `POST /api/v1/auth/login` com o admin inicial → copie `accessToken` → botão **Authorize**.
2. `POST /api/v1/admin/plantas` (ex.: Jibóia, R$ 39,90, 10 unidades) e `POST /api/v1/admin/plantas/{id}/imagem`.
3. `GET /api/v1/plantas?nome=jiboia` — encontra "Jibóia" (busca sem acento).
4. `POST /api/v1/carrinho/validar` e depois `POST /api/v1/pedidos` → recebe o Pix copia-e-cola e o QR Code
   (cole `data:image/png;base64,<qrCodeBase64>` na barra do navegador para ver). O estoque cai na hora.
5. `POST /api/v1/dev/pix/{txid}/simular-pagamento` → `GET /api/v1/pedidos/{id}` mostra `PAGO`.
6. Um pedido não pago vira `EXPIRADO` após 30 min e o estoque volta
   (para demonstrar mais rápido, rode com `--pix.expiracao-minutos=1`).
7. `GET /api/v1/admin/pedidos` e `PATCH .../itens/{itemId}/status-entrega` para acompanhar a entrega.

## Variáveis de ambiente

| Variável | Uso |
| :--- | :--- |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Conexão PostgreSQL |
| `JWT_SECRET` | Segredo HMAC do JWT (**obrigatório**, ≥ 32 bytes) |
| `JWT_EXPIRACAO_HORAS` | Validade do token (padrão 8) |
| `FRONTEND_URL` | Origem permitida no CORS |
| `ADMIN_INICIAL_NOME`, `ADMIN_INICIAL_EMAIL`, `ADMIN_INICIAL_SENHA` | Primeiro administrador (criado se não houver nenhum) |
| `PIX_PROVEDOR` | `mock` (padrão) ou `psp` |
| `PIX_CHAVE`, `PIX_RECEBEDOR_NOME`, `PIX_RECEBEDOR_CIDADE` | Dados do recebedor |
| `PIX_WEBHOOK_SECRET` | Token exigido no webhook (`X-Webhook-Token`) |
| `PIX_PSP_BASE_URL`, `PIX_PSP_CLIENT_ID`, `PIX_PSP_CLIENT_SECRET`, `PIX_PSP_SSL_BUNDLE` | PSP real |
| `IMAGENS_DIRETORIO` | Pasta das imagens enviadas (padrão `uploads/plantas`) |
