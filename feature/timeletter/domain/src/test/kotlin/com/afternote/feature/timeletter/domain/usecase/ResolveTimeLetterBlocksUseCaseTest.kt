package com.afternote.feature.timeletter.domain.usecase

import com.afternote.core.domain.model.UploadedFile
import com.afternote.core.domain.testing.FakePhotoUploadRepository
import com.afternote.feature.timeletter.domain.model.BlockInput
import com.afternote.feature.timeletter.domain.model.TimeLetterBlockType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResolveTimeLetterBlocksUseCaseTest {
    @Test
    fun `local media is uploaded and uses remote url`() =
        runBlocking {
            val repository =
                photoUploadRepository(
                    Result.success(
                        UploadedFile(
                            fileUrl = "https://cdn/image.jpg",
                            fileKey = "timeletters/9/stored.jpg",
                        ),
                    ),
                )
            val useCase = ResolveTimeLetterBlocksUseCase(repository)

            val blocks =
                useCase(
                    listOf(
                        BlockInput.Media(
                            uriString = "content://image/1",
                            mimeType = "image/jpeg",
                            blockType = TimeLetterBlockType.IMAGE,
                        ),
                    ),
                )

            assertEquals("https://cdn/image.jpg", blocks.single().url)
            assertEquals("image/jpeg", blocks.single().mimeType)
            assertEquals(listOf("content://image/1" to "timeletters"), repository.uploads)
        }

    @Test
    fun `remote media is preserved without upload`() =
        runBlocking {
            val repository = photoUploadRepository(Result.success(UNUSED_UPLOAD))
            val useCase = ResolveTimeLetterBlocksUseCase(repository)

            val blocks =
                useCase(
                    listOf(
                        BlockInput.Media(
                            uriString = "https://cdn/audio.mp3",
                            mimeType = "audio/mpeg",
                            blockType = TimeLetterBlockType.AUDIO,
                        ),
                    ),
                )

            assertEquals("https://cdn/audio.mp3", blocks.single().url)
            assertTrue(repository.uploads.isEmpty())
        }

    @Test
    fun `blank text is removed and block order remains contiguous`() =
        runBlocking {
            val useCase = ResolveTimeLetterBlocksUseCase(photoUploadRepository(Result.success(UNUSED_UPLOAD)))

            val blocks =
                useCase(
                    listOf(
                        BlockInput.Text(" "),
                        BlockInput.Text("message"),
                        BlockInput.Link("https://example.com"),
                    ),
                )

            assertEquals(listOf(1, 2), blocks.map { it.blockOrder })
            assertEquals(listOf(TimeLetterBlockType.TEXT, TimeLetterBlockType.LINK), blocks.map { it.blockType })
        }

    @Test
    fun `image audio and file local media are all uploaded`() =
        runBlocking {
            val repository =
                photoUploadRepository(
                    Result.success(
                        UploadedFile(
                            fileUrl = "https://cdn/uploaded",
                            fileKey = "timeletters/9/uploaded",
                        ),
                    ),
                )
            val useCase = ResolveTimeLetterBlocksUseCase(repository)

            useCase(
                listOf(
                    BlockInput.Media("content://image", "image/png", TimeLetterBlockType.IMAGE),
                    BlockInput.Media("content://audio", "audio/mp4", TimeLetterBlockType.AUDIO),
                    BlockInput.Media("content://file", "application/pdf", TimeLetterBlockType.FILE),
                ),
            )

            assertEquals(3, repository.uploads.size)
        }

    @Test
    fun `upload failure stops block conversion`() {
        val error = IllegalStateException("upload failed")
        val useCase = ResolveTimeLetterBlocksUseCase(photoUploadRepository(Result.failure(error)))

        val result =
            runCatching {
                runBlocking {
                    useCase(
                        listOf(
                            BlockInput.Media("content://file", "application/pdf", TimeLetterBlockType.FILE),
                        ),
                    )
                }
            }

        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun `unsupported local media type is rejected before upload`() {
        val repository = photoUploadRepository(Result.success(UNUSED_UPLOAD))
        val useCase = ResolveTimeLetterBlocksUseCase(repository)

        val result =
            runCatching {
                runBlocking {
                    useCase(
                        listOf(
                            BlockInput.Media("content://file", "text/plain", TimeLetterBlockType.FILE),
                        ),
                    )
                }
            }

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(repository.uploads.isEmpty())
    }

    private fun photoUploadRepository(result: Result<UploadedFile>): FakePhotoUploadRepository =
        FakePhotoUploadRepository(onUpload = { _, _ -> result })
}

/** 업로드가 일어나면 안 되는 시나리오용 — 값 자체엔 의미가 없다. */
private val UNUSED_UPLOAD = UploadedFile(fileUrl = "unused", fileKey = "unused")
