import assert from "node:assert/strict";
import test from "node:test";

import {
    applyDismissalPlan,
    buildDismissalPlan,
    classifyRepositoryPermission,
    collectAuthorActions,
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

function commit({
    sha = "abc123",
    author = "Author",
    committedAt = "2026-08-26T01:30:00Z",
    parentCount = 1,
} = {}) {
    return {
        sha,
        author: { login: author },
        committer: { login: author },
        commit: {
            author: { date: committedAt },
            committer: { date: committedAt },
        },
        parents: Array.from({ length: parentCount }, (_, index) => ({ sha: `parent-${index}` })),
    };
}

function issueComment({
    id = 100,
    author = "Author",
    body = "/review-response\n요청한 변경 대신 현재 동작을 유지해야 하는 근거입니다.",
    createdAt = "2026-08-26T01:30:00Z",
} = {}) {
    return {
        id,
        body,
        created_at: createdAt,
        user: { login: author },
    };
}

function context(overrides = {}) {
    return {
        pullAuthor: "Author",
        commits: [],
        issueComments: [],
        reviewComments: [],
        ...overrides,
    };
}

const changesByAlice = review({
    id: 10,
    reviewer: "Alice",
    state: "CHANGES_REQUESTED",
    submittedAt: "2026-08-26T01:00:00Z",
});
const secondChangesByAlice = review({
    id: 15,
    reviewer: "Alice",
    state: "CHANGES_REQUESTED",
    submittedAt: "2026-08-26T02:00:00Z",
});
const approvalByBob = review({
    id: 20,
    reviewer: "Bob",
    state: "APPROVED",
    submittedAt: "2026-08-26T03:00:00Z",
});

function reviewedCommitPlan() {
    return buildDismissalPlan(
        [changesByAlice, secondChangesByAlice, approvalByBob],
        new Map([["alice", "write"], ["bob", "maintain"]]),
        context({ commits: [commit()] }),
    );
}

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
            submittedAt: "2026-08-26T03:30:00Z",
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

test("a later approval cannot dismiss an untouched change request", () => {
    const plan = buildDismissalPlan(
        [changesByAlice, approvalByBob],
        new Map([["alice", "write"], ["bob", "maintain"]]),
        context(),
    );

    assert.equal(plan.status, "awaiting-reviewed-author-response");
    assert.deepEqual(plan.blockingReviews.map(({ id }) => id), [10]);
    assert.deepEqual(plan.pendingReviewers, ["alice"]);
});

test("a non-merge author commit plus blocker and approver re-reviews permits dismissal", () => {
    const plan = reviewedCommitPlan();

    assert.equal(plan.status, "dismiss");
    assert.equal(plan.latestReview.id, 20);
    assert.deepEqual(plan.blockingReviews.map(({ id }) => id), [15]);
    assert.equal(plan.responseEvidence[0].priorBlockingReview.id, 10);
    assert.equal(plan.responseEvidence[0].action.kind, "commit");
});

test("a substantive author review-response comment can replace a code change", () => {
    const plan = buildDismissalPlan(
        [changesByAlice, secondChangesByAlice, approvalByBob],
        new Map([["alice", "write"], ["bob", "write"]]),
        context({ issueComments: [issueComment()] }),
    );

    assert.equal(plan.status, "dismiss");
    assert.equal(plan.responseEvidence[0].action.kind, "issue-comment");
});

test("an author reply in the blocking review thread counts only for that reviewer", () => {
    const root = {
        id: 100,
        pull_request_review_id: 10,
        created_at: "2026-08-26T00:59:00Z",
        user: { login: "Alice" },
    };
    const reply = {
        id: 101,
        in_reply_to_id: 100,
        created_at: "2026-08-26T01:30:00Z",
        user: { login: "Author" },
    };
    const plan = buildDismissalPlan(
        [changesByAlice, secondChangesByAlice, approvalByBob],
        new Map([["alice", "write"], ["bob", "write"]]),
        context({ reviewComments: [root, reply] }),
    );

    assert.equal(plan.status, "dismiss");
    assert.equal(plan.responseEvidence[0].action.kind, "review-reply");
    assert.equal(plan.responseEvidence[0].action.reviewer, "alice");
});

test("generic comments, empty commands, other users, and merge commits are not author responses", () => {
    const reviews = [changesByAlice, secondChangesByAlice, approvalByBob];
    const actionInputs = {
        commits: [commit({ parentCount: 2 }), commit({ author: "SomeoneElse" })],
        issueComments: [
            issueComment({ id: 100, body: "반영하지 않겠습니다." }),
            issueComment({ id: 101, body: "/review-response" }),
            issueComment({ id: 102, author: "SomeoneElse" }),
        ],
    };
    const actions = collectAuthorActions({
        pullAuthor: "Author",
        reviews,
        reviewComments: [],
        ...actionInputs,
    });
    const plan = buildDismissalPlan(
        reviews,
        new Map([["alice", "write"], ["bob", "write"]]),
        context(actionInputs),
    );

    assert.deepEqual(actions, []);
    assert.equal(plan.status, "awaiting-reviewed-author-response");
});

test("the blocker must re-review after the author response", () => {
    const plan = buildDismissalPlan(
        [changesByAlice, approvalByBob],
        new Map([["alice", "write"], ["bob", "write"]]),
        context({ commits: [commit({ committedAt: "2026-08-26T02:00:00Z" })] }),
    );

    assert.equal(plan.status, "awaiting-reviewed-author-response");
    assert.deepEqual(plan.pendingReviewers, ["alice"]);
});

test("the approver must review after the author response", () => {
    const plan = buildDismissalPlan(
        [changesByAlice, review({
            id: 20,
            reviewer: "Bob",
            state: "APPROVED",
            submittedAt: "2026-08-26T01:20:00Z",
        })],
        new Map([["alice", "write"], ["bob", "write"]]),
        context({ commits: [commit({ committedAt: "2026-08-26T01:30:00Z" })] }),
    );

    assert.equal(plan.status, "awaiting-reviewed-author-response");
});

test("a fresh change request after a post-response approval starts a new blocked round", () => {
    const approvalByAlice = review({
        id: 13,
        reviewer: "Alice",
        state: "APPROVED",
        submittedAt: "2026-08-26T02:00:00Z",
    });
    const freshChangesByAlice = review({
        id: 15,
        reviewer: "Alice",
        state: "CHANGES_REQUESTED",
        submittedAt: "2026-08-26T02:30:00Z",
    });
    const plan = buildDismissalPlan(
        [changesByAlice, approvalByAlice, freshChangesByAlice, approvalByBob],
        new Map([["alice", "write"], ["bob", "write"]]),
        context({ commits: [commit()] }),
    );

    assert.equal(plan.status, "awaiting-reviewed-author-response");
    assert.deepEqual(plan.pendingReviewers, ["alice"]);
});

test("every active blocker must review a common author response", () => {
    const changesByCarol = review({
        id: 30,
        reviewer: "Carol",
        state: "CHANGES_REQUESTED",
        submittedAt: "2026-08-26T01:10:00Z",
    });
    const secondChangesByCarol = review({
        id: 31,
        reviewer: "Carol",
        state: "CHANGES_REQUESTED",
        submittedAt: "2026-08-26T02:10:00Z",
    });
    const plan = buildDismissalPlan(
        [
            changesByAlice,
            changesByCarol,
            secondChangesByAlice,
            secondChangesByCarol,
            approvalByBob,
        ],
        new Map([["alice", "write"], ["bob", "write"], ["carol", "write"]]),
        context({ commits: [commit()] }),
    );

    assert.equal(plan.status, "dismiss");
    assert.deepEqual(plan.blockingReviews.map(({ reviewer }) => reviewer), ["alice", "carol"]);
    assert.deepEqual(plan.responseEvidence.map(({ reviewer }) => reviewer), ["alice", "carol"]);
});

test("a thread reply to one blocker does not satisfy another blocker", () => {
    const changesByCarol = review({
        id: 30,
        reviewer: "Carol",
        state: "CHANGES_REQUESTED",
        submittedAt: "2026-08-26T01:10:00Z",
    });
    const secondChangesByCarol = review({
        id: 31,
        reviewer: "Carol",
        state: "CHANGES_REQUESTED",
        submittedAt: "2026-08-26T02:10:00Z",
    });
    const root = {
        id: 100,
        pull_request_review_id: 10,
        created_at: "2026-08-26T00:59:00Z",
        user: { login: "Alice" },
    };
    const reply = {
        id: 101,
        in_reply_to_id: 100,
        created_at: "2026-08-26T01:30:00Z",
        user: { login: "Author" },
    };
    const plan = buildDismissalPlan(
        [
            changesByAlice,
            changesByCarol,
            secondChangesByAlice,
            secondChangesByCarol,
            approvalByBob,
        ],
        new Map([["alice", "write"], ["bob", "write"], ["carol", "write"]]),
        context({ reviewComments: [root, reply] }),
    );

    assert.equal(plan.status, "awaiting-reviewed-author-response");
    assert.deepEqual(plan.pendingReviewers, ["carol"]);
});

test("a later change request still blocks after an earlier approval", () => {
    const plan = buildDismissalPlan(
        [approvalByBob, review({
            id: 30,
            reviewer: "Alice",
            state: "CHANGES_REQUESTED",
            submittedAt: "2026-08-26T04:00:00Z",
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
        ],
        new Map([["alice", "write"]]),
    );

    assert.equal(plan.status, "already-approved");
    assert.deepEqual(plan.blockingReviews, []);
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
    const submittedAt = "2026-08-26T03:00:00Z";
    const plan = buildDismissalPlan(
        [
            changesByAlice,
            secondChangesByAlice,
            review({ id: 100, reviewer: "Alice", state: "CHANGES_REQUESTED", submittedAt }),
            review({ id: 101, reviewer: "Bob", state: "APPROVED", submittedAt }),
        ],
        new Map([["alice", "write"], ["bob", "write"]]),
        context({ commits: [commit()] }),
    );

    assert.equal(plan.status, "awaiting-reviewed-author-response");
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

function executableDismissalPlan() {
    return { pullNumber: 914, ...reviewedCommitPlan() };
}

test("dismissal calls the protected review endpoint and explains the reviewed author response", async () => {
    const calls = [];
    const client = {
        async request(path, options) {
            calls.push({ path, options });
            return { state: "DISMISSED" };
        },
    };

    const dismissed = await applyDismissalPlan(
        client,
        "Afternote/Afternote-FE",
        executableDismissalPlan(),
    );

    assert.deepEqual(dismissed.map(({ id }) => id), [15]);
    assert.equal(
        calls[0].path,
        "/repos/Afternote/Afternote-FE/pulls/914/reviews/15/dismissals",
    );
    assert.equal(calls[0].options.method, "PUT");
    assert.equal(calls[0].options.body.event, "DISMISS");
    assert.match(calls[0].options.body.message, /작성자 @author/);
    assert.match(calls[0].options.body.message, /@alice와 @bob/);
    assert.match(calls[0].options.body.message, /review #20/);
});

test("dismissal fails closed when reviewed response evidence is missing", async () => {
    const plan = executableDismissalPlan();
    plan.responseEvidence = [];
    await assert.rejects(
        applyDismissalPlan(
            { async request() { return { state: "DISMISSED" }; } },
            "Afternote/Afternote-FE",
            plan,
        ),
        /검토된 작성자 대응 근거가 없습니다/,
    );
});

test("an unexpected dismiss response fails instead of reporting success", async () => {
    const client = { async request() { return { state: "CHANGES_REQUESTED" }; } };
    await assert.rejects(
        applyDismissalPlan(client, "Afternote/Afternote-FE", executableDismissalPlan()),
        /DISMISSED가 아닙니다/,
    );
});

test("all pull requests are read and authorized before the first dismissal", async () => {
    const writes = [];
    const client = {
        async paginate(path) {
            if (path.includes("/pulls/1/reviews")) {
                return [changesByAlice, secondChangesByAlice, approvalByBob];
            }
            if (path.includes("/pulls/1/commits")) {
                return [commit()];
            }
            if (path.includes("/issues/1/comments") || path.includes("/pulls/1/comments")) {
                return [];
            }
            if (path.includes("/pulls/2/reviews")) {
                throw new Error("second PR review lookup failed");
            }
            return [];
        },
        async request(path, options = {}) {
            if (options.method === "PUT") {
                writes.push(path);
                return { state: "DISMISSED" };
            }
            if (path.endsWith("/pulls/1") || path.endsWith("/pulls/2")) {
                return { user: { login: "Author" } };
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
