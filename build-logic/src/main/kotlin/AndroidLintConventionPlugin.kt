import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidLintConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jlleitschuh.gradle.ktlint")
            configureKtlint(isAndroid = true)
        }
    }
}
