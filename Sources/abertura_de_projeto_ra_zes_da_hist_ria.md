# 🌱 Abertura de Projeto: E-commerce Raízes da História

## 🎯 Objetivos do Projeto

1. **Catálogo e Vendas (E-commerce):**

   * Fornecer um catálogo com as plantas disponíveis (adicionadas pelo administrador), exibindo nome popular, preço e informações de cultivo (rega e sol) em um layout tradicional e agradável de e-commerce.

   * Permitir que o visitante busque plantas por nome e tipo.

   * Disponibilizar carrinho de compras interativo (adicionar, editar quantidades, remover itens e fechar compra).

   * Gerar pagamento via QR Code Pix diretamente na tela ao finalizar o pedido.

2. **Encomenda de Plantas:**

   * Disponibilizar uma tela para o visitante encomendar plantas específicas, informando o tipo, nome da planta e anexando uma imagem de referência.

   * Ao finalizar, o sistema deve formular automaticamente uma mensagem/documento com os dados do pedido e enviá-lo ao administrador via WhatsApp ou E-mail.

3. **Hotel de Plantas:**

   * Exibir um calendário com a disponibilidade dos proprietários do espaço para hospedagem de plantas.

   * Permitir que o cliente selecione um intervalo de dias, calculando automaticamente o valor da hospedagem.

   * Coletar as informações pessoais do cliente e confirmar a reserva caso esteja de acordo com o valor.

4. **Mini Wikipédia de Plantas (Wiki Raízes da História):**

   * Área informativa editada pelo administrador contendo dados sobre diversas plantas, curiosidades e relações com fatos históricos.

   * Funcionalidade estritamente de consulta para os visitantes.

5. **Área Administrativa e Gestão:**

   * Acesso restrito ao administrador para gerenciar estoque de materiais e produção (plantas prontas para venda).

   * Controle financeiro integrado: vendas do catálogo, encomendas pagas e receitas do hotel de plantas devem alimentar automaticamente o módulo financeiro.

   * Geração de dashboards analíticos com base nos dados financeiros consolidados.

## 📋 Requisitos do Sistema

### Requisitos Funcionais (RF)

#### 👤 Perfil: Cliente

* Visualizar o catálogo de plantas disponíveis.

* Buscar e filtrar plantas por nome, tipo, necessidade de rega e incidência de sol.

* Adicionar plantas ao carrinho de compras.

* Visualizar o carrinho de compras.

* Editar o carrinho (adicionar mais unidades ou remover produtos).

* Fechar compra.

* Realizar pagamento via Pix (QR Code).

* Realizar encomenda de plantas informando nome, tipo e anexando imagem.

* Gerar pedido formalizado de encomenda com envio automático via WhatsApp ou E-mail.

* Realizar reserva de hospedagem no hotel de plantas com base na disponibilidade cadastrada.

* Visualizar o cálculo do preço da hospedagem conforme o intervalo de dias selecionado.

* Consultar a Wiki Raízes da História.

#### 🛠️ Perfil: Administrador

* Realizar cadastro no sistema.

* Realizar login (autenticação).

* Adicionar ou remover produtos do catálogo de vendas.

* Editar a disponibilidade de datas no hotel de plantas.

* Criar e editar artigos na Wiki de plantas.

* Adicionar ou remover itens no controle de estoque e produção.

* Visualizar informações financeiras consolidadas.

* Acessar dashboards gerenciais e financeiros.

### Requisitos Não Funcionais (RNF)

#### 🔒 Segurança e Privacidade
* **Autenticação e Autorização:** O acesso à área administrativa deve ser protegido por autenticação segura (ex: Spring Security com JWT), garantindo que apenas usuários autorizados gerenciem estoque, finanças e conteúdos.
* **Proteção de Dados (LGPD):** Os dados pessoais coletados dos clientes (especialmente nas reservas do hotel de plantas e encomendas) devem ser trafegados via protocolo HTTPS e armazenados de forma segura.
* **Integridade Financeira:** As transações via Pix e os registros que alimentam o módulo financeiro devem possuir validação no back-end para evitar manipulação de valores no lado do cliente.

#### 📱 Usabilidade e Experiência (PWA)
* **Responsividade (Mobile-First):** A interface deve adaptar-se perfeitamente a smartphones, tablets e desktops, priorizando a navegação móvel.
* **Recursos de PWA:** O sistema deve permitir a instalação na tela inicial dos dispositivos móveis (*Add to Home Screen*) e oferecer suporte básico de cache offline para consulta rápida do catálogo e da Wiki.
* **Acessibilidade e Navegação:** O layout deve ser intuitivo, com contraste adequado e feedbacks visuais claros para ações como adição ao carrinho, envio de encomendas e geração de QR Code.

#### ⚡ Desempenho e Armazenamento
* **Tempo de Resposta:** As buscas e filtros no catálogo e na Wiki devem retornar resultados em menos de 2 segundos em condições normais de rede.
* **Otimização de Mídia:** As imagens das plantas (catálogo, Wiki e anexos de encomendas) devem passar por compressão/validação de tamanho e formato (ex: WebP, JPEG, PNG até 5MB) para não degradar o carregamento da aplicação.

#### 🔧 Manutenibilidade e Arquitetura
* **Modularidade:** O back-end deve manter o isolamento entre os domínios (Catálogo, Encomendas, Hotel, Wiki, Financeiro) dentro do monólito modular, facilitando testes e futuras evoluções.
* **Compatibilidade:** A aplicação web deve funcionar corretamente nas versões mais recentes dos principais navegadores (Google Chrome, Safari, Firefox e Microsoft Edge).

## 🗓️ Planejamento e Análise

### Fases de Desenvolvimento

O desenvolvimento seguirá a ordem dos objetivos estabelecidos, dividido em **5 etapas**, com previsão de **1 semana corrida** para cada etapa (otimizada com o auxílio de Inteligência Artificial):

* **Etapa 1:** Catálogo e Carrinho (Pix)

* **Etapa 2:** Sistema de Encomendas

* **Etapa 3:** Hotel de Plantas

* **Etapa 4:** Wiki Raízes da História

* **Etapa 5:** Área do Administrador (Estoque, Finanças e Dashboards)

### Acompanhamento e Validação

* **Equipe:** Desenvolvimento individual (1 desenvolvedor).

* **Rito de Entrega:** Ao final de cada etapa concluída, será realizada uma reunião de alinhamento com o **Everaldo** para apresentação dos resultados e coleta de feedback visando ajustes e melhorias contínuas.

## 🏗️ Arquitetura e Design de Software

### Arquitetura

* **Back-end:** Monólito Modular (estruturado para facilitar uma eventual migração para microsserviços no futuro, caso haja necessidade de escala).

* **Front-end:** PWA (*Progressive Web Application*), garantindo experiência fluida tanto em desktop quanto em dispositivos móveis.

### Stack Tecnológica

* **Back-end:** Java com Spring Boot 3

* **Front-end:** TypeScript, React e Next.js

* **Banco de Dados:** PostgreSQL

* **Hospedagem / Infraestrutura:** A definir