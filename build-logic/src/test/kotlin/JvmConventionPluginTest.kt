import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class JvmConventionPluginTest {
    @get:Rule
    val projectDir = TemporaryFolder()

    @Test
    fun `JVM library 규약은 Java와 Kotlin을 17로 맞추고 JUnit을 제공한다`() {
        writeProject("afternote.jvm.library", libraryAssertions())

        val result = runner().build()

        assertEquals(TaskOutcome.SUCCESS, result.task(VERIFY_TASK)?.outcome)
    }

    @Test
    fun `JVM domain 규약은 Android 없이 test fixtures와 javax inject를 제공한다`() {
        writeProject("afternote.jvm.domain", domainAssertions())

        val result = runner().build()

        assertEquals(TaskOutcome.SUCCESS, result.task(VERIFY_TASK)?.outcome)
    }

    @Test
    fun `JVM domain 소스는 Android SDK 타입을 컴파일할 수 없다`() {
        writeProject("afternote.jvm.domain", domainAssertions())
        projectDir.newFolder("src", "main", "kotlin")
        projectDir.newFile("src/main/kotlin/AndroidLeak.kt").writeText(
            """
            import android.net.Uri

            val leaked: Uri? = null
            """.trimIndent() + "\n",
        )

        val result = runner("compileKotlin").buildAndFail()

        assertTrue(result.output.contains("Unresolved reference 'android'"))
    }

    private fun writeProject(
        conventionPlugin: String,
        assertions: String,
    ) {
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
            rootProject.name = "jvm-convention-stub"
            """.trimIndent() + "\n",
        )
        projectDir.newFile("gradle/libs.versions.toml").writeText(
            """
            [versions]
            junit = "4.13.2"
            javaxInject = "1"
            ktlint = "1.8.0"

            [libraries]
            junit = { module = "junit:junit", version.ref = "junit" }
            javax-inject = { module = "javax.inject:javax.inject", version.ref = "javaxInject" }
            """.trimIndent() + "\n",
        )
        projectDir.newFile("build.gradle.kts").writeText(
            """
            import org.gradle.api.plugins.JavaPluginExtension
            import org.jetbrains.kotlin.gradle.dsl.JvmTarget
            import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

            plugins {
                id("$conventionPlugin")
            }

            tasks.register("verifyConvention") {
                doLast {
                    $assertions
                }
            }
            """.trimIndent() + "\n",
        )
    }

    private fun libraryAssertions() =
        """
        check(project.pluginManager.hasPlugin("java-library"))
        check(project.pluginManager.hasPlugin("org.jetbrains.kotlin.jvm"))
        check(project.pluginManager.hasPlugin("org.jlleitschuh.gradle.ktlint"))
        val java = project.extensions.getByType<JavaPluginExtension>()
        check(java.sourceCompatibility == JavaVersion.VERSION_17)
        check(java.targetCompatibility == JavaVersion.VERSION_17)
        val kotlin = project.extensions.getByType<KotlinJvmProjectExtension>()
        check(kotlin.compilerOptions.jvmTarget.get() == JvmTarget.JVM_17)
        check(project.configurations.getByName("testImplementation").dependencies.any {
            it.group == "junit" && it.name == "junit"
        })
        check(project.tasks.findByName("ktlintCheck") != null)
        """.trimIndent()

    private fun domainAssertions() =
        """
        ${libraryAssertions()}
        check(project.pluginManager.hasPlugin("java-test-fixtures"))
        check(!project.pluginManager.hasPlugin("com.android.library"))
        check(project.configurations.findByName("testFixturesApiElements") != null)
        check(project.configurations.getByName("implementation").dependencies.any {
            it.group == "javax.inject" && it.name == "javax.inject"
        })
        """.trimIndent()

    private fun runner(vararg arguments: String): GradleRunner =
        GradleRunner
            .create()
            .withProjectDir(projectDir.root)
            .withPluginClasspath()
            .withArguments(*(arguments.ifEmpty { arrayOf("verifyConvention") }), "--stacktrace")

    private companion object {
        const val VERIFY_TASK = ":verifyConvention"
    }
}
