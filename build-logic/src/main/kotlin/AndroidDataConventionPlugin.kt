import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidDataConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("afternote.android.library")
            pluginManager.apply("afternote.android.retrofit")
            pluginManager.apply("afternote.android.hilt")

            afterNoteDependencies {
                project(":core:model")
                project(":core:network")
            }
        }
    }
}