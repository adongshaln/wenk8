# kotlinx serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class io.wenk8.app.**$$serializer { *; }
-keepclassmembers class io.wenk8.app.** {
    *** Companion;
}
-keepclasseswithmembers class io.wenk8.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
