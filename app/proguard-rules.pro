# Keep the app's development/admin controller and framework entry points intact.
# Dependencies are optimized using their own consumer rules. Retain app names
# while this app is in internal testing to simplify debugging.
-keep class com.dominic.photosweep.** { *; }
-keepattributes SourceFile,LineNumberTable
