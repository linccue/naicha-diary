-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class com.naicha.diary.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.naicha.diary.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.naicha.diary.data.**$$serializer { *; }

-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**
# 保护 Glance 依赖的 WorkManager 组件，防止被 R8 误删
-keep class * extends androidx.work.InputMerger { <init>(); }
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class androidx.glance.session.SessionWorker { *; }
