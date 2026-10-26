package com.afternote.konsist

import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import org.junit.Test

/**
 * 데일리 알림 예약은 로그인 관찰자 한 곳에서만 한다 (#2146).
 *
 * 데일리 알림 문구는 계정의 기록을 재촉한다. 세션을 보지 않는 자리에서 예약을 넣으면 로그인하지 않은
 * 기기에도 매일 알림이 뜬다. #2146 이 그 경우였다. App Startup Initializer 가 기동마다 예약했고,
 * Initializer 는 Hilt 에 닿지 못해 세션을 볼 수 없었다.
 *
 * 지금은 `DailyNotificationScheduleSynchronizer` 가 로그인 상태를 보고 예약과 취소를 함께 맡는다.
 * 다른 자리에서 예약을 넣으면 그 관찰자의 취소와 어긋나고, 로그아웃한 기기에 예약이 다시 생긴다.
 *
 * 관찰자를 `GlobalApplication` 이 시작하는지도 본다. 시작 줄을 지워도 컴파일은 통과하고, 그러면
 * 로그인한 사용자도 알림을 못 받는다.
 */
class DailyNotificationScheduleKonsistTest {
    @Test
    fun `데일리 알림 예약은 로그인 관찰자만 부른다`() {
        val callers =
            AfternoteKonsistScope.productionFiles
                .filter { SCHEDULE_REFERENCE.containsMatchIn(it.text.withoutComments()) }
                .map { it.normalizedProjectPath() }
                .toSet()

        check(OBSERVER_PATH in callers) {
            "$OBSERVER_PATH 에서 scheduleDailyNotification 호출을 못 찾았다. 관찰자를 옮기거나 이름을 바꿨으면 " +
                "이 가드를 함께 고칠 것. 못 찾은 채 초록이면 이 가드는 아무것도 안 본 것이다."
        }

        val violations = (callers - OBSERVER_PATH).sorted()
        check(violations.isEmpty()) {
            buildString {
                appendLine("로그인 관찰자 밖에서 데일리 알림을 예약한다 (${violations.size}건).")
                appendLine("세션을 보지 않고 예약하면 로그인하지 않은 기기에도 매일 알림이 뜬다 (#2146).")
                appendLine()
                violations.forEach { appendLine("  $it") }
                appendLine()
                appendLine("예약·취소는 $OBSERVER_PATH 가 로그인 상태를 보고 한다.")
            }
        }
    }

    @Test
    fun `GlobalApplication 이 데일리 알림 관찰자를 시작한다`() {
        val source = singleFile(APPLICATION_PATH).text.withoutComments()
        val property = OBSERVER_PROPERTY.find(source)?.groupValues?.get(1)

        check(property != null) {
            "$APPLICATION_PATH 에 $OBSERVER_CLASS 주입 필드가 없다. 관찰자를 시작하지 않으면 로그인한 사용자도 " +
                "데일리 알림을 못 받는다 (#2146)."
        }
        check(Regex("""\b${Regex.escape(property)}\s*\.\s*observeLogin\s*\(""").containsMatchIn(source)) {
            "$APPLICATION_PATH 가 $property.observeLogin() 을 부르지 않는다. 주입만 하고 시작하지 않으면 " +
                "예약도 취소도 일어나지 않는다 (#2146)."
        }
    }

    private fun singleFile(projectPath: String): KoFileDeclaration {
        val candidates = AfternoteKonsistScope.files.filter { it.normalizedProjectPath() == projectPath }

        check(candidates.size == 1) {
            "$projectPath 가 ${candidates.size}건 잡혔다. 파일이 옮겨졌거나 사본이 섞였다.\n" +
                candidates.joinToString("\n") { "  " + it.path }
        }
        return candidates.single()
    }

    private fun String.withoutComments(): String = replace(BLOCK_COMMENT, " ").replace(LINE_COMMENT, " ")

    private fun KoFileDeclaration.normalizedProjectPath(): String = projectPath.replace('\\', '/').trimStart('/')

    private companion object {
        const val OBSERVER_CLASS = "DailyNotificationScheduleSynchronizer"
        const val OBSERVER_PATH =
            "app/src/main/java/com/afternote/afternote_fe/notification/$OBSERVER_CLASS.kt"
        const val APPLICATION_PATH = "app/src/main/java/com/afternote/afternote_fe/GlobalApplication.kt"

        /** 호출과 함수 참조(`::scheduleDailyNotification`)를 잡고, 선언(`fun scheduleDailyNotification`)은 뺀다. */
        val SCHEDULE_REFERENCE = Regex("""(?<!\bfun\s)(?<![A-Za-z0-9_])scheduleDailyNotification(?![A-Za-z0-9_])""")

        /** `lateinit var 이름: DailyNotificationScheduleSynchronizer` 의 이름. */
        val OBSERVER_PROPERTY = Regex("""\b([A-Za-z_][A-Za-z0-9_]*)\s*:\s*$OBSERVER_CLASS\b""")

        val BLOCK_COMMENT = Regex("""/\*[\s\S]*?\*/""")
        val LINE_COMMENT = Regex("""//[^\n]*""")
    }
}
