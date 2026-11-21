# 🗄️ Esquema de Banco de Dados: E-commerce Raízes da História

**Banco de Dados:** PostgreSQL  
**Arquitetura:** Monólito Modular (Java Spring Boot 3)

---

## 1. Módulo de Usuários & Autenticação

### `administradores`
Gerencia o acesso à área restrita do sistema.
* `id` (`UUID`, PK)
* `nome` (`VARCHAR`)
* `email` (`VARCHAR`, UNIQUE)
* `senha_hash` (`VARCHAR`)
* `criado_em` (`TIMESTAMP`)

### `clientes`
Armazena os dados de compradores e hóspedes de plantas (permite checkout rápido ou cadastro opcional).
* `id` (`UUID`, PK)
* `nome` (`VARCHAR`)
* `email` (`VARCHAR`)
* `telefone_whatsapp` (`VARCHAR`)
* `cpf` (`VARCHAR`, NULLABLE)
* `criado_em` (`TIMESTAMP`)

---

## 2. Módulo de Catálogo & E-commerce (Etapa 1)

### `tipos_planta`
Categorias de plantas (ex.: Suculentas, Folhagens, Frutíferas, Medicinais).
* `id` (`SERIAL`, PK)
* `nome` (`VARCHAR`, UNIQUE)
* `descricao` (`TEXT`)

### `plantas`
Produtos disponíveis para venda direta no catálogo.
* `id` (`UUID`, PK)
* `tipo_id` (`INT`, FK $\rightarrow$ `tipos_planta.id`)
* `nome_popular` (`VARCHAR`)
* `nome_cientifico` (`VARCHAR`, NULLABLE)
* `preco` (`DECIMAL(10,2)`)
* `frequencia_rega` (`ENUM/VARCHAR`: `'DIARIA'`, `'SEMANAL'`, `'QUINZENAL'`)
* `exposicao_sol` (`ENUM/VARCHAR`: `'PLENO'`, `'MEIA_SOMBRA'`, `'SOMBRA'`)
* `quantidade_pronta_venda` (`INT`)
* `url_imagem` (`VARCHAR`)
* `ativo` (`BOOLEAN`)

### `pedidos`
Registra as compras feitas pelo carrinho e o status do pagamento via Pix.
* `id` (`UUID`, PK)
* `cliente_id` (`UUID`, FK $\rightarrow$ `clientes.id`)
* `valor_total` (`DECIMAL(10,2)`)
* `status_pagamento` (`ENUM`: `'PENDENTE'`, `'PAGO'`, `'CANCELADO'`, `'EXPIRADO'`)
* `pix_txid` (`VARCHAR`, UNIQUE)
* `pix_copia_cola` (`TEXT`)
* `criado_em` (`TIMESTAMP`)
* `pago_em` (`TIMESTAMP`, NULLABLE)

### `itens_pedido`
Tabela associativa entre pedidos e plantas (salva o preço histórico no momento da compra).
* `id` (`UUID`, PK)
* `pedido_id` (`UUID`, FK $\rightarrow$ `pedidos.id`)
* `planta_id` (`UUID`, FK $\rightarrow$ `plantas.id`)
* `quantidade` (`INT`)
* `preco_unitario` (`DECIMAL(10,2)`)
* `status_entrega` (`ENUM`: `'EM_SEPARACAO'`, `'PRONTA'`, `'ENTREGUE'`, `'CANCELADA'`)

---

## 3. Módulo de Encomendas Personalizadas (Etapa 2)

### `encomendas`
Solicitações de plantas que não estão no catálogo padrão.
* `id` (`UUID`, PK)
* `cliente_id` (`UUID`, FK $\rightarrow$ `clientes.id`)
* `tipo_planta_solicitado` (`VARCHAR`)
* `nome_planta_solicitada` (`VARCHAR`)
* `observacoes` (`TEXT`, NULLABLE)
* `url_imagem_referencia` (`VARCHAR`, NULLABLE)
* `valor_acordado` (`DECIMAL(10,2)`, NULLABLE)
* `status` (`ENUM`: `'SOLICITADA'`, `'ORCAMENTO_ENVIADO'`, `'PAGA'`, `'CONCLUIDA'`, `'CANCELADA'`)
* `criado_em` (`TIMESTAMP`)

---

## 4. Módulo de Hotel de Plantas (Etapa 3)

### `configuracao_hotel`
Regras gerais definidas pelo administrador (diária base e capacidade máxima de plantas por dia).
* `id` (`SERIAL`, PK)
* `valor_diaria` (`DECIMAL(10,2)`)
* `capacidade_maxima_diaria` (`INT`)
* `atualizado_em` (`TIMESTAMP`)

### `bloqueios_calendario`
Dias específicos em que o administrador fecha a agenda do hotel (férias, manutenção, lotação manual).
* `id` (`UUID`, PK)
* `data_bloqueada` (`DATE`, UNIQUE)
* `motivo` (`VARCHAR`, NULLABLE)

### `hospedagens`
Reservas de hospedagem feitas pelos clientes.
* `id` (`UUID`, PK)
* `cliente_id` (`UUID`, FK $\rightarrow$ `clientes.id`)
* `data_inicio` (`DATE`)
* `data_fim` (`DATE`)
* `quantidade_plantas` (`INT`, DEFAULT 1)
* `cuidados_especiais` (`TEXT`, NULLABLE)
* `valor_total` (`DECIMAL(10,2)`)
* `status` (`ENUM`: `'PENDENTE_PAGAMENTO'`, `'CONFIRMADA'`, `'EM_ANDAMENTO'`, `'FINALIZADA'`, `'CANCELADA'`)
* `criado_em` (`TIMESTAMP`)

---

## 5. Módulo Wiki "Raízes da História" (Etapa 4)

### `artigos_wiki`
Conteúdo enciclopédico e histórico de consulta pública.
* `id` (`UUID`, PK)
* `autor_id` (`UUID`, FK $\rightarrow$ `administradores.id`)
* `planta_id` (`UUID`, FK $\rightarrow$ `plantas.id`, NULLABLE — permite vincular o artigo a uma planta à venda no catálogo)
* `titulo` (`VARCHAR`)
* `slug` (`VARCHAR`, UNIQUE)
* `nome_cientifico` (`VARCHAR`, NULLABLE)
* `curiosidades` (`TEXT`)
* `contexto_historico` (`TEXT`)
* `url_imagem_capa` (`VARCHAR`, NULLABLE)
* `publicado_em` (`TIMESTAMP`)
* `atualizado_em` (`TIMESTAMP`)

---

## 6. Módulo Administrativo: Estoque, Produção & Finanças (Etapa 5)

### `insumos_estoque`
Controle de materiais físicos (vasos, substratos, adubos, fertilizantes).
* `id` (`UUID`, PK)
* `nome` (`VARCHAR`)
* `categoria` (`VARCHAR`)
* `quantidade_atual` (`DECIMAL(10,2)`)
* `unidade_medida` (`VARCHAR`: `'UN'`, `'KG'`, `'L'`)
* `estoque_minimo` (`DECIMAL(10,2)`)
* `atualizado_em` (`TIMESTAMP`)

### `registros_producao`
Histórico de mudas cultivadas até ficarem prontas para o catálogo.
* `id` (`UUID`, PK)
* `planta_id` (`UUID`, FK $\rightarrow$ `plantas.id`)
* `administrador_id` (`UUID`, FK $\rightarrow$ `administradores.id`)
* `quantidade_produzida` (`INT`)
* `data_registro` (`DATE`)
* `observacao` (`TEXT`, NULLABLE)

### `movimentacoes_financeiras`
Livro-caixa central que alimenta os dashboards. Recebe inserções automáticas via eventos do Spring (`ApplicationEventPublisher`) sempre que um **Pedido**, **Encomenda** ou **Hospedagem** é marcado como pago.
* `id` (`UUID`, PK)
* `tipo` (`ENUM`: `'RECEITA'`, `'DESPESA'`)
* `origem` (`ENUM`: `'VENDA_CATALOGO'`, `'ENCOMENDA'`, `'HOSPEDAGEM'`, `'COMPRA_INSUMO'`, `'OUTROS'`)
* `descricao` (`VARCHAR`)
* `valor` (`DECIMAL(10,2)`)
* `data_movimentacao` (`TIMESTAMP`)
* `pedido_id` (`UUID`, FK $\rightarrow$ `pedidos.id`, NULLABLE)
* `encomenda_id` (`UUID`, FK $\rightarrow$ `encomendas.id`, NULLABLE)
* `hospedagem_id` (`UUID`, FK $\rightarrow$ `hospedagens.id`, NULLABLE)

---

## 🔗 Resumo dos Relacionamentos (Cardinalidade)

| Tabela Origem | Cardinalidade | Tabela Destino | Regra de Negócio |
| :--- | :---: | :--- | :--- |
| `tipos_planta` | `1 : N` | `plantas` | Um tipo agrupa várias plantas no catálogo. |
| `clientes` | `1 : N` | `pedidos` | Um cliente pode realizar vários pedidos no e-commerce. |
| `pedidos` | `1 : N` | `itens_pedido` | Um pedido contém um ou mais itens. |
| `plantas` | `1 : N` | `itens_pedido` | Uma planta do catálogo pode constar em vários pedidos. |
| `clientes` | `1 : N` | `encomendas` | Um cliente pode solicitar várias encomendas personalizadas. |
| `clientes` | `1 : N` | `hospedagens` | Um cliente pode fazer várias reservas no hotel de plantas. |
| `administradores` | `1 : N` | `artigos_wiki` | Um administrador cria e edita vários artigos históricos. |
| `plantas` | `1 : 1` *(opcional)* | `artigos_wiki` | Um artigo da Wiki pode referenciar uma planta disponível para compra. |
| `plantas` | `1 : N` | `registros_producao` | Cada lote produzido incrementa `quantidade_pronta_venda` da planta. |
| `pedidos` / `encomendas` / `hospedagens` | `1 : 1` *(opcional)* | `movimentacoes_financeiras` | Toda transação paga gera automaticamente um lançamento de receita. |