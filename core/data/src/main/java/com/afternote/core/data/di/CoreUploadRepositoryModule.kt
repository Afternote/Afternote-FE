package com.afternote.core.data.di

import com.afternote.core.data.repoimpl.PhotoUploadRepositoryImpl
import com.afternote.core.data.repoimpl.VideoUploadRepositoryImpl
import com.afternote.core.domain.repository.PhotoUploadRepository
import com.afternote.core.domain.repository.VideoUploadRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** 구현체와 같은 모듈에 있으므로 `internal` 로 닫는다 — 바깥에서 impl 을 만질 수 없다. */
@InstallIn(SingletonComponent::class)
@Module
internal interface CoreUploadRepositoryModule {
    @Binds
    @Singleton
    fun bindPhotoUploadRepository(impl: PhotoUploadRepositoryImpl): PhotoUploadRepository

    @Binds
    @Singleton
    fun bindVideoUploadRepository(impl: VideoUploadRepositoryImpl): VideoUploadRepository
}
