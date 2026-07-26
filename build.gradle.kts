// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.app.distribution) apply false
}

tasks.register<Exec>("installGitHooks") {
    group = "verification"
    description = "Installs git-hooks/{pre-commit,pre-push} into the shared git hooks dir (run once per clone)."
    workingDir(layout.projectDirectory)
    commandLine(
        "sh",
        "-c",
        // worktree 에서는 .git 이 디렉터리가 아니라 gitdir 포인터 파일이라 ".git/hooks" 가
        // 성립하지 않는다. hooks 는 메인 저장소와 공용이므로 항상
        // `git rev-parse --git-common-dir` 기준으로 설치한다.
        "HOOKS_DIR=\"\$(git rev-parse --git-common-dir 2>/dev/null)/hooks\"; " +
            "if test -d \"\$HOOKS_DIR\"; then " +
            "cp git-hooks/pre-commit \"\$HOOKS_DIR/pre-commit\" && " +
            "chmod +x \"\$HOOKS_DIR/pre-commit\" && " +
            "cp git-hooks/pre-push \"\$HOOKS_DIR/pre-push\" && " +
            "chmod +x \"\$HOOKS_DIR/pre-push\" && " +
            "echo \"Installed \$HOOKS_DIR/{pre-commit,pre-push}\"; " +
            "else echo \"installGitHooks: git hooks dir not found, skipping\"; fi",
    )
}
