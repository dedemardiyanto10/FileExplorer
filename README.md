# 📂 FileExplorer - Android File Management Utility

An advanced, feature-rich Android file explorer application built with **Java**, **Kotlin**, and **Material Design 3**, featuring robust local storage analytics, built-in archive extraction utilities, and modern expressive UI components.

---

## 🚀 Key Features

* **Advanced Storage Analyzer**: Detailed breakdown of internal storage consumption categorized by Media (Images, Videos, Audio), Documents, APKs, Archives, and Other files.
* **Modern Material 3 Expressive UI**: Built using dynamic color systems, sleek rounded container layouts, and responsive components.
* **Archive Management**: Seamless extraction and handling of ZIP, RAR, 7Z, TAR, and GZ formats powered by Apache Commons Compress.
* **Media & File Preview**: Integrated media playback via Media3 ExoPlayer and optimized thumbnail caching with Glide.
* **Smooth Navigation**: Optimized custom RecyclerView adapters and smooth list transitions.

---

## 🛠️ Tech Stack & Libraries

* **Language**: Java / Kotlin
* **UI Framework**: Material Components for Android (Material 3), ConstraintLayout
* **Local Storage & IO**: Android Storage APIs (`StatFs`, `MediaStore`, `Environment`)
* **Background Processing**: `ExecutorService` for lightweight, non-blocking disk I/O scans.
* **Third-Party Dependencies**:
  * [Apache Commons Compress](https://commons.apache.org/proper/commons-compress/) (Archive handling)
  * [Media3 ExoPlayer](https://developer.android.com/media/media3) (Media playback)
  * [Glide](https://github.com/bumptech.glide/glide) (Image loading & caching)
  * [RecyclerView Animators](https://github.com/wasabeef/recyclerview-animators) (UI animations)

---

## 📱 Project Structure

```text
com.fileexplorer.app/
│
├── StorageAnalyzerActivity.java  # Storage analysis and breakdown logic
├── SettingsHelper.java           # Dynamic theme & AMOLED mode manager
└── ...
