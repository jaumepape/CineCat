-- Migració inicial: les 3 entitats del domini (ESPECIFICACIO.md §3).
--
-- Decisions:
--  - IDs UUID generats per Postgres (gen_random_uuid() és nativa des de PG 13):
--    no són endevinables ni revelen quantes files hi ha, i els clients (web i
--    mòbil) els tracten com a text opac.
--  - Els "enums" (role, status) són text + CHECK: tan segur com un tipus ENUM
--    però més fàcil d'ampliar en una migració futura.
--  - timestamptz (amb zona horària): Postgres ho desa en UTC i no hi ha
--    ambigüitats entre servidor i clients.

CREATE TABLE users (
    id            uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    email         text        NOT NULL UNIQUE,
    alias         text        NOT NULL UNIQUE,
    password_hash text        NOT NULL,
    role          text        NOT NULL DEFAULT 'user' CHECK (role IN ('admin', 'user')),
    created_at    timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE movies (
    id           uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    title        text        NOT NULL,
    year         integer     NOT NULL,
    duration_min integer     NOT NULL CHECK (duration_min > 0),
    director     text        NOT NULL,
    synopsis     text        NOT NULL DEFAULT '',
    -- Array de gèneres (decisió §3). La llista vàlida es comprova al backend.
    genres       text[]      NOT NULL DEFAULT '{}',
    status       text        NOT NULL DEFAULT 'draft' CHECK (status IN ('published', 'draft')),
    -- Ruta del fitxer del pòster, NO els bytes. NULL = encara sense pòster (Fase 2).
    poster_path  text,
    created_at   timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE ratings (
    id           uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    -- Si s'esborra una pel·lícula, les seves valoracions ja no tenen sentit:
    -- ON DELETE CASCADE les esborra alhora.
    movie_id     uuid        NOT NULL REFERENCES movies (id) ON DELETE CASCADE,
    -- NULL = valoració anònima. Si s'esborra un usuari, les seves valoracions
    -- es conserven com a anònimes (compten per a la mitjana igualment).
    user_id      uuid        REFERENCES users (id) ON DELETE SET NULL,
    score        integer     NOT NULL CHECK (score BETWEEN 1 AND 10),
    comment      text,
    author_label text,
    created_at   timestamptz NOT NULL DEFAULT now()
);

-- La fitxa calcula AVG/COUNT filtrant per movie_id: l'índex evita recórrer
-- totes les valoracions de la taula.
CREATE INDEX ratings_movie_id_idx ON ratings (movie_id);
