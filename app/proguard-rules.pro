# --- AndroidX & General Optimization ---
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-keepattributes *Annotation*,Signature,EnclosingMethod,InnerClasses

# --- Glide (Image Loading) ---
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.AppGlideModule
-keep public class com.bumptech.glide.GeneratedAppGlideModule { *; }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** { *; }
-dontwarn com.bumptech.glide.load.resource.bitmap.VideoDecoder

# --- Apache Commons Compress (ZIP / RAR / Tar) ---
-dontwarn org.apache.commons.compress.**
-keep class org.apache.commons.compress.** { *; }
-keepclassmembers class * extends org.apache.commons.compress.archivers.ArchiveOutputStream { *; }
-keepclassmembers class * extends org.apache.commons.compress.archivers.ArchiveInputStream { *; }

# --- Media3 ExoPlayer ---
-dontwarn androidx.media3.exoplayer.**
-keep class androidx.media3.exoplayer.** { *; }

# --- RecyclerView Animators ---
-dontwarn jp.wasabeef.recyclerview.animators.**
-keep class jp.wasabeef.recyclerview.animators.** { *; }

# --- Keep Model / Data Classes (Jika menggunakan Gson/Serialization/Parcelable) ---
# Tambahkan baris ini jika model data kamu dibungkus Parcelable atau Gson
#-keep class com.fileexplorer.app.model.** { *; }
