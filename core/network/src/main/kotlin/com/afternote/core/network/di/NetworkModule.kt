package com.afternote.core.network.di

import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.afternote.core.network.BuildConfig
import com.afternote.core.network.interceptor.ApiErrorInterceptor
import com.afternote.core.network.interceptor.AuthInterceptor
import com.afternote.core.network.interceptor.FeatureNetworkInterceptor
import com.afternote.core.network.interceptor.OptionalDebugNetworkInterceptor
import com.afternote.core.network.interceptor.TokenAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

private const val CONNECT_TIMEOUT_SECONDS = 10L
private const val IO_TIMEOUT_SECONDS = 10L

/**
 * 한 호출의 전체 상한. OkHttp 기본값 0(무제한)에서는 read timeout 이 바이트 사이 간격에만 걸려,
 * 서버가 조금씩 흘려보내는 동안 요청이 끝나지 않고 화면도 로딩에서 못 벗어난다.
 */
private const val CALL_TIMEOUT_SECONDS = 30L

/** 업로드는 본문 크기에 비례해 길어진다 — 일반 호출과 같은 상한을 걸면 큰 파일이 전송 도중 끊긴다. */
private const val UPLOAD_IO_TIMEOUT_SECONDS = 60L
private const val UPLOAD_CALL_TIMEOUT_SECONDS = 10 * 60L

private fun OkHttpClient.Builder.withApiTimeouts(): OkHttpClient.Builder =
    connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(IO_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(IO_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)

@Module // 힐트야 여기 타입별로 객체 어떻게 만드는지 적어 놓은 설명서야
@InstallIn(SingletonComponent::class) // 앱 자체와 수명을 함께하는 창고에 이 설명서의 객체들을 보관해 줘
object NetworkModule { // 이 모듈은 오브젝트 클래스 선언해서 딱 하나만 만들게
    @Provides // Json 타입을 보면 이 함수로 객체를 만들어 제공해 줘
    @Singleton // 객체를 한 번만 만들어서 계속 그걸 써
    fun provideJson(): Json =
        Json {
            ignoreUnknownKeys = true // dto로 정의되지 않은 키가 있어도 크래시 나지 않게 함
            coerceInputValues =
                true // 응답 json으로 유효하지 않은 필드 값이 오더라도 대응되는 dto 변수의 디폴트 값이 있다면 그 값으로 치환
            // useAlternativeNames 는 기본 true — DTO 프로퍼티에 @JsonNames("대체키") 를 달면
            // 서버가 같은 필드를 다른 키로 내려줘도(명세·실서버 불일치, 키 리네임 과도기) 한 프로퍼티로 파싱된다.
            // 디코딩 전용이라 요청 직렬화 출력 키는 항상 @SerialName 을 따름. 사용 예: mindrecord DailyQuestionListItem
        }

    // 로깅 설정 인터셉터
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level =
                if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            redactHeader("Authorization")
        }

    // 리이슈를 할 때 일반용 OkhttpClient만 사용하면 액세스 토큰이 계속 헤더에 포함되어 401을 받는 행위가 무한 반복
    // 이를 해결하기 위해 401을 받았을 때는 토큰을 헤더에 포함하지 않고 요청을 보내는 버전
    @Provides
    @Singleton
    @Named("RefreshClient")
    fun provideRefreshOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient
            .Builder() // 이제부터 인터셉터를 담을게
            .withApiTimeouts()
            .addInterceptor(loggingInterceptor)
            .build() // 인터셉터 다 담았어

    @Provides
    @Singleton
    @Named("MainClient")
    fun provideMainOkHttpClient(
        @OptionalDebugNetworkInterceptor debugInterceptors: Set<@JvmSuppressWildcards Interceptor>,
        @FeatureNetworkInterceptor featureInterceptors: Set<@JvmSuppressWildcards Interceptor>,
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
        apiErrorInterceptor: ApiErrorInterceptor,
    ): OkHttpClient {
        val builder = OkHttpClient.Builder().withApiTimeouts()
        // 가장 외곽(응답 마지막) — 4xx/5xx 본문의 message 를 파싱해 ApiException 으로 변환
        builder.addInterceptor(apiErrorInterceptor)
        // 디버그 전용(피처 모듈) 인터셉터: 네트워크로 나가기 전에 가짜 응답을 반환할 수 있음
        debugInterceptors.forEach { builder.addInterceptor(it) }
        // 피처 모듈이 제공하는 프로덕션 인터셉터(예: 수신자 X-Auth-Code 자동 부착)
        featureInterceptors.forEach { builder.addInterceptor(it) }
        return builder
            // 요청은 인터셉터를 추가한 순서대로 인터셉터를 거쳐 서버로 가고 응답은 그 반대 순서로
            .addInterceptor(authInterceptor) // 액세스 토큰을 리퀘스트 헤더에 달아 주는 인터셉터
            // 액세스 토큰이 필요한 서비스가 많기 때문에 요청 필드로 일일이 보내는 대신 모든 요청의 헤더로 담는다
            .addInterceptor(loggingInterceptor)
            .authenticator(tokenAuthenticator)
            // 401 응답을 받았을 때 응답이 앱 쪽으로 넘어가기 전에 낚아채 곧바로 요청을 다시 보내는 투명한 재시도
            // 요청을 다시 보낼 때 다른 요청처럼 인터셉터를 거침
            .build()
    }

    // S3에 우리 앱의 액세스 토큰이 헤더로 전달되면 400/403이 뜨기 때문에 토큰 없는 순수한 클라이언트 필요
    @Provides
    @Singleton
    @Named("S3Upload")
    fun provideS3UploadOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(UPLOAD_IO_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(UPLOAD_IO_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(UPLOAD_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()

    /** Coil 전용. 인증 헤더 없이 이미지 호스트만 다루며, DEBUG 멀티바인딩 인터셉터(예: mock.image → Picsum)를 동일하게 적용한다.
     *  모든 이미지 요청에 동일한 `User-Agent` 를 부착해 presentation 의 ImageRequest 별 부착 boilerplate 를 제거한다. */
    @Provides
    @Singleton
    @Named("CoilImage")
    fun provideCoilImageOkHttpClient(
        @OptionalDebugNetworkInterceptor debugInterceptors: Set<@JvmSuppressWildcards Interceptor>,
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient {
        val builder = OkHttpClient.Builder().withApiTimeouts()
        builder.addInterceptor { chain ->
            chain.proceed(
                chain
                    .request()
                    .newBuilder()
                    .header("User-Agent", "Afternote Android App")
                    .build(),
            )
        }
        debugInterceptors.forEach { builder.addInterceptor(it) }
        return builder.addInterceptor(loggingInterceptor).build()
    }

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        @Named("CoilImage") coilOkHttpClient: OkHttpClient,
    ): ImageLoader =
        ImageLoader
            .Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = coilOkHttpClient))
            }.build()

    @Provides
    @Singleton
    fun provideRetrofit(
        @Named("MainClient") okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            // json과 코틀린의 dto 데이터 클래스 타입 간 번역기
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}
