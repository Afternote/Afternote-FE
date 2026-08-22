import assert from "node:assert/strict";
import test from "node:test";

import {
    assertMergedDevelopPullRequest,
    extractClosingIssueNumbers,
    isTargetCoveredByCompareStatus,
    selectMergedPullRequests,
} from "./collect-deployment-decision-context.mjs";

test("accepts only merged PRs targeting develop", () => {
    assert.doesNotThrow(() =>
        assertMergedDevelopPullRequest({
            number: 726,
            merged_at: "2026-08-04T01:00:00Z",
            merge_commit_sha: "abc",
            base: { ref: "develop" },
        }),
    );
    assert.throws(
        () =>
            assertMergedDevelopPullRequest({
                number: 726,
                merged_at: "2026-08-04T01:00:00Z",
                merge_commit_sha: "abc",
                base: { ref: "main" },
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

test("selects the cumulative merged PR window in merge order", () => {
    const pullRequests = [
        {
            number: 3,
            merged_at: "2026-08-04T03:00:00Z",
            base: { ref: "develop" },
        },
        {
            number: 1,
            merged_at: "2026-08-04T01:00:00Z",
            base: { ref: "develop" },
        },
        {
            number: 2,
            merged_at: "2026-08-04T02:00:00Z",
            base: { ref: "develop" },
        },
        {
            number: 4,
            merged_at: "2026-08-04T02:30:00Z",
            base: { ref: "main" },
        },
    ];

    assert.deepEqual(
        selectMergedPullRequests(
            pullRequests,
            "2026-08-04T01:30:00Z",
            "2026-08-04T03:00:00Z",
        ).map((pullRequest) => pullRequest.number),
        [2, 3],
    );
});

test("recognizes compare statuses that include the target commit", () => {
    assert.equal(isTargetCoveredByCompareStatus("ahead"), true);
    assert.equal(isTargetCoveredByCompareStatus("identical"), true);
    assert.equal(isTargetCoveredByCompareStatus("behind"), false);
    assert.equal(isTargetCoveredByCompareStatus("diverged"), false);
});
