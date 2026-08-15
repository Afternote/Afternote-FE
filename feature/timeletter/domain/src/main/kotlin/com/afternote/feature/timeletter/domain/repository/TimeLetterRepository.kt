package com.afternote.feature.timeletter.domain.repository

import com.afternote.feature.timeletter.domain.model.NewTimeLetterBlock
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterDeliveryMode
import com.afternote.feature.timeletter.domain.model.TimeLetterList
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus

interface TimeLetterRepository {
    suspend fun getTimeLetters(): TimeLetterList

    suspend fun getTemporaryTimeLetters(): TimeLetterList

    suspend fun getTimeLetter(timeLetterId: Long): TimeLetter

    suspend fun createTimeLetter(
        title: String?,
        blocks: List<NewTimeLetterBlock>,
        sendAt: String?,
        deliveryMode: TimeLetterDeliveryMode,
        status: TimeLetterStatus,
        receiverIds: List<Long>,
    ): TimeLetter

    suspend fun updateTimeLetter(
        timeLetterId: Long,
        title: String?,
        blocks: List<NewTimeLetterBlock>,
        sendAt: String?,
        deliveryMode: TimeLetterDeliveryMode?,
        status: TimeLetterStatus?,
    ): TimeLetter

    suspend fun deleteTimeLetters(timeLetterIds: List<Long>)

    suspend fun deleteAllTemporary()
}
