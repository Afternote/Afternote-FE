package com.afternote.feature.timeletter.domain.testing

import com.afternote.feature.timeletter.domain.model.RecordedAudio
import com.afternote.feature.timeletter.domain.repository.VoiceRecorderRepository

object FakeVoiceRecorderRepository : VoiceRecorderRepository {
    override suspend fun start(): Result<Unit> = unexpectedCall("VoiceRecorderRepository.start")

    override suspend fun stop(): Result<RecordedAudio> = unexpectedCall("VoiceRecorderRepository.stop")

    override suspend fun discard() = Unit

    override fun retainRecordedFile() = Unit

    override suspend fun deleteRecordedFile(uriString: String) = Unit

    override fun release() = Unit
}
