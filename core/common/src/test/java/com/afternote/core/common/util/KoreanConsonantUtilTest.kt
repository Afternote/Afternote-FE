package com.afternote.core.common.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [KoreanConsonantUtil] 의 한글 초성 분류 / 그룹화 회귀 가드.
 * 입출력이 명확한 순수 함수라 분기/경계 케이스만 고정.
 */
class KoreanConsonantUtilTest {
    // ========================================
    // getInitialConsonant
    // ========================================

    @Test
    fun `getInitialConsonant - 빈 문자열은 # 반환`() {
        assertEquals('#', KoreanConsonantUtil.getInitialConsonant(""))
    }

    @Test
    fun `getInitialConsonant - 비한글 첫 글자는 # 반환`() {
        assertEquals('#', KoreanConsonantUtil.getInitialConsonant("apple"))
        assertEquals('#', KoreanConsonantUtil.getInitialConsonant("123"))
        assertEquals('#', KoreanConsonantUtil.getInitialConsonant("!hello"))
    }

    @Test
    fun `getInitialConsonant - 자음 단독은 한글 음절 범위 밖이라 # 반환`() {
        // 'ㄱ' (U+3131) 은 KOREAN_UNICODE_START(U+AC00) 미만이라 한글 음절 아님.
        assertEquals('#', KoreanConsonantUtil.getInitialConsonant("ㄱ"))
    }

    @Test
    fun `getInitialConsonant - 받침 없는 한글 음절`() {
        assertEquals('ㄱ', KoreanConsonantUtil.getInitialConsonant("가"))
        assertEquals('ㅎ', KoreanConsonantUtil.getInitialConsonant("하"))
    }

    @Test
    fun `getInitialConsonant - 받침 있는 한글 음절`() {
        assertEquals('ㄱ', KoreanConsonantUtil.getInitialConsonant("강"))
        assertEquals('ㅎ', KoreanConsonantUtil.getInitialConsonant("힣"))
    }

    @Test
    fun `getInitialConsonant - 쌍자음은 기본 자음 섹션으로 접힌다`() {
        // 인덱스 바 라벨이 기본 자음뿐이라, ㄲ 섹션을 따로 만들면 점프도 하이라이트도 안 된다.
        assertEquals('ㄱ', KoreanConsonantUtil.getInitialConsonant("까"))
        assertEquals('ㄷ', KoreanConsonantUtil.getInitialConsonant("뜨"))
        assertEquals('ㅂ', KoreanConsonantUtil.getInitialConsonant("뽀"))
        assertEquals('ㅅ', KoreanConsonantUtil.getInitialConsonant("쓰"))
        assertEquals('ㅈ', KoreanConsonantUtil.getInitialConsonant("쪼"))
    }

    @Test
    fun `getInitialConsonant - NFD 조합형 자모도 완성형으로 정규화해 판정`() {
        // macOS·iOS 경로로 들어온 이름은 U+1100 블록 조합형이라 완성형 범위 밖으로 떨어진다.
        val nfdGang = "\u1100\u1161\u11BC" // 조합형 '강'
        assertEquals('ㄱ', KoreanConsonantUtil.getInitialConsonant(nfdGang))

        val nfdHana = "\u1112\u1161\u1102\u1161" // 조합형 '하나'
        assertEquals('ㅎ', KoreanConsonantUtil.getInitialConsonant(nfdHana))
    }

    @Test
    fun `getInitialConsonant - 한글 음절 범위 경계값`() {
        // U+AC00 = '가' (시작), U+D7A3 = '힣' (끝)
        assertEquals('ㄱ', KoreanConsonantUtil.getInitialConsonant("가"))
        assertEquals('ㅎ', KoreanConsonantUtil.getInitialConsonant("힣"))
    }

    @Test
    fun `getInitialConsonant - 첫 글자만 본다`() {
        assertEquals('ㅇ', KoreanConsonantUtil.getInitialConsonant("안녕하세요"))
        assertEquals('#', KoreanConsonantUtil.getInitialConsonant("a가"))
    }

    // ========================================
    // groupByInitialConsonant
    // ========================================

    @Test
    fun `groupByInitialConsonant - 빈 리스트는 빈 맵`() {
        val result = KoreanConsonantUtil.groupByInitialConsonant(emptyList<String>()) { it }
        assertEquals(emptyMap<Char, List<String>>(), result)
    }

    @Test
    fun `groupByInitialConsonant - 한글 초성별 그룹화`() {
        val items = listOf("강아지", "고양이", "나무", "다람쥐")
        val result = KoreanConsonantUtil.groupByInitialConsonant(items) { it }

        assertEquals(listOf("강아지", "고양이"), result['ㄱ'])
        assertEquals(listOf("나무"), result['ㄴ'])
        assertEquals(listOf("다람쥐"), result['ㄷ'])
    }

    @Test
    fun `groupByInitialConsonant - 한글 초성 순서 ㄱ-ㅎ 보장`() {
        val items = listOf("하늘", "가방", "나비")
        val result = KoreanConsonantUtil.groupByInitialConsonant(items) { it }

        // 정렬된 키 순서는 ㄱ < ㄴ < ... < ㅎ
        assertEquals(listOf('ㄱ', 'ㄴ', 'ㅎ'), result.keys.toList())
    }

    @Test
    fun `groupByInitialConsonant - 비한글은 # 그룹 + 한글 뒤로 정렬`() {
        val items = listOf("apple", "가방", "banana")
        val result = KoreanConsonantUtil.groupByInitialConsonant(items) { it }

        // '#' 은 KOREAN_CONSONANTS.indexOf == -1 → Int.MAX_VALUE 로 정렬돼 한글보다 뒤.
        assertEquals(listOf('ㄱ', '#'), result.keys.toList())
        assertEquals(listOf("apple", "banana"), result['#'])
        assertEquals(listOf("가방"), result['ㄱ'])
    }

    @Test
    fun `groupByInitialConsonant - keySelector 로 필드 추출`() {
        data class Item(
            val name: String,
        )
        val items = listOf(Item("강아지"), Item("나무"))

        val result = KoreanConsonantUtil.groupByInitialConsonant(items) { it.name }

        assertEquals(listOf(Item("강아지")), result['ㄱ'])
        assertEquals(listOf(Item("나무")), result['ㄴ'])
    }

    @Test
    fun `groupByInitialConsonant - 그룹 내부가 가나다 순`() {
        // 서버는 이름순을 보장하지 않는다. 입력 순서가 그대로 새면 초성 점프 UI 에서 섹션 안이 무순서가 된다.
        val items = listOf("김영희", "강민", "고은", "나래", "김철수")

        val result = KoreanConsonantUtil.groupByInitialConsonant(items) { it }

        assertEquals(listOf("강민", "고은", "김영희", "김철수"), result['ㄱ'])
        assertEquals(listOf("나래"), result['ㄴ'])
    }

    @Test
    fun `groupByInitialConsonant - 쌍자음 이름도 기본 자음 그룹에 가나다 순으로 섞인다`() {
        val items = listOf("까치", "강민", "김영희")

        val result = KoreanConsonantUtil.groupByInitialConsonant(items) { it }

        assertEquals(listOf('ㄱ'), result.keys.toList())
        assertEquals(listOf("강민", "김영희", "까치"), result['ㄱ'])
    }

    @Test
    fun `groupByInitialConsonant - NFD 조합형 이름도 같은 그룹에 정렬돼 들어간다`() {
        val nfdGo = "\u1100\u1169\u110B\u1173\u11AB" // 조합형 '고은'
        val items = listOf("김영희", nfdGo, "강민")

        val result = KoreanConsonantUtil.groupByInitialConsonant(items) { it }

        assertEquals(listOf('ㄱ'), result.keys.toList())
        assertEquals(listOf("강민", nfdGo, "김영희"), result['ㄱ'])
    }

    @Test
    fun `groupByInitialConsonant - # 그룹 내부도 정렬된다`() {
        val items = listOf("zoe", "123", "alice")

        val result = KoreanConsonantUtil.groupByInitialConsonant(items) { it }

        assertEquals(listOf("123", "alice", "zoe"), result['#'])
    }

    @Test
    fun `groupByInitialConsonant - # 그룹은 대소문자를 무시하고 정렬된다`() {
        // 코드포인트 순으로 두면 대문자가 전부 소문자 앞으로 몰려 Benny · anna · carol 로 늘어선다.
        val items = listOf("carol", "Benny", "anna")

        val result = KoreanConsonantUtil.groupByInitialConsonant(items) { it }

        assertEquals(listOf("anna", "Benny", "carol"), result['#'])
    }

    @Test
    fun `groupByInitialConsonant - 대소문자 무시 비교기가 한글 가나다 순을 바꾸지 않는다`() {
        val items = listOf("김영희", "강민", "까치", "고은")

        val result = KoreanConsonantUtil.groupByInitialConsonant(items) { it }

        assertEquals(listOf("강민", "고은", "김영희", "까치"), result['ㄱ'])
    }
}
