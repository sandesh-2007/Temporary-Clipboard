# Temporary Online Clipboard (Web Application)

A modern, fast, and secure **Temporary Online Clipboard Website** for instantly transferring text snippets, passwords, links, codes, and files between any devices (Phone, PC, Mac, Tablet) using a **4-digit PIN** or **dynamic QR Code**.

---

## 🌐 Live Website & Online Access

This project is a high-performance **Web Application** that runs in any browser with zero installation needed.

- **Transfer Text & Code**: Share notes, links, passwords, and tokens effortlessly.
- **File & Photo Sharing**: Transfer attachments, pictures, and documents up to 20MB.
- **4-Digit Quick Code**: Pair devices in 2 seconds without creating an account or logging in.
- **QR Code Scanning**: Point your phone camera at the screen to instantly load the clipboard data.
- **Self-Destruct (Burn on Read)**: Permanent deletion right after the recipient retrieves it.
- **10-Minute Expiry**: Auto-purged in 10 minutes to protect your sensitive data.

---

## 🚀 Instant Deployment (1-Click)

You can host and run this website for free on **GitHub Pages**, **Vercel**, or **Netlify**:

### 1. GitHub Pages (Free & Automatic)
1. Go to your repository on GitHub.
2. Click on **Settings** > **Pages** (in the left sidebar).
3. Under **Branch**, select `main` and root folder `/` (or `/public`).
4. Click **Save**. Your website will be live at:
   `https://<your-username>.github.io/<repo-name>/`

### 2. Vercel / Netlify
1. Go to [vercel.com](https://vercel.com) or [netlify.com](https://netlify.com).
2. Import your GitHub repository.
3. Deploy! Since it is standard HTML5/CSS/JS, it requires 0 build configuration.

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

## 📱 Mobile Friendly & Android App

- **Responsive Web App**: Works seamlessly on all mobile screens, iPhones, and Android browsers.
- **Native Android App**: The project also includes a full native Jetpack Compose Android app in `/app` and pre-built APKs in the repository.

---

## ☁️ Supabase Setup (Optional Cloud Storage)

1. Create a free project at [supabase.com](https://supabase.com).
2. Execute the schema from [`supabase/schema.sql`](supabase/schema.sql) in your Supabase SQL editor.
3. Click the **Settings ⚙️** icon on the website to enter your Supabase URL & Anon key, or enjoy the built-in local clipboard demo mode!

---

## 📄 License
MIT License - Created with ❤️ by Sandesh Vishwakarma
