-- =============================================================================
-- V3__account_locking.sql
-- Adds brute-force protection columns to system_users.
--
-- Depends on: V2__auth_and_user_schema.sql (system_users table must exist)
--
-- Changes:
--   system_users
--     + failed_login_attempts  INTEGER  NOT NULL DEFAULT 0
--     + locked_until           TIMESTAMP NULL
--
--   New index on locked_until so AuthService lock-check queries stay fast.
-- =============================================================================

-- ── Add columns ───────────────────────────────────────────────────────────────

ALTER TABLE system_users
    ADD COLUMN failed_login_attempts INTEGER   NOT NULL DEFAULT 0,
    ADD COLUMN locked_until          TIMESTAMP NULL;

-- ── Constraints ───────────────────────────────────────────────────────────────

ALTER TABLE system_users
    ADD CONSTRAINT chk_failed_login_attempts
        CHECK (failed_login_attempts >= 0);

-- ── Index — only index non-null rows (locked accounts are a small fraction) ───

CREATE INDEX idx_user_locked_until
    ON system_users (locked_until)
    WHERE locked_until IS NOT NULL;

-- ── Comments ──────────────────────────────────────────────────────────────────

COMMENT ON COLUMN system_users.failed_login_attempts IS
    'Consecutive failed logins since last success. Reset to 0 on successful login.';

COMMENT ON COLUMN system_users.locked_until IS
    'Account locked until this timestamp. NULL = not locked. Expires passively — no cron needed.';

-- =============================================================================
-- END OF MIGRATION
-- =============================================================================
