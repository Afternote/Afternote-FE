package com.afternote.feature.timeletter.domain.repository

import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterList
import com.afternote.feature.timeletter.domain.model.TimeLetterMediaType
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus

interface TimeLetterRepository {
    // 타임레터 전체 조회 (SCHEDULED)
    suspend fun getTimeLetters(): TimeLetterList

    // 임시저장 전체 조회 (DRAFT)
    suspend fun getTemporaryTimeLetters(): TimeLetterList

    // 타임레터 단일 조회
    suspend fun getTimeLetter(timeLetterId: Long): TimeLetter

    // 타임레터 등록
    suspend fun createTimeLetter(
        title: String?,
        content: String?,
        sendAt: String?,
        status: TimeLetterStatus,
        mediaList: List<Pair<TimeLetterMediaType, String>>?,
        receiverIds: List<Long>?,
        deliveredAt: String?,
    ): TimeLetter

    // 타임레터 수정
    suspend fun updateTimeLetter(
        timeLetterId: Long,
        title: String?,
        content: String?,
        sendAt: String?,
        status: TimeLetterStatus?,
        mediaList: List<Pair<TimeLetterMediaType, String>>?,
    ): TimeLetter

    // 타임레터 단일/다건 삭제
    suspend fun deleteTimeLetters(timeLetterIds: List<Long>)

    // 임시저장 전체 삭제
    suspend fun deleteAllTemporary()
}
