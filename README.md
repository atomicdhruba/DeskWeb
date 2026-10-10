# DeskWeb 🌐

**DeskWeb** is a desktop-style web browser for Android devices, tablets, Android TV, and desktop-mode environments (Samsung DeX, Motorola Ready For, PC launchers).

It brings the complete desktop browser experience to Android, featuring a desktop tab strip, bookmarks bar, custom search engines, privacy tools, user profiles, and keyboard shortcuts.

---

## ✨ Features

- **🖥️ Desktop-Class Interface**
  - Horizontal tab bar with reordering and one-click new tab (`Ctrl+T` / `F11`)
  - Full address bar (Omnibox) with smart detection (URLs, search, telephone `tel:`, email `mailto:`, calculations)
  - Dedicated bookmarks bar with quick-add, edit, and rename support
  - Compact and Comfortable UI density switcher

- **🛡️ Privacy & Security**
  - Built-in Ad & Tracker Blocker
  - HTTPS-Only Mode (automatic upgrade with fallback alert)
  - Do Not Track (DNT) header enforcement
  - Cookies and Site Data inspector with instant cache clear
  - Page Security & SSL Certificate viewer dialog

- **👥 Multi-Profile & Guest Mode**
  - Profile switcher with avatar customization
  - Guest mode with private browsing (no history retention)

- **⚡ Productivity & Utilities**
  - In-browser History search & manager
  - Custom Search Engines (Google, DuckDuckGo, Bing, Yahoo, Startpage, Ecosia)
  - Full page / viewport screenshot capture with instant gallery save
  - Custom download folder picker (internal storage or external SD)
  - Desktop Site User-Agent toggling
  - Fullscreen (F11) and Immersive Mode (hide system navigation & status bars)

---

## 🛠️ Building from Source

### Prerequisites
- JDK 17 or higher
- Android SDK 35 (compileSdk 35, minSdk 24)

### Clone & Build
```bash
git clone https://github.com/<your-username>/DeskWeb.git
cd DeskWeb
./gradlew :chrome_pc:assembleDebug
```

The compiled APK will be located at:
```
chrome_pc/build/outputs/apk/debug/chrome_pc-debug.apk
```

---

## 📄 License
This project is open-source software provided under the [Apache 2.0 License](LICENSE).
