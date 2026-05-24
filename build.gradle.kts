// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

tasks.register<Exec>("installGitHooks") {
    group = "verification"
    description = "Installs git-hooks/pre-commit into .git/hooks (run once per clone)."
    workingDir(layout.projectDirectory)
    commandLine(
        "sh",
        "-c",
        "if test -d .git/hooks; then " +
            "cp git-hooks/pre-commit .git/hooks/pre-commit && " +
            "chmod +x .git/hooks/pre-commit && " +
            "echo \"Installed .git/hooks/pre-commit\"; " +
            "else echo \"installGitHooks: .git/hooks not found, skipping\"; fi",
    )
}
