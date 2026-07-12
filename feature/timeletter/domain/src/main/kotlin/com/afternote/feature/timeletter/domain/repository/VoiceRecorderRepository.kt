package com.afternote.feature.timeletter.domain.repository

import com.afternote.feature.timeletter.domain.model.RecordedAudio

interface VoiceRecorderRepository {
    suspend fun start(): Result<Unit>

    suspend fun stop(): Result<RecordedAudio>

    suspend fun discard()

    fun retainRecordedFile()

    fun release()
}
