package com.afternote.feature.afternote.presentation.author.editor.model

/**
 * 편집 화면 카테고리.
 *
 * [displayLabel]은 UI에 보여지는 한국어 문자열,
 * [serverValue]는 서버 API에 전송하는 코드 문자열입니다.
 */
enum class EditorCategory(
    val displayLabel: String,
    val serverValue: String,
) {
    SOCIAL("소셜네트워크", "SOCIAL"),
    GALLERY("갤러리 및 파일", "GALLERY"),
    MEMORIAL("추모 가이드라인", "PLAYLIST"),
    ;

    companion object {
        /** UI 표시 문자열 → EditorCategory. 일치하지 않으면 SOCIAL 반환. */
        fun fromDisplayLabel(label: String): EditorCategory = entries.find { it.displayLabel == label } ?: SOCIAL

        /** 서버 카테고리 코드 → EditorCategory. */
        fun fromServerValue(value: String): EditorCategory =
            when (value.uppercase()) {
                "SOCIAL" -> SOCIAL
                "GALLERY" -> GALLERY
                "PLAYLIST", "MUSIC" -> MEMORIAL
                else -> SOCIAL
            }
    }
}
