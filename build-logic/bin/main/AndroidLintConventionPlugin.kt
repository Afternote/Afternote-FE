import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.jlleitschuh.gradle.ktlint.KtlintExtension

class AndroidLintConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jlleitschuh.gradle.ktlint")

            extensions.configure(KtlintExtension::class.java) {
                debug.set(false)
                verbose.set(true)
                android.set(true)
                outputToConsole.set(true)
                ignoreFailures.set(false)
                filter {
                    exclude { it.file.path.contains("build/") }
                }
            }

            // VersionCatalog 명시적 접근
            val libs = extensions
                .getByType<VersionCatalogsExtension>()
                .named("libs")

            dependencies.add(
                "ktlintRuleset",
                libs.findLibrary("compose-rules").get()
            )
        }
    }
}