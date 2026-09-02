package com.afternote.feature.afternote.presentation.author.editor

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.repository.UserRepository
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.repository.author.AfternoteRepository
import com.afternote.feature.afternote.domain.repository.author.MediaInput
import com.afternote.feature.afternote.domain.repository.author.MemorialMediaUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialThumbnailUploadRepository
import com.afternote.feature.afternote.domain.usecase.editor.ResolveMemorialMediaForSaveUseCase
import com.afternote.feature.afternote.presentation.author.editor.state.MemorialVideoAttachment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Proxy

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AfternoteEditorReceiverSnapshotTest {
    @Test
    fun `receiver id 스냅샷은 숫자로 기록하고 다시 복원한다`() {
        val savedStateHandle = SavedStateHandle(mapOf("initialType" to AfternoteType.SOCIAL_NETWORK))
        val viewModel = viewModel(savedStateHandle)

        viewModel.addReceiverIfAbsent(Long.MAX_VALUE, "김수신", "딸")

        val raw = requireNotNull(savedStateHandle.get<String>(SNAPSHOT_KEY))
        assertTrue(raw.contains("\"id\":${Long.MAX_VALUE}"))
        val restoredId =
            viewModel(savedStateHandle)
                .currentForm()
                .afternoteEditReceivers
                .single()
                .id
        assertEquals(Long.MAX_VALUE, restoredId)
    }

    @Test
    fun `v4 영상 편집 상태는 왕복 후 새 선택 제거 시 서버 원본으로 돌아간다`() {
        val persisted =
            MemorialVideoAttachment(
                url = "https://cdn.test/farewell.mp4",
                thumbnailUrl = "https://cdn.test/farewell-thumb.jpg",
            )
        val selection =
            MemorialVideoAttachment(
                url = "content://videos/replacement",
                thumbnailUrl = "https://cdn.test/replacement-thumb.jpg",
            )
        val savedStateHandle =
            SavedStateHandle(
                mapOf(
                    "initialType" to AfternoteType.MEMORIAL,
                    SNAPSHOT_KEY to
                        """
                        {
                          "type":"MEMORIAL",
                          "memorialVideo":{
                            "persisted":{
                              "url":"${persisted.url}",
                              "thumbnailUrl":"${persisted.thumbnailUrl}"
                            },
                            "selection":{
                              "url":"${selection.url}",
                              "thumbnailUrl":"${selection.thumbnailUrl}"
                            }
                          }
                        }
                        """.trimIndent(),
                ),
            )
        val viewModel = viewModel(savedStateHandle)

        assertEquals(selection, viewModel.currentForm().displayedMemorialVideo)
        assertTrue(viewModel.currentForm().canRemoveMemorialVideo)

        viewModel.setMemorialThumbnail("https://cdn.test/round-trip-thumb.jpg")
        val roundTrippedRaw = requireNotNull(savedStateHandle.get<String>(SNAPSHOT_KEY))
        assertTrue(roundTrippedRaw.contains("\"memorialVideo\""))

        val restoredViewModel = viewModel(savedStateHandle)
        val roundTripped = restoredViewModel.currentForm()
        assertEquals(
            selection.copy(thumbnailUrl = "https://cdn.test/round-trip-thumb.jpg"),
            roundTripped.displayedMemorialVideo,
        )
        assertEquals(MediaInput.Local(selection.url), roundTripped.memorialVideo?.toMediaInput())
        assertTrue(roundTripped.canRemoveMemorialVideo)

        restoredViewModel.setMemorialVideo(null)

        assertEquals(persisted, restoredViewModel.currentForm().displayedMemorialVideo)
        assertEquals(MediaInput.Remote(persisted.url), restoredViewModel.currentForm().memorialVideo?.toMediaInput())
        // 서버 원본이 남아 있으니 삭제 항목은 계속 열려 있다(#1597).
        assertTrue(restoredViewModel.currentForm().canRemoveMemorialVideo)
    }

    private fun viewModel(savedStateHandle: SavedStateHandle): AfternoteEditorViewModel =
        AfternoteEditorViewModel(
            savedStateHandle = savedStateHandle,
            userRepository = repositoryProxy<UserRepository>(),
            afternoteRepository = repositoryProxy<AfternoteRepository>(),
            memorialThumbnailUploadRepository =
                MemorialThumbnailUploadRepository { error("썸네일 업로드가 호출되면 안 됩니다") },
            resolveMemorialMediaForSave =
                ResolveMemorialMediaForSaveUseCase(
                    MemorialMediaUploadRepository { _, _ -> error("미디어 저장이 호출되면 안 됩니다") },
                ),
            errorReporter = repositoryProxy<ErrorReporter>(),
        )

    private inline fun <reified T> repositoryProxy(): T =
        Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java),
        ) { _, method, _ -> error("${T::class.java.simpleName}.${method.name} 호출은 이 테스트에서 예상하지 않았습니다") } as T

    private companion object {
        const val SNAPSHOT_KEY = "editor_form_snapshot_v4"
    }
}
