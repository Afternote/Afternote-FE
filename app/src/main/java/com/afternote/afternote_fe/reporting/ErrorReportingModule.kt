package com.afternote.afternote_fe.reporting

import com.afternote.core.common.reporting.ErrorReporter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * handled 실패 리포팅 바인딩.
 *
 * 다른 모듈은 [ErrorReporter] 추상만 주입받고, 실제 Crashlytics 결선은 여기(app)에서만 일어난다.
 * 추상을 쓰는 쪽이 구현이 아니라 계약에만 의존하게 하는 배치로, 리포팅 도구를 갈아끼워도
 * 바뀌는 범위가 이 모듈 안에 갇힌다.
 * 근거: https://developer.android.com/topic/modularization/patterns
 *
 * `@Binds` 만 두고 `@Provides` 는 섞지 않는다 — 같은 모듈에 인스턴스 `@Provides` 를 두면
 * Dagger 가 거부한다(`@Binds` 는 구현체가 만들어지지 않는 선언이라 호출 대상이 없다).
 * 둘 다 필요해지면 모듈을 나눠 `includes` 로 엮는 것이 공식 해법이다.
 * 근거: https://dagger.dev/dev-guide/faq.html
 */
@InstallIn(SingletonComponent::class)
@Module
interface ErrorReportingModule {
    /**
     * 스코프는 구현 클래스가 아니라 이 바인딩에 건다 — 주입받는 쪽은 항상 [ErrorReporter] 타입이라
     * 여기에 걸어야 실효가 있고, 양쪽에 걸면 같은 객체를 두 번 스코프하는 셈이 된다.
     * `@Singleton` 이 없으면 요청할 때마다 새 인스턴스가 만들어진다(Hilt 기본은 unscoped).
     * 근거: https://developer.android.com/training/dependency-injection/hilt-android
     */
    @Binds
    @Singleton
    fun bindErrorReporter(impl: CrashlyticsErrorReporter): ErrorReporter
}
