-- ============================================================
--  MIGRATION: Replace comma-separated tags with normalized tags table
--  Date: 2026-03-04
-- ============================================================

-- Sequence for tags
CREATE SEQUENCE tag_id_seq START WITH 1 INCREMENT BY 1;

-- ============================================================
--  TABLE: TAGS
--  Represents a normalized tag/genre for comics.
-- ============================================================

CREATE TABLE tags
(
    id              bigint        NOT NULL PRIMARY KEY,   -- Unique tag ID (from tag_id_seq)
    name            varchar(255)  NOT NULL,               -- Display name (original casing)
    normalized_name varchar(255)  NOT NULL UNIQUE,        -- Lowercase name for dedup matching
    comic_count     integer       NOT NULL DEFAULT 0,     -- Denormalized count of comics using this tag
    created_at      timestamp(6),                         -- Record creation timestamp
    updated_at      timestamp(6)                          -- Record last update timestamp
);

-- ============================================================
--  TABLE: COMIC_TAGS (Junction table)
--  Many-to-many relationship between comics and tags.
-- ============================================================

CREATE TABLE comic_tags
(
    comic_id bigint NOT NULL,
    tag_id   bigint NOT NULL,
    PRIMARY KEY (comic_id, tag_id)
);

ALTER TABLE comic_tags
    ADD CONSTRAINT fk_comic_tags_comic
        FOREIGN KEY (comic_id) REFERENCES comics (id)
            ON DELETE CASCADE
            DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE comic_tags
    ADD CONSTRAINT fk_comic_tags_tag
        FOREIGN KEY (tag_id) REFERENCES tags (id)
            ON DELETE CASCADE
            DEFERRABLE INITIALLY DEFERRED;

-- ============================================================
--  INDEXES
-- ============================================================

CREATE INDEX idx_tags_normalized_name ON tags (normalized_name);
CREATE INDEX idx_comic_tags_comic     ON comic_tags (comic_id);
CREATE INDEX idx_comic_tags_tag       ON comic_tags (tag_id);

-- ============================================================
--  DROP old tags column from comics
-- ============================================================

ALTER TABLE comics DROP COLUMN IF EXISTS tags;
