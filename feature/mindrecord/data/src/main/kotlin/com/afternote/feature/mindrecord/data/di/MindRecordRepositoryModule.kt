package com.afternote.feature.mindrecord.data.di

import com.afternote.feature.mindrecord.data.repositoryimpl.DailyQuestionRepositoryImpl
import com.afternote.feature.mindrecord.data.repositoryimpl.DeepThoughtRepositoryImpl
import com.afternote.feature.mindrecord.data.repositoryimpl.DiaryRepositoryImpl
import com.afternote.feature.mindrecord.data.repositoryimpl.MindRecordReceiverRepositoryImpl
import com.afternote.feature.mindrecord.data.repositoryimpl.WeeklyReportRepositoryImpl
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.domain.repository.DeepThoughtRepository
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import com.afternote.feature.mindrecord.domain.repository.MindRecordReceiverRepository
import com.afternote.feature.mindrecord.domain.repository.WeeklyReportRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface MindRecordRepositoryModule {
    @Binds
    @Singleton
    fun bindDailyQuestionRepository(impl: DailyQuestionRepositoryImpl): DailyQuestionRepository

    @Binds
    @Singleton
    fun bindDiaryRepository(impl: DiaryRepositoryImpl): DiaryRepository

    @Binds
    @Singleton
    fun bindDeepThoughtRepository(impl: DeepThoughtRepositoryImpl): DeepThoughtRepository

    @Binds
    @Singleton
    fun bindMindRecordReceiverRepository(impl: MindRecordReceiverRepositoryImpl): MindRecordReceiverRepository

    @Binds
    @Singleton
    fun bindWeeklyReportRepository(impl: WeeklyReportRepositoryImpl): WeeklyReportRepository
}
