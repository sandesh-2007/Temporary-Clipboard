# Temporary Online Clipboard (Web Application)

A modern, fast, and secure **Temporary Online Clipboard Website** for instantly transferring text snippets, passwords, links, codes, and files between any devices (Phone, PC, Mac, Tablet) using a **4-digit PIN** or **dynamic QR Code**.

---

## 🌐 Live Website

[![Deploy to Netlify](https://www.netlify.com/img/deploy/button.svg)](https://app.netlify.com/start/deploy)

- **Live Preview URL**: [Temporary Clipboard Web](https://ais-dev-2csewbkym5azvcw2ji4ruj-806742738147.asia-southeast1.run.app)
- **Netlify Deploy Ready**: Pre-configured with `netlify.toml` for automated continuous deployment.

---

## ✨ Features

- **Transfer Text & Code**: Share notes, links, passwords, and tokens effortlessly.
- **File & Photo Sharing**: Transfer attachments, pictures, and documents up to 20MB.
- **4-Digit Quick Code**: Pair devices in 2 seconds without creating an account or logging in.
- **QR Code Scanning**: Point your phone camera at the screen to instantly load the clipboard data.
- **Self-Destruct (Burn on Read)**: Permanent deletion right after the recipient retrieves it.
- **10-Minute Expiry**: Auto-purged in 10 minutes to protect your sensitive data.
- **Dark / Light Mode**: Modern glassmorphism design with responsive mobile UI.

---

## 🚀 Instant Netlify Deployment (Zero-Config)

### Method 1: Connect via GitHub (Automatic Updates)
1. Login to **[Netlify](https://app.netlify.com)** using your GitHub account.
2. Click **"Add new site"** > **"Import an existing project"**.
3. Select **GitHub** and choose this repository.
4. Netlify will automatically detect `netlify.toml` with:
   - **Publish directory**: `.`
   - **Build command**: `echo 'Static clipboard web app ready'`
5. Click **"Deploy site"**.
6. Netlify will give you a free live URL (e.g. `https://your-clipboard.netlify.app`)! Whenever you push code, Netlify will auto-deploy.

### Method 2: Netlify Drop (Instant Drag & Drop without linking)
1. Go to **[Netlify Drop](https://app.netlify.com/drop)**.
2. Drag and drop this folder (or download the ZIP and drag the folder containing `index.html`).
3. Your site is live in 10 seconds!

---

## 💻 Running Locally

Simply run a lightweight HTTP server or open `index.html` directly in your web browser:

```bash
# Using npx serve
npx serve . -p 3000

# Or using Python
python3 -m http.server 3000
```
Open [http://localhost:3000](http://localhost:3000) in your browser.

---

## ☁️ Supabase Setup (Optional Real-Time Cloud Storage)

1. Create a free project at [supabase.com](https://supabase.com).
2. Execute the schema from [`supabase/schema.sql`](supabase/schema.sql) in your Supabase SQL editor.
3. Click the **Settings ⚙️** icon on the website to enter your Supabase URL & Anon key, or enjoy the built-in local clipboard demo mode!

---

## 📄 License
MIT License - Created with ❤️ by Sandesh Vishwakarma
