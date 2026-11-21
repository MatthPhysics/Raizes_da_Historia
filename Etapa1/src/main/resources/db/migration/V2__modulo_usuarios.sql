-- ===== Módulo de Usuários & Autenticação =====

CREATE TABLE administradores (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    nome        VARCHAR(120) NOT NULL,
    email       VARCHAR(160) NOT NULL,
    senha_hash  VARCHAR(100) NOT NULL,
    criado_em   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_administradores_email UNIQUE (email)
);

-- Clientes: checkout rápido (sem senha); e-mail NÃO é único,
-- pois o mesmo comprador pode fazer vários checkouts sem cadastro.
CREATE TABLE clientes (
    id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    nome               VARCHAR(120) NOT NULL,
    email              VARCHAR(160) NOT NULL,
    telefone_whatsapp  VARCHAR(20)  NOT NULL,
    cpf                VARCHAR(11),
    criado_em          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_clientes_cpf CHECK (cpf IS NULL OR cpf ~ '^[0-9]{11}$')
);

CREATE INDEX idx_clientes_email ON clientes (lower(email));
