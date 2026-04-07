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

tasks.register("installGitHooks") {
    group = "verification"
    description = "Installs git-hooks/pre-commit into .git/hooks (run once per clone)."
    notCompatibleWithConfigurationCache("Copies files from Project layout in doLast.")
    doLast {
        val source = rootDir.resolve("git-hooks/pre-commit")
        val dest = rootDir.resolve(".git/hooks/pre-commit")
        val hooksDir = dest.parentFile
        if (!hooksDir.isDirectory) {
            logger.lifecycle("installGitHooks: ${hooksDir.path} not found; skipping.")
            return@doLast
        }
        if (!source.isFile) {
            throw GradleException("Missing hook script: ${source.path}")
        }
        source.copyTo(dest, overwrite = true)
        dest.setExecutable(true, false)
        logger.lifecycle("Installed pre-commit hook -> ${dest.path}")
    }
}
