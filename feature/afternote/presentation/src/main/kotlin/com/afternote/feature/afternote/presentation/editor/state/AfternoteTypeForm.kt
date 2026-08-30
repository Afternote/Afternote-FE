package com.afternote.feature.afternote.presentation.editor.state

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.editor.memorial.Song
import com.afternote.feature.afternote.presentation.editor.model.EditorContentPrefill
import com.afternote.feature.afternote.presentation.editor.processing.ProcessingMethodItem

/**
 * 카테고리 전용 입력 묶음. [EditorFormState] 가 합성으로 소유한다.
 *
 * 상속(`sealed class EditorFormState`)이 아니라 합성인 이유는 추상 타입에 `copy` 가 없어서다 —
 * 이탈 가드 지문의 공용 필드 중립화를 하위 타입마다 손으로 재작성하게 되고, 그 순간 "필드 추가 자동 포함" 성질을 잃는다.
 */
sealed interface AfternoteTypeForm {
    val type: AfternoteType

    /** 이탈 가드 지문에 실을 조각. 사용자가 이 카테고리에 실제로 넣은 값이 없으면 `null`. */
    fun enteredContentOrNull(): String?

    sealed interface WithServiceAndProcessingMethods : AfternoteTypeForm {
        val selectedService: String?
        val processingMethods: List<ProcessingMethodItem>

        fun withService(service: String?): WithServiceAndProcessingMethods

        fun withProcessingMethods(methods: List<ProcessingMethodItem>): WithServiceAndProcessingMethods
    }

    data class Social(
        override val selectedService: String? = null,
        override val processingMethods: List<ProcessingMethodItem> = emptyList(),
    ) : WithServiceAndProcessingMethods {
        override val type = AfternoteType.SOCIAL_NETWORK

        override fun withService(service: String?) = copy(selectedService = service)

        override fun withProcessingMethods(methods: List<ProcessingMethodItem>) = copy(processingMethods = methods)

        override fun enteredContentOrNull(): String? = takeUnless { it == PRISTINE }?.toString()

        private companion object {
            val PRISTINE = Social()
        }
    }

    data class Business(
        override val selectedService: String? = null,
        override val processingMethods: List<ProcessingMethodItem> = emptyList(),
    ) : WithServiceAndProcessingMethods {
        override val type = AfternoteType.BUSINESS

        override fun withService(service: String?) = copy(selectedService = service)

        override fun withProcessingMethods(methods: List<ProcessingMethodItem>) = copy(processingMethods = methods)

        override fun enteredContentOrNull(): String? = takeUnless { it == PRISTINE }?.toString()

        private companion object {
            val PRISTINE = Business()
        }
    }

    data class Gallery(
        override val selectedService: String? = null,
        override val processingMethods: List<ProcessingMethodItem> = emptyList(),
    ) : WithServiceAndProcessingMethods {
        override val type = AfternoteType.GALLERY_AND_FILES

        override fun withService(service: String?) = copy(selectedService = service)

        override fun withProcessingMethods(methods: List<ProcessingMethodItem>) = copy(processingMethods = methods)

        override fun enteredContentOrNull(): String? = takeUnless { it == PRISTINE }?.toString()

        private companion object {
            val PRISTINE = Gallery()
        }
    }

    data class Memorial(
        val pickedPhotoUri: String? = null,
        val videoUrl: String? = null,
        val thumbnailUrl: String? = null,
        val photoUrl: String? = null,
        val playlistSongs: List<Song> = emptyList(),
    ) : AfternoteTypeForm {
        override val type = AfternoteType.MEMORIAL

        fun displayPhotoUri(): String? = pickedPhotoUri ?: photoUrl

        /** 썸네일은 영상에서 자동 파생된 값이라 사용자 입력이 아니다 — pristine 판정 전에 지운다. */
        override fun enteredContentOrNull(): String? = copy(thumbnailUrl = null).takeUnless { it == PRISTINE }?.toString()

        private companion object {
            val PRISTINE = Memorial()
        }
    }

    /** 전용 입력이 0개인 "준비 중" 카테고리 (이슈 #195). 지문 중립 원소로도 쓰인다. */
    data object Estate : AfternoteTypeForm {
        override val type = AfternoteType.ESTATE

        override fun enteredContentOrNull(): String? = null
    }

    companion object {
        /** 카테고리 전환의 유일한 생성 경로 — 항상 입력이 비어 있는 하위 타입을 만든다. */
        fun pristineFor(type: AfternoteType): AfternoteTypeForm =
            when (type) {
                AfternoteType.SOCIAL_NETWORK -> Social()
                AfternoteType.BUSINESS -> Business()
                AfternoteType.GALLERY_AND_FILES -> Gallery()
                AfternoteType.MEMORIAL -> Memorial()
                AfternoteType.ESTATE -> Estate
            }

        fun fromPrefill(content: EditorContentPrefill): AfternoteTypeForm =
            when (content) {
                is EditorContentPrefill.SocialNetwork -> {
                    Social(selectedService = content.serviceName, processingMethods = content.processingMethods)
                }

                is EditorContentPrefill.Business -> {
                    Business(selectedService = content.serviceName, processingMethods = content.processingMethods)
                }

                is EditorContentPrefill.Gallery -> {
                    Gallery(selectedService = content.serviceName, processingMethods = content.processingMethods)
                }

                is EditorContentPrefill.Memorial -> {
                    Memorial(
                        videoUrl = content.videoUrl,
                        thumbnailUrl = content.thumbnailUrl,
                        photoUrl = content.photoUrl,
                        playlistSongs = content.playlistSongs,
                    )
                }

                EditorContentPrefill.Estate -> {
                    Estate
                }
            }
    }
}
