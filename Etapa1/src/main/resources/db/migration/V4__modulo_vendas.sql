-- ===== Pedidos e Itens (Etapa 1) =====

CREATE TABLE pedidos (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    cliente_id        UUID          NOT NULL,
    valor_total       NUMERIC(10,2) NOT NULL,
    status_pagamento  VARCHAR(20)   NOT NULL DEFAULT 'PENDENTE',
    pix_txid          VARCHAR(35),
    pix_copia_cola    TEXT,
    criado_em         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    pago_em           TIMESTAMPTZ,
    CONSTRAINT fk_pedidos_cliente  FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT uk_pedidos_pix_txid UNIQUE (pix_txid),
    CONSTRAINT ck_pedidos_valor    CHECK (valor_total > 0),
    CONSTRAINT ck_pedidos_status   CHECK (status_pagamento IN ('PENDENTE', 'PAGO', 'CANCELADO', 'EXPIRADO')),
    CONSTRAINT ck_pedidos_pago_em  CHECK (status_pagamento <> 'PAGO' OR pago_em IS NOT NULL)
);

CREATE INDEX idx_pedidos_cliente           ON pedidos (cliente_id);
CREATE INDEX idx_pedidos_status_criado_em  ON pedidos (status_pagamento, criado_em);

CREATE TABLE itens_pedido (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    pedido_id       UUID          NOT NULL,
    planta_id       UUID          NOT NULL,
    quantidade      INT           NOT NULL,
    preco_unitario  NUMERIC(10,2) NOT NULL,
    status_entrega  VARCHAR(20)   NOT NULL DEFAULT 'EM_SEPARACAO',
    CONSTRAINT fk_itens_pedido_pedido  FOREIGN KEY (pedido_id) REFERENCES pedidos (id) ON DELETE CASCADE,
    CONSTRAINT fk_itens_pedido_planta  FOREIGN KEY (planta_id) REFERENCES plantas (id),
    CONSTRAINT uk_itens_pedido_planta  UNIQUE (pedido_id, planta_id),
    CONSTRAINT ck_itens_quantidade     CHECK (quantidade > 0),
    CONSTRAINT ck_itens_preco          CHECK (preco_unitario > 0),
    CONSTRAINT ck_itens_status         CHECK (status_entrega IN ('EM_SEPARACAO', 'PRONTA', 'ENTREGUE', 'CANCELADA'))
);

CREATE INDEX idx_itens_pedido_pedido ON itens_pedido (pedido_id);
CREATE INDEX idx_itens_pedido_planta ON itens_pedido (planta_id);
