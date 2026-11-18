# ===================================================================
# Dosezy ProGuard & R8 Optimization Rules
# Ensures high optimization, obfuscation, and shrinking (>75%)
# while strictly safeguarding Room Database, Gson, and Glance Widgets
# ===================================================================

# 1. Preserve essential runtime attributes and stack trace line numbers for Play Console deobfuscation
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions,SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 2. Android Framework & Glance App Widgets
# Note: Activities, Services, Receivers, and Application classes are already
# preserved by getDefaultProguardFile("proguard-android-optimize.txt").
# Only Glance Widgets and custom widget receivers require explicit rules.
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
# Guard: Keep Room entities, DAOs, and TypeConverters for reflection and SQLite table schemas
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.TypeConverter class * { *; }
-keep @androidx.room.TypeConverters class * { *; }
-keep class com.example.dosezy.data.database.** { *; }
-keep class com.example.dosezy.data.dao.** { *; }
-keep class com.example.dosezy.data.converters.** { *; }
-dontwarn androidx.room.**

# 6. Hilt Dependency Injection
# Note: Hilt and Dagger AARs include their own consumer ProGuard rules.
# Only app-specific DI entry points and modules are declared here.
-keep @dagger.hilt.android.lifecycle.HiltViewModel class *
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keep class com.example.dosezy.di.** { *; }
-dontwarn dagger.hilt.**

# 7. Gson Serialization
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# 8. Kotlin Coroutines & Standard Library
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# 9. Suppress benign library warnings
# Note: Compose, Coil, and DataStore package their own consumer rules inside their AARs.
-dontwarn androidx.compose.**
-dontwarn coil.**
-dontwarn androidx.datastore.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.bouncycastle.**
