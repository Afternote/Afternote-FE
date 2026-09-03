package com.afternote.feature.afternote.presentation.editor.state

import com.afternote.feature.afternote.presentation.shared.util.AfternoteServiceCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorFormStateServiceCatalogTest {
    @Test
    fun `서비스 선택 3개 카테고리는 고정 catalog 순서를 그대로 제공한다`() {
        assertEquals(
            AfternoteServiceCatalog.socialServices,
            EditorFormState(typeForm = AfternoteTypeForm.Social()).currentServiceOptions,
        )
        assertEquals(
            AfternoteServiceCatalog.galleryServices,
            EditorFormState(typeForm = AfternoteTypeForm.Gallery()).currentServiceOptions,
        )
        assertEquals(
            AfternoteServiceCatalog.businessServices,
            EditorFormState(typeForm = AfternoteTypeForm.Business()).currentServiceOptions,
        )
    }

    @Test
    fun `어느 catalog에도 직접 추가하기를 노출하지 않는다`() {
        val allOptions =
            listOf(
                EditorFormState(typeForm = AfternoteTypeForm.Social()),
                EditorFormState(typeForm = AfternoteTypeForm.Gallery()),
                EditorFormState(typeForm = AfternoteTypeForm.Business()),
            ).flatMap(EditorFormState::currentServiceOptions)

        assertFalse("직접 추가하기" in allOptions)
    }

    @Test
    fun `서비스 선택이 없는 카테고리는 빈 catalog다`() {
        assertTrue(EditorFormState(typeForm = AfternoteTypeForm.Memorial()).currentServiceOptions.isEmpty())
        assertTrue(EditorFormState(typeForm = AfternoteTypeForm.Estate).currentServiceOptions.isEmpty())
    }
}
