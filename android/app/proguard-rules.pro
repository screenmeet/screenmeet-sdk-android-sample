-keep class com.screenmeet.sdk.** { *; }
-keep class org.webrtc.** { *; }
-keep class org.mediasoup.** { *; }

-keepattributes InnerClasses
-keepclasseswithmembers class com.camerakit.preview.CameraSurfaceView {
    native <methods>;
}

-dontwarn org.webrtc.**
-dontwarn com.google.android.gms.**
-dontwarn org.bouncycastle.jsse.*
-dontwarn org.bouncycastle.jsse.provider.*
-dontwarn org.conscrypt.*
-dontwarn org.openjsse.javax.net.ssl.*
-dontwarn org.openjsse.net.ssl.*

 -keep,allowobfuscation,allowshrinking interface retrofit2.Call
 -keep,allowobfuscation,allowshrinking class retrofit2.Response

 -keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation