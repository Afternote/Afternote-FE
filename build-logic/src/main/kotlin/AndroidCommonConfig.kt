import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

internal fun Project.configureAndroidCommon(extension: CommonExtension) {
    // core-ktx 1.19 / lifecycle 2.11 이 android-37 컴파일을 요구한다 (targetSdk 는 36 유지).
    extension.compileSdk = 37

    extension.configureDefaultConfig(this)

    checkNotNull(extensions.findByType(KotlinAndroidProjectExtension::class.java)) {
        "$path 에 Kotlin Android extension 이 없어 explicit API 정책을 적용할 수 없습니다."
    }.apply {
        configureProductionExplicitApi(path)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    afterNoteDependencies {
        implementation("androidx-core-ktx")
        testImplementation("junit")
        androidTestImplementation("androidx-junit")
        androidTestImplementation("androidx-espresso-core")
    }
}

private fun CommonExtension.configureDefaultConfig(project: Project) {
    when (this) {
        is ApplicationExtension -> {
            defaultConfig {
                minSdk = 26
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }

        is LibraryExtension -> {
            defaultConfig {
                minSdk = 26
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                // consumer-rules.pro 가 있는 모듈만 등록 — 없는 모듈(domain·res 등)엔 빈 파일을 강요하지 않음
                if (project.file("consumer-rules.pro").exists()) {
                    consumerProguardFiles("consumer-rules.pro")
                }
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }
}

internal fun Project.configureCompose(extension: CommonExtension) {
    when (extension) {
        is ApplicationExtension -> extension.buildFeatures { compose = true }
        is LibraryExtension -> extension.buildFeatures { compose = true }
    }

    afterNoteDependencies {
        val bom = libs.findLibrary("androidx-compose-bom").get()
        implementation(platform(bom))
        androidTestImplementation(platform(bom))
        implementation("androidx-compose-ui")
        implementation("androidx-compose-material3")
        implementation("androidx-activity-compose")
        implementation("androidx-compose-ui-tooling-preview")
        debugImplementation("androidx-compose-ui-tooling")
    }
}
