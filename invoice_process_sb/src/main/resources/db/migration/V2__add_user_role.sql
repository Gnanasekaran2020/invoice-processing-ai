-- ============================================================
--  V2__add_user_role.sql
--  Add role column to users; update admin seed
-- ============================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- Promote the seeded admin account
UPDATE users SET role = 'ADMIN' WHERE email = 'admin_u2a@cts.com';

CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
