package com.afternote.feature.timeletter.data.testing

import android.content.Context
import com.afternote.feature.timeletter.data.repositoryImpl.VoiceRecorderRepositoryImpl
import com.afternote.feature.timeletter.domain.repository.VoiceRecorderRepository
import kotlinx.coroutines.CoroutineDispatcher

/**
 * `VoiceRecorderRepositoryImpl` 은 프로덕션에서 이 모듈의 `TimeLetterModule` 만 사용해 internal
 * 이다. 실제 `MediaRecorder → filesDir → FileProvider` 경계를 검증하는 androidTest 는 다른
 * Gradle 모듈(`app`)에 있으므로, internal 을 넓히는 대신 공개 계약만 돌려주는 이 팩토리를 통해
 * 실제 구현을 얻는다.
 */
fun createVoiceRecorderRepositoryForTesting(
    context: Context,
    ioDispatcher: CoroutineDispatcher,
): VoiceRecorderRepository = VoiceRecorderRepositoryImpl(context, ioDispatcher)
