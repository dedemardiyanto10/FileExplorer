# 📂 FileExplorer - Android File Management Utility

An advanced, feature-rich Android file explorer application built with **Java**, **Kotlin**, and **Material Design 3**, featuring robust local storage analytics, built-in archive extraction utilities, and modern expressive UI components.

---

## 🚀 Key Features

* **Advanced Storage Analyzer**: Detailed breakdown of internal storage consumption categorized by Media (Images, Videos, Audio), Documents, APKs, Archives, and Other files.
* **Modern Material 3 Expressive UI**: Built using dynamic color systems, sleek rounded container layouts, and responsive components (`MaterialCardView`, `LinearProgressIndicator`).
* **Archive Management**: Seamless extraction and handling of ZIP, RAR, 7Z, TAR, and GZ formats powered by Apache Commons Compress.
* **Media & File Preview**: Integrated media playback via Media3 ExoPlayer and optimized thumbnail caching with Glide.
* **Smooth Navigation**: Optimized custom RecyclerView adapters and smooth list item animations.

---

## 🛠️ Tech Stack & Dependencies

* **Languages**: Java, Kotlin
* **UI Framework**: Material Components for Android (Material 3), ConstraintLayout
* **Local Storage & IO**: Android Storage APIs (`StatFs`, `MediaStore`, `Environment`)
* **Core Libraries**:
  * **Material Components**: `com.google.android.material:material:1.14.0`
  * **Apache Commons Compress**: `org.apache.commons:commons-compress:1.26.0`
  * **RecyclerView Animators**: `jp.wasabeef:recyclerview-animators:4.0.2`
  * **Glide**: `com.github.bumptech.glide:glide:5.0.5`
  * **AndroidX Media3 ExoPlayer**: `androidx.media3:media3-exoplayer:1.10.1`

---

## 📱 Build Specifications

* **Compile SDK**: 36
* **Target SDK**: 34
* **Min SDK**: 24 (Android 7.0+)
* **Build Features**: View Binding enabled
* **Java Compatibility**: JavaVersion 17

---

## ⚙️ ProGuard Configuration

Optimized and obfuscated for release builds with custom rules protecting Glide, Apache Commons Compress, and Media3 ExoPlayer reflection mechanisms.

---

## 📄 License

This project is developed as a high-performance personal utility software. Feel free to explore and adapt!
