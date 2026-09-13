package com.afternote.feature.onboarding.presentation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingPasswordRuleTest {
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
            assertTrue("${case.description}: ${case.password}", OnboardingPasswordRule.isSatisfied(case.password))
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
                PasswordCase("전각 숫자", "Aa\uFF11!aaaa"),
                PasswordCase("아라비아-인도 숫자", "Aa\u0661!aaaa"),
            )

        rejectedPasswords.forEach { case ->
            assertFalse("${case.description}: ${case.password}", OnboardingPasswordRule.isSatisfied(case.password))
        }
    }
}

private data class PasswordCase(
    val description: String,
    val password: String,
)
