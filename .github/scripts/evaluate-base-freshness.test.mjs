import assert from "node:assert/strict";
import test from "node:test";

import {
    branchFromRef,
    evaluateMergeGroupFreshness,
    evaluatePullRequestFreshness,
    freshnessFromComparisonStatus,
    publishFromEvent,
    STATUS_CONTEXT,
    statusDescription,
} from "./evaluate-base-freshness.mjs";

const BASE_A = "a".repeat(40);
const BASE_B = "b".repeat(40);
const HEAD_A = "c".repeat(40);
const HEAD_B = "d".repeat(40);

function pullRequest({ number = 17, baseRef = "develop", headSha = HEAD_A, state = "open" } = {}) {
    return {
        number,
        state,
        base: { ref: baseRef, repo: { full_name: "Afternote/Afternote-FE" } },
        head: { sha: headSha },
    };
}

function fakeApi({ pulls, baseShas, comparisonStatuses, listedPulls = [], pullErrors = new Set() }) {
    const statuses = [];
    const pullQueue = [...pulls];
    const baseQueue = [...baseShas];
    const comparisonQueue = [...comparisonStatuses];
    return {
        statuses,
        repository: "Afternote/Afternote-FE",
        async listPullRequests() {
            return listedPulls;
        },
        async getPullRequest(number) {
            if (pullErrors.has(number)) {
                throw new Error(`simulated PR #${number} API failure`);
            }
            assert.ok(pullQueue.length > 0, "unexpected pull request lookup");
            return pullQueue.shift();
        },
        async getBranchHead() {
            assert.ok(baseQueue.length > 0, "unexpected branch lookup");
            return baseQueue.shift();
        },
        async compare() {
            assert.ok(comparisonQueue.length > 0, "unexpected comparison lookup");
            return { status: comparisonQueue.shift() };
        },
        async setStatus(sha, status) {
            statuses.push({ sha, ...status });
        },
    };
}

test("base refs and GitHub comparison states are interpreted fail closed", () => {
    assert.equal(branchFromRef("refs/heads/develop"), "develop");
    assert.equal(branchFromRef("refs/heads/feat/stack"), "feat/stack");
    assert.throws(() => branchFromRef("refs/tags/v1"), /expected a branch ref/);
    assert.equal(freshnessFromComparisonStatus("ahead"), true);
    assert.equal(freshnessFromComparisonStatus("identical"), true);
    assert.equal(freshnessFromComparisonStatus("behind"), false);
    assert.equal(freshnessFromComparisonStatus("diverged"), false);
    assert.throws(() => freshnessFromComparisonStatus("unknown"), /unexpected GitHub comparison status/);
});

test("a PR head containing the live base receives pending then success", async () => {
    const api = fakeApi({
        pulls: [pullRequest(), pullRequest(), pullRequest()],
        baseShas: [BASE_A, BASE_A, BASE_A],
        comparisonStatuses: ["ahead"],
    });

    const result = await evaluatePullRequestFreshness(api, {
        number: 17,
        repository: "Afternote/Afternote-FE",
        targetUrl: "https://github.example/run/1",
        expectedHeadSha: HEAD_A,
    });

    assert.equal(result.kind, "fresh");
    assert.deepEqual(api.statuses.map(({ state }) => state), ["pending", "success"]);
    assert.equal(api.statuses[1].sha, HEAD_A);
    assert.match(api.statuses[1].description, /Up to date with develop@aaaaaaaaaaaa/);
});

test("a diverged PR is a real failing status instead of a stale log message", async () => {
    const api = fakeApi({
        pulls: [pullRequest(), pullRequest(), pullRequest()],
        baseShas: [BASE_A, BASE_A, BASE_A],
        comparisonStatuses: ["diverged"],
    });

    const result = await evaluatePullRequestFreshness(api, {
        number: 17,
        repository: "Afternote/Afternote-FE",
        targetUrl: "https://github.example/run/2",
    });

    assert.equal(result.kind, "stale");
    assert.deepEqual(api.statuses.map(({ state }) => state), ["pending", "failure"]);
    assert.match(api.statuses[1].description, /merge or rebase/);
});

test("an obsolete workflow run cannot overwrite the current PR head", async () => {
    const api = fakeApi({
        pulls: [pullRequest({ headSha: HEAD_B })],
        baseShas: [],
        comparisonStatuses: [],
    });

    const result = await evaluatePullRequestFreshness(api, {
        number: 17,
        repository: "Afternote/Afternote-FE",
        targetUrl: "https://github.example/run/3",
        expectedHeadSha: HEAD_A,
    });

    assert.equal(result.kind, "obsolete");
    assert.deepEqual(api.statuses, []);
});

test("base movement during comparison retries against the new live base", async () => {
    const api = fakeApi({
        pulls: [pullRequest(), pullRequest(), pullRequest(), pullRequest(), pullRequest()],
        baseShas: [BASE_A, BASE_B, BASE_B, BASE_B, BASE_B],
        comparisonStatuses: ["ahead", "diverged"],
    });

    const result = await evaluatePullRequestFreshness(api, {
        number: 17,
        repository: "Afternote/Afternote-FE",
        targetUrl: "https://github.example/run/4",
    });

    assert.equal(result.kind, "stale");
    assert.deepEqual(api.statuses.map(({ state }) => state), ["pending", "pending", "failure"]);
    assert.match(api.statuses.at(-1).description, /bbbbbbbbbbbb/);
});

test("base movement immediately after a final write replaces the obsolete success", async () => {
    const api = fakeApi({
        pulls: [
            pullRequest(),
            pullRequest(),
            pullRequest(),
            pullRequest(),
            pullRequest(),
            pullRequest(),
        ],
        baseShas: [BASE_A, BASE_A, BASE_B, BASE_B, BASE_B, BASE_B],
        comparisonStatuses: ["ahead", "diverged"],
    });

    const result = await evaluatePullRequestFreshness(api, {
        number: 17,
        repository: "Afternote/Afternote-FE",
        targetUrl: "https://github.example/run/5",
    });

    assert.equal(result.kind, "stale");
    assert.deepEqual(
        api.statuses.map(({ state }) => state),
        ["pending", "success", "pending", "failure"],
    );
    assert.match(api.statuses.at(-1).description, /bbbbbbbbbbbb/);
});

test("every coalesced publisher event sweeps all open pull requests", async () => {
    const api = fakeApi({
        listedPulls: [pullRequest({ number: 17 }), pullRequest({ number: 18, headSha: HEAD_B })],
        pulls: [
            pullRequest({ number: 17 }),
            pullRequest({ number: 17 }),
            pullRequest({ number: 17 }),
            pullRequest({ number: 18, headSha: HEAD_B }),
            pullRequest({ number: 18, headSha: HEAD_B }),
            pullRequest({ number: 18, headSha: HEAD_B }),
        ],
        baseShas: [BASE_A, BASE_A, BASE_A, BASE_B, BASE_B, BASE_B],
        comparisonStatuses: ["ahead", "diverged"],
    });

    const results = await publishFromEvent(
        api,
        {
            workflow_run: {
                name: "Base Freshness Probe",
                event: "pull_request",
                pull_requests: [{ number: 17 }],
            },
        },
        "workflow_run",
        "https://github.example/run/6",
    );

    assert.deepEqual(
        results.map(({ number, kind }) => ({ number, kind })),
        [
            { number: 17, kind: "fresh" },
            { number: 18, kind: "stale" },
        ],
    );
    assert.deepEqual(
        api.statuses.filter(({ state }) => state !== "pending").map(({ sha, state }) => ({ sha, state })),
        [
            { sha: HEAD_A, state: "success" },
            { sha: HEAD_B, state: "failure" },
        ],
    );
});

test("one pull request API failure does not block later freshness updates", async () => {
    const api = fakeApi({
        listedPulls: [pullRequest({ number: 17 }), pullRequest({ number: 18, headSha: HEAD_B })],
        pullErrors: new Set([17]),
        pulls: [
            pullRequest({ number: 18, headSha: HEAD_B }),
            pullRequest({ number: 18, headSha: HEAD_B }),
            pullRequest({ number: 18, headSha: HEAD_B }),
        ],
        baseShas: [BASE_B, BASE_B, BASE_B],
        comparisonStatuses: ["diverged"],
    });

    await assert.rejects(
        publishFromEvent(
            api,
            { workflow_run: { name: "Base Freshness Probe", event: "pull_request" } },
            "workflow_run",
            "https://github.example/run/7",
        ),
        /1 of 2 pull request freshness evaluations failed/,
    );
    assert.deepEqual(
        api.statuses.map(({ sha, state }) => ({ sha, state })),
        [
            { sha: HEAD_B, state: "pending" },
            { sha: HEAD_B, state: "failure" },
        ],
    );
});

test("merge groups must contain the current base and retry when that base moves", async () => {
    const api = fakeApi({
        pulls: [],
        baseShas: [BASE_A, BASE_B, BASE_B, BASE_B],
        comparisonStatuses: ["ahead", "ahead"],
    });

    const result = await evaluateMergeGroupFreshness(api, {
        base_ref: "refs/heads/develop",
        head_sha: HEAD_A,
    });

    assert.deepEqual(result, { baseRef: "develop", baseSha: BASE_B, headSha: HEAD_A });
    const staleApi = fakeApi({
        pulls: [],
        baseShas: [BASE_A, BASE_A],
        comparisonStatuses: ["behind"],
    });
    await assert.rejects(
        evaluateMergeGroupFreshness(staleApi, {
            base_ref: "refs/heads/develop",
            head_sha: HEAD_A,
        }),
        /does not contain current develop/,
    );
});

test("the required status context and descriptions stay stable", () => {
    assert.equal(STATUS_CONTEXT, "Base Freshness");
    assert.equal(
        statusDescription({ baseRef: "develop", baseSha: BASE_A, fresh: false }),
        "Missing develop@aaaaaaaaaaaa; merge or rebase the current base",
    );
});
