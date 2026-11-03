# Docfold has no reflection of its own; these rules cover libraries whose bundled rules miss R8 full mode.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# WorkManager (pulled in by the Ads SDK) creates its Room database reflectively; without this the release
# build crashes on launch with "Failed to create an instance of androidx.work.impl.WorkDatabase".
-keep class * extends androidx.room.RoomDatabase { <init>(); }

# ML Kit discovers its components from manifest metadata and instantiates them by reflection.
-keep class com.google.mlkit.** implements com.google.firebase.components.ComponentRegistrar { <init>(); }
-keep class com.google.android.gms.internal.mlkit_** implements com.google.firebase.components.ComponentRegistrar { <init>(); }
