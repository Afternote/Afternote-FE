package com.afternote.feature.afternote.data.di

import com.afternote.feature.afternote.data.repositoryimpl.AfternoteRepositoryImpl
import com.afternote.feature.afternote.data.repositoryimpl.MemorialThumbnailUploadRepositoryImpl
import com.afternote.feature.afternote.data.repositoryimpl.MusicSearchRepositoryImpl
import com.afternote.feature.afternote.domain.repository.AfternoteRepository
import com.afternote.feature.afternote.domain.repository.MemorialThumbnailUploadRepository
import com.afternote.feature.afternote.domain.repository.MemorialVideoUploadRepository
import com.afternote.feature.afternote.domain.repository.MusicSearchRepository
import com.kuit.afternote.data.repositoryimpl.MemorialVideoUploadRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Afternote Repository + ApiService DI 설정.
 *
 * - AfternoteRepositoryImpl ↔ AfternoteRepository
 * - MusicSearchRepositoryImpl ↔ MusicSearchRepository
 * - Retrofit → AfternoteApiService, MusicApiService
 */
@Module
@InstallIn(SingletonComponent::class)
interface AfternoteRepositoryModule {
    @Suppress("unused")
    @Binds
    @Singleton
    fun bindAfternoteRepository(impl: AfternoteRepositoryImpl): AfternoteRepository

    @Suppress("unused")
    @Binds
    @Singleton
    fun bindMusicSearchRepository(impl: MusicSearchRepositoryImpl): MusicSearchRepository

    @Suppress("unused")
    @Binds
    @Singleton
    fun bindMemorialThumbnailUploadRepository(impl: MemorialThumbnailUploadRepositoryImpl): MemorialThumbnailUploadRepository

    @Suppress("unused")
    @Binds
    @Singleton
    fun bindMemorialVideoUploadRepository(impl: MemorialVideoUploadRepositoryImpl): MemorialVideoUploadRepository
}
