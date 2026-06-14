-- Per-account UI preferences. Mirrors the Node backend so a user's theme/density
-- follow their account instead of leaking across users on a shared device.
ALTER TABLE users ADD COLUMN theme VARCHAR(20) NOT NULL DEFAULT 'light'
    CHECK (theme IN ('light', 'dark'));
ALTER TABLE users ADD COLUMN density VARCHAR(20) NOT NULL DEFAULT 'comfortable'
    CHECK (density IN ('comfortable', 'compact'));
