package com.afternote.core.domain.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 새 비밀번호 규칙이 서버 `PasswordValidation.REGEX` 와 같은 판정을 내는지 지킨다.
 *
 * 사례는 dev 서버에 실제로 보내 본 결과(1차·2차)를 그대로 옮겼다. 유니코드 숫자는 **허용 목록 밖
 * 문자라서** 거절된다 — `[0-9]` 를 `\d` 로 되돌리는 변이는 이 단언으로 잡히지 않는다. 허용 목록
 * `[A-Za-z0-9@$!%*#?&]` 이 이미 그 문자들을 닫고 있어 두 정규식의 판정이 갈리는 입력이 없고, JVM
 * 유닛 테스트는 호스트 정규식 엔진이라 Android 의 유니코드 문자 클래스도 재현하지 못한다. `\d` 를
 * 쓰지 않는 근거는 [NewPasswordRule] KDoc 이 갖는다.
 */
class NewPasswordRuleTest {
    @Test
    fun `dev 서버를 통과한 비밀번호를 모두 허용한다`() {
        val acceptedPasswords =
            listOf(
                PasswordCase("1차 #1", "Password1!"),
                PasswordCase("1차 #2 - 15자", "Password12345!a"),
                PasswordCase("1차 #7 - 소문자 영문", "password12345!a"),
                PasswordCase("2차 기준", "Aa1!aaaa"),
                PasswordCase("2차 15자", "Aa1!aaaaaaaaaaa"),
                PasswordCase("2차 대문자만", "AA1!AAAA"),
                PasswordCase("2차 소문자만", "aa1!aaaa"),
                PasswordCase("허용 특수문자 @", "Aa1@aaaa"),
                PasswordCase("허용 특수문자 $", "Aa1\$aaaa"),
                PasswordCase("허용 특수문자 !", "Aa1!aaaa"),
                PasswordCase("허용 특수문자 %", "Aa1%aaaa"),
                PasswordCase("허용 특수문자 *", "Aa1*aaaa"),
                PasswordCase("허용 특수문자 #", "Aa1#aaaa"),
                PasswordCase("허용 특수문자 ?", "Aa1?aaaa"),
                PasswordCase("허용 특수문자 &", "Aa1&aaaa"),
            )

        acceptedPasswords.forEach { case ->
            assertTrue("${case.description}: ${case.password}", NewPasswordRule.isSatisfied(case.password))
        }
    }

    @Test
    fun `dev 서버가 거절한 비밀번호와 Unicode 숫자를 모두 차단한다`() {
        val rejectedPasswords =
            listOf(
                PasswordCase("1차 #3 - 16자", "Password12345!ab"),
                PasswordCase("1차 #4 - 하이픈", "Password123-abc"),
                PasswordCase("1차 #5 - 언더바", "Password123_abc"),
                PasswordCase("1차 #6 - 물결", "Password123~abc"),
                PasswordCase("1차 #8 - 한글", "Password1가나다"),
                PasswordCase("1차 #9 - 공백", "Password1 abc"),
                PasswordCase("2차 하이픈", "Aa1!aaa-"),
                PasswordCase("2차 언더바", "Aa1!aaa_"),
                PasswordCase("2차 물결", "Aa1!aaa~"),
                PasswordCase("2차 마침표", "Aa1!aaa."),
                PasswordCase("2차 한글", "Aa1!aaa가"),
                PasswordCase("2차 중간 공백", "Aa1!a a a"),
                PasswordCase("2차 7자", "Aa1!aaa"),
                PasswordCase("2차 16자", "Aa1!aaaaaaaaaaaa"),
                PasswordCase("영문 없음", "12345678!"),
                PasswordCase("숫자 없음", "Abcdefgh!"),
                PasswordCase("특수문자 없음", "Abcdefg12"),
                PasswordCase("전각 숫자", "Aa１!aaaa"),
                PasswordCase("아라비아-인도 숫자", "Aa١!aaaa"),
            )

        rejectedPasswords.forEach { case ->
            assertFalse("${case.description}: ${case.password}", NewPasswordRule.isSatisfied(case.password))
        }
    }
}

private data class PasswordCase(
    val description: String,
    val password: String,
)
