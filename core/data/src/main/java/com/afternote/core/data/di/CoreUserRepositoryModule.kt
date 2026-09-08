package com.afternote.core.data.di

import com.afternote.core.data.repoimpl.MyProfileRepositoryImpl
import com.afternote.core.data.repoimpl.UserProfileCacheRepositoryImpl
import com.afternote.core.data.repoimpl.UserReceiverRepositoryImpl
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

    // 책임별 좁은 계약 2종 (#1282). 상태를 가진 `UserReceiverRepositoryImpl` 은 클래스에 @Singleton 이 있어
    // 합본 `UserRepositoryImpl` 이 생성자로 받는 인스턴스와 여기서 바인딩되는 인스턴스가 같다 — 좁은 계약으로
    // 만든 수신자가 합본 구독자의 목록도 갱신한다. `MyProfileRepositoryImpl` 은 상태가 없어 unscoped 다.
    @Binds
    internal abstract fun bindUserReceiverRepository(impl: UserReceiverRepositoryImpl): UserReceiverRepository

    @Binds
    internal abstract fun bindMyProfileRepository(impl: MyProfileRepositoryImpl): MyProfileRepository

    @Binds
    @Singleton
    internal abstract fun bindUserProfileCacheRepository(impl: UserProfileCacheRepositoryImpl): UserProfileCacheRepository
}
