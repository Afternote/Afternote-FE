import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Base64

class DebugSigningDiagnosticsTest {
    @get:Rule
    val projectDir = TemporaryFolder()

    @Test
    fun `기본 서명 경고는 configuration cache 재사용 빌드에도 출력된다`() {
        writeStubProject(usingDefaultKeystore = true)

        val first = runner("assembleDebug").build()
        val second = runner("assembleDebug").build()

        assertTrue(first.output.contains("Configuration cache entry stored"))
        assertTrue(second.output.contains("Reusing configuration cache"))
        listOf(first, second).forEach { result ->
            assertEquals(TaskOutcome.SUCCESS, result.task(":checkDebugSigningForKakao")?.outcome)
            assertTrue(result.output.contains("AGP 기본 debug keystore로 폴백합니다"))
            assertTrue(result.output.contains(":app:printDebugKakaoKeyHash"))
            assertFalse(result.output.contains("test-store-password"))
        }
    }

    @Test
    fun `공유 서명은 캐시 재사용 빌드에서도 폴백 경고를 내지 않는다`() {
        writeStubProject(usingDefaultKeystore = false)

        runner("assembleDebug").build()
        val second = runner("assembleDebug").build()

        assertTrue(second.output.contains("Reusing configuration cache"))
        assertEquals(TaskOutcome.SUCCESS, second.task(":checkDebugSigningForKakao")?.outcome)
        assertFalse(second.output.contains("AGP 기본 debug keystore로 폴백합니다"))
    }

    @Test
    fun `release와 help 경로에서는 debug 경고를 실행하지 않는다`() {
        writeStubProject(usingDefaultKeystore = true)

        listOf("assembleRelease", "help").forEach { task ->
            val result = runner(task).build()
            assertNull(result.task(":checkDebugSigningForKakao"))
            assertFalse(result.output.contains("AGP 기본 debug keystore로 폴백합니다"))
        }
    }

    @Test
    fun `JKS debug 인증서 해시는 keytool 지문과 일치하며 매번 출력된다`() {
        assertKeyHash("JKS", usingDefaultKeystore = true)
    }

    @Test
    fun `PKCS12 공유 인증서 해시는 설정된 alias를 사용한다`() {
        assertKeyHash("PKCS12", usingDefaultKeystore = false)
    }

    @Test
    fun `존재하지 않는 alias는 조용히 다른 인증서로 대체하지 않는다`() {
        generateKeyStore("PKCS12")
        writeStubProject(usingDefaultKeystore = false, alias = "missing-alias")

        val result = runner("printDebugKakaoKeyHash").buildAndFail()

        assertTrue(result.output.contains("DEBUG_KEY_ALIAS를 확인하세요"))
        assertFalse(result.output.contains("test-store-password"))
        assertFalse(result.output.contains("Debug Kakao key hash:"))
    }

    private fun assertKeyHash(
        storeType: String,
        usingDefaultKeystore: Boolean,
    ) {
        generateKeyStore(storeType)
        // 다른 인증서가 먼저 있어도 지정한 alias의 해시를 읽어야 한다.
        generateKeyStore(storeType, alias = "selected-debug-key")
        writeStubProject(usingDefaultKeystore, alias = "selected-debug-key")
        val listing = keytool("-list", "-v", "-alias", "selected-debug-key")
        val fingerprint = Regex("SHA1: ([0-9A-F:]+)").find(listing)!!.groupValues[1]
        val expected = Base64.getEncoder().encodeToString(fingerprint.split(":").map { it.toInt(16).toByte() }.toByteArray())

        val first = runner("printDebugKakaoKeyHash").build()
        val second = runner("printDebugKakaoKeyHash").build()

        assertTrue(second.output.contains("Reusing configuration cache"))
        listOf(first, second).forEach { result ->
            assertTrue(result.output.contains("Debug Kakao key hash: $expected"))
            assertEquals(TaskOutcome.SUCCESS, result.task(":printDebugKakaoKeyHash")?.outcome)
            assertTrue(
                result.tasks.indexOf(result.task(":validateSigningDebug")) < result.tasks.indexOf(result.task(":printDebugKakaoKeyHash")),
            )
            assertFalse(result.output.contains("test-store-password"))
            assertFalse(result.output.contains("PRIVATE KEY"))
        }
    }

    private fun generateKeyStore(
        storeType: String,
        alias: String = "other-key",
    ) {
        keytool(
            "-genkeypair",
            "-alias",
            alias,
            "-keyalg",
            "RSA",
            "-keysize",
            "2048",
            "-validity",
            "1",
            "-dname",
            "CN=Afternote Test",
            "-storetype",
            storeType,
            "-keypass",
            "test-store-password",
        )
    }

    private fun keytool(vararg arguments: String): String {
        val command =
            listOf(
                File(System.getProperty("java.home"), "bin/keytool").absolutePath,
                "-J-Duser.language=en",
                "-keystore",
                File(projectDir.root, "debug.keystore").absolutePath,
                "-storepass",
                "test-store-password",
            ) + arguments
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        assertEquals(output, 0, process.waitFor())
        return output
    }

    private fun writeStubProject(
        usingDefaultKeystore: Boolean,
        alias: String = "other-key",
    ) {
        val classpath =
            System
                .getProperty("guardClasspath")
                .split(File.pathSeparator)
                .filter { it.isNotBlank() }
                .joinToString(", ") { "\"${it.replace("\\", "/")}\"" }
        projectDir.newFile("settings.gradle.kts").writeText("rootProject.name = \"debug-signing-stub\"\n")
        projectDir.newFile("build.gradle.kts").writeText(
            """
            buildscript { dependencies { classpath(files($classpath)) } }
            registerDebugSigningDiagnostics(
                usingDefaultKeystore = $usingDefaultKeystore,
                storeFile = file("debug.keystore"),
                storePassword = "test-store-password",
                keyAlias = "$alias",
            )
            tasks.register("preDebugBuild")
            tasks.register("assembleDebug") { dependsOn("preDebugBuild") }
            tasks.register("assembleRelease")
            tasks.register("validateSigningDebug")
            """.trimIndent() + "\n",
        )
    }

    private fun runner(vararg arguments: String): GradleRunner =
        GradleRunner
            .create()
            .withProjectDir(projectDir.root)
            .withArguments(*arguments, "--configuration-cache", "--configuration-cache-problems=fail", "--console=plain")
}
