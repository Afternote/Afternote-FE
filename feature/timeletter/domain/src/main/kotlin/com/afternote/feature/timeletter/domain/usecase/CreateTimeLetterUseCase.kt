package com.afternote.feature.timeletter.domain.usecase

import com.afternote.feature.timeletter.domain.model.BlockInput
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus
import com.afternote.feature.timeletter.domain.repository.TimeLetterRepository
import javax.inject.Inject

class CreateTimeLetterUseCase
    @Inject
    constructor(
        private val timeLetterRepository: TimeLetterRepository,
        private val resolveTimeLetterBlocksUseCase: ResolveTimeLetterBlocksUseCase,
    ) {
        suspend operator fun invoke(
            title: String?,
            blocks: List<BlockInput>,
            sendAt: String?,
            status: TimeLetterStatus,
            receiverIds: List<Long>?,
        ): Result<TimeLetter> =
            runCatching {
                val newBlocks = resolveTimeLetterBlocksUseCase(blocks)
                timeLetterRepository.createTimeLetter(
                    title = title,
                    blocks = newBlocks,
                    sendAt = sendAt,
                    status = status,
                    receiverIds = receiverIds,
                )
            }
    }
