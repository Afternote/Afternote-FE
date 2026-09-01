import assert from "node:assert/strict";
import test from "node:test";

import {
    changedPathsFromGithubFiles,
    githubOutputLines,
    isAndroidModuleBuild,
    resolvePrImpact,
} from "./resolve-pr-impact.mjs";

const modules = [
    module(":app", { screenshot: false }),
    module(":core:common"),
    module(":core:ui", { screenshot: true }),
    module(":feature:home:presentation", { screenshot: true }),
    module(":feature:setting:domain", { android: false }),
    module(":konsist", { android: false, coverage: false }),
];

const dependencies = new Map([
    [":app", new Set([":feature:home:presentation"])],
    [":core:common", new Set()],
    [":core:ui", new Set([":core:common"])],
    [":feature:home:presentation", new Set([":core:ui"])],
    [":feature:setting:domain", new Set()],
    [":konsist", new Set()],
]);

function module(projectPath, overrides = {}) {
    return {
        projectPath,
        directory: projectPath.slice(1).replaceAll(":", "/"),
        android: true,
        coverage: true,
        screenshot: false,
        ...overrides,
    };
}

test("GitHub pagination and rename payloads include both old and new paths", () => {
    assert.deepEqual(
        changedPathsFromGithubFiles([
            [{ filename: "feature/home/New.kt", previous_filename: "feature/home/Old.kt" }],
            [{ filename: "README.md" }],
        ]),
        ["README.md", "feature/home/New.kt", "feature/home/Old.kt"],
    );
});

test("JVM and Android test plugins are excluded from Android lint task selection", () => {
    assert.equal(isAndroidModuleBuild('plugins { id("java-library") }'), false);
    assert.equal(isAndroidModuleBuild('plugins { id("afternote.jvm.library") }'), false);
    assert.equal(isAndroidModuleBuild('plugins { id("afternote.jvm.domain") }'), false);
    assert.equal(isAndroidModuleBuild('plugins { id("com.android.test") }'), false);
    assert.equal(isAndroidModuleBuild('plugins { alias(libs.plugins.android.test) }'), false);
    assert.equal(isAndroidModuleBuild('plugins { id("afternote.android.library") }'), true);

    const mixedModules = [module(":app"), module(":core:model", { android: false })];
    const mixedDependencies = new Map([
        [":app", new Set([":core:model"])],
        [":core:model", new Set()],
    ]);
    const impact = resolvePrImpact(
        ["core/model/src/main/kotlin/UserModel.kt"],
        mixedModules,
        mixedDependencies,
    );

    assert.deepEqual(impact.coverageModules, [":app", ":core:model"]);
    assert.deepEqual(impact.androidLintTasks, [
        ":app:lintDebug",
        ":app:processDebugMainManifest",
    ]);
    assert.ok(!impact.androidLintTasks.includes(":core:model:lintDebug"));
});

test("production changes run direct formatting and reverse-dependent tests and lint", () => {
    const impact = resolvePrImpact(
        ["feature/home/presentation/src/main/kotlin/HomeScreen.kt"],
        modules,
        dependencies,
    );

    assert.deepEqual(impact.ktlintTasks, [":feature:home:presentation:ktlintCheck"]);
    assert.deepEqual(impact.coverageModules, [":app", ":feature:home:presentation"]);
    assert.deepEqual(impact.androidLintTasks, [
        ":app:lintDebug",
        ":feature:home:presentation:lintDebug",
        ":app:processDebugMainManifest",
    ]);
    assert.equal(impact.verifyManifest, true);
    assert.deepEqual(impact.screenshotTasks, [
        ":feature:home:presentation:validateScreenshotTest",
    ]);
    assert.equal(impact.codeqlJavaKotlin, true);
});

test("shared UI changes select screenshot consumers but not unrelated modules", () => {
    const impact = resolvePrImpact(
        ["core/ui/src/main/kotlin/SharedButton.kt"],
        modules,
        dependencies,
    );

    assert.deepEqual(impact.screenshotModules, [":core:ui", ":feature:home:presentation"]);
    assert.deepEqual(impact.coverageModules, [
        ":app",
        ":core:ui",
        ":feature:home:presentation",
    ]);
    assert.ok(!impact.coverageModules.includes(":core:common"));
});

test("unit-test-only changes do not run consumer modules or screenshots", () => {
    const impact = resolvePrImpact(
        ["core/common/src/test/kotlin/CommonTest.kt"],
        modules,
        dependencies,
    );

    assert.deepEqual(impact.coverageModules, [":core:common"]);
    assert.deepEqual(impact.unitTestTasks, [":core:common:koverXmlReportCi"]);
    assert.deepEqual(impact.screenshotTasks, []);
    assert.deepEqual(impact.androidLintTasks, []);
    assert.equal(impact.codeqlJavaKotlin, false);
});

test("konsist guard changes run the guard they touch", () => {
    // :konsist 는 src/main 이 없어 프로덕션 소스 분기를 못 타고, kover 도 없어 coverageModules
    // 에서도 걸러진다. 이 두 조건이 겹쳐 unitTestTasks 가 통째로 비면 unit_test_required 가
    // false 로 내려가 Unit Test job 자체가 skip 된다 — 가드를 고친 PR 이 가드를 안 돌린다(#1521).
    const impact = resolvePrImpact(
        ["konsist/src/test/kotlin/com/afternote/konsist/LayerDependencyKonsistTest.kt"],
        modules,
        dependencies,
    );

    assert.deepEqual(impact.unitTestTasks, [":konsist:test"]);
    assert.deepEqual(impact.coverageModules, []);
    assert.deepEqual(impact.ktlintTasks, [":konsist:ktlintCheck"]);
    assert.deepEqual(impact.androidLintTasks, []);
    assert.deepEqual(impact.screenshotTasks, []);
    assert.equal(impact.codeqlJavaKotlin, false);
    assert.equal(impact.repositoryQualityFull, false);
});

test("impact-policy workflow changes fail closed to every lane", () => {
    const impact = resolvePrImpact(
        [".github/workflows/pr-validation.yml", ".github/scripts/pr-gate-policy.test.mjs"],
        modules,
        dependencies,
    );

    assert.equal(impact.runNodeTests, true);
    assert.equal(impact.codeqlActions, true);
    assert.equal(impact.codeqlJavaKotlin, true);
    assert.ok(impact.ktlintTasks.length > 0);
    assert.ok(impact.unitTestTasks.length > 0);
    assert.ok(impact.screenshotTasks.length > 0);
});

test("expected-failure list changes fail closed to every lane", () => {
    for (const policyPath of [
        ".github/ci-expected-failures.json",
        ".github/ci-expected-failures.init.gradle",
        ".github/scripts/ci-expected-failures.mjs",
    ]) {
        const impact = resolvePrImpact([policyPath], modules, dependencies);

        assert.equal(impact.repositoryQualityFull, true, `${policyPath} must force full validation`);
        assert.ok(impact.unitTestTasks.length > 0);
        assert.ok(impact.screenshotTasks.length > 0);
    }
});

test("documentation-only changes have no heavy impact", () => {
    const impact = resolvePrImpact(["README.md", "docs/testing.md"], modules, dependencies);

    assert.deepEqual(impact.ktlintTasks, []);
    assert.deepEqual(impact.androidLintTasks, []);
    assert.deepEqual(impact.unitTestTasks, []);
    assert.deepEqual(impact.screenshotTasks, []);
    assert.equal(impact.runNodeTests, false);
    assert.equal(impact.codeqlActions, false);
    assert.equal(impact.codeqlJavaKotlin, false);
});

test("global Gradle changes fail closed to every module and full screenshot scope", () => {
    const impact = resolvePrImpact(["gradle/libs.versions.toml"], modules, dependencies);

    assert.deepEqual(impact.ktlintTasks, ["ktlintCheck"]);
    assert.deepEqual(impact.coverageModules, [
        ":app",
        ":core:common",
        ":core:ui",
        ":feature:home:presentation",
        ":feature:setting:domain",
    ]);
    assert.deepEqual(impact.screenshotModules, [":core:ui", ":feature:home:presentation"]);
    assert.ok(impact.unitTestTasks.includes(":konsist:test"));
    assert.ok(impact.unitTestTasks.includes(":app:compileDebugAndroidTestKotlin"));
    assert.equal(impact.codeqlJavaKotlin, true);
});

test("unknown paths fail closed instead of silently skipping validation", () => {
    const impact = resolvePrImpact(["unexpected/runtime-config.toml"], modules, dependencies);

    assert.equal(impact.codeqlActions, true);
    assert.equal(impact.codeqlJavaKotlin, true);
    assert.ok(impact.unitTestTasks.length > 0);
    assert.ok(impact.screenshotTasks.length > 0);
});

test("GitHub outputs preserve empty scopes and booleans", () => {
    const impact = resolvePrImpact(["README.md"], modules, dependencies);
    const output = githubOutputLines(impact).join("\n");

    assert.match(output, /^ktlint_required=false$/m);
    assert.match(output, /^unit_test_tasks=$/m);
    assert.match(output, /^codeql_actions=false$/m);
});
