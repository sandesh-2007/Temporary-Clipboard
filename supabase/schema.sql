-- ==============================================================================
-- TEMPORARY ONLINE CLIPBOARD - SUPABASE DATABASE & STORAGE SCHEMA
-- ==============================================================================

-- 1. Create clipboard table
CREATE TABLE IF NOT EXISTS public.clipboard (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(4) NOT NULL,
    content TEXT,
    file_path TEXT,
    file_name TEXT,
    file_size BIGINT,
    type VARCHAR(20) NOT NULL DEFAULT 'text', -- 'text', 'file', 'image'
    burn_on_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '10 minutes')
);

-- 2. Indexes & Performance Optimization
CREATE INDEX IF NOT EXISTS idx_clipboard_code_expires_at 
    ON public.clipboard (code, expires_at);

CREATE INDEX IF NOT EXISTS idx_clipboard_expires_at 
    ON public.clipboard (expires_at);

-- 3. Row Level Security (RLS) Policies
ALTER TABLE public.clipboard ENABLE ROW LEVEL SECURITY;

-- Allow anonymous users to query clips by code that have NOT expired
CREATE POLICY "Allow reading active clips by code"
    ON public.clipboard
    FOR SELECT
    TO anon, authenticated
    USING (expires_at > NOW());

-- Allow anonymous users to insert new clips with a 10-minute expiry
CREATE POLICY "Allow inserting new clips"
    ON public.clipboard
    FOR INSERT
    TO anon, authenticated
    WITH CHECK (
        expires_at <= (NOW() + INTERVAL '11 minutes') AND
        expires_at > NOW()
    );

-- Allow anonymous users to delete clips (used for Burn on Read or manual burn)
CREATE POLICY "Allow deleting active clips"
    ON public.clipboard
    FOR DELETE
    TO anon, authenticated
    USING (expires_at > NOW());

-- 4. Supabase Storage Bucket Configuration
-- Create storage bucket 'temporary_clips' if it does not already exist
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'temporary_clips',
    'temporary_clips',
    true, -- or false if using signed URLs
    20971520, -- 20MB limit (in bytes)
    ARRAY['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'text/plain', 'application/pdf', 'application/zip', 'application/octet-stream', 'application/json']
)
ON CONFLICT (id) DO UPDATE SET
    file_size_limit = 20971520,
    public = true;

-- Storage RLS Policies
CREATE POLICY "Allow public uploads to temporary_clips"
    ON storage.objects
    FOR INSERT
    TO anon, authenticated
    WITH CHECK (bucket_id = 'temporary_clips');

CREATE POLICY "Allow public downloads from temporary_clips"
    ON storage.objects
    FOR SELECT
    TO anon, authenticated
    USING (bucket_id = 'temporary_clips');

CREATE POLICY "Allow public deletes from temporary_clips"
    ON storage.objects
    FOR DELETE
    TO anon, authenticated
    USING (bucket_id = 'temporary_clips');

-- 5. Auto-Purge Expiry Routine
-- Function to clean up expired clipboard records and delete corresponding storage objects
CREATE OR REPLACE FUNCTION public.cleanup_expired_clips()
RETURNS integer
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    deleted_count integer := 0;
    rec RECORD;
BEGIN
    -- Remove matching storage objects for expired files
    FOR rec IN 
        SELECT file_path FROM public.clipboard 
        WHERE expires_at <= NOW() AND file_path IS NOT NULL
    LOOP
        DELETE FROM storage.objects 
        WHERE bucket_id = 'temporary_clips' AND name = rec.file_path;
    END LOOP;

    -- Delete expired records
    WITH deleted AS (
        DELETE FROM public.clipboard
        WHERE expires_at <= NOW()
        RETURNING id
    )
    SELECT count(*) INTO deleted_count FROM deleted;

    RETURN deleted_count;
END;
$$;

-- 6. Schedule Periodic Expiry Cleanup (Supabase pg_cron)
-- If the pg_cron extension is enabled on your Supabase project:
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_cron') THEN
        PERFORM cron.schedule(
            'purge-expired-temporary-clips',
            '*/2 * * * *', -- Run every 2 minutes
            'SELECT public.cleanup_expired_clips();'
        );
    END IF;
END $$;
