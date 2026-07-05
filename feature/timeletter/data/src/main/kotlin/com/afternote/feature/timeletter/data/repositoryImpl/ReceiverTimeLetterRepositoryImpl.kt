package com.afternote.feature.timeletter.data.repositoryImpl

import com.afternote.core.network.model.requireData
import com.afternote.feature.timeletter.data.api.ReceiverTimeLetterApiService
import com.afternote.feature.timeletter.data.mapper.toDomain
import com.afternote.feature.timeletter.domain.model.ReceivedTimeLetter
import com.afternote.feature.timeletter.domain.model.ReceivedTimeLetterList
import com.afternote.feature.timeletter.domain.repository.ReceiverTimeLetterRepository
import javax.inject.Inject

class ReceiverTimeLetterRepositoryImpl
    @Inject
    constructor(
        private val receiverTimeLetterApiService: ReceiverTimeLetterApiService,
    ) : ReceiverTimeLetterRepository {
        override suspend fun getReceivedTimeLetters(): ReceivedTimeLetterList =
            receiverTimeLetterApiService
                .getReceivedTimeLetters()
                .requireData()
                .toDomain()

        override suspend fun getReceivedTimeLetterDetail(timeLetterReceiverId: Long): ReceivedTimeLetter =
            receiverTimeLetterApiService
                .getReceivedTimeLetterDetail(timeLetterReceiverId)
                .requireData()
                .toDomain()
    }
