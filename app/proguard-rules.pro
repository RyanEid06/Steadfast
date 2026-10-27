# Steadfast ProGuard / R8 Rules for Release Build

# Keep Moshi JSON classes and generated adapters
-keepclasseswithmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep class *JsonAdapter {
    public <init>(...);
}
-keep @com.squareup.moshi.JsonClass class * { *; }
-dontwarn com.squareup.moshi.**

# Room Database & Entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# AndroidX DataStore & Preferences
-keepclassmembers class * extends androidx.datastore.preferences.core.Preferences { *; }

# AndroidX Biometric
-keep class androidx.biometric.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
