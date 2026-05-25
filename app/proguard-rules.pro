# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Room — RoomDatabase 서브클래스가 reflection 으로 인스턴스화될 때
# Class.canonicalName 매칭이 필요해서 obfuscate 되면 안 됨.
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Database class *
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# androidx.work — WorkDatabase 가 Room 기반이고 InitializationProvider 가 부팅 시 인스턴스화.
-keep class androidx.work.impl.WorkDatabase
-keep class androidx.work.impl.WorkDatabase_Impl
-keep class androidx.work.impl.** { *; }

# androidx.startup — Initializer reflection 기반 호출.
-keep class androidx.startup.InitializationProvider { *; }
-keep class * extends androidx.startup.Initializer

# Kakao SDK — AccessTokenInterceptor 가 ClientErrorCause.TokenNotFound 등
# enum entry 를 Class.getField 로 접근. SDK 의 consumer-rules 가 enum 까지
# 보호하지 않아 NoSuchFieldException 발생.
-keep class com.kakao.sdk.**.model.** { *; }
-keep enum com.kakao.sdk.** { *; }