package com.afternote.core.data.di

import com.afternote.core.data.repoimpl.auth.PasskeyRepositoryImpl
import com.afternote.core.domain.repository.auth.PasskeyRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 패스키 로그인 저장소 바인딩.
 *
 * 이 모듈이 `public` 인 이유 — `app` 의 androidTest 가
 * `@TestInstallIn(replaces = [CorePasskeyRepositoryModule::class])` 로 이 바인딩을 페이크로 갈아끼운다.
 * `replaces` 는 모듈 클래스를 참조하므로 `internal` 로 닫을 수 없다.
 *
 * 그래서 형제 모듈들과 달리 `interface` 가 아니라 `abstract class` 다. 바인딩 메서드가 `public` 이면
 * `'public' function exposes its 'internal' parameter type` 으로 컴파일되지 않는데, interface 멤버에는
 * 가시성을 줄 수 없다. abstract class 라야 클래스는 열어 두고 메서드만 `internal` 로 닫는다.
 *
 * 갈아끼우지 않으면 계측이 실 Retrofit 을 지난다 — 로그인 화면 진입만으로
 * `LoginEntry` 의 `LaunchedEffect` 가 `auth/passkey/authenticate/options` 를 실제로 호출하고,
 * 성공하면 기기의 Credential Manager 선택기까지 부른다 (#764).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CorePasskeyRepositoryModule {
    @Binds
    @Singleton
    internal abstract fun bindPasskeyRepository(impl: PasskeyRepositoryImpl): PasskeyRepository
}
