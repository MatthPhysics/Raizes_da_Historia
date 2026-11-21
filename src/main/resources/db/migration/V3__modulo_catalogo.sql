-- ===== Módulo de Catálogo (Etapa 1) =====

CREATE TABLE tipos_planta (
    id         SERIAL       PRIMARY KEY,
    nome       VARCHAR(80)  NOT NULL,
    descricao  TEXT,
    CONSTRAINT uk_tipos_planta_nome UNIQUE (nome)
);

CREATE TABLE plantas (
    id                       UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo_id                  INT           NOT NULL,
    nome_popular             VARCHAR(120)  NOT NULL,
    nome_cientifico          VARCHAR(160),
    preco                    NUMERIC(10,2) NOT NULL,
    frequencia_rega          VARCHAR(20)   NOT NULL,
    exposicao_sol            VARCHAR(20)   NOT NULL,
    quantidade_pronta_venda  INT           NOT NULL DEFAULT 0,
    url_imagem               VARCHAR(500),
    ativo                    BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_plantas_tipo        FOREIGN KEY (tipo_id) REFERENCES tipos_planta (id),
    CONSTRAINT ck_plantas_preco       CHECK (preco > 0),
    CONSTRAINT ck_plantas_quantidade  CHECK (quantidade_pronta_venda >= 0),
    CONSTRAINT ck_plantas_rega        CHECK (frequencia_rega IN ('DIARIA', 'SEMANAL', 'QUINZENAL')),
    CONSTRAINT ck_plantas_sol         CHECK (exposicao_sol   IN ('PLENO', 'MEIA_SOMBRA', 'SOMBRA'))
);

CREATE INDEX idx_plantas_tipo        ON plantas (tipo_id);
CREATE INDEX idx_plantas_ativo       ON plantas (ativo);
CREATE INDEX idx_plantas_filtros     ON plantas (frequencia_rega, exposicao_sol) WHERE ativo;
-- Índices de busca sem acento/caixa: devem usar a MESMA expressão das queries (f_unaccent(lower(...)))
CREATE INDEX idx_plantas_nome_popular_trgm    ON plantas USING gin (f_unaccent(lower(nome_popular)) gin_trgm_ops);
CREATE INDEX idx_plantas_nome_cientifico_trgm ON plantas USING gin (f_unaccent(lower(nome_cientifico)) gin_trgm_ops);

-- Carga inicial de tipos (exemplos do esquema)
INSERT INTO tipos_planta (nome, descricao) VALUES
    ('Suculentas', 'Plantas que armazenam água em folhas e caules.'),
    ('Folhagens',  'Plantas ornamentais cultivadas pela beleza das folhas.'),
    ('Frutíferas', 'Plantas que produzem frutos comestíveis.'),
    ('Medicinais', 'Plantas com uso tradicional terapêutico.');
