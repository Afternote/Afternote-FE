package com.afternote.core.network.di

import coil3.ImageLoader
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CoilImageLoaderEntryPoint {
    fun imageLoader(): ImageLoader
}
