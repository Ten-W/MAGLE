# First optimized release: keep names stable while removing unused code/resources.
-dontobfuscate

# Optional compile-time annotations and SLF4J binding (falls back to its NOP logger).
-dontwarn aQute.bnd.annotation.spi.ServiceProvider
-dontwarn edu.umd.cs.findbugs.annotations.SuppressFBWarnings
-dontwarn org.slf4j.impl.StaticLoggerBinder

# StAX implementation selected through a system property, not a direct constructor.
-keep class com.ctc.wstx.stax.WstxInputFactory { public <init>(); }

# MinIO's XML model is populated reflectively by Simple XML.
-keep class io.minio.messages.** { *; }
-keepclassmembers class * {
    @org.simpleframework.xml.* <fields>;
    @org.simpleframework.xml.* <methods>;
}
