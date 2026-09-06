package com.afternote.feature.afternote.data.di

import com.afternote.feature.afternote.data.repositoryimpl.author.AfternoteRepositoryImpl
import com.afternote.feature.afternote.data.repositoryimpl.author.MemorialAudioUploadRepositoryImpl
import com.afternote.feature.afternote.data.repositoryimpl.author.MemorialMediaUploadRepositoryImpl
import com.afternote.feature.afternote.data.repositoryimpl.author.MemorialThumbnailUploadRepositoryImpl
import com.afternote.feature.afternote.data.repositoryimpl.author.MusicSearchRepositoryImpl
import com.afternote.feature.afternote.domain.repository.author.AfternoteRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialAudioUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialMediaUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialThumbnailUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MusicSearchRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface AfternoteAuthorRepositoryModule {
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
    fun bindMemorialMediaUploadRepository(impl: MemorialMediaUploadRepositoryImpl): MemorialMediaUploadRepository

    @Suppress("unused")
    @Binds
    @Singleton
    fun bindMemorialAudioUploadRepository(impl: MemorialAudioUploadRepositoryImpl): MemorialAudioUploadRepository
}
