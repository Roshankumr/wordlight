# Keep kotlinx.serialization generated serializers for our @Serializable classes
# (navigation routes + level DTOs).
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers @kotlinx.serialization.Serializable class com.wordlight.game.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.wordlight.game.**$$serializer { *; }
-keepclassmembers class com.wordlight.game.** {
    *** Companion;
}
