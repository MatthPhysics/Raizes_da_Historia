CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- unaccent() é STABLE e não pode ser usada em índices.
-- Este wrapper IMMUTABLE (dicionário fixo) permite indexar a busca sem acento.
CREATE OR REPLACE FUNCTION f_unaccent(texto TEXT)
    RETURNS TEXT
    LANGUAGE sql
    IMMUTABLE PARALLEL SAFE STRICT
AS $$
    SELECT public.unaccent('public.unaccent'::regdictionary, texto)
$$;
