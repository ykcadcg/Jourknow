# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\kyang\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-keep class com.iflytek.**{*;}
-keep class com.github.mikephil.charting.** { *; }
-keep class com.android.vending.billing.**
-dontwarn com.ibm.watson.**
-keep class com.ibm.watson.** { *;}


-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** w(...);
    public static *** v(...);
    public static *** i(...);
}
-dontwarn java.awt.**

#below is from leancloud https://leancloud.cn/docs/android_faq.html
-dontwarn com.alibaba.fastjson.**
-keep class com.alibaba.fastjson.** { *;}
-dontwarn com.avos.**
-keep class com.avos.** { *;}
-dontwarn okio.**


#other, making build much faster:
-keepattributes Signature
-dontwarn com.jcraft.jzlib.**
-keep class com.jcraft.jzlib.**  { *;}

-dontwarn sun.misc.**
-keep class sun.misc.** { *;}

-dontwarn sun.security.**
-keep class sun.security.** { *; }

-dontwarn com.google.**
-keep class com.google.** { *;}

-keep public class android.net.http.SslError
-keep public class android.webkit.WebViewClient

-dontwarn android.webkit.WebView
-dontwarn android.net.http.SslError
-dontwarn android.webkit.WebViewClient

-dontwarn android.support.**

-dontwarn org.apache.**
-keep class org.apache.** { *;}

-dontwarn org.jivesoftware.smack.**
-keep class org.jivesoftware.smack.** { *;}

-dontwarn com.loopj.**
-keep class com.loopj.** { *;}

-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

-dontwarn org.xbill.**
-keep class org.xbill.** { *;}

-keepattributes *Annotation*