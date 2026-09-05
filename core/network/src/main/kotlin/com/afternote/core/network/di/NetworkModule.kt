package com.afternote.core.network.di

import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.afternote.core.network.BuildConfig
import com.afternote.core.network.calladapter.ApiErrorCallAdapterFactory
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
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
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

/**
 * 주간 리포트(`GET /api/v1/mind-record`) 전용 여유.
 *
 * 이 엔드포인트만 유독 느리다 — 같은 계정·같은 시각에 일기 목록이 4.0초인데 이쪽은
 * 16.9 · 20.2 · 25.9초였다(실측 2026-08-25). 10초 read timeout 에 매번 걸려 주간리포트
 * 탭이 **항상** 실패 화면으로 떴고, 재시도도 같은 자리에서 끊겨 복구 수단이 없었다 (#1122).
 *
 * 전역 상한을 올리지 않는 이유: 다른 화면의 느린 실패를 늦게 알게 된다. 느린 것이 확인된
 * 경로에만 여유를 주는 것은 업로드 경로가 이미 쓰는 방식이다.
 *
 * **증상 완화이지 해결이 아니다** — 응답 시간 자체는 BE 몫으로 #1122 에 남겼다.
 */
private const val SLOW_ENDPOINT_IO_TIMEOUT_SECONDS = 60L
private const val SLOW_ENDPOINT_CALL_TIMEOUT_SECONDS = 90L

private val SLOW_ENDPOINT_PATHS = setOf("/api/v1/mind-record")

/** 업로드는 본문 크기에 비례해 길어진다 — 일반 호출과 같은 상한을 걸면 큰 파일이 전송 도중 끊긴다. */
private const val UPLOAD_IO_TIMEOUT_SECONDS = 60L
private const val UPLOAD_CALL_TIMEOUT_SECONDS = 10 * 60L

private fun OkHttpClient.Builder.withApiTimeouts(): OkHttpClient.Builder =
    connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(IO_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(IO_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)

/**
 * 느린 것이 확인된 엔드포인트만 여유 있는 파생 클라이언트로 보낸다 (#1122).
 *
 * 인터셉터로는 `callTimeout` 을 못 늘린다 — read 만 늘려도 30초 호출 상한에 다시 걸린다.
 * 그래서 클라이언트를 파생시키고 요청 경로로 갈라 태운다. 커넥션 풀·디스패처는 원본과
 * 공유되므로 소켓이 두 벌 생기지 않는다.
 *
 * BE 응답이 정상 범위로 돌아오면 이 팩토리째로 지운다.
 */
internal class SlowEndpointCallFactory(
    private val default: OkHttpClient,
) : Call.Factory {
    private val slow: OkHttpClient =
        default
            .newBuilder()
            .readTimeout(SLOW_ENDPOINT_IO_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(SLOW_ENDPOINT_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

    override fun newCall(request: Request): Call =
        if (request.url.encodedPath in SLOW_ENDPOINT_PATHS) {
            slow.newCall(request)
        } else {
            default.newCall(request)
        }
}

@Module // 힐트야 여기 타입별로 객체 어떻게 만드는지 적어 놓은 설명서야
@InstallIn(SingletonComponent::class) // 앱 자체와 수명을 함께하는 창고에 이 설명서의 객체들을 보관해 줘
object NetworkModule { // 이 모듈은 오브젝트 클래스 선언해서 딱 하나만 만들게
    @Provides // Json 타입을 보면 이 함수로 객체를 만들어 제공해 줘
    @Singleton // 객체를 한 번만 만들어서 계속 그걸 써
    fun provideJson(): Json =
        Json {
            ignoreUnknownKeys = true // dto로 정의되지 않은 키가 있어도 크래시 나지 않게 함
            // coerceInputValues 는 두지 않는다. non-null 프로퍼티에 null 이 와도 기본값이 있으면 조용히
            // 치환해 계약 위반을 삼킨다 — 이 팀 기준은 «제외하더라도 세어서 보고» 다(#1010).
            // 응답 DTO 에 보정형 기본값을 새로 두는 것은 ResponseDtoContractKonsistTest 가 막는다.
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

    /**
     * 모든 클라이언트의 공통 뿌리. 파생은 [OkHttpClient.newBuilder] 로 — 설정값만이 아니라
     * ConnectionPool · Dispatcher · 스레드풀을 실제로 공유한다.
     * 인터셉터는 여기 두지 않는다: 로깅은 각 클라이언트가 마지막에 달아야 최종 요청·응답을 관찰하는데,
     * base 에 두면 파생 시 맨 앞으로 밀린다.
     */
    @Provides
    @Singleton
    @Named("BaseClient")
    fun provideBaseOkHttpClient(): OkHttpClient = OkHttpClient.Builder().withApiTimeouts().build()

    // 리이슈를 할 때 일반용 OkhttpClient만 사용하면 액세스 토큰이 계속 헤더에 포함되어 401을 받는 행위가 무한 반복
    // 이를 해결하기 위해 401을 받았을 때는 토큰을 헤더에 포함하지 않고 요청을 보내는 버전
    @Provides
    @Singleton
    @Named("RefreshClient")
    fun provideRefreshOkHttpClient(
        @Named("BaseClient") baseClient: OkHttpClient,
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient =
        baseClient
            .newBuilder()
            .addInterceptor(loggingInterceptor)
            .build()

    @Provides
    @Singleton
    @Named("MainClient")
    fun provideMainOkHttpClient(
        @Named("BaseClient") baseClient: OkHttpClient,
        @OptionalDebugNetworkInterceptor debugInterceptors: Set<@JvmSuppressWildcards Interceptor>,
        @FeatureNetworkInterceptor featureInterceptors: Set<@JvmSuppressWildcards Interceptor>,
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient {
        val builder = baseClient.newBuilder()
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
    fun provideS3UploadOkHttpClient(
        @Named("BaseClient") baseClient: OkHttpClient,
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient =
        baseClient
            .newBuilder()
            .readTimeout(UPLOAD_IO_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(UPLOAD_IO_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(UPLOAD_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()

    /**
     * Coil 전용. 인증 헤더가 붙지 않는 이미지 호스트만 다룬다.
     * Coil 기본 로더를 그냥 쓰지 않는 이유는 이 두 가지뿐 — base 파생이라 커넥션 풀·디스패처를
     * 공유하면서, 이미지 요청에도 디버그 로깅과 호출 상한([CALL_TIMEOUT_SECONDS])이 걸린다.
     */
    @Provides
    @Singleton
    @Named("CoilImage")
    fun provideCoilImageOkHttpClient(
        @Named("BaseClient") baseClient: OkHttpClient,
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient =
        baseClient
            .newBuilder()
            .addInterceptor(loggingInterceptor)
            .build()

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
        apiErrorCallAdapterFactory: ApiErrorCallAdapterFactory,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(BuildConfig.BASE_URL)
            // 느린 것이 확인된 경로만 여유 있는 파생 클라이언트로 태운다 (#1122).
            .callFactory(SlowEndpointCallFactory(okHttpClient))
            // HTTP 응답을 받은 뒤 400..599 본문을 ApiException 으로 변환한다.
            .addCallAdapterFactory(apiErrorCallAdapterFactory)
            // json과 코틀린의 dto 데이터 클래스 타입 간 번역기
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}
