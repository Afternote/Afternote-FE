package com.afternote.core.data.di

import com.afternote.core.data.repoimpl.UserProfileCacheRepositoryImpl
import com.afternote.core.data.repoimpl.UserRepositoryImpl
import com.afternote.core.data.repoimpl.auth.AuthRepositoryImpl
import com.afternote.core.domain.repository.MyProfileRepository
import com.afternote.core.domain.repository.UserProfileCacheRepository
import com.afternote.core.domain.repository.UserReceiverRepository
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.domain.repository.auth.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 세션·사용자 저장소 바인딩.
 *
 * 이 모듈만 `public` 인 이유 — `app` 의 androidTest 가
 * `@TestInstallIn(replaces = [CoreUserRepositoryModule::class])` 로 이 셋을 페이크로 갈아끼운다.
 * `replaces` 는 모듈 클래스를 참조하므로 `internal` 로 닫을 수 없다.
 *
 * 그래서 형제 모듈들과 달리 `interface` 가 아니라 `abstract class` 다. 바인딩 메서드가 `public` 이면
 * `'public' function exposes its 'internal' parameter type` 으로 컴파일되지 않는데, interface 멤버에는
 * 가시성을 줄 수 없다. abstract class 라야 클래스는 열어 두고 메서드만 `internal` 로 닫는다.
 */
@InstallIn(SingletonComponent::class)
@Module
abstract class CoreUserRepositoryModule {
    @Binds
    @Singleton
    internal abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    internal abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    // 책임별 좁은 계약 2종 (#1282). 구현은 `UserReceiverRepositoryImpl`·`MyProfileRepositoryImpl` 로
    // 갈라져 있지만 바인딩은 그 둘이 아니라 [bindUserRepository] 를 경유한다 — 두 구현을 Hilt 가 따로
    // 만들면 `UserRepository` 로 등록한 수신자가 `UserReceiverRepository` 구독자의 목록을 갱신하지
    // 못하고 revision 이 갈린다. `UserRepositoryImpl` 을 다시 요청해도 바인딩마다 별도 인스턴스가
    // 생기므로(클래스가 아니라 위 @Binds 가 @Singleton), 이미 싱글턴인 그 바인딩으로 위임한다.
    // 소비자가 전부 좁은 계약으로 이관돼 `UserRepositoryImpl` 이 사라질 때 바인딩을 좁은 구현으로 옮긴다.
    @Binds
    internal abstract fun bindUserReceiverRepository(impl: UserRepository): UserReceiverRepository

    @Binds
    internal abstract fun bindMyProfileRepository(impl: UserRepository): MyProfileRepository

    @Binds
    @Singleton
    internal abstract fun bindUserProfileCacheRepository(impl: UserProfileCacheRepositoryImpl): UserProfileCacheRepository
}
