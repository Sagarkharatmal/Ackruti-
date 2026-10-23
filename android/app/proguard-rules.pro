# Release builds are not minified yet (see app/build.gradle.kts). Rules kept
# here for when minification is switched on.
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
