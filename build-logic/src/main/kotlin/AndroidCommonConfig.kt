import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

internal fun Project.configureAndroidCommon(
    extension: CommonExtension
) {
    extension.compileSdk = 36

    when (extension) {
        is ApplicationExtension -> extension.configureDefaultConfig()
        is LibraryExtension -> extension.configureDefaultConfig()
    }

    extensions.findByType(org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension::class.java)
        ?.compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }

    afterNoteDependencies {
        implementation("androidx-core-ktx")
        testImplementation("junit")
        androidTestImplementation("androidx-junit")
        androidTestImplementation("androidx-espresso-core")

    }
}

private fun CommonExtension.configureDefaultConfig() {
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
                consumerProguardFiles("consumer-rules.pro")
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }
}

internal fun Project.configureCompose(
    extension: CommonExtension
) {
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

private fun ApplicationExtension.configureDefaultConfig() {
    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

private fun LibraryExtension.configureDefaultConfig() {
    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
