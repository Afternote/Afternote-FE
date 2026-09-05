package com.afternote.feature.onboarding.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingProfilePickerResultTest {
    @Test
    fun `갤러리 취소는 기존 프로필 이미지를 변경하지 않는다`() {
        val previouslySelectedUri = "content://profile/selected"
        var profileImageUri = previouslySelectedUri

        handleProfileImagePickerResult(uri = null) { pickedUri ->
            profileImageUri = pickedUri.toString()
        }

        assertEquals(previouslySelectedUri, profileImageUri)
    }
}
