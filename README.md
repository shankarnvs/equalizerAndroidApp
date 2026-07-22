# Equalizer Pro - Android Audio Tuning App

Equalizer Pro is a powerful, professional-grade audio enhancement tool for Android. Built with modern Android technologies, it provides system-wide audio tuning that works across wired headsets, Bluetooth devices, and internal speakers.

## 🚀 Key Features

*   **20-Band High-Precision EQ**: Advanced frequency control using the `DynamicsProcessing` API (Android 9+). Fine-tune your audio from 31Hz to 16kHz with 0.01dB precision.
*   **"Flow Style" Bass Boost**: Aggressive sub-bass enhancement that combines hardware-level processing with dynamic software gain for deep, chest-thumping low end.
*   **Extreme Loudness Enhancement**: Boost your device volume safely with up to **30dB** of additional gain.
*   **Background Persistence**: Runs as a robust Foreground Service. Once started, it remains active in the background until explicitly stopped, ensuring your tune is never interrupted.
*   **Dynamic Material 3 UI**: Built with Jetpack Compose. The UI color scheme dynamically reacts to your current system volume and selected preset.
*   **Smart Settings Persistence**: Automatically saves your manual adjustments and reloads them on app startup.
*   **Audio Effects**: Includes Virtualization and Balance control.
*   **Zero 3rd Party Plugins**: Built using 100% native Android and Jetpack libraries for maximum stability and performance.

## 🛠 Tech Stack

*   **UI**: Jetpack Compose, Material 3
*   **Language**: Kotlin
*   **Architecture**: MVVM
*   **Audio Engine**: Android `AudioEffect` APIs (`DynamicsProcessing`, `BassBoost`, `LoudnessEnhancer`, `Virtualizer`)
*   **Persistence**: SharedPreferences

## 📸 Screenshots

| High Precision EQ | Dynamic Theming | System Notification |
| :---: | :---: | :---: |
| Fine-tune 20 frequency bands | Colors change with volume | Control EQ from anywhere |

## 📦 Permissions

The app requires the following permissions to function:
*   `MODIFY_AUDIO_SETTINGS`: To apply global audio filters.
*   `FOREGROUND_SERVICE`: To maintain operation in the background.
*   `POST_NOTIFICATIONS`: To show the service control notification (Android 13+).

## 🚀 Getting Started

1.  Clone the repository.
2.  Open in **Android Studio**.
3.  Build and run the `:androidApp` module.
4.  Click **"Start Equalizer"** and enjoy professional-grade sound.

---
Created with ❤️ for Android Audio Enthusiasts.
