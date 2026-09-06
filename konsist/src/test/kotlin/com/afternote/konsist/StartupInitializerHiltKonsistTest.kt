package com.afternote.konsist

import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import org.junit.Test

/**
 * `androidx.startup.Initializer` 구현체는 Hilt 엔트리포인트를 꺼내지 않는다 (#1889).
 *
 * `Initializer.create()` 는 `ContentProvider` 단계에서, 즉 `Application.onCreate()` **이전**에 돈다.
 * 거기서 `EntryPointAccessors.fromApplication(...)` 으로 Hilt 그래프를 만지면 프로덕션에선 우연히
 * 되지만, 앱 모듈 androidTest 는 `HiltTestApplication` 이 아직 그래프를 세우기 전이라 **전멸한다** —
 * 테스트마다 같은 자리에서 죽고 원인은 앱 코드 쪽에 있어 한참을 헤맨다.
 *
 * Hilt 가 필요한 시작 훅은 `GlobalApplication.onCreate` 에 둔다. `Initializer` 는 Hilt 없이 되는
 * 일(SDK 초기화·WorkManager 예약)만 맡는다.
 */
class StartupInitializerHiltKonsistTest {
    @Test
    fun `Initializer 구현체는 Hilt 엔트리포인트에 닿지 않는다`() {
        val initializers = initializerClasses()
        val seen = initializers.map { it.name }.toSet()

        check(KNOWN_INITIALIZERS.all { it in seen }) {
            "알던 Initializer 를 못 찾았다: ${KNOWN_INITIALIZERS - seen}. 옮겼거나 지웠으면 이 목록을 함께 고칠 것 — 못 찾은 채 초록이면 이 가드는 아무것도 안 본 것이다."
        }

        val violations =
            initializers
                .filter { initializer ->
                    val code = initializer.containingFile.text.withoutComments()
                    HILT_ENTRY_POINT_ACCESSORS.any { it in code }
                }.map { "${it.containingFile.normalizedProjectPath()} — ${it.name}" }

        check(violations.isEmpty()) {
            buildString {
                appendLine("Initializer 안에서 Hilt 엔트리포인트를 꺼낸다 (${violations.size}건).")
                appendLine("Initializer 는 Application.onCreate 이전에 돌아 앱 모듈 androidTest 가 전멸한다.")
                appendLine()
                violations.sorted().forEach { appendLine("  $it") }
                appendLine()
                appendLine("Hilt 가 필요한 시작 훅은 GlobalApplication.onCreate 로 옮긴다 (#1889).")
            }
        }
    }

    private fun initializerClasses(): List<KoClassDeclaration> =
        AfternoteKonsistScope.productionFiles
            .flatMap { it.classes() }
            // konsist 는 부모 이름을 제네릭 인자까지 붙여 준다(`Initializer<Unit>`) — 타입 인자 앞까지만 본다.
            .filter { klass -> klass.parents().any { parent -> parent.name.substringBefore('<').substringAfterLast('.') == INITIALIZER } }

    private fun String.withoutComments(): String = replace(BLOCK_COMMENT, " ").replace(LINE_COMMENT, " ")

    private fun com.lemonappdev.konsist.api.declaration.KoFileDeclaration.normalizedProjectPath(): String =
        projectPath.replace('\\', '/').trimStart('/')

    private companion object {
        const val INITIALIZER = "Initializer"

        /** 지금 저장소의 Initializer 구현. 하나라도 안 보이면 스캔이 잘못된 것이다. */
        val KNOWN_INITIALIZERS = setOf("KakaoInitializer", "DailyNotificationInitializer")

        val HILT_ENTRY_POINT_ACCESSORS = listOf("EntryPointAccessors", "EntryPoints.get")

        val BLOCK_COMMENT = Regex("""/\*[\s\S]*?\*/""")
        val LINE_COMMENT = Regex("""//[^\n]*""")
    }
}
