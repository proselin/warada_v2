-- ============================================================
--  SEQUENCES
--  Used to auto-generate unique IDs for each table entity.
-- ============================================================

CREATE SEQUENCE chapter_id_seq START WITH 1 INCREMENT BY 1; -- For chapters.id
CREATE SEQUENCE comic_id_seq START WITH 1 INCREMENT BY 1;   -- For comics.id
CREATE SEQUENCE image_id_seq START WITH 1 INCREMENT BY 1;   -- For images.id

-- ============================================================
--  TABLE: COMICS
--  Represents a single comic or manga series.
-- ============================================================

CREATE TABLE comics
(
    id                 bigint       NOT NULL PRIMARY KEY,  -- Unique comic ID (generated from comic_id_seq)
    slug               varchar(200) NOT NULL UNIQUE,       -- Unique identifier slug for URLs (e.g., 'one-piece')
    title              varchar(255),                       -- Comic title
    author             varchar(255),                       -- Author name
    description        text,                               -- Comic description or summary
    tags               varchar(255),                       -- Comma-separated list of tags/genres
    status             varchar(255) NOT NULL,              -- Current publication status (e.g., 'ongoing', 'completed')
    chapter_count      integer,                            -- Number of chapters
    crawling_status    varchar(255) NOT NULL,              -- Crawl progress status (e.g., 'pending', 'done', 'error')
    origin_id          varchar(255) UNIQUE,                -- Original ID from source website
    origin_url         varchar(255) UNIQUE,                -- Original URL from source website
    origin_path_params varchar(500),                       -- Extracted path segment (for exact matching)
    thumb_image_id     bigint       UNIQUE,                -- Reference to cover/thumbnail image
    created_at         timestamp(6),                       -- Record creation timestamp
    updated_at         timestamp(6)                        -- Record last update timestamp
);


-- ============================================================
--  TABLE: CHAPTERS
--  Represents individual chapters belonging to a comic.
-- ============================================================

CREATE TABLE chapters
(
    id                 bigint       NOT NULL PRIMARY KEY,  -- Unique chapter ID (from chapter_id_seq)
    comic_id           bigint,                             -- Foreign key reference to comics.id
    chapter_num        varchar(255),                       -- Chapter number (e.g., 'Chapter 10')
    position           integer,                            -- Chapter order within the comic
    crawling_status    varchar(255),                       -- Crawl progress (e.g., 'pending', 'done', 'error')
    type               varchar(255),                       -- Type of chapter (e.g., 'main', 'extra')
    source_url         varchar(255) UNIQUE,                -- Original URL for this chapter
    origin_path_params varchar(500) UNIQUE,                -- Extracted path parameters from URL
    created_at         timestamp(6),                       -- When this record was inserted
    updated_at         timestamp(6)                        -- Last modification timestamp

);


-- ============================================================
--  TABLE: IMAGES
--  Represents image pages within a chapter.
-- ============================================================

CREATE TABLE images
(
    id          bigint       NOT NULL PRIMARY KEY,        -- Unique image ID (from image_id_seq)
    chapter_id  bigint       UNIQUE,                      -- Reference to the chapter this image belongs to
    position    integer,                                  -- Page order within the chapter
    file_name   varchar(255) UNIQUE,                      -- Saved file name (e.g., 'page_01.webp')
    origin_url  varchar(255) UNIQUE,                      -- Original image URL from the source
    type        varchar(255),                             -- Image type (e.g., 'webp', 'jpg', 'png')
    created_at  timestamp(6),                             -- When this record was inserted
    updated_at  timestamp(6)                             -- Last modification timestamp
);

-- ============================================================
-- FOREIGN KEYS (DEFERRABLE)
-- ============================================================

ALTER TABLE chapters
    ADD CONSTRAINT fk_chapters_comic
        FOREIGN KEY (comic_id) REFERENCES comics(id)
            ON DELETE CASCADE
            DEFERRABLE INITIALLY DEFERRED;   -- Each chapter belongs to one comic
ALTER TABLE images
    ADD CONSTRAINT fk_images_chapter
        FOREIGN KEY (chapter_id) REFERENCES chapters(id)
            ON DELETE CASCADE
            DEFERRABLE INITIALLY DEFERRED;   -- Each image belongs to one chapter
ALTER TABLE comics
    ADD CONSTRAINT fk_comics_thumb_image
        FOREIGN KEY (thumb_image_id) REFERENCES images(id)
            ON DELETE SET NULL
            DEFERRABLE INITIALLY DEFERRED;   -- One comic has one thumbnail image

-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX idx_chapters_comic     ON chapters(comic_id);
CREATE INDEX idx_images_chapter     ON images(chapter_id);
CREATE INDEX idx_comics_thumb_image ON comics(thumb_image_id);
