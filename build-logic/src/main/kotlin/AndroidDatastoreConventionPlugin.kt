import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidDatastoreConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("afternote.android.library")
            pluginManager.apply("afternote.android.hilt")
            afterNoteDependencies {
            }
        }
    }
}
