package com.afternote.feature.afternote.presentation.editor.state

import kotlinx.serialization.Serializable

/**
 * 추모 영상 한 벌 — 영상과 그 썸네일.
 *
 * 두 값은 서버 계약에서도 짝이다: BE `AfternotePlaylist.MemorialVideo` 가 `@Embeddable` 로 두 칸을
 * 묶어 통째 교체한다. 폼에서도 묶어 두면 「영상이 바뀌면 그 영상의 썸네일은 무효」·「영상을 되돌리면
 * 썸네일도 함께」 같은 규칙을 호출부마다 손으로 지키지 않아도 된다.
 *
 * [url] 이 non-null 인 것이 핵심이다 — **영상 없이 썸네일만 있는 상태를 타입에서 배제**한다.
 * 종전에는 두 칸이 따로 놀아, 영상을 지운 뒤 늦게 도착한 썸네일 업로드가 썸네일 칸만 채우는
 * 상태(`videoUrl = null, thumbnailUrl = <url>`)가 실제로 만들어졌다.
 */
@Serializable
data class MemorialVideoAttachment(
    val url: String,
    val thumbnailUrl: String? = null,
) {
    /**
     * 썸네일을 뗀 사본 — 이탈 가드 지문에 실린다. 썸네일은 영상에서 자동 파생돼 나중에 채워지는 값이라
     * 지문에서 뺀다. 프로세스 재생성 복원 뒤 같은 로컬 영상에서 썸네일을 다시 뽑아 올리면 **다른 URL** 이
     * 붙는데, 빼지 않으면 사용자가 손도 대지 않은 폼이 「변경됨」 으로 판정돼 이탈 팝업이 뜬다.
     */
    fun withoutThumbnail(): MemorialVideoAttachment = copy(thumbnailUrl = null)

    companion object {
        /** 빈 문자열은 첨부 없음으로 본다 — 값 객체가 존재하면 재생할 영상이 있다는 뜻이어야 한다. */
        fun ofOrNull(
            url: String?,
            thumbnailUrl: String? = null,
        ): MemorialVideoAttachment? =
            url
                ?.takeIf { it.isNotBlank() }
                ?.let { MemorialVideoAttachment(url = it, thumbnailUrl = thumbnailUrl?.takeIf { t -> t.isNotBlank() }) }
    }
}
