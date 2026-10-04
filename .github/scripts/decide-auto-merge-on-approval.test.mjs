import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

import { decideAutoMerge } from "./decide-auto-merge-on-approval.mjs";

const HEAD = "0123456789abcdef0123456789abcdef01234567";
const OLD = "fedcba9876543210fedcba9876543210fedcba98";

function approvedPullRequest(overrides = {}) {
    return {
        state: "OPEN",
        isDraft: false,
        baseRefName: "develop",
        headRefOid: HEAD,
        reviewDecision: "APPROVED",
        autoMergeRequest: null,
        isInMergeQueue: false,
        reviews: { nodes: [{ state: "APPROVED", commit: { oid: HEAD } }] },
        ...overrides,
    };
}

test("develop 대상 PR 이 최신 커밋에서 승인되면 자동 머지를 예약한다", () => {
    assert.deepEqual(decideAutoMerge(approvedPullRequest(), "develop"), {
        enable: true,
        reason: "승인됨 — 자동 머지 예약",
        headSha: HEAD,
    });
});

test("닫힘·Draft·다른 base 는 예약하지 않는다", () => {
    for (const overrides of [
        { state: "CLOSED" },
        { state: "MERGED" },
        { isDraft: true },
        { baseRefName: "main" },
        { baseRefName: "feat/123-parent" },
    ]) {
        assert.equal(decideAutoMerge(approvedPullRequest(overrides), "develop").enable, false, JSON.stringify(overrides));
    }
    assert.equal(decideAutoMerge(null, "develop").enable, false);
});

test("마지막 리뷰가 승인이 아니면 예약하지 않는다 — 끈 예약을 코멘트가 되살리지 않는다", () => {
    for (const state of ["COMMENTED", "CHANGES_REQUESTED", "DISMISSED"]) {
        const pullRequest = approvedPullRequest({
            reviews: { nodes: [{ state: "APPROVED", commit: { oid: HEAD } }, { state, commit: { oid: HEAD } }] },
        });
        assert.equal(decideAutoMerge(pullRequest, "develop").enable, false, state);
    }
    assert.equal(decideAutoMerge(approvedPullRequest({ reviews: { nodes: [] } }), "develop").enable, false);
});

test("PR 전체 판정이 승인이 아니면 예약하지 않는다", () => {
    for (const reviewDecision of ["CHANGES_REQUESTED", "REVIEW_REQUIRED", null]) {
        assert.equal(decideAutoMerge(approvedPullRequest({ reviewDecision }), "develop").enable, false, String(reviewDecision));
    }
});

test("승인 뒤 새 커밋이 있으면 예약하지 않는다", () => {
    const pullRequest = approvedPullRequest({ reviews: { nodes: [{ state: "APPROVED", commit: { oid: OLD } }] } });
    assert.equal(decideAutoMerge(pullRequest, "develop").enable, false);
});

test("이미 예약됐거나 큐에 있으면 다시 걸지 않는다", () => {
    assert.equal(decideAutoMerge(approvedPullRequest({ autoMergeRequest: { enabledAt: "2026-10-04T00:00:00Z" } }), "develop").enable, false);
    assert.equal(decideAutoMerge(approvedPullRequest({ isInMergeQueue: true }), "develop").enable, false);
});

test("워크플로는 리컨사일 뒤에만, 승인한 HEAD 로만 예약한다", async () => {
    const workflow = await readFile(
        new URL("../workflows/latest-review-decision-reconcile.yml", import.meta.url),
        "utf8",
    );
    const job = workflow.slice(workflow.indexOf("  auto-merge:"));
    assert.match(job, /needs: reconcile/);
    assert.match(job, /github\.event_name == 'workflow_run'/);
    assert.match(job, /decide-auto-merge-on-approval\.mjs/);
    assert.match(job, /--match-head-commit "\$HEAD_SHA"/);
    // 우회 머지 금지. merge queue 와 required check 를 그대로 탄다.
    assert.doesNotMatch(job, /--admin/);
});
