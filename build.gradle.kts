import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.kotlin.dsl.configure

// AGP 9.2.1 이 buildscript classpath 로 끌어오는 bouncycastle 1.79 는 GHSA-574f-3g2m-x479(critical)
// 영향권이다(#921). 루트 classpath 는 plugins 블록 처리 시점에 리졸브가 끝나 아래 본문 훅으로는 늦고,
// buildscript 블록에서는 버전 카탈로그 accessor 를 쓸 수 없다 — libs.versions.toml 의 bouncycastle
// 과 같은 값을 리터럴로 유지할 것.
buildscript {
    dependencies {
        constraints {
            listOf("bcprov-jdk18on", "bcpkix-jdk18on", "bcutil-jdk18on").forEach { artifact ->
                add("classpath", "org.bouncycastle:$artifact:1.84") {
                    because("GHSA-574f-3g2m-x479 등 1.84 미만 취약 — #921")
                }
            }
        }
    }
}

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

// 빌드·테스트 클래스패스의 bouncycastle 하한(#921). Robolectric 4.15.1(bcprov 1.80)·AGP 9.2.1(1.79)
// 전이 의존성이 GHSA-574f-3g2m-x479(critical) 영향권인데 상류 최신판도 1.84 미만이라 constraint 로
// 올린다. androidLintTool 처럼 AGP 가 뒤늦게 만드는 configuration 까지 잡도록 configureEach 로 걸고,
// require 시맨틱이라 상류가 1.84 이상을 선언하게 되면 그쪽이 이긴다. APK 산출물에는 bouncycastle 이
// 없다(releaseRuntimeClasspath 0건 실측). build-logic 은 별도 빌드라 build-logic/build.gradle.kts 가
// 같은 하한을 선언한다.
val bouncycastleArtifacts = listOf("bcprov-jdk18on", "bcpkix-jdk18on", "bcutil-jdk18on")
val bouncycastleFloor = libs.versions.bouncycastle.get()

fun Project.bouncycastleConstraintSet() =
    bouncycastleArtifacts.map { artifact ->
        dependencies.constraints.create("org.bouncycastle:$artifact:$bouncycastleFloor") {
            because("GHSA-574f-3g2m-x479 등 1.84 미만 취약 — #921")
        }
    }

allprojects {
    // Gradle 9 는 declarable 이 아닌 configuration(compileClasspath 등)에 constraint 선언을 금지한다.
    // 전용 declarable configuration 에 담아 모든 resolvable configuration 이 extend 하게 한다.
    val floor =
        configurations.create("bouncycastleConstraints") {
            isCanBeResolved = false
            isCanBeConsumed = false
            bouncycastleConstraintSet().forEach { dependencyConstraints.add(it) }
        }
    configurations.configureEach {
        if (name != floor.name && isCanBeResolved) extendsFrom(floor)
    }
}
// buildscript 컨테이너에는 configuration 을 추가할 수 없어 declarable 인 classpath 에 constraint 를
// 직접 단다. 루트 자신의 classpath 는 최상단 buildscript 블록이 담당한다(여기 시점엔 리졸브 완료라 불가).
subprojects {
    buildscript.configurations.named("classpath") {
        bouncycastleConstraintSet().forEach { dependencyConstraints.add(it) }
    }
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
