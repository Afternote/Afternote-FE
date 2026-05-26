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
    fun `getInitialConsonant - 쌍자음 초성`() {
        assertEquals('ㄲ', KoreanConsonantUtil.getInitialConsonant("까"))
        assertEquals('ㄸ', KoreanConsonantUtil.getInitialConsonant("뜨"))
        assertEquals('ㅃ', KoreanConsonantUtil.getInitialConsonant("뽀"))
        assertEquals('ㅆ', KoreanConsonantUtil.getInitialConsonant("쓰"))
        assertEquals('ㅉ', KoreanConsonantUtil.getInitialConsonant("쪼"))
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
}
