package com.afternote.feature.afternote.presentation.shared.util

import com.afternote.feature.afternote.domain.AfternoteServiceType
import com.afternote.feature.afternote.presentation.shared.model.AfternoteService

/**
 * Single source of truth for afternote service names by category.
 * Used for edit-screen dropdowns and for inferring [AfternoteServiceType] from service name
 * (e.g. dummy/legacy data). Shared by writer and receiver flows.
 */
object AfternoteServiceCatalog {
    /** Display names for 소셜 네트워크 (SOCIAL) category. */
    val socialServices: List<String> =
        listOf(
            AfternoteService.INSTAGRAM.displayKey,
            AfternoteService.FACEBOOK.displayKey,
            AfternoteService.X.displayKey,
            AfternoteService.THREAD.displayKey,
            AfternoteService.TIKTOK.displayKey,
            AfternoteService.YOUTUBE.displayKey,
            AfternoteService.KAKAOTALK.displayKey,
            AfternoteService.KAKAOSTORY.displayKey,
            AfternoteService.NAVER_BLOG.displayKey,
            AfternoteService.NAVER_CAFE.displayKey,
            AfternoteService.NAVER_BAND.displayKey,
            AfternoteService.DISCORD.displayKey,
        )

    /** Display names for 갤러리 및 파일 (GALLERY) category. */
    val galleryServices: List<String> =
        listOf(
            AfternoteService.GALLERY.displayKey,
            AfternoteService.FILES.displayKey,
            AfternoteService.GOOGLE_PHOTO.displayKey,
            AfternoteService.MYBOX.displayKey,
            AfternoteService.ICLOUD.displayKey,
            AfternoteService.ONEDRIVE.displayKey,
            AfternoteService.TALKDRIVE.displayKey,
        )

    private val MEMORIAL_SERVICE_NAME: String = AfternoteService.MEMORIAL_GUIDELINE.displayKey

    /**
     * Infers [AfternoteServiceType] from a service display name.
     * Only for dummy/legacy data; when type is known from API or list item, use that instead.
     */
    fun serviceTypeFor(serviceName: String): AfternoteServiceType =
        when {
            galleryServices.contains(serviceName) -> AfternoteServiceType.GALLERY_AND_FILES
            serviceName == MEMORIAL_SERVICE_NAME -> AfternoteServiceType.MEMORIAL
            else -> AfternoteServiceType.SOCIAL_NETWORK
        }

    /** Default service when social category is selected (first in list). */
    val defaultSocialService: String
        get() = socialServices.first()

    /** Default service when gallery category is selected (first in list). */
    val defaultGalleryService: String
        get() = galleryServices.first()
}
