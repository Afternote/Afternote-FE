package com.afternote.feature.afternote.presentation.author.editor

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.repository.UserRepository
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.repository.author.AfternoteRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialMediaUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialThumbnailUploadRepository
import com.afternote.feature.afternote.domain.usecase.editor.ResolveMemorialMediaForSaveUseCase
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
    fun `기존 문자열 receiver id 스냅샷을 Long 폼으로 복원한다`() {
        val savedStateHandle =
            SavedStateHandle(
                mapOf(
                    "initialType" to AfternoteType.SOCIAL_NETWORK,
                    SNAPSHOT_KEY to
                        """{"type":"SOCIAL_NETWORK","selectedService":"인스타그램","receivers":[{"id":"7","name":"김수신","label":"딸"}]}""",
                ),
            )

        val form = viewModel(savedStateHandle).currentForm()

        assertEquals("인스타그램", form.selectedService)
        assertEquals(7L, form.afternoteEditReceivers.single().id)
    }

    @Test
    fun `새 receiver id 스냅샷은 숫자로 기록하고 다시 복원한다`() {
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
        const val SNAPSHOT_KEY = "editor_form_snapshot_v2"
    }
}
