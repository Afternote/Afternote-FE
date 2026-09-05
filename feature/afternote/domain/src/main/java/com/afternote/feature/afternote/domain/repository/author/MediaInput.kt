package com.afternote.feature.afternote.domain.repository.author

/**
 * 추억 노트 미디어(장례식에 남길 영상·영정사진) 입력의 출처 상태.
 *
 * 진입 시점(presentation)에 로컬/원격을 *한 번* 확정해 resolve 경계로 넘긴다. 그러면 data 레이어가
 * `content://` 같은 prefix 를 런타임에 문자열 비교하지 않고 [when] 분기(exhaustive)로 처리할 수 있다.
 * 새 상태가 늘면 분기 누락이 컴파일 에러로 드러난다.
 */
sealed interface MediaInput {
    /** 사용자가 이번 세션에 새로 고른 로컬 파일(`content://` URI). 업로드 대상. */
    data class Local(
        val uri: String,
    ) : MediaInput

    /** 이미 서버에 있는 원격 URL(영구 public 또는 presigned). 그대로 유지. */
    data class Remote(
        val url: String,
    ) : MediaInput

    /** 미첨부. */
    data object None : MediaInput
}
