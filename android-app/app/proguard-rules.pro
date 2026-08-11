-keepattributes Signature
-keepattributes *Annotation*

-keep class com.hashibridge.master.** { *; }
-keep class org.nanohttpd.** { *; }
-keep class org.java_websocket.** { *; }
-keep class com.google.gson.** { *; }

-dontwarn javax.annotation.**
-dontwarn org.nanohttpd.**
-dontwarn org.java_websocket.**
