package com.afternote.core.data.di

import com.afternote.core.data.repoimpl.UserProfileRepositoryImpl
import com.afternote.core.data.repoimpl.UserRepositoryImpl
import com.afternote.core.data.repoimpl.auth.AuthRepositoryImpl
import com.afternote.core.domain.repository.MyAccountRepository
import com.afternote.core.domain.repository.MyProfileRepository
import com.afternote.core.domain.repository.MyPushSettingRepository
import com.afternote.core.domain.repository.UserProfileRepository
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

    // 책임별 좁은 계약 4종 (#1282). `UserRepositoryImpl` 을 다시 요청하면 바인딩마다 별도
    // 인스턴스가 생기므로(클래스가 아니라 위 @Binds 가 @Singleton), 이미 싱글턴인
    // [bindUserRepository] 바인딩을 경유해 전부 같은 인스턴스로 위임한다.
    @Binds
    internal abstract fun bindUserReceiverRepository(impl: UserRepository): UserReceiverRepository

    @Binds
    internal abstract fun bindMyProfileRepository(impl: UserRepository): MyProfileRepository

    @Binds
    internal abstract fun bindMyAccountRepository(impl: UserRepository): MyAccountRepository

    @Binds
    internal abstract fun bindMyPushSettingRepository(impl: UserRepository): MyPushSettingRepository

    @Binds
    @Singleton
    internal abstract fun bindUserProfileRepository(impl: UserProfileRepositoryImpl): UserProfileRepository
}
