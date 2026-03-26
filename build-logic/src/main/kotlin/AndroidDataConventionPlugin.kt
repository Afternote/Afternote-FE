import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidDataConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("afternote.android.library")
            pluginManager.apply("afternote.android.hilt")
            pluginManager.apply("afternote.android.retrofit")

            afterNoteDependencies {
                project(":core:datastore")
                project(":core:domain")
                project(":core:model")
                project(":core:network")
            }
        }
    }
}
