import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidHiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.google.devtools.ksp")
                apply("dagger.hilt.android.plugin")
            }

            afterNoteDependencies {
                implementation("hilt-android")
                implementation("hilt-navigation")
                ksp("hilt-compiler")
            }
        }
    }
}