package com.afternote.feature.timeletter.domain.usecase

import com.afternote.core.domain.repository.PhotoUploadRepository
import com.afternote.feature.timeletter.domain.model.BlockInput
import com.afternote.feature.timeletter.domain.model.NewTimeLetterBlock
import com.afternote.feature.timeletter.domain.model.TimeLetterBlockType
import javax.inject.Inject

class ResolveTimeLetterBlocksUseCase
    @Inject
    constructor(
        private val photoUploadRepository: PhotoUploadRepository,
    ) {
        suspend operator fun invoke(inputs: List<BlockInput>): List<NewTimeLetterBlock> {
            var order = 1

            return buildList {
                for (input in inputs) {
                    when (input) {
                        is BlockInput.Text -> {
                            if (input.content.isNotBlank()) {
                                add(
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
                                if (input.uriString.startsWith(CONTENT_URI_PREFIX)) {
                                    validateLocalMedia(input)
                                    photoUploadRepository
                                        .upload(input.uriString, TIME_LETTER_DIRECTORY)
                                        .getOrElse { throw it }
                                        .fileUrl
                                } else {
                                    input.uriString
                                }
                            add(
                                NewTimeLetterBlock(
                                    blockType = input.blockType,
                                    blockOrder = order++,
                                    url = url,
                                    mimeType = input.mimeType,
                                ),
                            )
                        }

                        is BlockInput.Link -> {
                            add(
                                NewTimeLetterBlock(
                                    blockType = TimeLetterBlockType.LINK,
                                    blockOrder = order++,
                                    url = input.url,
                                ),
                            )
                        }
                    }
                }
            }
        }

        private fun validateLocalMedia(input: BlockInput.Media) {
            val supportedMimeTypes =
                when (input.blockType) {
                    TimeLetterBlockType.IMAGE -> IMAGE_MIME_TYPES
                    TimeLetterBlockType.AUDIO -> AUDIO_MIME_TYPES
                    TimeLetterBlockType.FILE -> FILE_MIME_TYPES
                    else -> emptySet()
                }
            require(input.mimeType in supportedMimeTypes) {
                "Unsupported ${input.blockType} MIME type: ${input.mimeType}"
            }
        }

        private companion object {
            const val CONTENT_URI_PREFIX = "content://"
            const val TIME_LETTER_DIRECTORY = "timeletters"

            val IMAGE_MIME_TYPES =
                setOf(
                    "image/jpeg",
                    "image/jpg",
                    "image/png",
                    "image/gif",
                    "image/webp",
                    "image/heic",
                    "image/heif",
                )
            val AUDIO_MIME_TYPES =
                setOf(
                    "audio/mpeg",
                    "audio/mp3",
                    "audio/mp4",
                    "audio/m4a",
                    "audio/x-m4a",
                    "audio/wav",
                    "audio/x-wav",
                )
            val FILE_MIME_TYPES = setOf("application/pdf")
        }
    }
