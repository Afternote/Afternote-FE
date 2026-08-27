package com.afternote.core.domain.model

/**
 * 업로드가 끝난 파일. presigned 응답이 내려주는 두 값을 그대로 들고 있는다.
 *
 * 종전에는 [fileUrl] 하나만 돌려줘, 키가 필요한 호출부가 URL 앞을 잘라 역산했다. 그 역산은
 * `fileUrl == "<스킴>://<호스트>/" + fileKey` 를 가정하는데, 경로 프리픽스나 쿼리스트링이
 * 붙는 순간 조용히 어긋난 키가 나간다 (#1017). 두 값을 다 실어 보내 추측을 없앤다.
 */
data class UploadedFile(
    /** 화면에 그대로 띄울 수 있는 전체 URL. */
    val fileUrl: String,
    /** 서버가 파일을 식별하는 키. */
    val fileKey: String,
)
