# Temporary Online Clipboard - Deployment & Setup Guide

This guide covers setting up your Supabase project, executing the database schema, configuring the storage bucket, and deploying the frontend to **Vercel** or **Netlify**.

---

## 1. Supabase Project Setup

### Step 1: Create a Project
1. Log in to [Supabase](https://supabase.com/).
2. Click **"New project"**, select an organization, and choose a region close to your users.
3. Save your database password securely.

### Step 2: Run the SQL Schema
1. Navigate to the **SQL Editor** in your Supabase dashboard.
2. Open or copy the contents of `supabase/schema.sql`.
3. Click **Run** to execute the script.
   - This creates the `clipboard` table.
   - Sets up indexing on `code` and `expires_at`.
   - Enables Row Level Security (RLS) with anonymous read, insert, and delete policies.
   - Configures the `temporary_clips` storage bucket (20MB limit).
   - Creates the `cleanup_expired_clips()` procedure and schedules `pg_cron` (if supported).

### Step 3: Enable pg_cron for Auto-Purge (Optional)
1. Go to **Database** -> **Extensions**.
2. Search for `pg_cron` and click **Enable**.
3. Re-run the cron schedule command from `schema.sql` if not activated automatically:
   ```sql
   SELECT cron.schedule(
       'purge-expired-temporary-clips',
       '*/2 * * * *',
       'SELECT public.cleanup_expired_clips();'
   );
   ```

### Step 4: Obtain Project Credentials
1. Go to **Project Settings** -> **API**.
2. Copy:
   - **Project URL** (e.g., `https://xyzproject.supabase.co`)
   - **Project API Anon Key** (public `anon` key)

---

## 2. Deploying on Vercel

### Option A: Via Vercel CLI
```bash
# Install Vercel CLI
npm install -g vercel

# Deploy from the web folder
cd web
vercel
```

### Option B: Via Vercel Dashboard (GitHub / Git)
1. Push your repository to GitHub.
2. In [Vercel](https://vercel.com/), click **Add New** -> **Project**.
3. Select your repository.
4. Set **Root Directory** to `web` (or root if using `index.html` at root).
5. Add Environment Variables (optional, or enter them in the app settings modal):
   - `NEXT_PUBLIC_SUPABASE_URL` = `https://your-project.supabase.co`
   - `NEXT_PUBLIC_SUPABASE_ANON_KEY` = `your-anon-key`
6. Click **Deploy**.

---

## 3. Deploying on Netlify

### Option A: Netlify Drop (Zero Configuration)
1. Open [Netlify Drop](https://app.netlify.com/drop).
2. Drag and drop the `web` folder into the upload zone.
3. Your site is live immediately with a secure HTTPS URL.

### Option B: Netlify CLI
```bash
# Install Netlify CLI
npm install -g netlify-cli

# Deploy site
cd web
netlify deploy --prod --dir=.
```

---

## 4. Key Architectural Highlights
- **No Login / Frictionless Sharing:** End-to-end PIN access protected by Row Level Security (RLS).
- **Auto-Destruction:** All clips expire in 10 minutes (`expires_at <= NOW()`).
- **Burn-on-Read:** Instantly triggers row deletion and bucket storage purge upon single read.
- **Direct Client Upload:** Files stream directly to the Supabase Storage bucket with signed or public paths, supporting up to 20MB.
