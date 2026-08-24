package com.afternote.core.common.util

import java.text.Normalizer

/**
 * 한글 초성(자음) 관련 유틸리티
 */
object KoreanConsonantUtil {
    /**
     * 완성형 음절의 초성 인덱스 → 초성 문자 룩업.
     *
     * 호환 자모 블록(`U+3131`~)은 겹받침이 섞여 있어 초성 순서가 아니므로 계산으로 대체할 수 없다.
     * 섹션 키 정렬 순서도 이 리스트의 인덱스를 쓴다.
     */
    private val KOREAN_CONSONANTS =
        listOf(
            'ㄱ',
            'ㄲ',
            'ㄴ',
            'ㄷ',
            'ㄸ',
            'ㄹ',
            'ㅁ',
            'ㅂ',
            'ㅃ',
            'ㅅ',
            'ㅆ',
            'ㅇ',
            'ㅈ',
            'ㅉ',
            'ㅊ',
            'ㅋ',
            'ㅌ',
            'ㅍ',
            'ㅎ',
        )

    /**
     * 쌍자음 → 기본 자음.
     *
     * 초성 인덱스 바가 기본 자음만 라벨로 그리므로, 섹션 키도 거기에 맞춰 접는다.
     * 접지 않으면 `까치` 가 바에 없는 ㄲ 섹션으로 갈려 점프도 하이라이트도 안 된다.
     */
    private val COMPOUND_TO_BASE =
        mapOf(
            'ㄲ' to 'ㄱ',
            'ㄸ' to 'ㄷ',
            'ㅃ' to 'ㅂ',
            'ㅆ' to 'ㅅ',
            'ㅉ' to 'ㅈ',
        )

    private const val KOREAN_UNICODE_START = 0xAC00
    private const val KOREAN_UNICODE_END = 0xD7A3
    private const val VOWEL_COUNT = 21
    private const val FINAL_CONSONANT_COUNT = 28

    /** 한글 완성형이 아닌 이름이 모이는 섹션 키 */
    const val NON_KOREAN_SECTION = '#'

    /**
     * 문자열의 첫 글자에서 초성 섹션 키를 추출합니다.
     *
     * 쌍자음은 기본 자음으로 접고(`까` → `ㄱ`), 한글 완성형이 아니면 [NON_KOREAN_SECTION] 을 반환합니다.
     */
    fun getInitialConsonant(text: String): Char = sectionKeyOf(normalize(text))

    /**
     * 리스트를 초성별로 그룹화합니다.
     *
     * 그룹 내부는 가나다 순입니다 — 한글 완성형은 코드포인트 순서가 곧 가나다 순이라 별도 `Collator` 가 필요 없습니다.
     *
     * 다만 [NON_KOREAN_SECTION] 그룹에는 영문이 섞이는데, 코드포인트 순은 대문자를 전부 소문자 앞으로 몰아
     * `Benny` · `anna` · `carol` 처럼 늘어선다. 대소문자를 무시해야 사람이 기대하는 순서가 된다.
     * 한글에는 대소문자가 없어 이 비교기를 써도 위의 가나다 순이 그대로 유지된다.
     *
     * @param items 그룹화할 아이템 리스트
     * @param keySelector 초성을 추출할 문자열을 반환하는 함수
     * @return 초성을 키로 하고 해당 초성으로 시작하는 아이템 리스트를 값으로 하는 Map
     */
    fun <T> groupByInitialConsonant(
        items: List<T>,
        keySelector: (T) -> String,
    ): Map<Char, List<T>> =
        items
            .map { it to normalize(keySelector(it)) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { (_, name) -> name })
            .groupBy({ (_, name) -> sectionKeyOf(name) }) { (item, _) -> item }
            .toSortedMap(
                compareBy {
                    KOREAN_CONSONANTS.indexOf(it).takeIf { idx -> idx >= 0 } ?: Int.MAX_VALUE
                },
            )

    /** 정규화가 끝난 문자열에서 섹션 키를 뽑습니다. */
    private fun sectionKeyOf(normalized: String): Char {
        if (normalized.isEmpty()) return NON_KOREAN_SECTION

        val unicode = normalized.first().code
        if (unicode !in KOREAN_UNICODE_START..KOREAN_UNICODE_END) return NON_KOREAN_SECTION

        val consonant =
            KOREAN_CONSONANTS[(unicode - KOREAN_UNICODE_START) / (VOWEL_COUNT * FINAL_CONSONANT_COUNT)]
        return COMPOUND_TO_BASE[consonant] ?: consonant
    }

    /**
     * NFD 조합형 자모(`U+1100` 블록)로 들어온 이름을 완성형으로 되돌립니다.
     *
     * macOS · iOS 경로로 들어온 이름이 조합형이면 완성형 범위 밖이라 전부 [NON_KOREAN_SECTION] 으로 떨어진다.
     */
    private fun normalize(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFC)
}
