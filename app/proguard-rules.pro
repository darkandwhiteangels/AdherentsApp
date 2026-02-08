# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- Firestore & modèles de l'app (indispensable) ---
-keep class com.antechrist.adherentsapp.domain.model.** { *; }
-keep class com.antechrist.adherentsapp.data.model.** { *; }

# Garder annotations & signatures (utiles pour Firestore/Gson)
-keepattributes *Annotation*, Signature

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Hilt / Dagger (safe)
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-dontwarn javax.inject.**
-keep class **_Factory { *; }
-keep class **_HiltModules* { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }

# Kotlin / Coroutines
-keep class kotlin.Metadata { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Jetpack Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
