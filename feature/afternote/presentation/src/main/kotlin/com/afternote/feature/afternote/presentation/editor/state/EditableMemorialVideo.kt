package com.afternote.feature.afternote.presentation.editor.state

import com.afternote.feature.afternote.domain.repository.author.MediaInput
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 추모 영상 칸의 편집 상태 — 저장을 누르면 무엇을 해야 하는지가 이름이다.
 *
 * [NoVideo] · [Uploaded] · [PendingUpload] 셋이다. [Uploaded] 는 이미 서버에 올라가 있어 저장 때 URL 만
 * 보내고([MediaInput.Remote]), [PendingUpload] 는 이번에 새로 골라 아직 안 올렸으니 저장 때 업로드하며
 * ([MediaInput.Local]), [NoVideo] 는 명시적 `null` 로 나간다([MediaInput.None], BE 삭제 계약 #1596·#1597).
 * 이 판정을 URL 모양으로 재추론하지 않는다(#1406).
 *
 * 종전(#1901)에는 서버 원본 위에 새 선택을 얹은 `Replaced` 가 넷째 상태로 있었다. 그 상태가 들고 있던
 * 서버 원본은 표시·저장·삭제·이탈 가드 어디서도 읽히지 않았다 — 시트의 삭제는 슬롯을 통째로 비우고
 * (`withMemorialVideoRemoved`), 선택만 지워 서버 원본으로 돌아가는 경로는 picker 가 빈 URI 를 내지 않아
 * 프로덕션에 없으며, 이탈 가드 지문은 진입 기준선(`fromServer` 결과)과 통째로 비교하므로 새 선택이 얹힌
 * 순간 이미 다르다(#2114). 그래서 새 선택이 있으면 서버 원본 유무와 무관하게 [PendingUpload] 다.
 *
 * 직렬화 wire 형태는 하위 타입 판별자가 붙은 `{"type":"pending_upload",…}` 이고 판별자 값은
 * `@SerialName` 으로 고정해 클래스 이름 변경에 끌려가지 않는다. 상태 집합이 넷에서 셋으로 줄어 옛 v5
 * payload(`"replaced"`)를 읽을 수 없으므로 스냅샷 키는 `v6` 이다(`AfternoteEditorViewModel` 의
 * `EDITOR_FORM_SNAPSHOT_KEY`).
 *
 * 하위 타입이 `private` 이라 밖에서는 `when` 으로 분해할 수 없다 — 첨부 값은 이 파일을 벗어나지 않는다.
 */
@Serializable
internal sealed class EditableMemorialVideo {
    @Serializable
    @SerialName("no_video")
    private data object NoVideo : EditableMemorialVideo()

    @Serializable
    @SerialName("uploaded")
    private data class Uploaded(
        val video: MemorialVideoAttachment,
    ) : EditableMemorialVideo()

    @Serializable
    @SerialName("pending_upload")
    private data class PendingUpload(
        val video: MemorialVideoAttachment,
    ) : EditableMemorialVideo()

    /** 화면에 표시하고 payload의 영상·썸네일 한 벌로 사용할 현재 값. */
    internal val displayed: MemorialVideoAttachment?
        get() =
            when (this) {
                NoVideo -> null
                is Uploaded -> video
                is PendingUpload -> video
            }

    /** 시트에 삭제 항목을 내놓을 수 있는지 — 표시된 영상이 있으면 출처와 무관하게 지울 수 있다(#1597). */
    internal val canRemove: Boolean
        get() = displayed != null

    /** 새 선택으로 교체한다. 이전 선택의 썸네일은 물려주지 않는다. 빈 문자열은 [MemorialVideoAttachment.ofOrNull] 규칙대로 첨부 없음이다. */
    internal fun withSelection(url: String): EditableMemorialVideo = MemorialVideoAttachment.ofOrNull(url)?.let(::PendingUpload) ?: NoVideo

    /** 미저장 선택에서 파생된 썸네일만 갱신한다. 선택이 사라졌다면 늦은 결과를 버린다. */
    internal fun withSelectionThumbnail(url: String): EditableMemorialVideo =
        when (this) {
            NoVideo -> this
            is Uploaded -> this
            is PendingUpload -> PendingUpload(video.copy(thumbnailUrl = url))
        }

    /**
     * 썸네일만 뗀 사본 — 이탈 가드 지문에 실린다. 왜 썸네일을 빼는지는
     * [MemorialVideoAttachment.withoutThumbnail], 왜 서버 영상이 지문에 남아야 하는지는
     * [AfternoteTypeForm.Memorial.enteredContentOrNull] 이 말한다.
     */
    internal fun withoutThumbnail(): EditableMemorialVideo =
        when (this) {
            NoVideo -> this
            is Uploaded -> Uploaded(video.withoutThumbnail())
            is PendingUpload -> PendingUpload(video.withoutThumbnail())
        }

    /** 출처를 URL 모양으로 재추론하지 않고 저장 경계의 명시적인 입력 타입으로 바꾼다. */
    internal fun toMediaInput(): MediaInput =
        when (this) {
            NoVideo -> MediaInput.None
            is Uploaded -> MediaInput.Remote(video.url)
            is PendingUpload -> MediaInput.Local(video.url)
        }

    internal companion object {
        internal fun empty(): EditableMemorialVideo = NoVideo

        /**
         * 서버 영상으로 만든다. 결과에 미저장 선택이 없다 — 수정 진입 기준선(이탈 가드)이 이 값으로
         * 잡힌다. 언제 부르는지(진입·프리필)는 호출부 사정이라 이름에 넣지 않는다.
         */
        internal fun fromServer(video: MemorialVideoAttachment?): EditableMemorialVideo = video?.let(::Uploaded) ?: NoVideo
    }
}
