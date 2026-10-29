-keepattributes *Annotation*, InnerClasses

-dontnote kotlinx.serialization.**

-keepclassmembers class com.example.xiangqi.data.** {
    *** Companion;
}

-keepclasseswithmembers class com.example.xiangqi.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.example.xiangqi.data.**$$serializer { *; }
