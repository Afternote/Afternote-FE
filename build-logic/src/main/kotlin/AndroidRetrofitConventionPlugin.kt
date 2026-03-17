import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidRetrofitConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            afterNoteDependencies {
                implementation("retrofit")
                implementation("okhttp")
                implementation("okhttp-logging")
                implementation("retrofit-converter-kotlinx-serialization")
                implementation("kotlinx-serialization-json")
            }
        }
    }
}
