import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType

internal val Project.androidExtension: CommonExtension
    get() = extensions.findByType<ApplicationExtension>()
        ?: extensions.findByType<LibraryExtension>()
        ?: throw IllegalStateException("Project '$name' is not an Android Application or Library module.")


fun Project.setNamespace(name: String) {
    androidExtension.namespace = "com.afternote.$name"
}