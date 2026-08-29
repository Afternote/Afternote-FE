package com.afternote.feature.afternote.presentation.author.editor.state

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.Song
import com.afternote.feature.afternote.presentation.author.editor.model.EditorContentPrefill
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodItem

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
        /**
         * 수정 모드 prefill 로 받은 **서버에 저장된** 영상. [videoUrl] 이 로컬 교체분으로 덮인 뒤에도
         * 「서버에는 아직 이게 있다」를 잃지 않기 위해 따로 든다.
         *
         * 수정(PATCH) 계약은 삭제를 표현하지 못한다 — 필드를 비워 보내면 BE 가 기존 값 유지로
         * 읽는다. 그래서 서버 영상은 이 폼에서 지울 수 없고, 지운 척하면 폼만 비고 서버에는
         * 그대로 남는다(#1406). 되돌아갈 자리를 남겨 거짓 빈 슬롯을 만들지 않는다.
         */
        val serverVideoUrl: String? = null,
        /** [serverVideoUrl] 의 썸네일. 로컬 교체를 취소하면 영상과 함께 이 값으로 돌아간다. */
        val serverThumbnailUrl: String? = null,
    ) : AfternoteTypeForm {
        override val type = AfternoteType.MEMORIAL

        fun displayPhotoUri(): String? = pickedPhotoUri ?: photoUrl

        /**
         * 썸네일은 영상에서 자동 파생된 값이라 사용자 입력이 아니다 — pristine 판정 전에 지운다.
         * 서버 원본 두 칸도 같은 이유로 뺀다. prefill 이 채워 넣는 값이지 사용자가 넣은 값이 아니다.
         */
        override fun enteredContentOrNull(): String? =
            copy(thumbnailUrl = null, serverVideoUrl = null, serverThumbnailUrl = null)
                .takeUnless { it == PRISTINE }
                ?.toString()

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
                        // 표시용 칸과 같은 값으로 시작하지만, 로컬 교체가 들어와도 이쪽은 그대로 남는다.
                        serverVideoUrl = content.videoUrl,
                        serverThumbnailUrl = content.thumbnailUrl,
                    )
                }

                EditorContentPrefill.Estate -> {
                    Estate
                }
            }
    }
}
