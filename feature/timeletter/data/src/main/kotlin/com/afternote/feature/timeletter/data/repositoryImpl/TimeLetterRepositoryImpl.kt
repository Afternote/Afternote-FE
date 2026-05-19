package com.afternote.feature.timeletter.data.repositoryImpl

import com.afternote.core.network.model.requireData
import com.afternote.core.network.model.requireStatus
import com.afternote.feature.timeletter.data.api.TimeLetterApiService
import com.afternote.feature.timeletter.data.dto.TimeLetterBlockRequest
import com.afternote.feature.timeletter.data.dto.TimeLetterBlockTypeDto
import com.afternote.feature.timeletter.data.dto.TimeLetterCreateRequest
import com.afternote.feature.timeletter.data.dto.TimeLetterDeleteRequest
import com.afternote.feature.timeletter.data.dto.TimeLetterUpdateRequest
import com.afternote.feature.timeletter.data.mapper.toDomain
import com.afternote.feature.timeletter.data.mapper.toDto
import com.afternote.feature.timeletter.domain.model.TimeLetter
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
            content: String?,
            sendAt: String?,
            status: TimeLetterStatus,
            receiverIds: List<Long>?,
        ): TimeLetter =
            timeLetterApiService
                .createTimeLetter(
                    TimeLetterCreateRequest(
                        title = title,
                        sendAt = sendAt,
                        status = status.toDto(),
                        blocks = buildBlocks(content),
                        receiverIds = receiverIds ?: emptyList(),
                    ),
                ).requireData()
                .toDomain()

        override suspend fun updateTimeLetter(
            timeLetterId: Long,
            title: String?,
            content: String?,
            sendAt: String?,
            status: TimeLetterStatus?,
        ): TimeLetter =
            timeLetterApiService
                .updateTimeLetter(
                    timeLetterId = timeLetterId,
                    request =
                        TimeLetterUpdateRequest(
                            title = title,
                            sendAt = sendAt,
                            status = status?.toDto(),
                            blocks = buildBlocks(content),
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

        private fun buildBlocks(content: String?): List<TimeLetterBlockRequest> =
            listOfNotNull(
                content?.takeIf { it.isNotBlank() }?.let {
                    TimeLetterBlockRequest(
                        blockType = TimeLetterBlockTypeDto.TEXT,
                        blockOrder = 1,
                        textContent = it,
                    )
                },
            )
    }
