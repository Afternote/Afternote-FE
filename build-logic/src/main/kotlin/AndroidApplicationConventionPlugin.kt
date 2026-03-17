import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            pluginManager.apply("afternote.android.hilt")

            extensions.configure<ApplicationExtension> {
                configureAndroidCommon(this) // 기본 뼈대
                configureCompose(this)       // Compose 부품

                defaultConfig.targetSdk = 34
            }

            afterNoteDependencies {
                implementation("androidx-activity-compose")
                implementation("androidx-lifecycle-runtime-ktx")
            }
        }
    }
}