import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependencyBundle
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.project

internal fun Project.afterNoteDependencies(block: AfterNoteDependencyScope.() -> Unit) {
    dependencies {
        AfterNoteDependencyScope(this@afterNoteDependencies, this).apply(block)
    }
}

internal class AfterNoteDependencyScope(
    private val project: Project,
    private val dependencies: DependencyHandlerScope,
) {
    val libs: VersionCatalog =
        project.extensions
            .getByType<VersionCatalogsExtension>()
            .named("libs")

    private fun findLibrary(alias: String): Provider<MinimalExternalModuleDependency> {
        val library = libs.findLibrary(alias)
        if (library.isEmpty) {
            error("Library alias '$alias'를 libs.versions.toml에서 찾을 수 없습니다.")
        }
        return library.get()
    }

    private fun findBundle(alias: String): Provider<ExternalModuleDependencyBundle> {
        val bundle = libs.findBundle(alias)
        if (bundle.isEmpty) {
            error("Bundle alias '$alias'를 libs.versions.toml에서 찾을 수 없습니다.")
        }
        return bundle.get()
    }

    fun implementation(alias: String) {
        dependencies.add("implementation", findLibrary(alias))
    }

    fun implementation(dependency: Any) {
        dependencies.add("implementation", dependency)
    }

    fun ksp(alias: String) {
        dependencies.add("ksp", findLibrary(alias))
    }

    fun api(alias: String) {
        dependencies.add("api", findLibrary(alias))
    }

    fun testImplementation(alias: String) {
        dependencies.add("testImplementation", findLibrary(alias))
    }

    fun androidTestImplementation(alias: String) {
        dependencies.add("androidTestImplementation", findLibrary(alias))
    }

    fun androidTestImplementation(dependency: Any) {
        dependencies.add("androidTestImplementation", dependency)
    }

    fun debugImplementation(alias: String) {
        dependencies.add("debugImplementation", findLibrary(alias))
    }

    fun project(path: String) {
        dependencies.add("implementation", dependencies.project(path))
    }

    fun platform(dependency: Any): Any {
        val resolvedDependency =
            if (dependency is Provider<*>) {
                dependency.get()
            } else {
                dependency
            }
        return project.dependencies.platform(resolvedDependency)
    }
}
