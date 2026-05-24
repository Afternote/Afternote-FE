package com.afternote.feature.mindrecord.data.di

import com.afternote.feature.mindrecord.data.api.DailyQuestionApiService
import com.afternote.feature.mindrecord.data.api.DeepThoughtApiService
import com.afternote.feature.mindrecord.data.api.DiaryApiService
import com.afternote.feature.mindrecord.data.api.MindRecordReceiverApiService
import com.afternote.feature.mindrecord.data.api.WeeklyReportApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MindRecordServiceModule {
    @Provides
    @Singleton
    fun provideDailyQuestionApiService(retrofit: Retrofit): DailyQuestionApiService = retrofit.create(DailyQuestionApiService::class.java)

    @Provides
    @Singleton
    fun provideDiaryApiService(retrofit: Retrofit): DiaryApiService = retrofit.create(DiaryApiService::class.java)

    @Provides
    @Singleton
    fun provideDeepThoughtApiService(retrofit: Retrofit): DeepThoughtApiService = retrofit.create(DeepThoughtApiService::class.java)

    @Provides
    @Singleton
    fun provideMindRecordReceiverApiService(retrofit: Retrofit): MindRecordReceiverApiService =
        retrofit.create(MindRecordReceiverApiService::class.java)

    @Provides
    @Singleton
    fun provideWeeklyReportApiService(retrofit: Retrofit): WeeklyReportApiService = retrofit.create(WeeklyReportApiService::class.java)
}
