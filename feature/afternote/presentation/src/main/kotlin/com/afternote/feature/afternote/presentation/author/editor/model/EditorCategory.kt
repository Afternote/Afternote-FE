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
    BUSINESS("비즈니스", "BUSINESS"),
    GALLERY("갤러리 및 파일", "GALLERY"),
    ESTATE("재산 처리", "ESTATE"),
    MEMORIAL("추억 노트", "PLAYLIST"),
    ;

    /**
     * 서비스명 선택(카탈로그·드롭다운)이 있는 카테고리인지.
     *
     * MEMORIAL 은 구버전 에디터 시안(보류_추모가이드라인 행 34:4020)에 서비스명 필드가 없고 카테고리별 아이콘
     * 카탈로그(34:3342)에도 항목이 없으며, 저장 시 고정 라벨을 title 로 쓴다(SaveAfternotePayloadBuilder).
     * 단, 현행 정본 에디터 시안은 부재(NEW 기억 노트 섹션은 상세·플레이리스트까지만 재설계) — 신규 시안 도착 시 재확인.
     * ESTATE 는 하위 항목 4종(뱅킹·부동산·증권·유산)이 시안 카탈로그(34:3342)에 정의돼 있으나
     * 에디터 화면 시안이 미설계라 서비스명 UI 를 그리지 않는다 — 구현이 열리면 카탈로그와 함께
     * true 대상이 된다 (추적 이슈 #491). 카탈로그의 "제목만 구현" 주석은 하위 항목을 아이콘 없이
     * 제목 텍스트로만 표시한다는 뜻으로 읽힌다 (아이콘 정리 프레임에서 이 열만 아이콘 부재).
     */
    val hasServiceSelection: Boolean
        get() = this == SOCIAL || this == BUSINESS || this == GALLERY

    companion object {
        /** UI 표시 문자열 → EditorCategory. 일치하지 않으면 SOCIAL 반환. */
        fun fromDisplayLabel(label: String): EditorCategory = entries.find { it.displayLabel == label } ?: SOCIAL

        /** 네비게이션 키(enum name) → EditorCategory. 일치하지 않으면 SOCIAL 반환. */
        fun fromNavKey(key: String): EditorCategory = entries.find { it.name == key } ?: SOCIAL

        /** 서버 카테고리 코드 → EditorCategory. */
        fun fromServerValue(value: String): EditorCategory =
            when (value.uppercase()) {
                "SOCIAL" -> SOCIAL
                "BUSINESS" -> BUSINESS
                "GALLERY" -> GALLERY
                "ESTATE" -> ESTATE
                "PLAYLIST", "MUSIC" -> MEMORIAL
                else -> SOCIAL
            }
    }
}
