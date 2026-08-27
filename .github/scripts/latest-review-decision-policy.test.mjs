import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const signal = await readFile(
    new URL("../workflows/latest-review-decision-event.yml", import.meta.url),
    "utf8",
);
const reconcile = await readFile(
    new URL("../workflows/latest-review-decision-reconcile.yml", import.meta.url),
    "utf8",
);
const readme = await readFile(new URL("../../README.md", import.meta.url), "utf8");

test("the review event workflow is an unprivileged signal only", () => {
    assert.match(signal, /^\s{2}pull_request_review:\n\s{4}types: \[submitted\]/m);
    assert.match(signal, /^permissions: \{\}$/m);
    assert.doesNotMatch(signal, /actions\/checkout@/);
    assert.doesNotMatch(signal, /GITHUB_TOKEN|GH_TOKEN|gh api|pull-requests:\s*write/);
});

test("the trusted workflow reconciles from the default branch with minimal write access", () => {
    assert.match(reconcile, /^\s{2}workflow_run:/m);
    assert.match(reconcile, /workflows: \[Latest Review Decision Event\]/);
    assert.match(reconcile, /^\s{2}workflow_dispatch:/m);
    assert.match(reconcile, /^permissions: \{\}$/m);
    assert.match(reconcile, /^\s{6}contents: read$/m);
    assert.match(reconcile, /^\s{6}pull-requests: write$/m);
    assert.match(reconcile, /ref: \$\{\{ github\.event\.repository\.default_branch \}\}/);
    assert.match(reconcile, /persist-credentials: false/);
    assert.match(reconcile, /reconcile-latest-review-decision\.mjs/);
    assert.doesNotMatch(reconcile, /github\.event\.pull_request\.head|refs\/pull/);
});

test("automatic reconciliation is serialized and historical PRs can be swept manually", () => {
    assert.match(reconcile, /cancel-in-progress: false/);
    assert.match(reconcile, /inputs\.pull_number/);
    assert.match(reconcile, /github\.event\.workflow_run\.pull_requests\[0\]\.number/);
    assert.match(reconcile, /github\.event\.workflow_run\.conclusion == 'success'/);
    assert.match(reconcile, /github\.event\.workflow_run\.event == 'pull_request_review'/);
});

test("the README documents the latest write-access review as the final decision", () => {
    assert.match(
        readme,
        /쓰기 권한이 있는 리뷰어별 최신 `APPROVED`·`CHANGES_REQUESTED` 가운데 PR 전체에서 가장 늦은 판정을 최종 판정으로 사용한다/,
    );
    assert.doesNotMatch(readme, /\/review-response/);
});
