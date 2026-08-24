import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.kotlin.dsl.configure

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.kover)
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.app.distribution) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}

val coverageProjects =
    subprojects.filter { project ->
        project.buildFile.isFile &&
            (
                project.path == ":app" ||
                    project.path.startsWith(":core:") ||
                    project.path.startsWith(":feature:")
            )
    }

val coverageClassExclusions =
    arrayOf(
        // Hilt/Dagger and Compose compiler output contains no hand-written product decisions.
        "*Dagger*",
        "*Hilt_*",
        "*HiltWrapper_*",
        "hilt_aggregated_deps.*",
        "*_ComponentTreeDeps*",
        "*_*Factory*",
        "*_GeneratedInjector*",
        "*_HiltModules*",
        "*_MembersInjector*",
        "*ComposableSingletons*",
    )

fun KoverProjectExtension.configureCoverageReports() {
    reports {
        filters {
            excludes {
                // BuildConfig, R, Manifest and other Android-generated classes.
                androidGeneratedClasses()
                classes(*coverageClassExclusions)
            }
        }
    }
}

kover {
    currentProject {
        // The root owns only the merged report; source/test variants come from dependencies below.
        createVariant("ci") {}
    }
    configureCoverageReports()
}

coverageProjects.forEach { coverageProject ->
    coverageProject.pluginManager.apply("org.jetbrains.kotlinx.kover")
    coverageProject.extensions.configure<KoverProjectExtension> {
        configureCoverageReports()
    }
    coverageProject.pluginManager.withPlugin("com.android.application") {
        coverageProject.extensions.configure<KoverProjectExtension> {
            currentProject {
                createVariant("ci") {
                    add("debug")
                }
            }
        }
    }
    coverageProject.pluginManager.withPlugin("com.android.library") {
        coverageProject.extensions.configure<KoverProjectExtension> {
            currentProject {
                createVariant("ci") {
                    add("debug")
                }
            }
        }
    }
    coverageProject.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        coverageProject.extensions.configure<KoverProjectExtension> {
            currentProject {
                createVariant("ci") {
                    add("jvm")
                }
            }
        }
    }
}

dependencies {
    coverageProjects.forEach { coverageProject ->
        kover(project(coverageProject.path))
    }
}

tasks.register<Exec>("installGitHooks") {
    group = "verification"
    description = "Installs git-hooks/pre-commit into the shared git hooks dir (run once per clone)."
    workingDir(layout.projectDirectory)
    commandLine(
        "sh",
        "-c",
        // worktree 에서는 .git 이 디렉터리가 아니라 gitdir 포인터 파일이라 ".git/hooks" 가
        // 성립하지 않는다. hooks 는 메인 저장소와 공용이므로 항상
        // `git rev-parse --git-common-dir` 기준으로 설치한다.
        // pre-push(컴파일 검증)는 제거됨 — PR 시점 검증(CI·create-pr)으로 위임(#478).
        // 과거 클론에 설치된 잔존본도 여기서 걷어낸다.
        "HOOKS_DIR=\"\$(git rev-parse --git-common-dir 2>/dev/null)/hooks\"; " +
            "if test -d \"\$HOOKS_DIR\"; then " +
            "cp git-hooks/pre-commit \"\$HOOKS_DIR/pre-commit\" && " +
            "chmod +x \"\$HOOKS_DIR/pre-commit\" && " +
            "rm -f \"\$HOOKS_DIR/pre-push\" && " +
            "echo \"Installed \$HOOKS_DIR/pre-commit (removed legacy pre-push)\"; " +
            "else echo \"installGitHooks: git hooks dir not found, skipping\"; fi",
    )
}
