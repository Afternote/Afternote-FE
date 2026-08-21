package com.afternote.feature.afternote.presentation.author.editor.model

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.Song
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageTextBlock
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodItem
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiver

/**
 * ViewModel/Mapper가 [com.afternote.feature.afternote.domain.model.author.Detail] 등에서 조립해
 * [com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorState.applyFormPrefill]에 넘기는 값 묶음.
 * 분기·enum 해석·메시지 파싱은 여기 이전 단계에서 끝난다.
 */
data class EditorFormPrefill(
    val loadedItemId: String,
    val content: EditorContentPrefill,
    /** 서버 상세 응답의 공통 `leaveMessage` 필드. 지원 타입과 무관하게 수정 시 보존한다. */
    val leaveMessageBlocks: List<EditorMessageTextBlock>,
    /**
     * 이 애프터노트에 지정된 수신자 (#566). 신규 작성 경로가 폼에 넣는
     * `AfternoteSaveState.authorReceivers`(= 작성자가 등록한 수신자 **전체**) 와 다른 값이다.
     */
    val receivers: List<AfternoteEditorReceiver>,
) {
    val type: AfternoteType get() = content.type

    /** 기존 에디터 폼이 사용하는 종류 값. EditorCategory 제거 단계에서 함께 사라진다. */
    val category: EditorCategory
        get() =
            when (type) {
                AfternoteType.SOCIAL_NETWORK -> EditorCategory.SOCIAL
                AfternoteType.BUSINESS -> EditorCategory.BUSINESS
                AfternoteType.GALLERY_AND_FILES -> EditorCategory.GALLERY
                AfternoteType.ESTATE -> EditorCategory.ESTATE
                AfternoteType.MEMORIAL -> EditorCategory.MEMORIAL
            }
}

/** 수정할 애프터노트 종류에 실제로 존재하는 입력만 담는다. */
sealed interface EditorContentPrefill {
    val type: AfternoteType

    data class SocialNetwork(
        val serviceName: String,
        val credentials: EditorCredentialsPrefill,
        val processingMethods: List<ProcessingMethodItem>,
    ) : EditorContentPrefill {
        override val type: AfternoteType = AfternoteType.SOCIAL_NETWORK
    }

    data class Business(
        val serviceName: String,
        val credentials: EditorCredentialsPrefill,
        val processingMethods: List<ProcessingMethodItem>,
    ) : EditorContentPrefill {
        override val type: AfternoteType = AfternoteType.BUSINESS
    }

    data class Gallery(
        val serviceName: String,
        val processingMethods: List<ProcessingMethodItem>,
    ) : EditorContentPrefill {
        override val type: AfternoteType = AfternoteType.GALLERY_AND_FILES
    }

    data class Memorial(
        val videoUrl: String?,
        val thumbnailUrl: String?,
        val photoUrl: String?,
        val playlistSongs: List<Song>,
    ) : EditorContentPrefill {
        override val type: AfternoteType = AfternoteType.MEMORIAL
    }

    data object Estate : EditorContentPrefill {
        override val type: AfternoteType = AfternoteType.ESTATE
    }
}

data class EditorCredentialsPrefill(
    val id: String,
    val password: String,
)
