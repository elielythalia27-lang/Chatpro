# ChatPro Secure Hardened ProGuard / R8 Rules

-optimizationpasses 5
-allowaccessmodification
-repackageclasses 'com.aistudio.chatpro.core'
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Strip all Android logging from release builds to avoid exposing network URLs or tokens
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# Preserve Moshi models and annotations
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
    @com.squareup.moshi.JsonQualifier <fields>;
}
-keepattributes RuntimeVisible*Annotations
-dontwarn com.squareup.moshi.**
-keep class com.squareup.moshi.** { *; }

# Preserve Retrofit interfaces
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Preserve Room components
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Compose rules
-keep class androidx.compose.** { *; }

# Security classes preservation
-keep class com.example.security.** { *; }
-keep class com.example.data.model.** { *; }
