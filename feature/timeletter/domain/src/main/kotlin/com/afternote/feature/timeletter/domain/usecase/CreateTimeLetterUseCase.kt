package com.afternote.feature.timeletter.domain.usecase

import com.afternote.core.domain.repository.PhotoUploadRepository
import com.afternote.feature.timeletter.domain.model.BlockInput
import com.afternote.feature.timeletter.domain.model.NewTimeLetterBlock
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterBlockType
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus
import com.afternote.feature.timeletter.domain.repository.TimeLetterRepository
import javax.inject.Inject

class CreateTimeLetterUseCase
@Inject
constructor(
    private val timeLetterRepository: TimeLetterRepository,
    private val photoUploadRepository: PhotoUploadRepository,
) {
    suspend operator fun invoke(
        title: String?,
        blocks: List<BlockInput>,
        sendAt: String?,
        status: TimeLetterStatus,
        receiverIds: List<Long>?,
    ): Result<TimeLetter> =
        runCatching {
            val newBlocks = buildBlocks(blocks)
            timeLetterRepository.createTimeLetter(
                title = title,
                blocks = newBlocks,
                sendAt = sendAt,
                status = status,
                receiverIds = receiverIds,
            )
        }

    private suspend fun buildBlocks(inputs: List<BlockInput>): List<NewTimeLetterBlock> {
        val result = mutableListOf<NewTimeLetterBlock>()
        var order = 1
        for (input in inputs) {
            when (input) {
                is BlockInput.Text -> {
                    if (input.content.isNotBlank()) {
                        result.add(
                            NewTimeLetterBlock(
                                blockType = TimeLetterBlockType.TEXT,
                                blockOrder = order++,
                                textContent = input.content,
                            ),
                        )
                    }
                }

                is BlockInput.Media -> {
                    val url =
                        photoUploadRepository
                            .upload(input.uriString, "timeletters")
                            .getOrElse { throw it }
                    result.add(
                        NewTimeLetterBlock(
                            blockType = input.blockType,
                            blockOrder = order++,
                            url = url,
                            mimeType = input.mimeType,
                        ),
                    )
                }

                is BlockInput.Link -> {
                    result.add(
                        NewTimeLetterBlock(
                            blockType = TimeLetterBlockType.LINK,
                            blockOrder = order++,
                            url = input.url,
                        ),
                    )
                }
            }
        }
        return result
    }
}
