<div align="center">

<img src="assets/logo.png" alt="DeskWeb Logo" width="130" height="130" />

# DeskWeb

### 🖥️ Desktop-Class Web Browser for Android & PC Launchers

*Bring the full desktop browser experience to your Android tablets, foldables, Samsung DeX, Motorola Ready For, and Android TV setups.*

<br/>

[![Latest Release](https://img.shields.io/github/v/release/atomicdhruba/DeskWeb?color=00B4D8&label=Release&logo=github&style=for-the-badge)](https://github.com/atomicdhruba/DeskWeb/releases/latest)
[![Android](https://img.shields.io/badge/Platform-Android%207.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue?style=for-the-badge)](LICENSE)
[![DeX Ready](https://img.shields.io/badge/Samsung%20DeX-Optimized-1428A0?style=for-the-badge&logo=samsung&logoColor=white)](#)
[![Tracker Blocker](https://img.shields.io/badge/AdBlock-Built--in-E63946?style=for-the-badge&logo=adblock&logoColor=white)](#)

<br/>

[![Download APK](https://img.shields.io/badge/⬇️_Direct_Download-DeskWeb_v1.0.0.apk-2ea44f?style=for-the-badge&logo=android&logoColor=white)](https://github.com/atomicdhruba/DeskWeb/releases/download/v1.0.0/DeskWeb.apk)
[![Releases Page](https://img.shields.io/badge/📦_All_Releases-GitHub_Releases-gray?style=for-the-badge&logo=github)](https://github.com/atomicdhruba/DeskWeb/releases)

</div>

---

## 🌟 Highlights

**DeskWeb** is designed from the ground up for productivity and desktop multitasking on Android. Unlike standard mobile browsers that push simplified mobile layouts, DeskWeb defaults to a full desktop layout with horizontal tabs, instant bookmarking, keyboard navigation, and privacy control.

- 💻 **True Desktop Tab Strip** — Multitask with real horizontal tabs that you can reorder, switch between, and open instantly with `Ctrl+T`.
- 🔖 **Smart Bookmarks Bar** — Chrome-style bookmarks row with site favicons, one-click editing, and custom renaming.
- 🛡️ **Privacy & AdBlock Suite** — Comprehensive tracking prevention, automatic HTTPS upgrades, Do Not Track headers, and cookie isolation.
- 🔍 **Omnibox Intelligence** — Smart address bar that detects math calculations, telephone dials (`tel:`), emails (`mailto:`), and direct searches.
- ⚡ **Multi-Engine Search** — Switch on the fly between Google, DuckDuckGo, Bing, Yahoo, Startpage, and Ecosia.
- 👥 **Profiles & Guest Mode** — Multiple avatar-based profiles plus an isolated Guest Mode that leaves zero browsing traces.
- 🎛️ **Density & Display Scaling** — Toggle between **Comfortable** and **Compact** density to fit your display size.

---

## 🚀 Key Features

### 🖥️ Desktop Interface
| Feature | Description |
| :--- | :--- |
| **Horizontal Tab Bar** | Multi-tab strip with active highlights, page loading indicators, and quick close buttons. |
| **Desktop Site (User-Agent)** | Request genuine desktop web versions with a single toggle. |
| **Full Screen (F11)** | Immerse yourself in full-screen browsing with an auto-floating exit control. |
| **System Immersive Mode** | Hide Android status bar and navigation gestures for maximum screen estate. |
| **UI Density Switcher** | Compact mode for high-density monitors and Comfortable mode for touch screens. |

### 🛡️ Privacy & Security
| Tool | What It Does |
| :--- | :--- |
| **Ad & Tracker Blocker** | Blocks intrusive ad networks, telemetry scripts, and malware domains at the WebView request level. |
| **HTTPS-Only Mode** | Automatically upgrades all non-secure HTTP links to encrypted HTTPS connections. |
| **Do Not Track (DNT)** | Broadcasts strict privacy signals (`DNT: 1`, `Sec-GPC: 1`) to participating web servers. |
| **Cookies Manager** | Inspect active site cookies with 1-tap cache and cookie purger. |
| **Page Certificate Viewer** | View SSL/TLS security status, encryption issuer, and security certificates. |

### ⚡ Productivity & Power Tools
| Tool | Description |
| :--- | :--- |
| **Omnibox Smart Detection** | Type math equations, phone numbers (`tel:`), or email addresses (`mailto:`) for quick actions. |
| **Custom Search Engines** | Choose your default engine: Google, DuckDuckGo, Bing, Yahoo, Startpage, or Ecosia. |
| **Page Screenshot** | Capture crisp full-viewport screenshots saved directly into your device Gallery. |
| **Custom Download Location** | Configure custom download folders on internal storage or external SD cards. |
| **In-Browser History** | Searchable local history manager with instant search filtering and clean-all support. |
| **User Profiles & Guest Mode** | Create distinct user profiles with avatars or browse privately in Guest Mode. |

---

## ⌨️ Desktop Keyboard Shortcuts

When using a physical keyboard, Bluetooth keyboard, or desktop dock (DeX, Ready For):

| Shortcut | Action |
| :--- | :--- |
| <kbd>Ctrl</kbd> + <kbd>T</kbd> | Open New Tab |
| <kbd>Ctrl</kbd> + <kbd>W</kbd> | Close Current Tab |
| <kbd>Ctrl</kbd> + <kbd>R</kbd> / <kbd>F5</kbd> | Reload Current Page |
| <kbd>Ctrl</kbd> + <kbd>L</kbd> | Focus Address Bar (Omnibox) |
| <kbd>Ctrl</kbd> + <kbd>D</kbd> | Bookmark Current Page |
| <kbd>Ctrl</kbd> + <kbd>H</kbd> | Open History Dialog |
| <kbd>Ctrl</kbd> + <kbd>J</kbd> | Open Downloads |
| <kbd>F11</kbd> | Toggle Fullscreen Mode |
| <kbd>Ctrl</kbd> + <kbd>+</kbd> / <kbd>-</kbd> | Zoom In / Out |

---

## 📥 Installation

### Method 1: Download Pre-built APK (Recommended)
1. Go to the [**Latest Release**](https://github.com/atomicdhruba/DeskWeb/releases/latest).
2. Download **`DeskWeb.apk`**.
3. Open the APK on your Android device and allow **Install from Unknown Sources** if prompted.
4. Launch **DeskWeb** and enjoy desktop browsing!

### Method 2: Build from Source
```bash
# Clone the repository
git clone https://github.com/atomicdhruba/DeskWeb.git
cd DeskWeb

# Build Debug APK
./gradlew :chrome_pc:assembleDebug

# The APK will be ready at:
# chrome_pc/build/outputs/apk/debug/chrome_pc-debug.apk
```

---

## 🏗️ Tech Stack & Architecture

- **Language**: Kotlin 2.0+
- **Minimum Android SDK**: Android 7.0 Nougat (API 24)
- **Target SDK**: Android 15 (API 35)
- **Engine**: AndroidX WebKit / Chromium WebView
- **UI Architecture**: ViewBinding, Material Components 3, Custom Canvas Views
- **Storage**: Encrypted SharedPreferences, SQLite Suggestions & History Engine
- **Coroutines**: Kotlin Coroutines (`kotlinx-coroutines-android`) for asynchronous I/O and ad-list lookups

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome!
Feel free to check the [issues page](https://github.com/atomicdhruba/DeskWeb/issues) to report bugs or submit feature ideas.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

Distributed under the **Apache License 2.0**. See [`LICENSE`](LICENSE) for more information.

<br/>

<div align="center">

Made with ❤️ by [**Atomic Dhruba**](https://github.com/atomicdhruba)

⭐ **If you find DeskWeb useful, please consider giving it a star on GitHub!** ⭐

</div>
