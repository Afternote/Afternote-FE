package com.afternote.core.data.di

import com.afternote.core.data.repoimpl.account.AccountRepositoryImpl
import com.afternote.core.domain.repository.account.AccountRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** 구현체와 같은 모듈에 있으므로 `internal` 로 닫는다 — 바깥에서 impl 을 만질 수 없다. */
@InstallIn(SingletonComponent::class)
@Module
internal interface CoreAccountRepositoryModule {
    @Binds
    @Singleton
    fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository
}
