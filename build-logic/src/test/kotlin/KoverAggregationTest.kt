import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 루트의 커버리지 집계 목록과 규약을 적용한 모듈 목록이 어긋나지 않는지 검사한다.
 *
 * 루트가 `subprojects` 를 열거하던 시절에는 모듈이 늘면 자동으로 딸려 왔지만, 이관 뒤에는
 * 모듈 `plugins { id("afternote.kover") }` 와 루트 `kover(project(...))` 두 줄이 짝이다.
 * 한쪽만 늘면 그 모듈이 커버리지에서 조용히 빠지므로 여기서 짝을 강제한다(#918).
 */
class KoverAggregationTest {
    private val repositoryRoot = File("..").canonicalFile

    private val aggregatedProjects: List<String> =
        AGGREGATION_PATTERN
            .findAll(File(repositoryRoot, "build.gradle.kts").readText())
            .map { it.groupValues[1] }
            .toList()

    private val includedProjects: List<String> =
        INCLUDE_PATTERN
            .findAll(File(repositoryRoot, "settings.gradle.kts").readText())
            .map { it.groupValues[1] }
            .toList()

    @Test
    fun `규약을 적용한 모듈과 루트 집계 목록이 일치한다`() {
        val convention =
            includedProjects
                .filter { path -> buildFileOf(path).readText().contains("""id("afternote.kover")""") }
                .sorted()

        assertEquals(convention, aggregatedProjects.sorted())
    }

    @Test
    fun `집계 대상은 코드를 담는 모듈뿐이다`() {
        // :konsist 는 아키텍처 규칙 검사 전용이라 프로덕션 코드가 없다 — 집계에 들어가면
        // 커버리지 분모만 흐린다. 루트가 열거하던 시절의 필터(:app · :core:* · :feature:*)를 유지한다.
        val unexpected =
            aggregatedProjects.filterNot { path ->
                path == ":app" || path.startsWith(":core:") || path.startsWith(":feature:")
            }

        assertEquals(emptyList<String>(), unexpected)
    }

    @Test
    fun `집계 목록에 중복이 없고 모든 대상이 실재한다`() {
        assertEquals(aggregatedProjects.distinct(), aggregatedProjects)
        aggregatedProjects.forEach { path ->
            assertTrue("$path 의 build.gradle.kts 가 없다", buildFileOf(path).isFile)
        }
    }

    private fun buildFileOf(path: String) = File(repositoryRoot, path.removePrefix(":").replace(':', '/') + "/build.gradle.kts")
}

private val AGGREGATION_PATTERN = """kover\(project\("(:[^"]+)"\)\)""".toRegex()

private val INCLUDE_PATTERN = """^include\("(:[^"]+)"\)""".toRegex(RegexOption.MULTILINE)
