import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

import { ASSIGNEE_BY_MODULE, HANDOVER_BY_MODULE } from "./reconcile-issue-metadata.mjs";
import {
    formatViolations,
    isProductionPath,
    moduleKeyOf,
    validatePullRequestModuleOwner,
} from "./validate-pr-module-owner.mjs";

// 기본 이슈 번호는 모든 이관 경계 위다. 경계 아래 판정은 아래 전용 테스트가 따로 본다.
const pr = (login, labels = [], type = "User", issueNumber = 9999) => ({
    number: 1,
    title: `fix(scope): 요약 (#${issueNumber})`,
    user: { login, type },
    labels: labels.map((name) => ({ name })),
});

test("경로가 모듈 키로 갈린다 — feature·core·platform·소유자 없음", () => {
    assert.equal(moduleKeyOf("feature/mindrecord/presentation/src/main/kotlin/A.kt"), "mindrecord");
    assert.equal(moduleKeyOf("feature/home/presentation/src/main/kotlin/receiver/ReceiverHomeScreen.kt"), "home");
    assert.equal(moduleKeyOf("core/ui/src/main/kotlin/Scaffold.kt"), "core");
    assert.equal(moduleKeyOf("app/src/main/AndroidManifest.xml"), "platform");
    assert.equal(moduleKeyOf(".github/workflows/lint.yml"), "platform");
    assert.equal(moduleKeyOf("settings.gradle.kts"), "platform");
    assert.equal(moduleKeyOf("docs/qa/assumptions.md"), null);
    assert.equal(moduleKeyOf("README.md"), null);
});

test("테스트 소스셋은 프로덕션이 아니고, 모듈의 빌드 스크립트·리소스는 프로덕션이다", () => {
    for (const p of [
        "feature/setting/presentation/src/test/kotlin/T.kt",
        "app/src/androidTest/java/A.kt",
        "core/domain/src/testFixtures/kotlin/Fake.kt",
        "feature/home/presentation/src/screenshotTest/kotlin/S.kt",
        // 골든은 variant 접미사 소스셋에 산다 — core:ui 변경이 남의 모듈 골든을 갱신하는 것은 정상 경로다.
        "feature/setting/presentation/src/screenshotTestDebug/reference/com/afternote/feature/setting/presentation/screen/S_0.png",
        "feature/afternote/presentation/src/screenshotTestRelease/reference/x.png",
    ]) assert.equal(isProductionPath(p), false, p);
    for (const p of [
        "feature/setting/presentation/src/main/kotlin/A.kt",
        "feature/setting/presentation/build.gradle.kts",
        "app/src/debug/AndroidManifest.xml",
        "core/ui/src/main/res/values/strings.xml",
    ]) assert.equal(isProductionPath(p), true, p);
});

test("자기 모듈만 바꾼 PR 은 통과한다", () => {
    const { violations } = validatePullRequestModuleOwner({
        pullRequest: pr("Sadturtleman"),
        changedPaths: ["feature/mindrecord/presentation/src/main/kotlin/A.kt", "feature/home/presentation/src/main/kotlin/B.kt"],
    });
    assert.deepEqual(violations, []);
});

test("남의 모듈 프로덕션 파일이 섞이면 모듈·담당자와 함께 실패한다", () => {
    const { violations } = validatePullRequestModuleOwner({
        pullRequest: pr("Sadturtleman"),
        changedPaths: ["feature/mindrecord/presentation/src/main/kotlin/A.kt", "core/ui/src/main/kotlin/Theme.kt", "app/src/main/res/values/themes.xml"],
    });
    assert.deepEqual(violations, [
        { path: "core/ui/src/main/kotlin/Theme.kt", module: "core", owner: ASSIGNEE_BY_MODULE.core },
        { path: "app/src/main/res/values/themes.xml", module: "platform", owner: ASSIGNEE_BY_MODULE.platform },
    ]);
    const message = formatViolations(pr("Sadturtleman"), violations);
    assert.match(message, /core\(담당 @1hyok\) 1건/);
    assert.match(message, /platform\(담당 @1hyok\) 1건/);
    assert.match(message, /issue-assignee-exempt/);
});

test("남의 모듈이라도 테스트 소스셋·문서는 위반이 아니다 (0830 확정)", () => {
    const { violations } = validatePullRequestModuleOwner({
        pullRequest: pr("Sadturtleman"),
        changedPaths: ["core/ui/src/test/kotlin/ThemeTest.kt", "app/src/androidTest/java/X.kt", "docs/qa/assumptions.md", "README.md"],
    });
    assert.deepEqual(violations, []);
});

test("issue-assignee-exempt 라벨은 파일 담당 대조도 함께 면제한다", () => {
    const result = validatePullRequestModuleOwner({
        pullRequest: pr("Sadturtleman", ["issue-assignee-exempt"]),
        changedPaths: ["core/ui/src/main/kotlin/Theme.kt"],
    });
    assert.equal(result.skipped, "issue-assignee-exempt 라벨");
    assert.deepEqual(result.violations, []);
});

test("봇 작성자는 판정하지 않는다", () => {
    for (const bot of [pr("dependabot[bot]"), pr("github-actions", [], "Bot")]) {
        const result = validatePullRequestModuleOwner({ pullRequest: bot, changedPaths: ["gradle/libs.versions.toml"] });
        assert.equal(result.skipped, "봇 작성자");
    }
});

test("담당자 비교는 대소문자를 가리지 않는다", () => {
    const { violations } = validatePullRequestModuleOwner({
        pullRequest: pr("SADTURTLEMAN"),
        changedPaths: ["feature/mindrecord/presentation/src/main/kotlin/A.kt"],
    });
    assert.deepEqual(violations, []);
});

test("지도는 reconcile-issue-metadata 하나다 — 이 스크립트에 담당 표를 다시 적지 않는다", async () => {
    const source = await readFile(new URL("./validate-pr-module-owner.mjs", import.meta.url), "utf8");
    assert.match(source, /import \{ assigneeForIssue \} from "\.\/reconcile-issue-metadata\.mjs"/);
    for (const login of new Set(Object.values(ASSIGNEE_BY_MODULE))) {
        assert.doesNotMatch(source, new RegExp(`"${login}"`), `담당자 ${login} 가 스크립트에 하드코딩됐다`);
    }
});

test("Repository Quality 가 이 검증기를 링크 게이트 뒤에 PR 전용으로 부른다", async () => {
    const workflow = await readFile(new URL("../workflows/repository-quality.yml", import.meta.url), "utf8");
    const linked = workflow.indexOf("- name: Require linked Issue");
    const owner = workflow.indexOf("- name: Require module owner");
    assert.notEqual(owner, -1, "Require module owner 스텝이 없다");
    assert.ok(owner > linked, "링크 게이트 뒤에 와야 한다 — 이슈 담당 대조가 먼저다");
    assert.match(workflow, /- name: Require module owner\n\s+if: inputs\.pull_request_number > 0/);
    assert.match(workflow, /validate-pr-module-owner\.mjs "\$PULL_REQUEST_JSON" "\$FILES_JSON"/);
});

test("이관 경계 아래 이슈로 연 PR 은 옛 담당자 기준으로 판정한다 (#1910)", () => {
    // 경로만 보는 가드가 지도값만 읽으면, 이관 전에 열린 이슈로 진행 중인 PR 이 남의 모듈을
    // 건드린 것으로 잡힌다. 이슈 담당 대조는 통과시키는데 이쪽만 막는 상태가 된다.
    for (const [module, handover] of Object.entries(HANDOVER_BY_MODULE)) {
        const path = `feature/${module}/presentation/src/main/kotlin/A.kt`;
        assert.equal(moduleKeyOf(path), module);

        const before = validatePullRequestModuleOwner({
            pullRequest: pr(handover.before, [], "User", handover.fromIssue - 1),
            changedPaths: [path],
        });
        assert.deepEqual(before.violations, [], `${module} 경계 아래는 옛 담당자 몫이다`);

        const after = validatePullRequestModuleOwner({
            pullRequest: pr(handover.before, [], "User", handover.fromIssue),
            changedPaths: [path],
        });
        assert.deepEqual(
            after.violations,
            [{ path, module, owner: ASSIGNEE_BY_MODULE[module] }],
            `${module} 경계 위는 새 담당자 몫이다`,
        );
    }
});

test("대표 이슈 번호를 못 읽으면 대조를 건너뛴다 — 제목 게이트가 앞에서 막는다", () => {
    const result = validatePullRequestModuleOwner({
        pullRequest: {
            number: 1,
            title: "fix(core): 제목이 대표 이슈로 끝나지 않는다",
            user: { login: "Sadturtleman", type: "User" },
            labels: [],
        },
        changedPaths: ["core/ui/src/main/kotlin/Theme.kt"],
    });
    assert.equal(result.skipped, "대표 이슈 번호 없음");
    assert.deepEqual(result.violations, []);
});
