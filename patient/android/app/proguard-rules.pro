# ===================================================================
# Dosezy Safe Shrinker Rules (Dead Code Stripping WITHOUT Obfuscation)
# ===================================================================

# 1. ABSOLUTELY NEVER OBFUSCATE OR RENAME CLASSES / METHODS / FIELDS
# This guarantees 1000% compatibility with reflection, Room, Hilt, and Gson.
-dontobfuscate
-dontoptimize

# 2. Preserve essential runtime attributes
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions,SourceFile,LineNumberTable

# 3. KEEP 100% OF DOSEZY APPLICATION CODE UNTOUCHED
-keep class com.example.dosezy.** { *; }
-keep interface com.example.dosezy.** { *; }
-keep enum com.example.dosezy.** { *; }

# 4. Android Framework Components (Activities, Services, Receivers, Application)
-keep class * extends android.app.Activity { *; }
-keep class * extends android.app.Application { *; }
-keep class * extends android.app.Service { *; }
-keep class * extends android.content.BroadcastReceiver { *; }
-keep class * extends android.content.ContentProvider { *; }
-keep class * extends android.app.backup.BackupAgent { *; }

# 5. Room Database (Entities, DAOs, Database, TypeConverters)
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.TypeConverter class * { *; }
-keep @androidx.room.TypeConverters class * { *; }
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# 6. Hilt & Dagger Dependency Injection
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep class dagger.hilt.** { *; }
-keep class * implements dagger.hilt.** { *; }
-keep class * extends dagger.hilt.** { *; }
-keep class * extends dagger.internal.** { *; }
-keep class * implements dagger.internal.** { *; }
-dontwarn dagger.hilt.**

# 7. Gson & Data Models
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }
-keep class com.example.dosezy.data.model.** { *; }
-dontwarn com.google.gson.**

# 8. Jetpack Compose Runtime & UI
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }
-dontwarn androidx.compose.**

# 9. Kotlin Coroutines & Standard Library
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# 10. Coil (Image Loading)
-keep class io.coil-kt.** { *; }
-dontwarn coil.**

# 11. Datastore Preferences
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# 12. Suppress benign library warnings
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.bouncycastle.**