-- ============================================================
--  MIGRATION: Add scheduled update fields to comics table
--  Date: 2026-03-14
--  Feature: Redis job queue for automatic chapter re-crawl
-- ============================================================

-- update_on: WEEK, MONTH, or NONE — defines the recurrence schedule
ALTER TABLE comics ADD COLUMN update_on VARCHAR(10) DEFAULT 'NONE' NOT NULL;

-- fixed_time_to_next_update: 0-6 for WEEK (0=Monday), 1-31 for MONTH
ALTER TABLE comics ADD COLUMN fixed_time_to_next_update INTEGER DEFAULT 0 NOT NULL;

-- next_update_date: the computed date when the next crawl should run
ALTER TABLE comics ADD COLUMN next_update_date TIMESTAMP(6);

-- last_update_date: tracks when chapters were last re-crawled
ALTER TABLE comics ADD COLUMN last_update_date TIMESTAMP(6);

-- Index on next_update_date for the scheduler scan query
CREATE INDEX idx_comics_next_update_date ON comics (next_update_date)
    WHERE next_update_date IS NOT NULL;
