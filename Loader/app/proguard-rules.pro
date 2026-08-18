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

-keep class top.niunaijun.blackbox.** {*; }
-keep class top.niunaijun.blackbox.core.NativeCore { *; }
-keep class top.niunaijun.blackbox.BlackBoxCore { *; }
-keep class top.niunaijun.blackbox.app.BActivityThread { *; }
-keep class org.lsposed.hiddenapibypass.** { *; }

-keep class top.niunaijun.jnihook.** {*; }
-keep class black.** {*; }
-keep class android.** {*; }
-keep class com.android.** {*; }

-keep class androidx.work.** { *; }
-keep class androidx.startup.** { *; }
-keep class androidx.lifecycle.** { *; }
-keep class androidx.emoji2.** { *; }


-keep class com.zoro.loader.** { *; }
-keep class com.zoro.loader.floating.** { *; }
-keep class com.zoro.loader.floating.FloatLogo { *; }
-keep class com.zoro.loader.floating.Overlay { *; }
-keep class com.zoro.loader.floating.ESPView { *; }
-keep class android.** { *; }
-keepattributes *Annotation*
-dontwarn top.niunaijun.**
# Keep SLF4J logging classes
-keep class org.slf4j.** { *; }
-dontwarn org.slf4j.**