package com.afternote.feature.afternote.domain.repository.author

/**
 * 추모 음성으로 서버가 받아 주는 형식 (#1118).
 *
 * **정본은 서버다.** `S3Service.AUDIO_EXTENSIONS = {mp3, m4a, wav}` 이고, 저장 시
 * `PlaylistValidationStrategy.validatePlaylistMedia` 가 `memorialAudioUrl` 의 확장자를
 * `MediaKind.AUDIO` 로 다시 검사해 벗어나면 400(INVALID_FILE_EXTENSION) 을 낸다.
 * 즉 **업로드가 성공해도 확장자가 다르면 저장 단계에서 버려진다.**
 *
 * 그래서 판정을 한 곳에 둔다 — 첨부 시점(presentation)의 사전 차단과 업로드 시점(data)의
 * presigned 확장자 결정이 같은 표를 봐야, 화면은 통과했는데 저장이 400 으로 끝나는 어긋남이 안 생긴다.
 * 안드로이드 기본 녹음기가 내놓는 `audio/amr`·`audio/3gpp` 는 여기 없다 — 서버가 안 받으므로
 * 첨부 단계에서 막고 안내한다.
 */
object MemorialAudioFormats {
    /** MIME → 서버가 받는 확장자. presigned 발급에 그대로 실린다. */
    private val MIME_TO_EXTENSION =
        mapOf(
            "audio/mpeg" to "mp3",
            "audio/mp3" to "mp3",
            "audio/x-mpeg" to "mp3",
            "audio/mp4" to "m4a",
            "audio/m4a" to "m4a",
            "audio/x-m4a" to "m4a",
            "audio/aac" to "m4a",
            "audio/mp4a-latm" to "m4a",
            "audio/wav" to "wav",
            "audio/wave" to "wav",
            "audio/x-wav" to "wav",
            "audio/vnd.wave" to "wav",
        )

    /** 파일 선택기(SAF)에 넘길 MIME 필터. 고른 뒤 다시 [extensionFor] 로 확인한다 — 필터는 기기마다 느슨하다. */
    val supportedMimeTypes: List<String> = MIME_TO_EXTENSION.keys.sorted()

    /**
     * @return 서버가 받는 확장자, 지원하지 않는 형식(또는 MIME 을 못 읽은 경우)이면 `null`.
     *   대소문자·`;charset=` 파라미터가 붙어 오는 기기가 있어 정규화한 뒤 본다.
     */
    fun extensionFor(mimeType: String?): String? {
        val normalized =
            mimeType
                ?.substringBefore(';')
                ?.trim()
                ?.lowercase()
                ?: return null
        return MIME_TO_EXTENSION[normalized]
    }
}
