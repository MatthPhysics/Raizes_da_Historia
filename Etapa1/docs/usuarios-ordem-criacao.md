# Módulo `usuarios`: ordem lógica de criação dos arquivos

Este documento descreve a ordem em que os arquivos do módulo `usuarios` devem ser criados. A ordem segue as dependências: cada arquivo só usa o que foi criado antes dele.

Pasta do módulo: [`src/main/java/br/com/raizesdahistoria/usuarios/`](../src/main/java/br/com/raizesdahistoria/usuarios/)

> Este é o primeiro módulo de negócio a ser criado: o `catalogo` precisa dele para proteger as rotas `/admin/**`, e o `vendas` usa o `ClienteService` no checkout.

---

## 0. Pré-requisitos fora da pasta

| Arquivo | Por que vem antes |
| :--- | :--- |
| [`V2__modulo_usuarios.sql`](../src/main/resources/db/migration/V2__modulo_usuarios.sql) | Cria as tabelas `administradores` (e-mail único) e `clientes` (e-mail **não** único, CPF opcional com `CHECK`). Como `ddl-auto: validate`, as entidades precisam bater com essas tabelas, então o SQL vem antes do Java. |
| [`RaizesDaHistoriaApplication.java`](../src/main/java/br/com/raizesdahistoria/RaizesDaHistoriaApplication.java) | Precisa ter `@ConfigurationPropertiesScan`, senão os records `JwtProperties` e `AdminInicialProperties` não são carregados. |
| [`application.yml`](../src/main/resources/application.yml) / [`application-dev.yml`](../src/main/resources/application-dev.yml) | Chaves `raizes.jwt.*` (`JWT_SECRET`, `JWT_EXPIRACAO_HORAS`) e `raizes.admin-inicial.*` (`ADMIN_INICIAL_NOME/EMAIL/SENHA`). No `dev` há valores padrão; fora dele, só variáveis de ambiente. |
| `shared/exception` | `ConflitoException` (409), `RecursoNaoEncontradoException` (404), `NaoAutorizadoException` (401) e `MuitasTentativasException` (429), tratadas pelo `GlobalExceptionHandler`. |
| `shared/util` | `Mascaramento`, para logar e-mails sem expor o dado completo (LGPD). |
| `shared/validation` | Anotação `@Cpf` e `CpfValidator` (dígito verificador), usados em `NovoCliente`. |
| `shared/config` | `ClockConfig` (bean `Clock`), `CorsConfig` (usado por `.cors(...)` na `SecurityConfig`) e `OpenApiConfig` (constante `BEARER` do Swagger). |

---

## 1. Declaração do módulo

1. **[`package-info.java`](../src/main/java/br/com/raizesdahistoria/usuarios/package-info.java)**: marca `usuarios` como módulo do Spring Modulith ("Usuários & Autenticação"). A partir daí, o `ModularityTests` verifica as fronteiras dele.

## 2. Domínio (`internal/dominio/`)

2. **[`Administrador.java`](../src/main/java/br/com/raizesdahistoria/usuarios/internal/dominio/Administrador.java)** e **[`Cliente.java`](../src/main/java/br/com/raizesdahistoria/usuarios/internal/dominio/Cliente.java)**: entidades independentes, sem relacionamento entre si. `Administrador` guarda só o `senhaHash` (nunca a senha). `Cliente` não tem senha: é o comprador do checkout rápido.
3. **[`AdministradorRepository.java`](../src/main/java/br/com/raizesdahistoria/usuarios/internal/dominio/AdministradorRepository.java)**: `JpaRepository<Administrador, UUID>` com `findByEmail` (login) e `existsByEmail` (cadastro sem duplicidade).
4. **[`ClienteRepository.java`](../src/main/java/br/com/raizesdahistoria/usuarios/internal/dominio/ClienteRepository.java)**: `JpaRepository<Cliente, UUID>`, sem métodos extras.

## 3. API pública do módulo (usada por `vendas` e pelos controllers)

5. **[`AdministradorDto.java`](../src/main/java/br/com/raizesdahistoria/usuarios/AdministradorDto.java)** e **[`ClienteDto.java`](../src/main/java/br/com/raizesdahistoria/usuarios/ClienteDto.java)**: records que saem do módulo. `AdministradorDto` não tem `senhaHash`. `ClienteDto` tem dados pessoais e só pode aparecer em rotas administrativas.
6. **[`NovoCliente.java`](../src/main/java/br/com/raizesdahistoria/usuarios/NovoCliente.java)**: record de entrada do checkout com Bean Validation (e-mail, telefone só dígitos com DDD, `@Cpf` opcional). Fica no pacote raiz porque o `vendas` o usa direto no `CheckoutRequest`.
7. **[`AdministradorService.java`](../src/main/java/br/com/raizesdahistoria/usuarios/AdministradorService.java)**:
   - `cadastrar(nome, email, senha)`: normaliza o e-mail, devolve 409 se já existir e grava a senha com BCrypt;
   - `buscar(id)`: usado em `/admin/administradores/me`;
   - `existeAlgum()`: usado pelo seed do primeiro admin;
   - `normalizarEmail(...)`: estático, reaproveitado pelo login.
8. **[`ClienteService.java`](../src/main/java/br/com/raizesdahistoria/usuarios/ClienteService.java)**:
   - `criar(NovoCliente)`: sempre cria um novo registro (sem deduplicar por e-mail nesta etapa). Roda dentro da transação do checkout;
   - `buscar(id)`: usado no detalhe administrativo do pedido.

> O `AdministradorService` depende de um bean `PasswordEncoder`, que é declarado na `SecurityConfig` (camada 4). O código compila sem ela, mas a aplicação só sobe depois que a camada 4 existir.

## 4. Segurança e JWT (`internal/security/`)

9. **[`JwtProperties.java`](../src/main/java/br/com/raizesdahistoria/usuarios/internal/security/JwtProperties.java)**: record `@ConfigurationProperties("raizes.jwt")` com `secret` e `expiracaoHoras`.
10. **[`JwtConfig.java`](../src/main/java/br/com/raizesdahistoria/usuarios/internal/security/JwtConfig.java)**: cria `JwtEncoder` e `JwtDecoder` com HMAC-SHA256. Se `JWT_SECRET` tiver menos de 32 bytes, lança exceção e a aplicação não sobe.
11. **[`TokenService.java`](../src/main/java/br/com/raizesdahistoria/usuarios/internal/security/TokenService.java)**: `emitirParaAdmin(id, email)` monta o JWT (`sub` = id do admin, claim `roles = ["ADMIN"]`) e devolve `TokenEmitido(token, expiraEm)`. Define a constante `CLAIM_ROLES`, usada pela `SecurityConfig`.
12. **[`ProblemDetailSecurityHandler.java`](../src/main/java/br/com/raizesdahistoria/usuarios/internal/security/ProblemDetailSecurityHandler.java)**: responde 401 e 403 no formato `application/problem+json` (RFC 7807), igual ao resto da API.
13. **[`SecurityConfig.java`](../src/main/java/br/com/raizesdahistoria/usuarios/internal/security/SecurityConfig.java)**:
    - `SecurityFilterChain` stateless, sem CSRF, com `/api/v1/admin/**` exigindo `ROLE_ADMIN` e as rotas públicas da §6.1 liberadas;
    - converte a claim `roles` em `ROLE_ADMIN`;
    - declara o `PasswordEncoder` (`BCryptPasswordEncoder(12)`).

    É o ponto central de segurança de **todos** os módulos: ao criar uma rota pública nova em `catalogo` ou `vendas`, ela precisa ser liberada aqui.
14. **[`LoginRateLimiter.java`](../src/main/java/br/com/raizesdahistoria/usuarios/internal/security/LoginRateLimiter.java)**: limita as tentativas de login por IP (padrão 5 por minuto, em memória). Acima do limite, lança `MuitasTentativasException` (429). Usa `Clock` para ser testável.
15. **[`LoginService.java`](../src/main/java/br/com/raizesdahistoria/usuarios/internal/security/LoginService.java)**: `autenticar(email, senha)` confere a senha com BCrypt e chama o `TokenService`. Para e-mail inexistente, compara com um hash fictício, para que a resposta demore o mesmo tempo (evita descobrir quais e-mails existem). A mensagem de erro é sempre a mesma: "E-mail ou senha inválidos."
16. **[`AdminInicialSeeder.java`](../src/main/java/br/com/raizesdahistoria/usuarios/internal/security/AdminInicialSeeder.java)**: `ApplicationRunner` que cria o primeiro administrador quando a tabela está vazia, a partir de `ADMIN_INICIAL_*`. A senha precisa ter pelo menos 8 caracteres. Ele não fica em migration porque a senha não pode ser versionada.

## 5. Camada web

17. **[`AuthController.java`](../src/main/java/br/com/raizesdahistoria/usuarios/internal/security/AuthController.java)**: `POST /api/v1/auth/login` → `{accessToken, tipo: "Bearer", expiraEm}`. Fica em `internal/security` (e não em `internal/web`) porque usa `LoginService`, `LoginRateLimiter` e `TokenEmitido`, que são package-private.
18. **[`AdministradorController.java`](../src/main/java/br/com/raizesdahistoria/usuarios/internal/web/AdministradorController.java)**: `POST /api/v1/admin/administradores` (cadastro feito por outro admin) e `GET /api/v1/admin/administradores/me` (lê o `sub` do JWT). O request `NovoAdministradorRequest` valida senha entre 8 e 72 caracteres (72 é o limite do BCrypt).

## 6. Testes

19. **[`LoginRateLimiterTest.java`](../src/test/java/br/com/raizesdahistoria/usuarios/internal/security/LoginRateLimiterTest.java)**: teste unitário do limite de 5 tentativas por minuto, com `Clock` controlado.
20. **[`TokensDeTeste.java`](../src/test/java/br/com/raizesdahistoria/usuarios/internal/security/TokensDeTeste.java)**: utilitário que emite um JWT de admin sem passar pelo login (que tem limite por IP). É usado pelos ITs dos outros módulos.
21. **[`AutenticacaoIT.java`](../src/test/java/br/com/raizesdahistoria/usuarios/AutenticacaoIT.java)**: teste de integração com Testcontainers que cobre o seed do admin inicial, login com JWT, login inválido (401 com mensagem genérica), limite de tentativas, `/admin/**` sem token (401), rotas públicas sem token, cadastro de admin com e-mail duplicado (409) e mensagens de validação em português.

---

## Resumo do fluxo

```
SQL (V2) + @ConfigurationPropertiesScan
  → package-info
  → Administrador / Cliente → repositórios
  → AdministradorDto, ClienteDto, NovoCliente
  → AdministradorService / ClienteService            (API pública)
  → JwtProperties → JwtConfig → TokenService
  → ProblemDetailSecurityHandler → SecurityConfig    (PasswordEncoder)
  → LoginRateLimiter → LoginService → AdminInicialSeeder
  → AuthController / AdministradorController
  → LoginRateLimiterTest, TokensDeTeste, AutenticacaoIT
```

## Regras de modularidade aplicadas

- Outros módulos usam apenas `AdministradorService`, `ClienteService`, `ClienteDto`, `AdministradorDto` e `NovoCliente`. Nunca importam nada de `usuarios.internal`.
- `Pedido` guarda `clienteId` (UUID), não um `@ManyToOne Cliente`. A FK `fk_pedidos_cliente` existe só no banco.
- Nenhuma resposta expõe `senha_hash`. `ClienteDto` só aparece em rotas `/admin/**`.
- Logs mascaram o e-mail com `Mascaramento.email(...)`.
- Antes de considerar o módulo pronto, rode `./mvnw verify` para executar o `AutenticacaoIT` e o `ModularityTests`.
