import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * `requireKeyForReleaseBuild`·`socialLoginKey` 의 빌드 계약 회귀 테스트 (Gradle TestKit).
 *
 * AGP 를 올리지 않은 스텁 프로젝트에 가짜 `preReleaseBuild` 를 두고 가드 계약만 고정한다 —
 * 실제 AGP 그래프(`assembleRelease`·`bundleRelease`·`lintRelease` 가 `preReleaseBuild` 를
 * 경유하는 것)는 AGP 소관이라 여기서 재검증하지 않는다(그 배선 증거는 PR #587 의
 * `--dry-run` 실측). 키 이름은 CI 가 실키(KAKAO_NATIVE_APP_KEY 등)를 환경변수로 주입해도
 * 결과가 흔들리지 않도록 합성 이름(TEST_SOCIAL_KEY)만 쓴다.
 */
class ReleaseKeyGuardTest {
    @get:Rule
    val projectDir = TemporaryFolder()

    @Test
    fun `키가 비어 있으면 release 경로가 지정 메시지로 실패한다`() {
        writeStubProject("""requireKeyForReleaseBuild("TEST_SOCIAL_KEY", "")""")

        val result = runner("assembleRelease").buildAndFail()

        assertEquals(TaskOutcome.FAILED, result.task(":checkTestSocialKeyForRelease")?.outcome)
        assertTrue(result.output.contains("TEST_SOCIAL_KEY 가 비어 있어 release 빌드를 중단합니다"))
    }

    @Test
    fun `공백만 있는 값도 누락으로 취급한다`() {
        writeStubProject("""requireKeyForReleaseBuild("TEST_SOCIAL_KEY", "   ")""")

        val result = runner("assembleRelease").buildAndFail()

        assertEquals(TaskOutcome.FAILED, result.task(":checkTestSocialKeyForRelease")?.outcome)
    }

    @Test
    fun `키가 있으면 release 경로가 통과한다`() {
        writeStubProject("""requireKeyForReleaseBuild("TEST_SOCIAL_KEY", "real-key")""")

        val result = runner("assembleRelease").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":checkTestSocialKeyForRelease")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":assembleRelease")?.outcome)
    }

    @Test
    fun `키가 비어 있어도 debug 경로는 가드를 태우지 않는다`() {
        writeStubProject("""requireKeyForReleaseBuild("TEST_SOCIAL_KEY", "")""")

        // build() 자체가 빌드 성공을 단언한다. 액션 없는 스텁 라이프사이클 태스크라
        // outcome 은 UP_TO_DATE 로 뜨므로 SUCCESS 를 따로 단언하지 않는다.
        val result = runner("assembleDebug").build()

        assertNull(result.task(":checkTestSocialKeyForRelease"))
    }

    @Test
    fun `preReleaseBuild 를 경유하는 다른 라이프사이클 태스크도 동일하게 실패한다`() {
        writeStubProject("""requireKeyForReleaseBuild("TEST_SOCIAL_KEY", "")""")

        val result = runner("bundleRelease").buildAndFail()

        assertEquals(TaskOutcome.FAILED, result.task(":checkTestSocialKeyForRelease")?.outcome)
    }

    @Test
    fun `socialLoginKey 는 루트 local properties 값을 읽고 가드까지 배선한다`() {
        writeStubProject(
            """
            val resolved = socialLoginKey("TEST_SOCIAL_KEY")
            println("resolved=[" + resolved + "]")
            """.trimIndent(),
        )
        projectDir.newFile("local.properties").writeText("TEST_SOCIAL_KEY=from-local-properties\n")

        val result = runner("assembleRelease").build()

        assertTrue(result.output.contains("resolved=[from-local-properties]"))
        assertEquals(TaskOutcome.SUCCESS, result.task(":checkTestSocialKeyForRelease")?.outcome)
    }

    @Test
    fun `socialLoginKey 는 어느 경로에도 키가 없으면 release 경로에서 실패한다`() {
        writeStubProject("""socialLoginKey("TEST_SOCIAL_KEY")""")

        val result = runner("assembleRelease").buildAndFail()

        assertEquals(TaskOutcome.FAILED, result.task(":checkTestSocialKeyForRelease")?.outcome)
        assertTrue(result.output.contains("TEST_SOCIAL_KEY 가 비어 있어 release 빌드를 중단합니다"))
    }

    @Test
    fun `서명 자격이 없으면 release 경로가 누락 항목과 함께 실패한다`() {
        writeStubProject(
            """requireReleaseSigningForReleaseBuild(listOf("RELEASE_STORE_FILE", "RELEASE_KEY_ALIAS"))""",
        )

        val result = runner("assembleRelease").buildAndFail()

        assertEquals(TaskOutcome.FAILED, result.task(":checkReleaseSigningForRelease")?.outcome)
        assertTrue(result.output.contains("release 서명 자격이 없어 release 빌드를 중단합니다"))
        assertTrue(result.output.contains("RELEASE_STORE_FILE, RELEASE_KEY_ALIAS"))
    }

    @Test
    fun `서명 자격이 갖춰지면 release 경로가 통과한다`() {
        writeStubProject("""requireReleaseSigningForReleaseBuild(emptyList<String>())""")

        val result = runner("assembleRelease").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":checkReleaseSigningForRelease")?.outcome)
    }

    @Test
    fun `서명 자격이 없어도 debug 경로는 가드를 태우지 않는다`() {
        writeStubProject("""requireReleaseSigningForReleaseBuild(listOf("RELEASE_STORE_FILE"))""")

        val result = runner("assembleDebug").build()

        assertNull(result.task(":checkReleaseSigningForRelease"))
    }

    private fun writeStubProject(guardWiring: String) {
        val classpathLiteral =
            guardClasspath()
                .split(File.pathSeparator)
                .filter { it.isNotBlank() }
                .joinToString(", ") { "\"${it.replace("\\", "/")}\"" }
        projectDir.newFile("settings.gradle.kts").writeText("rootProject.name = \"guard-stub\"\n")
        projectDir.newFile("build.gradle.kts").writeText(
            """
            buildscript {
                dependencies {
                    classpath(files($classpathLiteral))
                }
            }

            $guardWiring

            tasks.register("preReleaseBuild")
            tasks.register("assembleRelease") { dependsOn("preReleaseBuild") }
            tasks.register("bundleRelease") { dependsOn("preReleaseBuild") }
            tasks.register("assembleDebug")
            """.trimIndent() + "\n",
        )
    }

    private fun guardClasspath(): String =
        System.getProperty("guardClasspath")
            ?: error("guardClasspath 시스템 프로퍼티 누락 — build-logic/build.gradle.kts 의 Test 태스크 설정 참고")

    private fun runner(vararg arguments: String): GradleRunner =
        GradleRunner
            .create()
            .withProjectDir(projectDir.root)
            .withArguments(*arguments)
}
