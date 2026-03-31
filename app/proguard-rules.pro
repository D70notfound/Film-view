# NepuView ProGuard / R8 rules

# ── Kotlin ────────────────────────────────────────────────────────────────────
-keepclassmembers class **$WhenMappings { <fields>; }
-keep class kotlin.Metadata { *; }

# ── Hilt / Dagger ─────────────────────────────────────────────────────────────
-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# ── Room ──────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.**

# ── Navigation Safe Args ──────────────────────────────────────────────────────
-keepnames class androidx.navigation.fragment.NavHostFragment
-keepnames class * extends android.os.Parcelable
-keepnames class * extends java.io.Serializable

# ── Media3 / ExoPlayer ────────────────────────────────────────────────────────
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ── Glide ─────────────────────────────────────────────────────────────────────
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** { **[] $VALUES; public *; }
-dontwarn com.bumptech.glide.**

# ── OkHttp / Media3 datasource ────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**

# ── Gson / JSON ───────────────────────────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ── WebView JS interfaces ─────────────────────────────────────────────────────
# Keep @JavascriptInterface annotated methods so they aren't renamed
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ── Data classes used in JSON ─────────────────────────────────────────────────
-keep class net.nepuview.data.** { *; }

# ── Preferences ───────────────────────────────────────────────────────────────
-keep class androidx.preference.** { *; }
-dontwarn androidx.preference.**

# ── Shimmer ───────────────────────────────────────────────────────────────────
-dontwarn com.facebook.shimmer.**

# ── General Android ───────────────────────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
