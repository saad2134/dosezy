# ===================================================================
# Dosezy ProGuard & R8 Optimization Rules
# Ensures >= 25% Optimization & Obfuscation while strictly safeguarding
# Room Database, Hilt DI, Gson Serialization, and Glance Widgets
# ===================================================================

# 1. Preserve essential runtime attributes and stack trace line numbers for Play Console deobfuscation
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions,SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 2. Android Framework Components (Activities, Services, Receivers, Application, Widgets)
-keep class * extends android.app.Activity { *; }
-keep class * extends android.app.Application { *; }
-keep class * extends android.app.Service { *; }
-keep class * extends android.content.BroadcastReceiver { *; }
-keep class * extends android.content.ContentProvider { *; }
-keep class * extends android.app.backup.BackupAgent { *; }
-keep class * extends android.appwidget.AppWidgetProvider { *; }
-keep class com.example.dosezy.widget.** { *; }

# 3. Enums: Preserve values() and valueOf() so Room TypeConverters and JSON parsing never fail
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 4. Data Models, Enums, and JSON Schema Parity
# Guard: Keep all models, fields, and enums so Room queries and backup/restore JSON serialization maintain 100% parity
-keep class com.example.dosezy.data.model.** { *; }
-keep enum com.example.dosezy.data.model.** { *; }
-keep class com.example.dosezy.data.export.** { *; }

# 5. Room Database (Database, Entities, DAOs, TypeConverters)
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.TypeConverter class * { *; }
-keep @androidx.room.TypeConverters class * { *; }
-keep class androidx.room.** { *; }
-keep class com.example.dosezy.data.database.** { *; }
-keep class com.example.dosezy.data.dao.** { *; }
-keep class com.example.dosezy.data.converters.** { *; }
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
-keep class com.example.dosezy.di.** { *; }
-keep class *_HiltModules* { *; }
-keep class *_Factory { *; }
-dontwarn dagger.hilt.**

# 7. Gson Serialization
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }
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
