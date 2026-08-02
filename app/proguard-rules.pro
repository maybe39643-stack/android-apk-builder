# Add project specific ProGuard rules here.
-keepattributes SourceFile,LineNumberTable
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Fragment
-dontwarn com.google.android.gms.ads.**
-keep class com.google.android.gms.ads.** { *; }
