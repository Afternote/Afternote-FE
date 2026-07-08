package com.afternote.feature.timeletter.data.repositoryImpl

import com.afternote.core.network.model.requireData
import com.afternote.core.network.model.requireStatus
import com.afternote.feature.timeletter.data.api.TimeLetterApiService
import com.afternote.feature.timeletter.data.dto.TimeLetterCreateRequest
import com.afternote.feature.timeletter.data.dto.TimeLetterDeleteRequest
import com.afternote.feature.timeletter.data.dto.TimeLetterUpdateRequest
import com.afternote.feature.timeletter.data.mapper.toDomain
import com.afternote.feature.timeletter.data.mapper.toDto
import com.afternote.feature.timeletter.domain.model.NewTimeLetterBlock
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterDeliveryMode
import com.afternote.feature.timeletter.domain.model.TimeLetterList
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus
import com.afternote.feature.timeletter.domain.repository.TimeLetterRepository
import javax.inject.Inject

class TimeLetterRepositoryImpl
    @Inject
    constructor(
        private val timeLetterApiService: TimeLetterApiService,
    ) : TimeLetterRepository {
        override suspend fun getTimeLetters(): TimeLetterList =
            timeLetterApiService
                .getTimeLetters()
                .requireData()
                .toDomain()

        override suspend fun getTemporaryTimeLetters(): TimeLetterList =
            timeLetterApiService
                .getTemporaryTimeLetters()
                .requireData()
                .toDomain()

        override suspend fun getTimeLetter(timeLetterId: Long): TimeLetter =
            timeLetterApiService
                .getTimeLetter(timeLetterId)
                .requireData()
                .toDomain()

        override suspend fun createTimeLetter(
            title: String?,
            blocks: List<NewTimeLetterBlock>,
            sendAt: String?,
            status: TimeLetterStatus,
            receiverIds: List<Long>?,
            deliveryMode: TimeLetterDeliveryMode?,
        ): TimeLetter =
            timeLetterApiService
                .createTimeLetter(
                    TimeLetterCreateRequest(
                        title = title,
                        sendAt = sendAt,
                        deliveryMode = deliveryMode?.toDto(),
                        status = status.toDto(),
                        blocks = blocks.map { it.toDto() },
                        receiverIds = receiverIds ?: emptyList(),
                    ),
                ).requireData()
                .toDomain()

        override suspend fun updateTimeLetter(
            timeLetterId: Long,
            title: String?,
            blocks: List<NewTimeLetterBlock>,
            sendAt: String?,
            status: TimeLetterStatus?,
            deliveryMode: TimeLetterDeliveryMode?,
        ): TimeLetter =
            timeLetterApiService
                .updateTimeLetter(
                    timeLetterId = timeLetterId,
                    request =
                        TimeLetterUpdateRequest(
                            title = title,
                            sendAt = sendAt,
                            deliveryMode = deliveryMode?.toDto(),
                            status = status?.toDto(),
                            blocks = blocks.map { it.toDto() },
                        ),
                ).requireData()
                .toDomain()

        override suspend fun deleteTimeLetters(timeLetterIds: List<Long>) {
            timeLetterApiService
                .deleteTimeLetters(TimeLetterDeleteRequest(timeLetterIds = timeLetterIds))
                .requireStatus()
        }

        override suspend fun deleteAllTemporary() {
            timeLetterApiService
                .deleteAllTemporary()
                .requireStatus()
        }
    }
