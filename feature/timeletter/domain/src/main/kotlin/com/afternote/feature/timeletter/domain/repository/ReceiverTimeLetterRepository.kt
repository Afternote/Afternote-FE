package com.afternote.feature.timeletter.domain.repository

import com.afternote.feature.timeletter.domain.model.ReceivedTimeLetter
import com.afternote.feature.timeletter.domain.model.ReceivedTimeLetterList

interface ReceiverTimeLetterRepository {
    suspend fun getReceivedTimeLetters(): ReceivedTimeLetterList

    suspend fun getReceivedTimeLetterDetail(timeLetterReceiverId: Long): ReceivedTimeLetter
}
