ALTER TABLE public.users
    ADD COLUMN updated_at TIMESTAMPTZ;

UPDATE public.users
SET updated_at = created_at
WHERE updated_at IS NULL;