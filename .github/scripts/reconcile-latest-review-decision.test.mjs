import assert from "node:assert/strict";
import test from "node:test";

import {
    applyDismissalPlan,
    buildDismissalPlan,
    classifyRepositoryPermission,
    parseTargetPullNumber,
    reconcileLatestReviewDecisions,
    selectLatestDecisiveReviewsByReviewer,
} from "./reconcile-latest-review-decision.mjs";

function review({
    id,
    reviewer,
    state,
    submittedAt,
}) {
    return {
        id,
        state,
        submitted_at: submittedAt,
        user: { login: reviewer },
    };
}

const changesByAlice = review({
    id: 10,
    reviewer: "Alice",
    state: "CHANGES_REQUESTED",
    submittedAt: "2026-08-26T01:00:00Z",
});
const approvalByBob = review({
    id: 20,
    reviewer: "Bob",
    state: "APPROVED",
    submittedAt: "2026-08-26T02:00:00Z",
});

test("latest decisive review is selected separately for each reviewer", () => {
    const reviews = [
        changesByAlice,
        review({
            id: 11,
            reviewer: "alice",
            state: "COMMENTED",
            submittedAt: "2026-08-26T02:30:00Z",
        }),
        review({
            id: 12,
            reviewer: "ALICE",
            state: "APPROVED",
            submittedAt: "2026-08-26T03:00:00Z",
        }),
        approvalByBob,
    ];

    assert.deepEqual(
        selectLatestDecisiveReviewsByReviewer(reviews).map(({ id, reviewer, state }) => ({
            id,
            reviewer,
            state,
        })),
        [
            { id: 20, reviewer: "bob", state: "APPROVED" },
            { id: 12, reviewer: "alice", state: "APPROVED" },
        ],
    );
});

test("a later write-access approval dismisses another reviewer's blocking request", () => {
    const plan = buildDismissalPlan(
        [changesByAlice, approvalByBob],
        new Map([["alice", "write"], ["bob", "maintain"]]),
    );

    assert.equal(plan.status, "dismiss");
    assert.equal(plan.latestReview.id, 20);
    assert.deepEqual(plan.blockingReviews.map(({ id }) => id), [10]);
});

test("one later approval dismisses every older write-access blocking request", async () => {
    const changesByCarol = review({
        id: 15,
        reviewer: "Carol",
        state: "CHANGES_REQUESTED",
        submittedAt: "2026-08-26T01:30:00Z",
    });
    const plan = {
        pullNumber: 914,
        ...buildDismissalPlan(
            [changesByAlice, changesByCarol, approvalByBob],
            new Map([["alice", "write"], ["bob", "maintain"], ["carol", "admin"]]),
        ),
    };
    const calls = [];
    const client = {
        async request(path) {
            calls.push(path);
            return { state: "DISMISSED" };
        },
    };

    const dismissed = await applyDismissalPlan(client, "Afternote/Afternote-FE", plan);

    assert.deepEqual(dismissed.map(({ id }) => id), [10, 15]);
    assert.deepEqual(calls, [
        "/repos/Afternote/Afternote-FE/pulls/914/reviews/10/dismissals",
        "/repos/Afternote/Afternote-FE/pulls/914/reviews/15/dismissals",
    ]);
});

test("a later change request still blocks after an earlier approval", () => {
    const plan = buildDismissalPlan(
        [approvalByBob, review({
            id: 30,
            reviewer: "Alice",
            state: "CHANGES_REQUESTED",
            submittedAt: "2026-08-26T03:00:00Z",
        })],
        new Map([["alice", "admin"], ["bob", "write"]]),
    );

    assert.equal(plan.status, "latest-changes-requested");
    assert.deepEqual(plan.blockingReviews, []);
});

test("a reviewer's own later approval supersedes their earlier change request", () => {
    const plan = buildDismissalPlan(
        [
            changesByAlice,
            review({
                id: 15,
                reviewer: "Alice",
                state: "APPROVED",
                submittedAt: "2026-08-26T01:30:00Z",
            }),
            review({
                id: 12,
                reviewer: "Carol",
                state: "CHANGES_REQUESTED",
                submittedAt: "2026-08-26T01:15:00Z",
            }),
        ],
        new Map([["alice", "write"], ["carol", "write"]]),
    );

    assert.equal(plan.status, "dismiss");
    assert.deepEqual(plan.blockingReviews.map(({ id }) => id), [12]);
});

test("read-only approval never overrides a write-access change request", () => {
    const plan = buildDismissalPlan(
        [changesByAlice, approvalByBob],
        new Map([["alice", "write"], ["bob", "read"]]),
    );

    assert.equal(plan.status, "latest-changes-requested");
    assert.equal(plan.latestReview.id, 10);
});

test("same-second reviews use the larger review id as the later decision", () => {
    const submittedAt = "2026-08-26T01:00:00Z";
    const plan = buildDismissalPlan(
        [
            review({ id: 100, reviewer: "Alice", state: "CHANGES_REQUESTED", submittedAt }),
            review({ id: 101, reviewer: "Bob", state: "APPROVED", submittedAt }),
        ],
        new Map([["alice", "write"], ["bob", "write"]]),
    );

    assert.equal(plan.status, "dismiss");
    assert.equal(plan.latestReview.id, 101);
});

test("unknown or missing permissions fail closed", () => {
    assert.equal(classifyRepositoryPermission("admin"), "write");
    assert.equal(classifyRepositoryPermission("triage"), "read");
    assert.throws(() => classifyRepositoryPermission("mystery"), /알 수 없는/);
    assert.throws(
        () => buildDismissalPlan([approvalByBob], new Map()),
        /repository permission 이 없습니다/,
    );
});

test("only valid optional target pull numbers are accepted", () => {
    assert.equal(parseTargetPullNumber(undefined), null);
    assert.equal(parseTargetPullNumber(""), null);
    assert.equal(parseTargetPullNumber(" 914 "), 914);
    assert.throws(() => parseTargetPullNumber("0"), /양의 정수/);
    assert.throws(() => parseTargetPullNumber("abc"), /양의 정수/);
});

test("dismissal calls the protected review endpoint and verifies the response", async () => {
    const calls = [];
    const client = {
        async request(path, options) {
            calls.push({ path, options });
            return { state: "DISMISSED" };
        },
    };
    const plan = {
        pullNumber: 914,
        status: "dismiss",
        latestReview: {
            id: 20,
            reviewer: "bob",
            state: "APPROVED",
            submittedAt: "2026-08-26T02:00:00Z",
            submittedTimestamp: Date.parse("2026-08-26T02:00:00Z"),
        },
        blockingReviews: [{
            id: 10,
            reviewer: "alice",
            state: "CHANGES_REQUESTED",
            submittedAt: "2026-08-26T01:00:00Z",
            submittedTimestamp: Date.parse("2026-08-26T01:00:00Z"),
        }],
    };

    const dismissed = await applyDismissalPlan(client, "Afternote/Afternote-FE", plan);

    assert.deepEqual(dismissed.map(({ id }) => id), [10]);
    assert.equal(
        calls[0].path,
        "/repos/Afternote/Afternote-FE/pulls/914/reviews/10/dismissals",
    );
    assert.equal(calls[0].options.method, "PUT");
    assert.equal(calls[0].options.body.event, "DISMISS");
    assert.match(calls[0].options.body.message, /@bob/);
    assert.match(calls[0].options.body.message, /review #20/);
});

test("an unexpected dismiss response fails instead of reporting success", async () => {
    const client = { async request() { return { state: "CHANGES_REQUESTED" }; } };
    await assert.rejects(
        applyDismissalPlan(client, "Afternote/Afternote-FE", {
            pullNumber: 914,
            status: "dismiss",
            latestReview: approvalByBob,
            blockingReviews: [changesByAlice],
        }),
        /DISMISSED가 아닙니다/,
    );
});

test("all pull requests are read and authorized before the first dismissal", async () => {
    const writes = [];
    const client = {
        async paginate(path) {
            if (path.includes("/pulls/1/reviews")) {
                return [changesByAlice, approvalByBob];
            }
            throw new Error("second PR review lookup failed");
        },
        async request(path, options = {}) {
            if (options.method === "PUT") {
                writes.push(path);
                return { state: "DISMISSED" };
            }
            return { permission: "write" };
        },
    };

    await assert.rejects(
        reconcileLatestReviewDecisions({
            client,
            repository: "Afternote/Afternote-FE",
            pullNumbers: [1, 2],
            logger: { log() {} },
        }),
        /second PR review lookup failed/,
    );
    assert.deepEqual(writes, []);
});
