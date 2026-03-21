-- Ensure users.role is present, backfilled, and non-null for existing schemas.
ALTER TABLE IF EXISTS public.users
    ADD COLUMN IF NOT EXISTS role VARCHAR(20);

UPDATE public.users
SET role = 'USER'
WHERE role IS NULL OR TRIM(role) = '';

ALTER TABLE IF EXISTS public.users
    ALTER COLUMN role SET DEFAULT 'USER';

ALTER TABLE IF EXISTS public.users
    ALTER COLUMN role SET NOT NULL;
