import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

import { decideViewedAutoMerge } from "./find-viewed-auto-merge-targets.mjs";

const viewed = (path) => ({ path, viewerViewedState: "VIEWED" });

function pullRequest(overrides = {}) {
    return {
        number: 1,
        isDraft: false,
        headRefOid: "0123456789abcdef0123456789abcdef01234567",
        reviewDecision: "APPROVED",
        isInMergeQueue: false,
        autoMergeRequest: null,
        changedFiles: 2,
        ...overrides,
    };
}

test("모든 파일이 Viewed 이고 승인 상태면 예약한다", () => {
    assert.equal(decideViewedAutoMerge(pullRequest(), [viewed("a.kt"), viewed("b.kt")]).enable, true);
});

test("하나라도 Viewed 가 아니면 예약하지 않는다 — 체크 뒤 바뀐 파일(DISMISSED) 포함", () => {
    for (const state of ["UNVIEWED", "DISMISSED"]) {
        const files = [viewed("a.kt"), { path: "b.kt", viewerViewedState: state }];
        assert.equal(decideViewedAutoMerge(pullRequest(), files).enable, false, state);
    }
});

test("승인 상태가 아니면 Viewed 를 다 해도 예약하지 않는다", () => {
    for (const reviewDecision of ["CHANGES_REQUESTED", "REVIEW_REQUIRED", null]) {
        const decision = decideViewedAutoMerge(pullRequest({ reviewDecision }), [viewed("a.kt"), viewed("b.kt")]);
        assert.equal(decision.enable, false, String(reviewDecision));
    }
});

test("파일 목록을 다 못 읽었으면 다 봤다고 단정하지 않는다", () => {
    assert.equal(decideViewedAutoMerge(pullRequest({ changedFiles: 3 }), [viewed("a.kt"), viewed("b.kt")]).enable, false);
    assert.equal(decideViewedAutoMerge(pullRequest({ changedFiles: 0 }), []).enable, false);
});

test("Draft·이미 예약·큐에 있는 PR 은 건드리지 않는다", () => {
    const files = [viewed("a.kt"), viewed("b.kt")];
    assert.equal(decideViewedAutoMerge(pullRequest({ isDraft: true }), files).enable, false);
    assert.equal(decideViewedAutoMerge(pullRequest({ autoMergeRequest: { enabledAt: "2026-10-04T00:00:00Z" } }), files).enable, false);
    assert.equal(decideViewedAutoMerge(pullRequest({ isInMergeQueue: true }), files).enable, false);
});

test("워크플로는 본인 토큰으로 읽고 사람 토큰으로 승인한 HEAD 만 예약한다", async () => {
    const workflow = await readFile(new URL("../workflows/viewed-auto-merge.yml", import.meta.url), "utf8");
    assert.match(workflow, /^permissions: \{\}$/m);
    assert.match(workflow, /VIEWED_TOKEN: \$\{\{ secrets\.VIEWED_TOKEN \}\}/);
    assert.match(workflow, /ref: \$\{\{ github\.event\.repository\.default_branch \}\}/);
    assert.match(workflow, /persist-credentials: false/);
    // GITHUB_TOKEN 으로 예약·투입하면 merge_group CI 가 뜨지 않아 큐가 60분 막힌다(1005 #2208).
    assert.match(workflow, /GH_TOKEN: \$\{\{ secrets\.MERGE_QUEUE_TOKEN \}\}/);
    assert.doesNotMatch(workflow, /GH_TOKEN: \$\{\{ github\.token \}\}/);
    assert.match(workflow, /if \[ -z "\$GH_TOKEN" \]; then[\s\S]*?exit 0/);
    assert.match(workflow, /--match-head-commit "\$head_sha"/);
    // 우회 머지 금지. merge queue 와 required check 를 그대로 탄다.
    assert.doesNotMatch(workflow, /--admin/);
    // PR 코드를 체크아웃하지 않는다.
    assert.doesNotMatch(workflow, /github\.event\.pull_request\.head|refs\/pull/);
});
