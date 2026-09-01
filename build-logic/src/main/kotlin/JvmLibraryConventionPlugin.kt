import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/** 순수 JVM 라이브러리의 Java/Kotlin 타깃과 기본 단위 테스트 의존성을 통일한다. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("java-library")
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            pluginManager.apply("org.jlleitschuh.gradle.ktlint")

            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            extensions.configure<KotlinJvmProjectExtension> {
                configureProductionExplicitApi(path)
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }
            }
            configureKtlint(isAndroid = false)

            afterNoteDependencies {
                testImplementation("junit")
            }
        }
    }
}
