package com.afternote.konsist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** 새 프로덕션 Kotlin 모듈이 explicit API 정책 밖에서 생기지 못하게 하는 저장소 정책 가드. */
class ExplicitApiModulePolicyKonsistTest {
    @Test
    fun `프로덕션 Kotlin 모듈은 explicit API 적용 경로를 가진다`() {
        val projectRoot = AfternoteKonsistScope.projectRoot
        val modulePaths = includedModulePaths(File(projectRoot, "settings.gradle.kts").readText())
        val productionModules =
            modulePaths
                .filterNot(NON_API_MODULES::contains)
                .map { path -> path to projectRoot.resolveModule(path) }
                .filter { (_, directory) -> directory.hasProductionKotlinSource() }
                .toMap()

        val violations =
            productionModules
                .filter { (path, directory) -> !directory.hasExplicitApiPolicy(path) }
                .keys
                .sorted()

        assertTrue(
            "프로덕션 Kotlin 모듈에 explicit API 정책이 없다: ${violations.joinToString()}",
            violations.isEmpty(),
        )
    }

    @Test
    fun `warning inventory에는 기존 부채 모듈만 정확히 남는다`() {
        val inventory =
            File(AfternoteKonsistScope.projectRoot, "build-logic/src/main/kotlin/ExplicitApiConvention.kt")
                .readText()
                .substringAfter("explicitApiWarningInventory: Set<String> =")
                .substringAfter("setOf(")
                .substringBefore("\n    )")
        val actual = PROJECT_PATH.findAll(inventory).mapTo(sortedSetOf()) { match -> match.groupValues[1] }

        assertEquals(LEGACY_WARNING_CONVENTION_MODULES, actual)
    }

    @Test
    fun `com Android test 모듈만 explicit API 대상에서 제외한다`() {
        val projectRoot = AfternoteKonsistScope.projectRoot
        val actual =
            includedModulePaths(File(projectRoot, "settings.gradle.kts").readText())
                .filterTo(sortedSetOf()) { path ->
                    val buildScript = projectRoot.resolveModule(path).resolve("build.gradle.kts").readText()
                    "id(\"com.android.test\")" in buildScript
                }

        assertEquals(NON_API_MODULES, actual)
    }

    @Test
    fun `별도 포함 빌드인 build-logic도 explicit API를 적용한다`() {
        val buildLogic = File(AfternoteKonsistScope.projectRoot, "build-logic")

        assertTrue(buildLogic.hasProductionKotlinSource())
        assertTrue(
            "build-logic/build.gradle.kts에 기존 부채용 explicitApiWarning()이 없다",
            buildLogic.hasExplicitApiPolicy(BUILD_LOGIC_PATH),
        )
    }

    @Test
    fun `settings 모듈 경로를 저장소 디렉터리로 변환한다`() {
        val settings =
            """
            include(":core:model")
            include(":feature:new:domain")
            """.trimIndent()

        assertEquals(setOf(":core:model", ":feature:new:domain"), includedModulePaths(settings))
        assertEquals(
            File(AfternoteKonsistScope.projectRoot, "feature/new/domain"),
            AfternoteKonsistScope.projectRoot.resolveModule(":feature:new:domain"),
        )
    }

    @Test
    fun `컨벤션 모듈은 build script에서 explicit API mode를 덮어쓰지 않는다`() {
        val convention = """plugins { id("afternote.jvm.library") }"""

        assertTrue(convention.hasExplicitApiPolicy(":feature:new:domain"))
        assertTrue(
            !(convention + "\nkotlin { explicitApiWarning() }")
                .hasExplicitApiPolicy(":feature:new:domain"),
        )
        assertTrue(
            !(convention + "\nkotlin { explicitApi = ExplicitApiMode.Disabled }")
                .hasExplicitApiPolicy(":feature:new:domain"),
        )
        assertTrue(
            !"""plugins { id("afternote.jvm.library") apply false }"""
                .hasExplicitApiPolicy(":feature:new:domain"),
        )
        assertTrue(
            !"""plugins { id("afternote.jvm.library").apply(false) }"""
                .hasExplicitApiPolicy(":feature:new:domain"),
        )
        assertTrue("kotlin { explicitApiStrict() }".hasExplicitApiPolicy(":feature:new:domain"))
    }

    private fun includedModulePaths(settings: String): Set<String> =
        INCLUDE_MODULE
            .findAll(settings)
            .mapTo(linkedSetOf()) { match -> match.groupValues[1] }

    private fun File.resolveModule(path: String): File = resolve(path.removePrefix(":").replace(':', File.separatorChar))

    private fun File.hasProductionKotlinSource(): Boolean =
        PRODUCTION_SOURCE_SETS.any { sourceSet ->
            resolve("src/$sourceSet")
                .takeIf(File::isDirectory)
                ?.walkTopDown()
                ?.any { file -> file.isFile && file.extension == "kt" } == true
        }

    private fun File.hasExplicitApiPolicy(projectPath: String): Boolean {
        val buildScript =
            resolve("build.gradle.kts")
                .takeIf(File::isFile)
                ?.readText()
                .orEmpty()
                .replace(BLOCK_COMMENT, " ")
                .replace(LINE_COMMENT, " ")
        return buildScript.hasExplicitApiPolicy(projectPath)
    }

    private fun String.hasExplicitApiPolicy(projectPath: String): Boolean {
        val convention = EXPLICIT_API_CONVENTIONS.any { pluginId -> appliedPlugin(pluginId).containsMatchIn(this) }
        if (convention) return !EXPLICIT_API_CONFIGURATION.containsMatchIn(this)
        if (EXPLICIT_API_WARNING_CONFIGURATION.containsMatchIn(this)) {
            return projectPath in LEGACY_DIRECT_WARNING_MODULES
        }
        return EXPLICIT_API_STRICT_CONFIGURATION.containsMatchIn(this)
    }

    private fun appliedPlugin(pluginId: String): Regex =
        Regex(
            "id\\(\"${Regex.escape(pluginId)}\"\\)" +
                "(?!\\s*(?:apply\\s+false\\b|\\.apply\\(\\s*false\\s*\\)))",
        )

    private companion object {
        val INCLUDE_MODULE = Regex("""include\(\"(:[^\"]+)\"\)""")
        val PROJECT_PATH = Regex("\"(:[^\"]+)\"")
        val EXPLICIT_API_CONFIGURATION = Regex("""\bexplicitApi(?:Warning|Strict)?\s*(?:\(|=|\.set\s*\()""")
        val EXPLICIT_API_WARNING_CONFIGURATION =
            Regex("""\b(?:explicitApiWarning\s*\(|explicitApi\s*=\s*ExplicitApiMode\.Warning\b)""")
        val EXPLICIT_API_STRICT_CONFIGURATION =
            Regex("""\b(?:explicitApi(?:Strict)?\s*\(|explicitApi\s*=\s*ExplicitApiMode\.Strict\b)""")
        val BLOCK_COMMENT = Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL)
        val LINE_COMMENT = Regex("//.*")
        const val BUILD_LOGIC_PATH = "build-logic"
        val NON_API_MODULES = setOf(":baselineprofile")
        val PRODUCTION_SOURCE_SETS = setOf("main", "debug", "release")
        val LEGACY_DIRECT_WARNING_MODULES = setOf(":feature:setting:domain", BUILD_LOGIC_PATH)

        /** 이 집합은 줄이기만 한다. 새 모듈을 warning으로 추가하면 exact 정책 테스트가 실패한다. */
        val LEGACY_WARNING_CONVENTION_MODULES =
            sortedSetOf(
                ":app",
                ":core:common",
                ":core:data",
                ":core:datastore",
                ":core:domain",
                ":core:model",
                ":core:network",
                ":core:ui",
                ":feature:afternote:data",
                ":feature:afternote:domain",
                ":feature:afternote:presentation",
                ":feature:home:presentation",
                ":feature:mindrecord:data",
                ":feature:mindrecord:domain",
                ":feature:mindrecord:presentation",
                ":feature:onboarding:presentation",
                ":feature:receiver:data",
                ":feature:receiver:domain",
                ":feature:receiver:presentation",
                ":feature:setting:presentation",
                ":feature:timeletter:data",
                ":feature:timeletter:domain",
                ":feature:timeletter:presentation",
            )

        /** 직접 또는 내부에서 Android/JVM 공통 규약을 적용해 explicit API mode를 설정하는 plugin. */
        val EXPLICIT_API_CONVENTIONS =
            setOf(
                "afternote.android.application",
                "afternote.android.data",
                "afternote.android.datastore",
                "afternote.android.domain",
                "afternote.android.feature",
                "afternote.android.library",
                "afternote.android.library.compose",
                "afternote.jvm.domain",
                "afternote.jvm.library",
            )
    }
}
