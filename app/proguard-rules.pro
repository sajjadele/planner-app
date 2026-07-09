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

# ---- Room Database ----
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ---- Room Entities ----
-keep class com.example.plugins.planner.TaskEntity { *; }
-keep class com.example.plugins.planner.TaskEventEntity { *; }
-keep class com.example.plugins.notes.NoteEntity { *; }
-keep class com.example.core.database.ModuleSettingsEntity { *; }

# ---- Room DAOs ----
-keep class com.example.plugins.planner.TaskDao { *; }
-keep class com.example.plugins.planner.TaskEventDao { *; }
-keep class com.example.plugins.notes.NoteDao { *; }
-keep class com.example.core.database.ModuleSettingsDao { *; }

# ---- KSP-generated Room implementations ----
-keep class **_Impl { *; }
