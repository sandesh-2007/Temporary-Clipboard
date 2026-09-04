# 📋 Temporary Online Clipboard

[![Android](https://img.shields.io/badge/Platform-Android-green?logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Supabase](https://img.shields.io/badge/Backend-Supabase-3ECF8E?logo=supabase&logoColor=white)](https://supabase.com)
[![Build APK](https://github.com/actions/workflows/build-apk.yml/badge.svg)](../../actions)

An ultra-fast, cross-device temporary online clipboard for transferring text, snippets, links, and files between phones, computers, and tablets using a **4-digit PIN** or **instant QR code**. Built with modern Android (Kotlin + Jetpack Compose) and a companion standalone Web application with Supabase cloud storage.

---

## 📥 Download APK (Android App)

Aap niche diye gaye tareeqo se direct Android APK download karke apne phone me install kar sakte hain:

### Option 1: Direct Download from GitHub Releases (Recommended)
👉 **[Click Here to Download Latest APK (Releases)](../../releases/latest)**

1. Upar diye gaye link par click karein ya repository ke right side me **Releases** section par jayein.
2. Assets ke andar se **`app-debug.apk`** par click karein aur download start karein.
3. Download complete hone ke baad file par tap karein aur **Install** par click karein.

---

### Option 2: Download from GitHub Actions
1. Repo ke **[Actions Tab](../../actions)** me jayein.
2. Latest workflow run **"Build & Release Android APK"** par click karein.
3. Page ke bottom me **Artifacts** section se **`temporary-clipboard-apk`** download karein.

---

### 📲 How to Install APK on Android (Installation Guide)

1. **Download APK**: Phone ke browser se `app-debug.apk` download karein.
2. **Enable Unknown Sources**:
   - Agar phone me pop-up aaye *"Install unknown apps"*, toh **Settings** me jakar browser ke liye **"Allow from this source"** enable karein.
3. **Google Play Protect Warning**:
   - Agar Play Protect ka warning prompt aaye (*"Blocked by Play Protect / Unrecognized app"*), toh **More details** par click karke **"Install anyway"** tap karein (ye warning isliye aati hai kyunki APK direct developer build hai aur Play Store par published nahi hai).
4. **Launch the App**: App open karein aur cross-device clipboard ka maza lein!

---

## ✨ Key Features

- 🔢 **4-Digit Quick PIN**: Access clips instantly across devices without account registration.
- 📷 **Dynamic QR Code**: Scan with your smartphone camera to immediately load the clip.
- 🔥 **Burn on Read**: Optional self-destruct mode — clips are purged permanently after the first retrieval.
- ⏱️ **10-Minute Auto-Expiry**: Clips automatically disappear after 10 minutes, protecting sensitive tokens, passwords, and links.
- 📁 **File & Image Transfer**: Upload photos, documents, and code files with real-time progress and download links.
- 🌐 **Cross-Device Web Client**: Includes a standalone responsive web frontend in `/web` that can be deployed to Vercel or Netlify with zero configuration.
- ☁️ **Supabase Cloud + Local Mode**: Fully connected to Supabase PostgreSQL database and Storage with local fallback demo mode.

---

## 🚀 Project Structure

```
├── app/                  # Native Android App (Jetpack Compose & Kotlin)
│   ├── src/main/java/    # Android Source Code (ViewModel, Screens, UI)
│   └── src/main/res/     # App Resources & Launcher Icons
├── web/                  # Web Client (Tailwind CSS, Lucide Icons, Supabase JS)
│   └── index.html        # Single-file zero-dependency web interface
├── supabase/             # Database & Storage
│   └── schema.sql        # Full Supabase PostgreSQL schema & RLS policies
├── .github/workflows/    # CI/CD Workflows
│   └── build-apk.yml     # Automated GitHub Action to build and release APK
├── DEPLOYMENT.md         # Vercel, Netlify, and Cloud deployment guide
└── README.md             # Project documentation & APK download guide
```

---

## 🛠️ How to Build from Source

Agar aap khud se build karna chahte hain:

```bash
# Clone the repository
git clone https://github.com/<your-username>/<your-repo-name>.git
cd <your-repo-name>

# Build the Debug APK using Gradle
gradle assembleDebug

# The compiled APK will be available at:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 🌐 Supabase Database Setup

1. Create a new project at [supabase.com](https://supabase.com).
2. Go to the **SQL Editor** in your Supabase dashboard.
3. Copy the contents of [`supabase/schema.sql`](supabase/schema.sql) and execute it.
4. Open the Android app or web interface, tap **Settings**, and paste your Supabase Project URL and Anon Key.

---

## 📄 License
This project is open-source and available under the MIT License.
