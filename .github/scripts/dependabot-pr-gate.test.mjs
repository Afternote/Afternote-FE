import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

import { inspectCiTestPlan } from "./ci-test-plan.mjs";
import {
    DEPENDABOT_LOGIN,
    GATE_MARKER,
    ISSUE_ASSIGNEES,
    ISSUE_LABELS,
    applyGate,
    buildIssue,
    buildPlan,
    needsGate,
} from "./dependabot-pr-gate.mjs";

const BOT_TITLE = "chore(deps): bump the github-actions-updates group with 3 updates";
const BOT_BODY = "Bumps the github-actions-updates group with 3 updates: actions/setup-java ...";

test("처리되지 않은 봇 PR 만 게이트 대상이다", () => {
    assert.equal(needsGate({ title: BOT_TITLE, body: BOT_BODY }), true);
    assert.equal(needsGate({ title: `${BOT_TITLE} (#2111)`, body: BOT_BODY }), true, "제목만 고친 것은 본문이 아직 비었다");
    assert.equal(needsGate({ title: BOT_TITLE, body: `${GATE_MARKER}\n${BOT_BODY}` }), true, "본문만 고친 것은 제목이 아직 비었다");
    assert.equal(needsGate({ title: `${BOT_TITLE} (#2111)`, body: `${GATE_MARKER}\n${BOT_BODY}` }), false);
    assert.equal(needsGate({ title: null, body: null }), true);
});

test("mode 는 ci-test-plan 의 판정을 그대로 따른다: 기기 레인 정의를 건드리면 full, 아니면 none", () => {
    const full = buildPlan([".github/workflows/android-managed-device.yml", ".github/workflows/lint.yml"]);
    assert.equal(full.mode, "full");
    assert.deepEqual(full.requiredPaths, [".github/workflows/android-managed-device.yml"]);
    assert.match(full.reason, /android-managed-device\.yml/);

    const none = buildPlan([".github/workflows/codeql.yml", ".github/workflows/lint.yml"]);
    assert.equal(none.mode, "none");
    assert.deepEqual(none.requiredPaths, []);

    const gradle = buildPlan(["gradle/libs.versions.toml"]);
    assert.equal(gradle.mode, "full", "gradle/ 접두사도 full 이다");
});

test("봇 범프가 androidTest 경계 경로를 건드리면 자동으로 정하지 않고 실패한다", () => {
    assert.throws(
        () => buildPlan(["app/src/main/AndroidManifest.xml"]),
        /사람이 plan 을 정해야 합니다/,
    );
});

test("대표 이슈는 issue.yml 양식이고 platform maintenance 로 1hyok 에게 간다", () => {
    const plan = buildPlan([".github/workflows/codeql.yml"]);
    const issue = buildIssue({ title: BOT_TITLE, prNumber: 2106, changedPaths: [".github/workflows/codeql.yml"], plan });

    assert.equal(issue.title, "chore(ci): dependabot 범프 수용: bump the github-actions-updates group with 3 updates");
    assert.deepEqual(issue.labels, ISSUE_LABELS);
    assert.deepEqual(issue.assignees, ISSUE_ASSIGNEES);
    assert.deepEqual(ISSUE_LABELS, ["maintenance", "area:platform"]);
    assert.deepEqual(ISSUE_ASSIGNEES, ["1hyok"]);
    assert.match(issue.body, /^### 작업 유형\n\nmaintenance — /);
    assert.match(issue.body, /### 주 담당 모듈\n\nplatform — /);
    assert.match(issue.body, /PR #2106/);
    assert.match(issue.body, /- `\.github\/workflows\/codeql\.yml`/);
    assert.match(issue.body, /mode=none/);
    assert.match(issue.body, /### 참고/);
});

test("제목은 (#N) 으로 정확히 한 번 끝나고 본문은 Refs 와 유효한 CI Test Plan 으로 시작한다", () => {
    const plan = buildPlan([".github/workflows/android-managed-device.yml"]);
    const next = applyGate({ title: BOT_TITLE, body: BOT_BODY, issueNumber: 2111, plan });

    assert.equal(next.title, `${BOT_TITLE} (#2111)`);
    assert.equal((next.title.match(/\(#\d+\)/g) ?? []).length, 1);
    assert.ok(next.body.startsWith(`${GATE_MARKER}\nRefs #2111\n`));
    assert.ok(next.body.endsWith(BOT_BODY), "봇 본문은 그대로 아래에 남는다");
    assert.equal(needsGate(next), false, "한 번 적용한 결과는 다시 대상이 아니다");

    const inspected = inspectCiTestPlan(next.body, { pullRequestNumber: 2106 });
    assert.deepEqual(inspected.errors ?? [], [], JSON.stringify(inspected));
    assert.equal(inspected.plan?.androidTest?.mode ?? inspected.androidTest?.mode, "full");
});

test("이미 (#N) 이 붙은 제목에 다시 적용해도 번호가 겹치지 않는다", () => {
    const plan = buildPlan([".github/workflows/codeql.yml"]);
    const once = applyGate({ title: BOT_TITLE, body: BOT_BODY, issueNumber: 2111, plan });
    const twice = applyGate({ title: once.title, body: once.body, issueNumber: 2111, plan });
    assert.equal(twice.title, once.title);
    assert.equal((twice.body.match(/Refs #2111/g) ?? []).length, 2, "본문 재적용은 앞선 머리말을 봇 본문으로 취급한다");
    assert.equal((twice.body.match(new RegExp(GATE_MARKER.replace(/[-[\]{}()*+?.,\\^$|#\s]/g, "\\$&"), "g")) ?? []).length, 1);
});

test("워크플로는 봇 PR 에만 돌고 PR 코드를 체크아웃하지 않는다", async () => {
    const source = await readFile(new URL("../workflows/dependabot-pr-gate.yml", import.meta.url), "utf8");

    assert.match(source, /pull_request_target:/);
    assert.match(source, new RegExp(`if: github\\.event\\.pull_request\\.user\\.login == '${DEPENDABOT_LOGIN.replace(/[[\]]/g, "\\$&")}'`));
    assert.match(source, /ref: \$\{\{ github\.event\.repository\.default_branch \}\}/);
    assert.doesNotMatch(source, /pull_request\.head/, "PR 쪽 ref 를 참조하지 않는다");
    assert.match(source, /persist-credentials: false/);
    assert.match(source, /sparse-checkout: \.github\/scripts/);
    assert.match(source, /timeout-minutes: 5/);
    assert.match(source, /^permissions: \{\}/m);
    assert.match(source, /issues: write/);
    assert.match(source, /pull-requests: write/);
    assert.match(source, /run: node \.github\/scripts\/dependabot-pr-gate\.mjs/);
});
