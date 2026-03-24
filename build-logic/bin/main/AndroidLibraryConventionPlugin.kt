
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // 1. 안드로이드 라이브러리 플러그인 적용
            pluginManager.apply("com.android.library")
            pluginManager.apply("afternote.android.lint")

            extensions.configure<LibraryExtension> {
                configureAndroidCommon(this)
            }
        }
    }
}