import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ExplicitApiConventionTest {
    @get:Rule
    val projectDir = TemporaryFolder()

    private val repositoryRoot = File("..").canonicalFile

    @Test
    fun `소스 없는 기존 모듈과 새 경로는 warning inventory에 없다`() {
        val inventory = warningInventoryPaths()

        assertTrue(":core:model" in inventory)
        assertTrue(":feature:onboarding:data" !in inventory)
        assertTrue(":feature:setting:data" !in inventory)
        assertTrue(":feature:timeletter:res" !in inventory)
        assertTrue(":feature:new:domain" !in inventory)
    }

    @Test
    fun `warning inventory 는 실재 모듈이고 strict 전환 문서에 모두 기록된다`() {
        val settings = File(repositoryRoot, "settings.gradle.kts").readText()
        val convention = File(repositoryRoot, "docs/convention/production-visibility.md").readText()

        warningInventoryPaths().forEach { path ->
            assertTrue("$path 가 settings.gradle.kts 에 없다", settings.contains("""include("$path")"""))
            assertTrue("$path 의 strict 전환 순서가 문서에 없다", convention.contains("`$path` | warning"))
        }
    }

    @Test
    fun `기존 inventory 모듈의 암시적 public 선언은 warning으로 컴파일된다`() {
        writeJvmModule(projectPath = ":core:model")

        val result = runner(":core:model:compileKotlin").build()

        assertTrue(result.output.contains("Visibility must be specified in explicit API mode"))
    }

    @Test
    fun `신규 JVM 컨벤션 모듈의 암시적 public 선언은 컴파일에 실패한다`() {
        writeJvmModule()

        val result = runner(":future:compileKotlin").buildAndFail()

        assertTrue(result.output.contains("ImplicitApi.kt"))
        assertTrue(result.output.contains("Visibility must be specified in explicit API mode"))
    }

    @Test
    fun `신규 Android 컨벤션 모듈의 암시적 public 선언도 컴파일에 실패한다`() {
        writeAndroidModule()

        val result = runner(":future:compileDebugKotlin").buildAndFail()

        assertTrue(result.output.contains("ImplicitApi.kt"))
        assertTrue(result.output.contains("Visibility must be specified in explicit API mode"))
    }

    @Test
    fun `신규 strict 모듈이어도 테스트 소스의 암시적 선언은 허용한다`() {
        writeJvmModule(mainSource = "private fun productionOwner(): Unit = Unit")
        projectDir.newFolder("future", "src", "test", "kotlin")
        projectDir.newFile("future/src/test/kotlin/ImplicitTestApi.kt").writeText(
            "fun helperUsedOnlyByTests() = Unit\n",
        )

        val result = runner(":future:compileTestKotlin").build()

        assertTrue(result.task(":future:compileTestKotlin")?.outcome == TaskOutcome.SUCCESS)
    }

    private fun warningInventoryPaths(): Set<String> {
        val source = File(repositoryRoot, "build-logic/src/main/kotlin/ExplicitApiConvention.kt").readText()
        val inventory =
            source
                .substringAfter("explicitApiWarningInventory: Set<String> =")
                .substringAfter("setOf(")
                .substringBefore("\n    )")
        return PROJECT_PATH.findAll(inventory).mapTo(linkedSetOf()) { match -> match.groupValues[1] }
    }

    private fun writeJvmModule(
        projectPath: String = ":future",
        mainSource: String = "fun exposedOnlyForATest() = Unit",
    ) {
        val moduleDirectory = writeStubProject(projectPath)
        File(projectDir.root, "$moduleDirectory/src/main/kotlin").mkdirs()
        File(projectDir.root, "$moduleDirectory/build.gradle.kts").writeText(
            """
            plugins {
                id("afternote.jvm.library")
            }
            """.trimIndent() + "\n",
        )
        File(projectDir.root, "$moduleDirectory/src/main/kotlin/ImplicitApi.kt").writeText(
            "$mainSource\n",
        )
    }

    private fun writeAndroidModule() {
        val moduleDirectory = writeStubProject(":future")
        File(projectDir.root, "$moduleDirectory/src/main/kotlin").mkdirs()
        File(projectDir.root, "$moduleDirectory/build.gradle.kts").writeText(
            """
            plugins {
                id("afternote.android.library")
            }

            android {
                namespace = "com.afternote.future"
            }
            """.trimIndent() + "\n",
        )
        File(projectDir.root, "$moduleDirectory/src/main/kotlin/ImplicitApi.kt").writeText(
            "fun exposedOnlyForATest() = Unit\n",
        )
    }

    private fun writeStubProject(projectPath: String): String {
        val moduleDirectory = projectPath.removePrefix(":").replace(':', File.separatorChar)
        projectDir.newFolder("gradle")
        projectDir.newFile("settings.gradle.kts").writeText(
            """
            pluginManagement {
                repositories {
                    google()
                    mavenCentral()
                    gradlePluginPortal()
                }
            }
            dependencyResolutionManagement {
                repositories {
                    google()
                    mavenCentral()
                }
            }
            rootProject.name = "explicit-api-stub"
            include("$projectPath")
            """.trimIndent() + "\n",
        )
        projectDir.newFile("gradle/libs.versions.toml").writeText(
            """
            [versions]
            coreKtx = "1.19.0"
            espressoCore = "3.7.0"
            junit = "4.13.2"
            junitVersion = "1.3.0"
            ktlint = "1.8.0"

            [libraries]
            androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }
            androidx-espresso-core = { module = "androidx.test.espresso:espresso-core", version.ref = "espressoCore" }
            androidx-junit = { module = "androidx.test.ext:junit", version.ref = "junitVersion" }
            junit = { module = "junit:junit", version.ref = "junit" }
            """.trimIndent() + "\n",
        )
        return moduleDirectory
    }

    private fun runner(vararg arguments: String): GradleRunner =
        GradleRunner
            .create()
            .withProjectDir(projectDir.root)
            .withPluginClasspath()
            .withArguments(*arguments, "--stacktrace")

    private companion object {
        val PROJECT_PATH = Regex("\"(:[^\"]+)\"")
    }
}
