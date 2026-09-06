package com.afternote.afternote_fe.test

import android.util.Log
import com.afternote.core.network.interceptor.OptionalDebugNetworkInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Interceptor
import java.io.IOException
import javax.inject.Singleton

/**
 * 계측 그래프가 실서버로 나가는 것을 막고, 나간 시도를 logcat 에 남긴다.
 *
 * 왜 있나 — `core/network` 의 `BASE_URL` 은 buildType 과 무관하게 dev 서버 하나로 못박혀 있고,
 * CI 의 `AFTERNOTE_CI_CONFIG_MODE=stub` 도 이 값은 건드리지 않는다. 그래서 `app` androidTest 의
 * Hilt 그래프에 fake 로 대체되지 않은 Retrofit 저장소가 남아 있고 어떤 테스트가 그 화면에 닿으면,
 * CI 가 조용히 dev 서버로 실제 HTTP 를 보낸다. dev 서버는 03~12시(KST) 닫히므로 증상이 시각에
 * 따라 달라져, 실패가 나도 원인이 코드로 보이지 않는다. 같은 사고가 두 번 났다(#1288, #1618).
 *
 * 왜 «검출» 이 아니라 «차단» 인가 — 이 파일을 넣기 전 api30 실측에서 타임레터 탭이
 * `GET /api/v1/time-letters` 를 3번 보냈는데도 36개 테스트가 전부 초록이었다. 누수는 테스트를
 * 깨지 않으므로 검출 장치만으로는 아무도 알아채지 못한다. 반대로 여기서 끊으면 누수가 남아 있어도
 * 실서버로 나가지 못해, 시각에 따라 달라지는 실패 자체가 성립하지 않는다.
 *
 * 무엇을 하나 — `MainClient` 를 타는 모든 요청을 네트워크로 나가기 전에 끊고 [IOException] 을
 * 던진다. 던지는 자리가 인터셉터라 앱에는 "네트워크 실패" 로 보인다. 저장소들은 이미 그 경로를
 * 다루므로 프로세스가 죽지 않는다 — `FakeX.strict()` 로 닫았을 때처럼 호출자에서 터지지 않는다
 * (#1288 이 그 사고다). 이 파일을 넣은 뒤에도 36개 테스트가 그대로 전부 통과하는 것으로 확인했다.
 *
 * 어디까지 보나 — `Retrofit` 은 `@Named("MainClient")` 하나로 만들어지고 이 묶음은 그 클라이언트에만
 * 붙는다. 즉 서버 계약을 타는 모든 api service 호출이 여기를 지난다. 이미지 로딩은 지나지 않는다
 * (계측에서는 `GlobalApplication` 이 `HiltTestApplication` 으로 대체돼 Coil 이 `@Named("CoilImage")`
 * 클라이언트조차 쓰지 않고 자체 기본 로더로 간다). S3 업로드도 `@Named("S3Upload")` 라 지나지 않는다.
 * 둘 다 서버 계약이 아닌 외부 호스트를 향하고, S3 는 presigned URL 을 받는 선행 호출이 여기 걸리며,
 * 이미지 URL 은 응답 본문에서 오는데 그 응답 자체가 여기서 끊긴다. 계약 누수 판정에는 구멍이 없다.
 *
 * 새 누수가 생기면 — [TAG] 로 URL 이 logcat 에 남는다. GMD 산출물
 * `app/build/outputs/androidTest-results/managedDevice/<device>/logcat-*.txt` 를 이 태그로 훑으면
 * 어느 테스트가 무엇을 불렀는지 그대로 나온다. 처방은 그 저장소를 바인딩하는 모듈을
 * [TestHiltModules] 에서 `@TestInstallIn` 으로 대체하는 것이다.
 */
@Module
@InstallIn(SingletonComponent::class)
object TestRealBackendCallGuardModule {
    private const val TAG = "RealBackendCallGuard"

    @Provides
    @Singleton
    @IntoSet
    @OptionalDebugNetworkInterceptor
    fun provideRealBackendCallGuardInterceptor(): Interceptor =
        Interceptor { chain ->
            val url = chain.request().url.toString()
            Log.e(TAG, "계측 그래프가 실서버로 나갔다: $url")
            throw IOException("계측 테스트는 실서버로 나갈 수 없다: $url")
        }
}
