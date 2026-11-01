# Application Icons (Dungeon Master Manager)

This directory contains the application and taskbar icons used by **Dungeon Master Manager**.

---

## 🎨 How to replace with your own custom icon

You can easily replace the default icon placeholder with your own design:

### 1. Main Icon (In-App, Window Decorations, and Taskbar)
- **Path:** `desktopApp/src/main/resources/icons/app_icon.png`
- **Recommended format:** PNG with transparent background
- **Recommended resolution:** **512x512** or **256x256** pixels
- Simply overwrite `app_icon.png` with your desired image. The app automatically resizes it to 16x16, 24x24, 32x32, 48x48, 64x64, 128x128, and 256x256 for crisp rendering across all screen resolutions and taskbars.

### 2. Runtime Quick Override (without recompiling)
You can also drop an `app_icon.png` or `icon.png` directly into the working directory of the application. If present, the app will load it immediately over the default bundled icon.

---

## 📦 Native Distribution Icons (Installers & Desktop Shortcuts)

If you package the application for desktop platforms (`./gradlew :desktopApp:packageDistributionForCurrentOS`):

| Operating System | Recommended File Name | Format |
|---|---|---|
| **Windows** | `app_icon.ico` | Multi-resolution Windows Icon (`.ico`) |
| **macOS** | `app_icon.icns` | Apple Icon Image (`.icns`) |
| **Linux** | `app_icon.png` | Standard PNG (512x512) |

If `.ico` or `.icns` are not present, Gradle will automatically fall back to `app_icon.png`.
