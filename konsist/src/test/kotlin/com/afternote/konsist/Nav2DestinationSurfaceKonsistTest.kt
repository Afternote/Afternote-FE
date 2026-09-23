package com.afternote.konsist

import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import org.junit.Test

/**
 * Nav2 루트 목적지의 불투명 바탕 결선 가드 (#2145).
 *
 * predictive back 진행 중 줄어든 앞 화면 아래에는 뒤 화면이 그려진다. 화면 대부분이 투명한
 * `Scaffold` 라 목적지 자리가 바탕을 깔지 않으면 뒤 화면 글자가 앞 화면 사이로 비친다.
 * Nav3 로컬 스택은 core/ui 표준 데코레이터가 entry 마다 바탕을 깔고, 그 결선은
 * `FeatureNavDisplayTest` 가 픽셀로 본다. **Nav2 `NavHost` 에는 목적지 단위로 감쌀 자리가 없어서**
 * 등록부(`composable<T> { … }`)가 직접 `NavDestinationSurface` 로 감싸야 하고, 빠뜨려도 컴파일이
 * 통과한다. 그 빠뜨림을 소스에서 본다.
 *
 * ### 무엇을 목적지로 세는가
 *
 * 파일 경로를 박지 않고 내용으로 찾는다. `androidx.navigation.compose.composable` 을 import 하는
 * 프로덕션 파일의 `composable<…>` 호출이 전부 대상이다. 설정처럼 등록 헬퍼 안에서 한 번 감싸면
 * 그 헬퍼 안의 호출 하나만 세진다. 설정이 Nav3 로 옮겨 가 `SettingNavGraph.kt` 가 사라져도(#1695 사슬)
 * 이 가드는 그대로 돈다.
 *
 * 목적지 몸통이 아래 둘 중 하나면 통과다.
 * 1. `NavDestinationSurface` 를 부른다.
 * 2. `FeatureNavDisplay` 를 부르는 로컬 스택 host 를 부른다. 그 안의 entry 는 데코레이터가 칠한다.
 *
 * ### 예외
 *
 * - 남의 모듈([HANDOVER_EXEMPT_MODULES]): 담당자 인계 이슈가 받는다. 감싼 뒤 그 줄을 지운다.
 * - [BOTTOM_ONLY_ROUTES]: 루트 백스택의 바닥에만 서서 predictive back 의 앞 화면이 되지 않는
 *   목적지. 감싸도 보이는 것이 달라지지 않아 감싸지 않았다. 그 위치가 바뀌면 예외를 지운다.
 *
 * ### 못 잡는 것
 *
 * 바탕의 **색**과 실제로 불투명하게 그려지는지는 픽셀 판정이라 텍스트로 못 본다. Nav3 쪽은
 * `FeatureNavDisplayTest` 가, 기기 확인은 `docs/qa/predictive-back.md` 의 수동 절차가 받는다.
 */
class Nav2DestinationSurfaceKonsistTest {
    @Test
    fun `Nav2 목적지는 불투명한 바탕을 깐다`() {
        // 공유 진입점을 쓴다 — 워크트리 사본이 섞이면 남의 브랜치 소스로 헛짚는다 (#1659, ScanScopeKonsistTest).
        val files = AfternoteKonsistScope.productionFiles
        val hosts = localStackHosts(files)
        val destinations = nav2Destinations(files).filterNot { it.isExempt() }

        check(destinations.isNotEmpty()) {
            "Nav2 목적지(composable<…>)를 하나도 찾지 못했다. 루트가 NavDisplay 로 바뀌었다면(#1702) " +
                "이 가드를 지우고 바탕 결선은 core/ui 표준 데코레이터(FeatureNavDisplayTest)가 받는다."
        }

        val violations = destinations.filterNot { it.isCovered(hosts) }
        check(violations.isEmpty()) {
            "바탕 없이 등록된 Nav2 목적지가 있다 (#2145).\n" +
                violations.joinToString("\n") { "  ${it.path} : composable<${it.route}>" } +
                "\n몸통을 NavDestinationSurface { … } 로 감싼다. 설정은 settingDestination<T> 로 등록한다.\n" +
                "감싸지 않으면 predictive back 진행 중 줄어든 앞 화면 사이로 뒤 화면 글자가 비친다."
        }
    }

    @Test
    fun `목적지 몸통을 읽는 규칙은 감싸기와 host 를 가른다`() {
        val source =
            """
            import androidx.navigation.compose.composable
            fun NavGraphBuilder.graph() {
                // composable<Commented> { Screen() }
                composable<Wrapped> { NavDestinationSurface { Screen() } }
                composable<Bare>(deepLinks = listOf()) { entry -> Screen(label = "NavDestinationSurface") }
                composable<Host> { AfternoteNavHost(navigationCallbacks = callbacks) }
            }
            """.trimIndent()

        val found = destinationsIn(path = "sample.kt", source = source).associateBy { it.route }

        check(found.keys == setOf("Wrapped", "Bare", "Host")) { "주석 속 호출까지 셌거나 빠뜨렸다: ${found.keys}" }
        check(found.getValue("Wrapped").isCovered(emptySet())) { "NavDestinationSurface 로 감싼 몸통을 못 알아봤다" }
        check(!found.getValue("Bare").isCovered(emptySet())) { "문자열 속 이름을 감싸기로 셌다" }
        check(found.getValue("Host").isCovered(setOf("AfternoteNavHost"))) { "로컬 스택 host 를 못 알아봤다" }
        check(!found.getValue("Host").isCovered(emptySet())) { "host 가 아닌 호출을 host 로 셌다" }
    }

    private data class Nav2Destination(
        val path: String,
        val route: String,
        val body: String,
    ) {
        fun isCovered(hosts: Set<String>): Boolean = body.callsIdentifier(SURFACE) || hosts.any { host -> body.callsIdentifier(host) }

        fun isExempt(): Boolean = HANDOVER_EXEMPT_MODULES.keys.any { module -> path.startsWith("$module/") } || route in BOTTOM_ONLY_ROUTES
    }

    /** `FeatureNavDisplay` 를 부르는 프로덕션 함수 — 그 안의 entry 는 표준 데코레이터가 칠한다. */
    private fun localStackHosts(files: List<KoFileDeclaration>): Set<String> =
        files
            .flatMap { it.functions() }
            .filter { it.name != LOCAL_STACK_DISPLAY && codeOnly(it.text).callsIdentifier(LOCAL_STACK_DISPLAY) }
            .map { it.name }
            .toSet()

    private fun nav2Destinations(files: List<KoFileDeclaration>): List<Nav2Destination> =
        files
            .filter { it.text.contains(NAV2_COMPOSABLE_IMPORT) }
            .flatMap { destinationsIn(path = it.normalizedProjectPath(), source = it.text) }

    /** `composable<Route>` 호출마다 route 와 후행 람다 몸통을 뽑는다. 주석·문자열은 먼저 지운다. */
    private fun destinationsIn(
        path: String,
        source: String,
    ): List<Nav2Destination> {
        val code = codeOnly(source)
        return COMPOSABLE_CALL
            .findAll(code)
            .mapNotNull { match ->
                val typeEnd = closingIndex(code, match.range.last, open = '<', close = '>') ?: return@mapNotNull null
                val route = code.substring(match.range.last + 1, typeEnd).trim()
                var cursor = code.skipWhitespace(typeEnd + 1)
                if (code.getOrNull(cursor) == '(') {
                    cursor = code.skipWhitespace((closingIndex(code, cursor, open = '(', close = ')') ?: return@mapNotNull null) + 1)
                }
                if (code.getOrNull(cursor) != '{') return@mapNotNull null
                val bodyEnd = closingIndex(code, cursor, open = '{', close = '}') ?: return@mapNotNull null
                Nav2Destination(path = path, route = route, body = code.substring(cursor + 1, bodyEnd))
            }.toList()
    }

    /** [start] 의 여는 기호와 짝이 맞는 닫는 기호의 위치. */
    private fun closingIndex(
        text: String,
        start: Int,
        open: Char,
        close: Char,
    ): Int? {
        var depth = 0
        for (index in start until text.length) {
            when (text[index]) {
                open -> {
                    depth++
                }

                close -> {
                    depth--
                    if (depth == 0) return index
                }
            }
        }
        return null
    }

    private fun String.skipWhitespace(from: Int): Int {
        var index = from
        while (index < length && this[index].isWhitespace()) index++
        return index
    }

    /**
     * 주석과 문자열 리터럴을 공백으로 바꾼 사본. 목적지 몸통 안의 주석·문자열에 적힌 이름을 호출로
     * 세지 않으려는 것이다. Kotlin 블록 주석은 중첩되므로 깊이를 센다.
     */
    private fun codeOnly(source: String): String {
        val out = StringBuilder(source.length)
        var index = 0
        while (index < source.length) {
            when {
                source.startsWith("//", index) -> {
                    while (index < source.length && source[index] != '\n') index++
                }

                source.startsWith("/*", index) -> {
                    var depth = 0
                    while (index < source.length) {
                        if (source.startsWith("/*", index)) {
                            depth++
                            index += 2
                        } else if (source.startsWith("*/", index)) {
                            depth--
                            index += 2
                            if (depth == 0) break
                        } else {
                            index++
                        }
                    }
                    out.append(' ')
                }

                source.startsWith("\"\"\"", index) -> {
                    val end = source.indexOf("\"\"\"", index + 3).let { if (it < 0) source.length else it + 3 }
                    out.append("\"\"")
                    index = end
                }

                // 문자 리터럴('"', '\'')을 먼저 넘긴다. 안 그러면 따옴표 하나를 문자열 시작으로 읽는다.
                source[index] == '\'' -> {
                    index++
                    while (index < source.length && source[index] != '\'' && source[index] != '\n') {
                        index += if (source[index] == '\\') 2 else 1
                    }
                    index++
                    out.append("' '")
                }

                source[index] == '"' -> {
                    index++
                    while (index < source.length && source[index] != '"' && source[index] != '\n') {
                        index += if (source[index] == '\\') 2 else 1
                    }
                    index++
                    out.append("\"\"")
                }

                else -> {
                    out.append(source[index])
                    index++
                }
            }
        }
        return out.toString()
    }

    // 다른 가드와 같은 관례 — konsist 가 주는 projectPath 를 OS 구분자·선행 슬래시 없이 맞춘다.
    private fun KoFileDeclaration.normalizedProjectPath(): String = projectPath.replace('\\', '/').trimStart('/')

    private companion object {
        const val SURFACE = "NavDestinationSurface"
        const val LOCAL_STACK_DISPLAY = "FeatureNavDisplay"
        const val NAV2_COMPOSABLE_IMPORT = "import androidx.navigation.compose.composable"

        /** `composable<` 호출. 수신 표기(`builder.composable<`)도 잡고, 끝만 겹치는 다른 이름은 배제한다. */
        val COMPOSABLE_CALL = Regex("""(?<![A-Za-z0-9_])composable\s*<""")

        /** 남의 모듈 → 그 몫을 받는 인계 이슈. 감싸고 나면 줄을 지운다. */
        val HANDOVER_EXEMPT_MODULES =
            mapOf(
                "feature/timeletter" to "#2149",
                "feature/mindrecord" to "#2150",
            )

        /**
         * 루트 백스택의 바닥에만 서는 목적지. 로그인 뒤 `popUpTo(0)` 로 홀로 남고, 탭 이동은
         * `popUpTo<Route.Home>` 이라 그 위에 쌓이지 않는다(AppState·AppNavigationActions). 그래서
         * predictive back 의 앞 화면이 되지 않는다. 다른 목적지 위로 올라가는 길이 생기면 지운다.
         */
        val BOTTOM_ONLY_ROUTES = setOf("Route.Home")
    }
}

/** 이름을 식별자로 부르는지 본다. 부분 일치(`NavDestinationSurfaceOverride`)는 배제한다. */
private fun String.callsIdentifier(name: String): Boolean = Regex("""(?<![A-Za-z0-9_])${Regex.escape(name)}\s*[({]""").containsMatchIn(this)
