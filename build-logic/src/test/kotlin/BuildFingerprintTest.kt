import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * debug `versionName` 접미사의 빌드 계약 회귀 테스트 (Gradle TestKit).
 *
 * 지문을 만드는 두 함수는 `BuildFingerprint.kt` 안에서만 쓰이는 file-local 구현이라 테스트가
 * 직접 부르지 않는다 (#1671). 대신 빌드 스크립트가 실제로 부르는 `resolveDebugVersionNameSuffix()`
 * 를 스텁 프로젝트에서 돌려 **빌드에 최종 적용되는 문자열**을 고정한다 — 검증 대상이 계산식이
 * 아니라 «QA 가 `dumpsys` 로 읽게 될 값» 이라는 점도 이 방향이 맞다(#1135).
 *
 * 환경변수를 매번 명시적으로 넘긴다. CI 러너에는 `GITHUB_SHA` 가 이미 있어서, git 경로를 검증하는
 * 테스트가 조용히 CI 경로로 새어 들어가면 로컬에서만 초록인 테스트가 된다.
 */
class BuildFingerprintTest {
    @get:Rule
    val projectDir = TemporaryFolder()

    @Before
    fun setUp() {
        writeStubProject()
    }

    @Test
    fun `CI 는 GITHUB_SHA 를 다듬어 앞 7자리만 붙인다`() {
        assertEquals("+a3f91c2", suffixWithCommitSha("  A3F91C2B8D4E5F60718293A4B5C6D7E8F9012345  "))
    }

    /**
     * 형식에 안 맞는 `GITHUB_SHA` 는 커밋으로 받지 않는다.
     *
     * **세 입력이 각각 다른 회귀를 잡는다.** 케이스당 Gradle 실행 1회가 들어 한 테스트에 묶었다.
     *
     * | 입력 | 무너지면 나오는 값 | 무엇이 깨진 것인가 |
     * |---|---|---|
     * | `not-a-sha` | — | hex 런이 없어 어떤 완화에도 안 걸린다. 기본 경로 |
     * | `a3f91c2-dirty` | `+a3f91c2` | `matches`(완전일치)를 `containsMatchIn`/`find` 로 바꾼 것. **거짓 clean 지문** |
     * | `a3f91c`(6자) | `+a3f91c` | `{7,40}` 하한이 무너진 것. 자릿수가 흔들린다 |
     *
     * 가운데가 KDoc 이 말하는 QA 증거 사고 그 자체다 — dirty 빌드가 clean 지문을 달고 나간다.
     */
    @Test
    fun `16진수가 아닌 GITHUB_SHA 는 커밋으로 받지 않고 unknown 을 남긴다`() {
        // 접미사를 생략하면 «sha 를 못 읽었다» 와 «이 기능이 없던 빌드» 를 구분할 수 없다.
        assertEquals("+unknown", suffixWithCommitSha("not-a-sha"))

        // 부분 일치로 완화하면 여기서 «+a3f91c2» 라는 거짓 clean 지문이 나온다.
        assertEquals("+unknown", suffixWithCommitSha("a3f91c2-dirty"))

        // 하한이 무너지면 여기서 «+a3f91c» 로 자릿수가 흔들린다.
        assertEquals("+unknown", suffixWithCommitSha("a3f91c"))
    }

    @Test
    fun `커밋을 특정할 수 없는 빌드에도 접미사를 남긴다`() {
        // git 저장소가 아니라 rev-parse 가 실패한다. 커밋을 모르면 dirty 도 말할 수 없어 붙이지 않는다.
        assertEquals("+unknown", suffixFromGitWorkingTree())
    }

    @Test
    fun `워킹트리가 깨끗하면 short sha 만 붙이고 변경이 있으면 dirty 를 함께 붙인다`() {
        val shortSha = initGitRepositoryWithSingleCommit()

        assertEquals("+$shortSha", suffixFromGitWorkingTree())

        projectDir.newFile("uncommitted.txt").writeText("edited\n")

        // untracked 도 dirty 로 센다 — 거짓 clean 은 검증하지 않은 코드에 QA 증거를 붙이게 한다.
        assertEquals("+$shortSha-dirty", suffixFromGitWorkingTree())
    }

    private fun suffixWithCommitSha(ciCommitSha: String): String = resolvedSuffix(System.getenv() + mapOf(CI_COMMIT_SHA to ciCommitSha))

    private fun suffixFromGitWorkingTree(): String = resolvedSuffix(System.getenv() - CI_COMMIT_SHA)

    private fun resolvedSuffix(environment: Map<String, String>): String {
        val output =
            GradleRunner
                .create()
                .withProjectDir(projectDir.root)
                .withEnvironment(environment)
                .withArguments("showBuildFingerprint")
                .build()
                .output

        return SUFFIX_MARKER.find(output)?.groupValues?.get(1)
            ?: error("빌드 출력에서 접미사를 찾지 못했다:\n$output")
    }

    /** 스텁 프로젝트를 git 저장소로 만들고 커밋한 뒤 기대 short sha 를 돌려준다. */
    private fun initGitRepositoryWithSingleCommit(): String {
        // Gradle 이 빌드마다 만드는 산출물까지 dirty 로 세면 clean 경로를 검증할 수 없다.
        projectDir.newFile(".gitignore").writeText(".gradle/\nbuild/\n")
        git("init")
        git("add", "--all")
        git("-c", "user.email=build-logic@example.com", "-c", "user.name=build-logic", "commit", "--message", "stub")
        return git("rev-parse", "HEAD").take(7)
    }

    private fun git(vararg arguments: String): String {
        val process =
            ProcessBuilder(listOf("git", *arguments))
                .directory(projectDir.root)
                .redirectErrorStream(true)
                .start()
        val output =
            process.inputStream
                .bufferedReader()
                .readText()
                .trim()
        val exitCode = process.waitFor()
        check(exitCode == 0) { "git ${arguments.joinToString(" ")} 실패($exitCode):\n$output" }
        return output
    }

    private fun writeStubProject() {
        val classpathLiteral =
            guardClasspath()
                .split(File.pathSeparator)
                .filter { it.isNotBlank() }
                .joinToString(", ") { "\"${it.replace("\\", "/")}\"" }
        projectDir.newFile("settings.gradle.kts").writeText("rootProject.name = \"fingerprint-stub\"\n")
        projectDir.newFile("build.gradle.kts").writeText(
            """
            buildscript {
                dependencies {
                    classpath(files($classpathLiteral))
                }
            }

            println("suffix=[" + resolveDebugVersionNameSuffix() + "]")

            tasks.register("showBuildFingerprint")
            """.trimIndent() + "\n",
        )
    }

    private fun guardClasspath(): String =
        System.getProperty("guardClasspath")
            ?: error("guardClasspath 시스템 프로퍼티 누락 — build-logic/build.gradle.kts 의 Test 태스크 설정 참고")

    private companion object {
        const val CI_COMMIT_SHA = "GITHUB_SHA"
        val SUFFIX_MARKER = Regex("suffix=\\[(.*)]")
    }
}
