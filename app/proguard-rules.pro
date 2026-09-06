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

# release APK에 URL·상태·예외를 남기는 앱 Log 호출 제거.
# https://developer.android.com/topic/performance/app-optimization/additional-rule-types
-maximumremovedandroidloglevel 7 class com.afternote.** {
    <methods>;
}

# Hilt가 최적화 금지로 보존하는 ViewModel 안의 Log 호출도 release에서 제거.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
    public static int println(...);
}

# androidx.startup — Initializer reflection 기반 호출.
-keep class androidx.startup.InitializationProvider { *; }
-keep class * extends androidx.startup.Initializer

# Kakao SDK — AccessTokenInterceptor 가 ClientErrorCause.TokenNotFound 등
# enum entry 를 Class.getField 로 접근. SDK 의 consumer-rules 가 enum 까지
# 보호하지 않아 NoSuchFieldException 발생.
-keep class com.kakao.sdk.**.model.** { *; }
-keep enum com.kakao.sdk.** { *; }

# 도메인 예외 — Crashlytics 리포트에서 예외 타입을 식별하기 위해 이름만 유지.
# 스택 프레임은 mapping 으로 복원되지만 예외 타입 문자열에는 적용되지 않아,
# 난독화되면 이슈 제목이 `nq` 같은 축약명으로 떠 어떤 실패인지 알 수 없고
# 같은 지점의 실패가 서로 다른 이슈로 그룹핑된다.
# 이름만 필요하므로 멤버 난독화와 미사용 클래스 제거는 그대로 둔다.
-keepnames class com.afternote.** extends java.lang.Throwable

# 앱 enum — 이름을 «문자열로» 찾는 경로가 있어 이름만 유지.
# `@Serializable` navigation route 인자로 쓰인 enum 은 kotlinx.serialization 이 컴파일 시점
# FQCN 을 serialName 문자열로 바이트코드에 박고, navigation 이 그 문자열로 Class.forName 을
# 한다(값 파싱은 enum 상수의 이름 문자열 비교). R8 은 클래스·상수 이름만 바꾸고 그 문자열은
# 안 바꾸므로, 규칙이 없으면 release 빌드가 NavHost 조립에서 IllegalArgumentException 으로
# 죽는다 — 로그인 여부와 무관하게 모든 기동이 실패한다 (#1753).
# 지금 nav 인자로 쓰이는 enum 은 AfternoteType 하나지만 대상을 하나만 적으면 다음에 enum 인자를
# 추가하는 사람이 같은 크래시를 다시 만든다. 예외 타입 규칙과 같은 이유·같은 모양으로 앱 전체에 건다.
# 이름만 필요하므로 멤버 난독화와 미사용 클래스 제거는 그대로 둔다.
-keepnames class com.afternote.** extends java.lang.Enum
-keepclassmembernames class com.afternote.** extends java.lang.Enum {
    <fields>;
}
