package com.afternote.feature.mindrecord.data.di

import com.afternote.feature.mindrecord.data.api.DailyQuestionApiService
import com.afternote.feature.mindrecord.data.api.DiaryApiService
import com.afternote.feature.mindrecord.data.api.MindRecordReceiverApiService
import com.afternote.feature.mindrecord.data.api.WeeklyReportApiService
import com.afternote.feature.mindrecord.domain.sync.MindRecordChangeTracker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MindRecordServiceModule {
    /** 자동 갱신을 데이터 변경 기준으로 좁히기 위한 전역 버전 (#736). */
    @Provides
    @Singleton
    fun provideMindRecordChangeTracker(): MindRecordChangeTracker = MindRecordChangeTracker()

    @Provides
    @Singleton
    fun provideDailyQuestionApiService(retrofit: Retrofit): DailyQuestionApiService = retrofit.create<DailyQuestionApiService>()

    @Provides
    @Singleton
    fun provideDiaryApiService(retrofit: Retrofit): DiaryApiService = retrofit.create<DiaryApiService>()

    @Provides
    @Singleton
    fun provideMindRecordReceiverApiService(retrofit: Retrofit): MindRecordReceiverApiService =
        retrofit.create<MindRecordReceiverApiService>()

    @Provides
    @Singleton
    fun provideWeeklyReportApiService(retrofit: Retrofit): WeeklyReportApiService = retrofit.create<WeeklyReportApiService>()
}
