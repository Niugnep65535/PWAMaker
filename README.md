# PWA Maker

A modern Android utility built with Jetpack Compose and Material 3 that allows you to easily wrap any web URL into a borderless, fullscreen Progressive Web App (PWA) shortcut. Say goodbye to cluttered browser interfaces and build a dedicated app experience for your favorite websites!

 > [!CAUTION]
 > This program (which contains most of the content of this README) is a complete AI slop.

## 🌟 Key Features

* **📱 Borderless Fullscreen View**: Enjoy an immersive web browsing experience inside a dedicated WebView container.
* **🌍 Internationalization (i18n)**: Built-in multi-language switching supporting **Traditional Chinese, Simplified Chinese, English, and Japanese**, catering to a global user base.
* **🎨 Custom Icons**: Support for picking local images from your photo gallery (via Photo Picker) to craft a unique app icon.
* **🕵️‍♂️ Flexible User-Agent (UA) Settings**:
  * **Basic Mode**: One-tap toggle between Mobile or Desktop UA.
  * **Advanced Mode**: Freely input or customize the complete User-Agent string.
* **🔖 Saved PWA List Management**: Automatically saves your previously created PWA configurations, making it easy to edit, reload, or quickly open them anytime.
* **📸 Comprehensive Hardware & Web Permissions**:
  * Supports WebRTC (Camera and Microphone permissions).
  * Supports HTML Geolocation.
  * Supports `<input type="file">` file upload selectors.
  * Supports HTML fullscreen video playback.

## 🛠️ Tech Stack

* **UI Framework**: Jetpack Compose + Material 3
* **Async & Image Loading**: Coil (AsyncImage)
* **System Interaction**: ActivityResultContracts, WebChromeClient, WebViewClient
* **Storage Mechanism**: PwaStorage (Local settings persistence)

## 🚀 Quick Start

1. Enter your desired **App Name**.
2. Input the target **URL** (e.g., `https://example.com`).
3. Tap to select a preferred **Image Icon** (Optional; a default icon will be used if omitted).
4. Choose your preferred **User-Agent mode** based on your requirements.
5. Tap the **"Add to Home Screen"** button at the bottom to create a dedicated shortcut on your desktop!

## 🤝 Contribution & Feedback

If you encounter any bugs or have feature suggestions, feel free to open an issue or submit a pull request to help make this project even better!
