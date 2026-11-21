# Módulo `catalogo`: ordem lógica de criação dos arquivos

Este documento descreve a ordem em que os arquivos do módulo `catalogo` devem ser criados. A ordem segue as dependências: cada arquivo só usa o que foi criado antes dele.

Pasta do módulo: [`src/main/java/br/com/raizesdahistoria/catalogo/`](../src/main/java/br/com/raizesdahistoria/catalogo/)

---

## 0. Pré-requisitos fora da pasta

| Arquivo | Por que vem antes |
| :--- | :--- |
| [`V1__extensoes.sql`](../src/main/resources/db/migration/V1__extensoes.sql) | Cria `unaccent`, `pg_trgm` e a função `f_unaccent`, usadas pela busca por nome. |
| [`V3__modulo_catalogo.sql`](../src/main/resources/db/migration/V3__modulo_catalogo.sql) | Cria as tabelas `tipos_planta` e `plantas`. Como `ddl-auto: validate`, as entidades precisam bater com essas tabelas, então o SQL vem antes do Java. |
| `shared/exception` | Exceções base: `RecursoNaoEncontradoException`, `ConflitoException`, `RegraNegocioException`. |
| `shared/storage` | `ArmazenamentoImagemService` e `ImagemInvalidaException`, usados no upload de imagem. |

---

## 1. Declaração do módulo

1. **[`package-info.java`](../src/main/java/br/com/raizesdahistoria/catalogo/package-info.java)**: marca `catalogo` como módulo do Spring Modulith. A partir daí, o `ModularityTests` verifica as fronteiras dele.

## 2. Domínio (`internal/dominio/`)

2. **[`FrequenciaRega.java`](../src/main/java/br/com/raizesdahistoria/catalogo/internal/dominio/FrequenciaRega.java)** e **[`ExposicaoSol.java`](../src/main/java/br/com/raizesdahistoria/catalogo/internal/dominio/ExposicaoSol.java)**: enums sem dependências. Os valores são os mesmos dos `CHECK` da V3 (`DIARIA`/`SEMANAL`/`QUINZENAL` e `PLENO`/`MEIA_SOMBRA`/`SOMBRA`).
3. **[`TipoPlanta.java`](../src/main/java/br/com/raizesdahistoria/catalogo/internal/dominio/TipoPlanta.java)**: a entidade mais simples, que não referencia outra entidade.
4. **[`Planta.java`](../src/main/java/br/com/raizesdahistoria/catalogo/internal/dominio/Planta.java)**: depende de `TipoPlanta` (`@ManyToOne`, permitido dentro do mesmo módulo) e dos dois enums. As regras de estoque ficam nela: `reservar`, `devolver`, `ajustarEstoque` e `isDisponivel`.
5. **[`TipoPlantaRepository.java`](../src/main/java/br/com/raizesdahistoria/catalogo/internal/dominio/TipoPlantaRepository.java)**: `JpaRepository<TipoPlanta, Integer>`.
6. **[`PlantaRepository.java`](../src/main/java/br/com/raizesdahistoria/catalogo/internal/dominio/PlantaRepository.java)**:
   - `@EntityGraph(attributePaths = "tipo")` para evitar o problema de N+1;
   - `bloquearPorIds`, com `@Lock(PESSIMISTIC_WRITE)` e `order by p.id`, para evitar deadlock no checkout.
7. **[`PlantaSpecifications.java`](../src/main/java/br/com/raizesdahistoria/catalogo/internal/dominio/PlantaSpecifications.java)**: filtros `ativa`, `doTipo`, `comRega`, `comSol` e `nomeContem`. `nomeContem` usa `f_unaccent(lower(...))`, a mesma expressão dos índices trigram da V3. Sem isso, o índice não é usado.

## 3. Exceção do módulo

8. **[`PlantaNaoEncontradaException.java`](../src/main/java/br/com/raizesdahistoria/catalogo/internal/PlantaNaoEncontradaException.java)**: estende `RecursoNaoEncontradoException`, que o `GlobalExceptionHandler` transforma em 404.

## 4. Serviços internos (usados pelos controllers)

9. **[`CatalogoConsultaService.java`](../src/main/java/br/com/raizesdahistoria/catalogo/internal/CatalogoConsultaService.java)**: leituras públicas.
   - `listarTipos()`
   - `buscarCatalogo(...)`, que lista só plantas ativas, com paginação
   - `buscarAtiva(id)`, que lança 404 se a planta estiver inativa
10. **[`CatalogoAdminService.java`](../src/main/java/br/com/raizesdahistoria/catalogo/internal/CatalogoAdminService.java)**: operações administrativas.
    - CRUD de tipos (`excluirTipo` devolve 409 se houver plantas vinculadas)
    - CRUD de plantas, `desativar` (soft delete) e `definirAtivo`
    - `ajustarEstoque` e `atualizarImagem`

    É o maior arquivo do módulo e o último da camada de regras, porque usa todo o domínio, a exceção e o armazenamento de imagens.

## 5. API pública do módulo (usada pelo módulo `vendas`)

11. **[`PlantaResumo.java`](../src/main/java/br/com/raizesdahistoria/catalogo/PlantaResumo.java)**: record que sai do módulo, para que `vendas` nunca veja a entidade `Planta`.
12. **[`CatalogoService.java`](../src/main/java/br/com/raizesdahistoria/catalogo/CatalogoService.java)**:
    - `buscarResumos(ids)`: leitura usada na validação do carrinho;
    - `bloquearParaReserva(ids)` e `reservarEstoque(...)`: usam `Propagation.MANDATORY`, ou seja, só rodam dentro de uma transação já aberta pelo checkout;
    - `devolverEstoque(...)`: chamado na expiração e no cancelamento de pedidos.

> As camadas 4 e 5 não dependem uma da outra. A camada 5 pode ser criada antes se você quiser começar o checkout em `vendas` mais cedo.

## 6. Camada web (`internal/web/`)

13. **[`CatalogoDtos.java`](../src/main/java/br/com/raizesdahistoria/catalogo/internal/web/CatalogoDtos.java)**: todos os requests e responses (records com Bean Validation) e os métodos que convertem entidade em DTO. Os controllers nunca retornam entidades.
14. **[`CatalogoPublicoController.java`](../src/main/java/br/com/raizesdahistoria/catalogo/internal/web/CatalogoPublicoController.java)**: `GET /api/v1/tipos-planta`, `GET /api/v1/plantas` e `GET /api/v1/plantas/{id}`, com `Cache-Control: public, max-age=60`.
15. **[`CatalogoAdminController.java`](../src/main/java/br/com/raizesdahistoria/catalogo/internal/web/CatalogoAdminController.java)**: `/api/v1/admin/tipos-planta/**` e `/api/v1/admin/plantas/**`, incluindo `PATCH .../ativo`, `PATCH .../estoque` e o upload multipart em `POST .../imagem`.

## 7. Testes

16. **[`CatalogoIT.java`](../src/test/java/br/com/raizesdahistoria/catalogo/CatalogoIT.java)**: teste de integração com Testcontainers que cobre busca sem acento ("jiboia" encontra "Jibóia"), filtros combinados, plantas inativas e esgotadas, e o CRUD protegido.

---

## Resumo do fluxo

```
SQL (V1, V3)
  → package-info
  → enums → TipoPlanta → Planta
  → repositórios → PlantaSpecifications
  → PlantaNaoEncontradaException
  → CatalogoConsultaService / CatalogoAdminService      (internos)
  → PlantaResumo + CatalogoService                      (API pública)
  → CatalogoDtos → controllers
  → CatalogoIT
```

## Regras de modularidade aplicadas

- `vendas` usa apenas `CatalogoService` e `PlantaResumo`. Nunca importa nada de `catalogo.internal`.
- `ItemPedido` guarda `plantaId` (UUID), não um `@ManyToOne Planta`. A FK existe só no banco.
- Antes de considerar o módulo pronto, rode `./mvnw verify` para executar o `CatalogoIT` e o `ModularityTests`.
