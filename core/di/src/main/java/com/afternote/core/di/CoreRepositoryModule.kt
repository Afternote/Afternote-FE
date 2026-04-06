package com.afternote.core.di

@InstallIn(SingletonComponent::class)
@Module
interface CoreRepositoryModule {
    @Binds
    @Singleton
    fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    companion object {
        @Provides
        @Singleton
        fun provideKakaoTokenManageable(): TokenManageable = TokenManagerProvider.instance.manager
    }
}
