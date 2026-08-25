import assert from "node:assert/strict";
import test from "node:test";

import {
    assertReleasePullRequest,
    distributionRelationForCompareStatus,
    extractClosingIssueNumbers,
    isTargetCoveredByCompareStatus,
    selectMergedPullRequestsByAncestry,
    sortDistributionRuns,
    sourceShaForDistributionRun,
} from "./collect-release-scope-context.mjs";

test("accepts only develop -> main release PRs, merged or not", () => {
    assert.doesNotThrow(() =>
        assertReleasePullRequest({
            number: 1025,
            base: { ref: "main" },
            head: { ref: "develop", sha: "release-head" },
        }),
    );
    assert.throws(
        () =>
            assertReleasePullRequest({
                number: 1026,
                base: { ref: "develop" },
                head: { ref: "fix/940", sha: "abc" },
            }),
        /not main/,
    );
    assert.throws(
        () =>
            assertReleasePullRequest({
                number: 1027,
                base: { ref: "main" },
                head: { ref: "hotfix/urgent", sha: "abc" },
            }),
        /not develop/,
    );
});

test("extracts unique closing issue references only", () => {
    assert.deepEqual(
        extractClosingIssueNumbers("Closes #726, fixes: #713 and mentions #999. Resolves #726"),
        [726, 713],
    );
});

test("selects merged PRs by deployed and target ancestry instead of run time", () => {
    const pullRequests = [
        {
            number: 3,
            merged_at: "2026-08-04T03:00:00Z",
            merge_commit_sha: "future",
            base: { ref: "develop" },
        },
        {
            number: 1,
            merged_at: "2026-08-04T01:00:00Z",
            merge_commit_sha: "deployed",
            base: { ref: "develop" },
        },
        {
            number: 2,
            merged_at: "2026-08-04T02:00:00Z",
            merge_commit_sha: "pending-before-main-rerun",
            base: { ref: "develop" },
        },
        {
            number: 4,
            merged_at: "2026-08-04T02:30:00Z",
            merge_commit_sha: "main-pr",
            base: { ref: "main" },
        },
    ];
    const ancestors = new Set([
        "deployed:baseline",
        "deployed:target",
        "baseline:target",
        "pending-before-main-rerun:target",
    ]);
    const isAncestor = (ancestor, descendant) =>
        ancestor === descendant || ancestors.has(`${ancestor}:${descendant}`);

    assert.deepEqual(
        selectMergedPullRequestsByAncestry(
            pullRequests,
            "baseline",
            "target",
            isAncestor,
        ).map((pullRequest) => pullRequest.number),
        [2],
    );
});

test("resolves the deployed develop SHA for manual and main distributions", () => {
    assert.equal(
        sourceShaForDistributionRun({ head_branch: "develop", head_sha: "develop-run" }),
        "develop-run",
    );
    assert.equal(
        sourceShaForDistributionRun(
            { head_branch: "main", head_sha: "main-merge" },
            [
                {
                    merged_at: "2026-08-14T01:27:11Z",
                    base: { ref: "main" },
                    head: { ref: "develop", sha: "deployed-develop-head" },
                },
            ],
        ),
        "deployed-develop-head",
    );
    assert.equal(
        sourceShaForDistributionRun({ head_branch: "feat/726", head_sha: "feature-run" }),
        null,
    );
});

test("orders rerun distributions by their latest completion time", () => {
    const runs = [
        {
            id: 1,
            created_at: "2026-08-14T01:27:23Z",
            updated_at: "2026-08-14T05:09:32Z",
        },
        {
            id: 2,
            created_at: "2026-08-14T03:00:00Z",
            updated_at: "2026-08-14T03:10:00Z",
        },
    ];

    assert.deepEqual(sortDistributionRuns(runs).map((run) => run.id), [1, 2]);
});

test("recognizes compare statuses that include the target commit", () => {
    assert.equal(isTargetCoveredByCompareStatus("ahead"), true);
    assert.equal(isTargetCoveredByCompareStatus("identical"), true);
    assert.equal(isTargetCoveredByCompareStatus("behind"), false);
    assert.equal(isTargetCoveredByCompareStatus("diverged"), false);
});

test("maps commit comparison status to a distribution relation", () => {
    assert.equal(distributionRelationForCompareStatus("ahead"), "covered");
    assert.equal(distributionRelationForCompareStatus("identical"), "covered");
    assert.equal(distributionRelationForCompareStatus("behind"), "baseline");
    assert.equal(distributionRelationForCompareStatus("diverged"), null);
});
