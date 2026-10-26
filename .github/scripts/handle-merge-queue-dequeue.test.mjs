import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

import {
    NO_ACTION_REASONS,
    collectFailedJobs,
    countRequeuesForHead,
    decide,
    dispatchRequeue,
    provenInfrastructureJobNames,
    enqueueStacked,
    handleDequeue,
    renderMissingQueueTokenComment,
    requeueDispatched,
    REQUEUE_WORKFLOW_FILE,
    isStackBottom,
    isStackMember,
    lastEnqueuedAt,
    manualEnqueueCommand,
    queueBranchPrefix,
    renderFailureComment,
    renderGiveUpComment,
    renderRequeueComment,
    requeueMarker,
} from "./handle-merge-queue-dequeue.mjs";

const REPO = "Afternote/Afternote-FE";
const HEAD = "0123456789abcdef0123456789abcdef01234567";
const silent = { log() {} };
const immediateSleep = async () => {};

// ---- 판정표 (#1892) ----

test("실패 job 이 있으면 reason 과 무관하게 comment-failure — 재투입하지 않는다", () => {
    const failedJobs = [{ jobName: "Run Unit Tests", conclusion: "failure" }];
    for (const reason of ["CI_FAILURE", "UNKNOWN_REMOVAL_REASON", "MANUAL", "ROLL_BACK"]) {
        assert.equal(decide({ reason, failedJobs, requeueCount: 0 }), "comment-failure", reason);
    }
});

test("MANUAL·ALREADY_MERGED·MERGE(D) 는 표기와 무관하게 무동작", () => {
    for (const reason of [...NO_ACTION_REASONS, "merged", "manual"]) {
        assert.equal(decide({ reason, failedJobs: [], requeueCount: 0 }), "none", reason);
    }
    assert.equal(decide({ reason: "failed_checks", failedJobs: [], requeueCount: 0 }), "requeue");
});

test("실패 job 없는 방출은 같은 head 에서 한 번만 재투입하고, 두 번째는 comment-give-up", () => {
    assert.equal(decide({ reason: "UNKNOWN_REMOVAL_REASON", failedJobs: [], requeueCount: 0 }), "requeue");
    assert.equal(decide({ reason: "UNKNOWN_REMOVAL_REASON", failedJobs: [], requeueCount: 1 }), "comment-give-up");
    assert.equal(decide({ reason: "ROLL_BACK", failedJobs: [], requeueCount: 3 }), "comment-give-up");
});

test("실패 job 이 전부 증명된 인프라 실패면 같은 head 에서 한 번 재투입하고, 두 번째는 링크만 남긴다", () => {
    const infra = [{ jobName: "Pixel 2 API 30 androidTest", conclusion: "failure", infrastructure: true }];
    assert.equal(decide({ reason: "CI_FAILURE", failedJobs: infra, requeueCount: 0 }), "requeue");
    assert.equal(decide({ reason: "CI_FAILURE", failedJobs: infra, requeueCount: 1 }), "comment-failure");
    assert.equal(decide({ reason: "MANUAL", failedJobs: infra, requeueCount: 0 }), "none");
    const mixed = [...infra, { jobName: "Run Unit Tests", conclusion: "failure", infrastructure: false }];
    assert.equal(decide({ reason: "CI_FAILURE", failedJobs: mixed, requeueCount: 0 }), "comment-failure");
});

test("재시도 마커는 같은 run·attempt 이름만 인정한다 — 다른 run·만료·모르는 device 는 증명이 아니다", () => {
    const run = { id: 37178469871, run_attempt: 1 };
    assert.deepEqual([...provenInfrastructureJobNames(run, [{ name: "android-managed-device-retry-api30-37178469871-1" }])], ["Pixel 2 API 30 androidTest"]);
    assert.equal(provenInfrastructureJobNames(run, [{ name: "android-managed-device-retry-api30-37178469871-2" }]).size, 0);
    assert.equal(provenInfrastructureJobNames(run, [{ name: "android-managed-device-retry-api30-1-1" }]).size, 0);
    assert.equal(provenInfrastructureJobNames(run, [{ name: "android-managed-device-retry-api30-37178469871-1", expired: true }]).size, 0);
    assert.equal(provenInfrastructureJobNames(run, [{ name: "android-managed-device-retry-api26-37178469871-1" }]).size, 0);
});

// ---- 실패 job 수집 ----

test("이 PR 의 큐 브랜치 run 에서 failure·timed_out job 만 모은다 — 다른 PR 의 run, cancelled job 은 뺀다", () => {
    const runs = [
        { id: 1, name: "Unit Test", html_url: "https://x/runs/1", head_branch: queueBranchPrefix("develop", 1509) + "aaaa" },
        { id: 2, name: "Screenshot", html_url: "https://x/runs/2", head_branch: queueBranchPrefix("develop", 1509) + "aaaa" },
        { id: 3, name: "Unit Test", html_url: "https://x/runs/3", head_branch: queueBranchPrefix("develop", 1510) + "aaaa" },
        { id: 4, name: "Lint", html_url: "https://x/runs/4", head_branch: "refs/pull/1509/merge" },
    ];
    const jobsByRunId = new Map([
        [1, [
            { name: "Run Unit Tests", conclusion: "failure", html_url: "https://x/jobs/11" },
            { name: "Kover", conclusion: "cancelled", html_url: "https://x/jobs/12" },
        ]],
        [2, [{ name: "Validate", conclusion: "timed_out", html_url: "https://x/jobs/21" }]],
        [3, [{ name: "Run Unit Tests", conclusion: "failure", html_url: "https://x/jobs/31" }]],
        [4, [{ name: "ktlint", conclusion: "failure", html_url: "https://x/jobs/41" }]],
    ]);
    const failed = collectFailedJobs({ runs, jobsByRunId, baseRef: "develop", number: 1509 });
    assert.deepEqual(failed.map((job) => job.jobUrl), ["https://x/jobs/11", "https://x/jobs/21"]);
    assert.equal(failed[0].runName, "Unit Test");
    assert.equal(failed[1].conclusion, "timed_out");
});

test("마지막 투입 이전의 run 은 이번 방출의 근거가 아니다 — 큐 브랜치 이름은 base SHA 라 옛 실패가 같은 접두어로 잡힌다", () => {
    const branch = queueBranchPrefix("develop", 1582) + "aaaa";
    const runs = [
        { id: 1, name: "Android Managed Device Test", html_url: "https://x/runs/1", head_branch: branch, created_at: "2026-09-04T08:06:21Z" },
        { id: 2, name: "Android Managed Device Test", html_url: "https://x/runs/2", head_branch: branch, created_at: "2026-09-04T12:00:00Z" },
    ];
    const jobsByRunId = new Map([
        [1, [{ name: "api34", conclusion: "failure", html_url: "https://x/jobs/1" }]],
        [2, [{ name: "api34", conclusion: "success", html_url: "https://x/jobs/2" }]],
    ]);
    assert.equal(collectFailedJobs({ runs, jobsByRunId, baseRef: "develop", number: 1582 }).length, 1);
    assert.equal(collectFailedJobs({ runs, jobsByRunId, baseRef: "develop", number: 1582, since: "2026-09-04T11:00:00Z" }).length, 0);
    assert.equal(lastEnqueuedAt({ timelineItems: { nodes: [{ createdAt: "2026-09-04T08:06:00Z" }, { createdAt: "2026-09-04T11:00:00Z" }] } }), "2026-09-04T11:00:00Z");
    assert.equal(lastEnqueuedAt({ timelineItems: { nodes: [] } }), undefined);
});

// ---- 재투입 횟수 = 같은 head 의 마커 코멘트 수 ----

test("재투입 횟수는 같은 head 의 마커 코멘트만 센다 — 새 커밋이 올라오면 0 부터다", () => {
    const comments = [
        { body: renderRequeueComment({ reason: "UNKNOWN_REMOVAL_REASON", headSha: HEAD }) },
        { body: "그냥 코멘트" },
        { body: renderRequeueComment({ reason: "ROLL_BACK", headSha: "f".repeat(40) }) },
    ];
    assert.equal(countRequeuesForHead(comments, HEAD), 1);
    assert.equal(countRequeuesForHead(comments, "f".repeat(40)), 1);
    assert.equal(countRequeuesForHead(comments, "0".repeat(40)), 0);
    assert.ok(renderRequeueComment({ reason: "X", headSha: HEAD }).startsWith(requeueMarker(HEAD)));
});

test("실패 코멘트는 job 링크와 재투입 명령을 담고, 재투입했다고 말하지 않는다", () => {
    const body = renderFailureComment({
        reason: "CI_FAILURE",
        headSha: HEAD,
        repository: REPO,
        number: 1509,
        failedJobs: [{ runName: "Unit Test", jobName: "Run Unit Tests", jobUrl: "https://x/jobs/11", conclusion: "failure" }],
    });
    assert.match(body, /\[Unit Test \/ Run Unit Tests\]\(https:\/\/x\/jobs\/11\)/);
    assert.match(body, /gh pr merge 1509 --repo Afternote\/Afternote-FE/);
    assert.match(body, /재투입하지 않았다/);
    assert.ok(!body.includes(requeueMarker(HEAD)));
});

test("스택 PR 의 실패 코멘트는 gh pr merge 가 아니라 gh stack merge 를 안내한다 — gh pr merge 는 스택 PR 을 거부한다", () => {
    assert.equal(manualEnqueueCommand({ repository: REPO, number: 2123 }), "gh pr merge 2123 --repo Afternote/Afternote-FE");
    assert.equal(manualEnqueueCommand({ repository: REPO, number: 2123, stack: true }), "gh stack merge 2123 --yes", "gh stack 에는 --repo 가 없다");
    const body = renderFailureComment({
        reason: "CI_FAILURE",
        headSha: HEAD,
        repository: REPO,
        number: 2123,
        stack: true,
        failedJobs: [{ runName: "Unit Test", jobName: "Run Unit Tests", jobUrl: "https://x/jobs/11", conclusion: "failure" }],
    });
    assert.match(body, /gh stack merge 2123 --yes/);
    assert.ok(!body.includes("gh pr merge"));
});

test("재투입 중단 코멘트는 스택 여부에 맞는 수동 명령을 덧붙인다 — 기존 확인 안내를 보존한다", () => {
    for (const [stack, number, command] of [
        [true, 2123, "gh stack merge 2123 --yes"],
        [false, 1509, "gh pr merge 1509 --repo Afternote/Afternote-FE"],
    ]) {
        const body = renderGiveUpComment({ reason: "MERGE_CONFLICT", headSha: HEAD, repository: REPO, number, stack });
        assert.match(body, /같은 head 두 번째, 재투입하지 않는다/);
        assert.ok(body.includes("`gh run list --event merge_group` 으로 merge group run 을 확인하고 수동으로 투입한다."));
        assert.ok(body.split("\n").at(-1).includes(command));
        assert.ok(!body.includes(requeueMarker(HEAD)));
    }
});

// ---- 스택 판정 (2026-09-27 #2177) ----

test("stack 이 null·없음이면 비스택, 객체면 스택이다", () => {
    assert.equal(isStackMember({ stack: null }), false);
    assert.equal(isStackMember({}), false);
    assert.equal(isStackMember({ stack: { number: 2125 } }), true);
});

test("스택 기준 브랜치와 PR base 가 같을 때만 밑단이다 — 위쪽 PR 은 아래 PR 의 판정을 덮을 수 있다", () => {
    assert.equal(isStackBottom({ stack: null, baseRefName: "develop" }), false);
    assert.equal(isStackBottom({}), false);
    assert.equal(isStackBottom({ baseRefName: "develop", stack: { number: 2125, baseRefName: "develop" } }), true);
    assert.equal(isStackBottom({ baseRefName: "refactor/1429-setting-repositories", stack: { number: 2125, baseRefName: "develop" } }), false);
});

// ---- handleDequeue 흐름: live 상태가 정본, 쓰기 순서 ----

function fakeApi({
    live,
    liveAfter = live,
    runs = [],
    jobs = {},
    artifacts = {},
    comments = [],
    mergeAsync = {
        status: "pending",
        details: {
            message: "Merge request enqueued.",
            uuid: "u-1",
            merge_method: "default",
            merge_action: "merge_queue",
            expected_head_sha: HEAD,
        },
    },
    mergeAsyncPolls = [{ status: "enqueued", details: { message: "Pull request added to merge queue." } }],
}) {
    const calls = [];
    let liveReads = 0;
    let pollReads = 0;
    const respond = (response) => {
        if (response instanceof Error) throw response;
        return response;
    };
    const api = async (apiPath, options = {}) => {
        calls.push({ apiPath, method: options.method ?? "GET", body: options.body });
        if (apiPath === "/graphql") {
            const query = options.body.query;
            if (query.includes("enqueuePullRequest")) {
                return { data: { enqueuePullRequest: { mergeQueueEntry: { state: "QUEUED", position: 1 } } } };
            }
            return { data: { repository: { pullRequest: respond(liveReads++ === 0 ? live : liveAfter) } } };
        }
        if (/\/pulls\/\d+\/merge-async$/.test(apiPath) && options.method === "PUT") return respond(mergeAsync);
        if (/\/pulls\/\d+\/merge-async\/u-1$/.test(apiPath) && (options.method ?? "GET") === "GET") {
            return respond(mergeAsyncPolls[Math.min(pollReads++, mergeAsyncPolls.length - 1)]);
        }
        if (apiPath.startsWith(`/repos/${REPO}/actions/runs?`)) return { workflow_runs: runs };
        const artifactsMatch = /\/actions\/runs\/(\d+)\/artifacts/.exec(apiPath);
        if (artifactsMatch) return { artifacts: artifacts[artifactsMatch[1]] ?? [] };
        const jobsMatch = /\/actions\/runs\/(\d+)\/jobs/.exec(apiPath);
        if (jobsMatch) return { jobs: jobs[jobsMatch[1]] ?? [] };
        if (apiPath.includes("/comments") && (options.method ?? "GET") === "GET") return comments;
        if (apiPath.includes("/comments") && options.method === "POST") return { id: 1 };
        if (apiPath.endsWith("/dispatches") && options.method === "POST") return null;
        throw new Error(`unexpected ${apiPath}`);
    };
    api.calls = calls;
    return api;
}

const enqueueMutations = (api) => api.calls.filter((call) => call.body?.query?.includes("enqueuePullRequest"));
const mergeAsyncCalls = (api) => api.calls.filter((call) => call.apiPath.endsWith("/merge-async"));
const mergeAsyncGets = (api) => api.calls.filter((call) => call.method === "GET" && call.apiPath.endsWith("/merge-async/u-1"));
const liveQueries = (api) => api.calls.filter((call) => call.apiPath === "/graphql" && !call.body.query.includes("enqueuePullRequest"));
const commentPosts = (api) => api.calls.filter((call) => call.method === "POST" && call.apiPath.endsWith("/comments"));

const openLive = {
    id: "PR_1",
    state: "OPEN",
    headRefOid: HEAD,
    baseRefName: "develop",
    mergeQueueEntry: null,
    stack: null,
    timelineItems: { nodes: [{ createdAt: "2026-09-04T08:00:00Z" }] },
};

// 0926 #2123: base 는 develop 이지만 위에 #2124 가 얹혀 스택 2125 의 밑단이다.
const stackedLive = { ...openLive, stack: { number: 2125, baseRefName: "develop" } };
const stackUpperLive = { ...stackedLive, baseRefName: "refactor/1429-setting-repositories" };

test("MERGED·CLOSED 거나 이미 큐에 있으면 payload 와 무관하게 아무것도 쓰지 않는다", async () => {
    for (const live of [
        { ...openLive, state: "MERGED" },
        { ...openLive, state: "CLOSED" },
        { ...openLive, mergeQueueEntry: { state: "QUEUED", position: 2 } },
        { ...stackedLive, state: "MERGED" },
        { ...stackedLive, state: "CLOSED" },
        { ...stackedLive, mergeQueueEntry: { state: "QUEUED", position: 2 } },
        { ...stackUpperLive, state: "MERGED" },
        { ...stackUpperLive, state: "CLOSED" },
        { ...stackUpperLive, mergeQueueEntry: { state: "QUEUED", position: 2 } },
    ]) {
        const api = fakeApi({ live });
        const result = await handleDequeue({ api, repository: REPO, number: 1509, reason: "UNKNOWN_REMOVAL_REASON", logger: silent });
        assert.equal(result.action, "none");
        assert.equal(result.stack, live.stack?.number ?? null);
        assert.ok(!("enqueueMethod" in result));
        assert.equal(api.calls.filter((call) => call.method !== "GET" && call.apiPath !== "/graphql").length, 0);
        assert.ok(!api.calls.some((call) => call.body?.query?.includes("enqueuePullRequest")));
    }
});

test("조용한 방출: 마커 코멘트를 먼저 남기고 그 다음 enqueuePullRequest 를 쏜다", async () => {
    const api = fakeApi({ live: openLive });
    const result = await handleDequeue({ api, repository: REPO, number: 1509, reason: "UNKNOWN_REMOVAL_REASON", logger: silent });
    assert.equal(result.action, "requeue");
    assert.equal(result.stack, null);
    assert.equal(result.enqueueMethod, "graphql");
    const writes = api.calls.filter((call) => call.method !== "GET" && (call.apiPath !== "/graphql" || call.body.query.includes("enqueuePullRequest")));
    assert.equal(writes.length, 2);
    assert.match(writes[0].apiPath, /\/issues\/1509\/comments$/);
    assert.ok(writes[0].body.body.startsWith(requeueMarker(HEAD)));
    assert.equal(writes[1].apiPath, "/graphql");
    assert.equal(mergeAsyncCalls(api).length, 0, "비스택 PR 은 종전대로 GraphQL 로만 넣는다");
});

// ---- 스택 PR 의 재투입은 REST merge-async (2026-09-27 #2177) ----

test("live 조회는 stack 필드를 함께 읽는다 — 재투입 수단을 고르는 근거다", async () => {
    const api = fakeApi({ live: openLive });
    await handleDequeue({ api, repository: REPO, number: 1509, reason: "UNKNOWN_REMOVAL_REASON", dryRun: true, logger: silent });
    const liveQuery = api.calls.find((call) => call.apiPath === "/graphql").body.query;
    assert.match(liveQuery, /stack\s*\{\s*number\s+baseRefName\s*\}/);
});

test("스택 밑단은 마커 뒤 merge-async 로 넣고 GET enqueued 를 확인한다 — enqueuePullRequest 는 스택 PR 을 거부한다", async () => {
    const api = fakeApi({ live: stackedLive });
    const result = await handleDequeue({ api, repository: REPO, number: 2123, reason: "MERGE_CONFLICT", logger: silent, sleep: immediateSleep });
    assert.equal(result.action, "requeue");
    assert.equal(result.stack, 2125);
    assert.equal(result.enqueueMethod, "merge-async");
    assert.equal(enqueueMutations(api).length, 0, "GraphQL enqueuePullRequest 는 스택 PR 을 UNPROCESSABLE 로 거부한다");
    const writes = api.calls.filter((call) => call.method !== "GET" && call.apiPath !== "/graphql");
    assert.equal(writes.length, 2);
    assert.match(writes[0].apiPath, /\/issues\/2123\/comments$/);
    assert.ok(writes[0].body.body.startsWith(requeueMarker(HEAD)), "마커가 먼저다 — 재투입 뒤 코멘트가 실패하면 두 번째 재투입이 나간다");
    assert.deepEqual(
        { apiPath: writes[1].apiPath, method: writes[1].method, body: writes[1].body },
        { apiPath: `/repos/${REPO}/pulls/2123/merge-async`, method: "PUT", body: { merge_action: "merge_queue", sha: HEAD } },
    );
    assert.equal(mergeAsyncGets(api).length, 1);
    assert.equal(mergeAsyncGets(api)[0].apiPath, `/repos/${REPO}/pulls/2123/merge-async/u-1`);
    assert.ok(api.calls.indexOf(writes[1]) < api.calls.indexOf(mergeAsyncGets(api)[0]));
    assert.equal(liveQueries(api).length, 1, "최종 결과가 확인되면 live 재조회는 필요 없다");
});

test("스택 위쪽은 마커 없는 안내만 남긴다 — 아래 PR 의 실패 판정을 우회하는 자동 재투입을 막는다", async () => {
    const api = fakeApi({ live: stackUpperLive });
    const result = await handleDequeue({ api, repository: REPO, number: 2124, reason: "MERGE_CONFLICT", logger: silent, sleep: immediateSleep });
    assert.equal(result.action, "comment-stack-upper");
    assert.equal(result.stack, 2125);
    assert.ok(!("enqueueMethod" in result));
    assert.equal(mergeAsyncCalls(api).length, 0);
    assert.equal(mergeAsyncGets(api).length, 0);
    assert.equal(enqueueMutations(api).length, 0);
    assert.equal(commentPosts(api).length, 1);
    const body = commentPosts(api)[0].body.body;
    assert.ok(!body.includes("<!-- merge-queue-dequeue:requeued"));
    assert.match(body, /스택 위쪽 PR 이라 자동 재투입하지 않았다/);
    assert.match(body, /아래의 열린 PR 까지 큐에 다시 넣어 아래 PR 의 판정을 덮는다/);
    assert.match(body, /밑단부터 확인한 뒤 `gh stack merge 2124 --yes`/);
});

test("스택 여부는 판정표를 바꾸지 않는다 — 실패 job 이면 comment-failure, 같은 head 두 번째면 comment-give-up, 어느 쪽도 재투입 없음", async () => {
    for (const live of [stackedLive, stackUpperLive]) {
        const runs = [{ id: 7, name: "Unit Test", html_url: "https://x/runs/7", head_branch: queueBranchPrefix(live.baseRefName, 2123) + "abc", created_at: "2026-09-04T08:05:00Z" }];
        const jobs = { 7: [{ name: "Run Unit Tests", conclusion: "failure", html_url: "https://x/jobs/71" }] };
        const failed = fakeApi({ live, runs, jobs });
        assert.equal((await handleDequeue({ api: failed, repository: REPO, number: 2123, reason: "CI_FAILURE", logger: silent })).action, "comment-failure");
        assert.match(commentPosts(failed)[0].body.body, /gh stack merge 2123 --yes/);

        const second = fakeApi({ live, comments: [{ body: renderRequeueComment({ reason: "MERGE_CONFLICT", headSha: HEAD }) }] });
        assert.equal((await handleDequeue({ api: second, repository: REPO, number: 2123, reason: "MERGE_CONFLICT", logger: silent })).action, "comment-give-up");
        assert.match(commentPosts(second)[0].body.body, /gh stack merge 2123 --yes/);

        const manual = fakeApi({ live });
        assert.equal((await handleDequeue({ api: manual, repository: REPO, number: 2123, reason: "MANUAL", logger: silent })).action, "none");

        for (const api of [failed, second, manual]) {
            assert.equal(mergeAsyncCalls(api).length, 0);
            assert.equal(enqueueMutations(api).length, 0);
        }
        assert.equal(commentPosts(failed).length, 1);
        assert.equal(commentPosts(second).length, 1);
        assert.equal(commentPosts(manual).length, 0);
    }
});

test("merge-async GET 결과가 failed 면 마커 한 건을 남긴 채 예외로 올린다 — 배경 실패도 job 이 red 로 남는다", async () => {
    const failed = { status: "failed", details: { message: "Merge conflict: the pull request could not be merged." } };
    const api = fakeApi({ live: stackedLive, mergeAsyncPolls: [failed] });
    await assert.rejects(
        handleDequeue({ api, repository: REPO, number: 2123, reason: "MERGE_CONFLICT", logger: silent, sleep: immediateSleep }),
        { message: `merge-async 실패: ${JSON.stringify(failed)}` },
    );
    assert.equal(mergeAsyncCalls(api).length, 1);
    assert.equal(mergeAsyncGets(api).length, 1);
    assert.equal(commentPosts(api).length, 1);
    assert.ok(commentPosts(api)[0].body.body.startsWith(requeueMarker(HEAD)));
    assert.ok(api.calls.indexOf(commentPosts(api)[0]) < api.calls.indexOf(mergeAsyncCalls(api)[0]));
    assert.equal(liveQueries(api).length, 1, "확정된 실패를 live 조회로 성공 처리하지 않는다");
    await assert.rejects(enqueueStacked(api, REPO, 2123, HEAD, { sleep: immediateSleep }), /merge-async 실패/);
    assert.equal(mergeAsyncCalls(api).length, 2);
    assert.equal(mergeAsyncGets(api).length, 2);
    assert.equal(commentPosts(api).length, 1);
});

test("GET pending 두 번 뒤 enqueued 면 성공한다 — 매 조회 전에 주입한 sleep 으로 5초 간격을 지킨다", async () => {
    const api = fakeApi({
        live: stackedLive,
        mergeAsyncPolls: [{ status: "pending" }, { status: "pending" }, { status: "enqueued" }],
    });
    const sleeps = [];
    const result = await handleDequeue({
        api, repository: REPO, number: 2123, reason: "MERGE_CONFLICT", logger: silent,
        sleep: async (ms) => {
            sleeps.push(ms);
            assert.equal(mergeAsyncGets(api).length, sleeps.length - 1);
        },
    });
    assert.equal(result.action, "requeue");
    assert.equal(result.enqueueMethod, "merge-async");
    assert.deepEqual(sleeps, [5000, 5000, 5000]);
    assert.equal(mergeAsyncGets(api).length, 3);
    assert.equal(mergeAsyncCalls(api).length, 1);
    assert.equal(commentPosts(api).length, 1);
    assert.equal(liveQueries(api).length, 1);
});

test("PUT 200 enqueued·merged 는 즉시 반환한다 — 이미 끝난 요청은 GET 과 sleep 이 필요 없다", async () => {
    for (const status of ["enqueued", "merged"]) {
        const response = { status, details: { message: "Pull request is already in the merge queue." } };
        const api = fakeApi({ live: stackedLive, mergeAsync: response });
        const result = await enqueueStacked(api, REPO, 2123, HEAD, {
            sleep: async () => assert.fail("완료 응답은 기다리지 않는다"),
        });
        assert.deepEqual(result, response);
        assert.equal(mergeAsyncCalls(api).length, 1);
        assert.equal(mergeAsyncGets(api).length, 0);
    }
});

test("마지막 허용 GET 이 merged 면 성공으로 반환한다 — 폴링 상한에서 받은 완료도 확정 결과다", async () => {
    const response = { status: "merged", details: { message: "Pull request merged." } };
    const api = fakeApi({ live: stackedLive, mergeAsyncPolls: [{ status: "pending" }, response] });
    const sleeps = [];
    assert.deepEqual(await enqueueStacked(api, REPO, 2123, HEAD, {
        sleep: async (ms) => sleeps.push(ms), pollIntervalMs: 7, maxPolls: 2,
    }), response);
    assert.deepEqual(sleeps, [7, 7]);
    assert.equal(mergeAsyncGets(api).length, 2);
});

test("폴링 상한까지 pending 이면 마지막 응답에 unresolved 를 붙인다 — PUT 응답으로 되돌리지 않는다", async () => {
    const pending = { status: "pending", details: { message: "Still pending." } };
    const api = fakeApi({ live: stackedLive, mergeAsyncPolls: [pending] });
    const sleeps = [];
    assert.deepEqual(await enqueueStacked(api, REPO, 2123, HEAD, {
        sleep: async (ms) => sleeps.push(ms), pollIntervalMs: 7, maxPolls: 2,
    }), { ...pending, unresolved: true });
    assert.deepEqual(sleeps, [7, 7]);
    assert.equal(mergeAsyncGets(api).length, 2);
});

test("24회 pending 이어도 live 재조회에 큐 항목이 있으면 성공한다 — 비동기 결과의 미확정을 큐 상태로 보완한다", async () => {
    const api = fakeApi({
        live: stackedLive,
        liveAfter: { ...stackedLive, mergeQueueEntry: { state: "QUEUED", position: 1 } },
        mergeAsyncPolls: [{ status: "pending" }],
    });
    const sleeps = [];
    const logs = [];
    const result = await handleDequeue({
        api, repository: REPO, number: 2123, reason: "MERGE_CONFLICT",
        logger: { log: (line) => logs.push(line) }, sleep: async (ms) => sleeps.push(ms),
    });
    assert.equal(result.action, "requeue");
    assert.equal(result.stack, 2125);
    assert.equal(result.enqueueMethod, "merge-async");
    assert.equal(mergeAsyncGets(api).length, 24);
    assert.deepEqual(sleeps, Array(24).fill(5000));
    assert.equal(liveQueries(api).length, 2);
    assert.equal(commentPosts(api).length, 1);
    assert.equal(mergeAsyncCalls(api).length, 1);
    assert.ok(logs.some((line) => line.includes("스택 2125") && line.includes("큐 진입 확인")));
});

test("24회 pending 이고 live 재조회에도 큐 항목이 없으면 실패한다 — 미확정을 재투입 성공으로 보고하지 않는다", async () => {
    const api = fakeApi({ live: stackedLive, mergeAsyncPolls: [{ status: "pending" }] });
    const sleeps = [];
    await assert.rejects(handleDequeue({
        api, repository: REPO, number: 2123, reason: "MERGE_CONFLICT", logger: silent,
        sleep: async (ms) => sleeps.push(ms),
    }), /merge-async 결과 미확정: .*pending.* 큐에도 없다/);
    assert.equal(mergeAsyncGets(api).length, 24);
    assert.deepEqual(sleeps, Array(24).fill(5000));
    assert.equal(liveQueries(api).length, 2);
    assert.equal(commentPosts(api).length, 1);
    assert.ok(commentPosts(api)[0].body.body.startsWith(requeueMarker(HEAD)));
    assert.equal(mergeAsyncCalls(api).length, 1);
});

test("pending 에 uuid 가 없으면 live 큐 상태로 판정한다 — 조회할 비동기 요청을 추측하지 않는다", async () => {
    for (const mergeQueueEntry of [null, { state: "QUEUED", position: 1 }]) {
        const api = fakeApi({
            live: stackedLive, liveAfter: { ...stackedLive, mergeQueueEntry },
            mergeAsync: { status: "pending", details: { message: "Merge request enqueued." } },
        });
        const result = handleDequeue({
            api, repository: REPO, number: 2123, reason: "MERGE_CONFLICT", logger: silent,
            sleep: async () => assert.fail("uuid 가 없으면 기다리지 않는다"),
        });
        if (mergeQueueEntry) {
            assert.equal((await result).enqueueMethod, "merge-async");
        } else {
            await assert.rejects(result, /merge-async 결과 미확정: .* 큐에도 없다/);
        }
        assert.equal(mergeAsyncGets(api).length, 0);
        assert.equal(liveQueries(api).length, 2);
        assert.equal(commentPosts(api).length, 1);
        assert.equal(mergeAsyncCalls(api).length, 1);
    }
});

test("PUT HTTP 오류는 마커 한 건을 남긴 채 그대로 올라온다 — 400·409·422 를 성공으로 삼지 않는다", async () => {
    for (const status of [400, 409, 422]) {
        const error = new Error(`GitHub API PUT merge-async 실패: ${status}`);
        const api = fakeApi({ live: stackedLive, mergeAsync: error });
        await assert.rejects(
            handleDequeue({ api, repository: REPO, number: 2123, reason: "MERGE_CONFLICT", logger: silent, sleep: immediateSleep }),
            (caught) => caught === error,
        );
        assert.equal(commentPosts(api).length, 1);
        assert.ok(commentPosts(api)[0].body.body.startsWith(requeueMarker(HEAD)));
        assert.ok(api.calls.indexOf(commentPosts(api)[0]) < api.calls.indexOf(mergeAsyncCalls(api)[0]));
        assert.equal(mergeAsyncCalls(api).length, 1);
        assert.equal(mergeAsyncGets(api).length, 0);
        assert.equal(liveQueries(api).length, 1);
        assert.equal(enqueueMutations(api).length, 0);
    }
});

test("GET HTTP 오류도 그대로 올라온다 — 폴링 실패를 미확정 성공으로 삼지 않는다", async () => {
    const error = new Error("GitHub API GET merge-async/u-1 실패: 500");
    const api = fakeApi({ live: stackedLive, mergeAsyncPolls: [error] });
    await assert.rejects(
        handleDequeue({ api, repository: REPO, number: 2123, reason: "MERGE_CONFLICT", logger: silent, sleep: immediateSleep }),
        (caught) => caught === error,
    );
    assert.equal(commentPosts(api).length, 1);
    assert.ok(commentPosts(api)[0].body.body.startsWith(requeueMarker(HEAD)));
    assert.equal(mergeAsyncGets(api).length, 1);
    assert.equal(liveQueries(api).length, 1);
});

test("같은 head 의 두 번째 방출은 코멘트만 남기고 enqueuePullRequest 를 쏘지 않는다", async () => {
    const api = fakeApi({ live: openLive, comments: [{ body: renderRequeueComment({ reason: "X", headSha: HEAD }) }] });
    const result = await handleDequeue({ api, repository: REPO, number: 1509, reason: "UNKNOWN_REMOVAL_REASON", logger: silent });
    assert.equal(result.action, "comment-give-up");
    assert.ok(!api.calls.some((call) => call.body?.query?.includes("enqueuePullRequest")));
    assert.equal(api.calls.filter((call) => call.method === "POST" && call.apiPath.endsWith("/comments")).length, 1);
    assert.match(commentPosts(api)[0].body.body, /gh pr merge 1509 --repo Afternote\/Afternote-FE/);
});

test("CI 실패: 실패 job 링크 코멘트만, 재투입 없음", async () => {
    const runs = [{ id: 7, name: "Unit Test", html_url: "https://x/runs/7", head_branch: queueBranchPrefix("develop", 1509) + "abc", created_at: "2026-09-04T08:05:00Z" }];
    const jobs = { 7: [{ name: "Run Unit Tests", conclusion: "failure", html_url: "https://x/jobs/71" }] };
    const api = fakeApi({ live: openLive, runs, jobs });
    const result = await handleDequeue({ api, repository: REPO, number: 1509, reason: "CI_FAILURE", logger: silent });
    assert.equal(result.action, "comment-failure");
    const comment = api.calls.find((call) => call.method === "POST" && call.apiPath.endsWith("/comments"));
    assert.match(comment.body.body, /https:\/\/x\/jobs\/71/);
    assert.ok(!api.calls.some((call) => call.body?.query?.includes("enqueuePullRequest")));
});

test("GMD 인프라 실패만으로 방출되면 마커 코멘트 뒤 재투입한다 — 마커 없는 같은 job 은 실패 코멘트만", async () => {
    const runs = [{ id: 9, run_attempt: 1, name: "Android Managed Device Test", html_url: "https://x/runs/9", head_branch: queueBranchPrefix("develop", 1509) + "abc", created_at: "2026-09-04T08:05:00Z" }];
    const jobs = { 9: [
        { name: "Pixel 2 API 30 androidTest", conclusion: "failure", html_url: "https://x/jobs/91" },
        { name: "Pixel 2 API 34 accessibility smoke", conclusion: "success", html_url: "https://x/jobs/92" },
    ] };
    const proven = fakeApi({ live: openLive, runs, jobs, artifacts: { 9: [{ name: "android-managed-device-retry-api30-9-1" }] } });
    const result = await handleDequeue({ api: proven, repository: REPO, number: 1509, reason: "CI_FAILURE", logger: silent });
    assert.equal(result.action, "requeue");
    assert.equal(enqueueMutations(proven).length, 1);
    const comment = commentPosts(proven)[0].body.body;
    assert.ok(comment.startsWith(requeueMarker(HEAD)));
    assert.match(comment, /인프라 실패로 분류됨/);
    assert.match(comment, /https:\/\/x\/jobs\/91/);
    const writes = proven.calls.filter((call) => call.method === "POST");
    assert.ok(writes.findIndex((call) => call.apiPath.endsWith("/comments")) < writes.findIndex((call) => call.body?.query?.includes("enqueuePullRequest")));

    const unproven = fakeApi({ live: openLive, runs, jobs });
    const failed = await handleDequeue({ api: unproven, repository: REPO, number: 1509, reason: "CI_FAILURE", logger: silent });
    assert.equal(failed.action, "comment-failure");
    assert.equal(enqueueMutations(unproven).length, 0);

    const second = fakeApi({
        live: openLive, runs, jobs,
        artifacts: { 9: [{ name: "android-managed-device-retry-api30-9-1" }] },
        comments: [{ body: renderRequeueComment({ reason: "CI_FAILURE", headSha: HEAD }) }],
    });
    const again = await handleDequeue({ api: second, repository: REPO, number: 1509, reason: "CI_FAILURE", logger: silent });
    assert.equal(again.action, "comment-failure");
    assert.equal(enqueueMutations(second).length, 0);
});

test("dry-run 은 판정만 하고 아무것도 쓰지 않는다 — 스택 PR 도 같다", async () => {
    for (const live of [openLive, stackedLive, stackUpperLive]) {
        const api = fakeApi({ live });
        const result = await handleDequeue({ api, repository: REPO, number: 1509, reason: "ROLL_BACK", dryRun: true, logger: silent });
        assert.equal(result.action, live === stackUpperLive ? "comment-stack-upper" : "requeue");
        assert.equal(result.dryRun, true);
        assert.equal(result.stack, live.stack?.number ?? null);
        assert.ok(!("enqueueMethod" in result));
        assert.equal(api.calls.filter((call) => call.method !== "GET" && call.apiPath !== "/graphql").length, 0);
        assert.equal(enqueueMutations(api).length, 0);
        assert.equal(mergeAsyncCalls(api).length, 0);
        assert.equal(mergeAsyncGets(api).length, 0);
    }
});

// ---- 워크플로 정책 ----

test("워크플로는 default branch 정의로 dequeued 에만 반응하고, 테스트를 먼저 돌린 뒤 스크립트를 부른다", () => {
    const source = readFileSync(new URL("../workflows/merge-queue-dequeue.yml", import.meta.url), "utf8");
    assert.match(source, /^on:\n\s{2}pull_request_target:\n\s{4}types: \[dequeued\]$/m);
    assert.match(source, /^permissions: \{\}$/m);
    assert.match(source, /ref: \$\{\{ github\.event\.repository\.default_branch \}\}/);
    assert.match(source, /persist-credentials: false/);
    assert.ok(source.indexOf("handle-merge-queue-dequeue.test.mjs") < source.indexOf("node .github/scripts/handle-merge-queue-dequeue.mjs"));
    assert.match(source, /DEQUEUE_REASON: \$\{\{ github\.event\.reason \}\}/);
    assert.match(source, /cancel-in-progress: false/);
});

// ---- 재투입은 사람 토큰을 가진 워크플로가 한다 (1005 #2208) ----
// GITHUB_TOKEN 으로 넣은 큐 항목은 merge_group CI 를 띄우지 못해 60분 뒤 CI_TIMEOUT 으로 빠진다.

const dispatches = (api) => api.calls.filter((call) => call.apiPath.endsWith("/dispatches"));

test("방출 처리에 재투입 위임을 주면 마커를 먼저 남기고 직접 넣지 않는다", async () => {
    for (const live of [openLive, stackedLive]) {
        const api = fakeApi({ live });
        const delegated = [];
        const result = await handleDequeue({
            api, repository: REPO, number: 1509, reason: "CI_TIMEOUT", logger: silent,
            requeue: async (request) => {
                delegated.push(request);
                assert.equal(commentPosts(api).length, 1, "마커 코멘트가 위임보다 먼저다");
                return "dispatch";
            },
        });
        assert.equal(result.action, "requeue");
        assert.equal(result.enqueueMethod, "dispatch");
        assert.equal(delegated.length, 1);
        assert.equal(delegated[0].number, 1509);
        assert.equal(delegated[0].headSha, HEAD);
        assert.ok(commentPosts(api)[0].body.body.startsWith(requeueMarker(HEAD)));
        assert.equal(enqueueMutations(api).length, 0);
        assert.equal(mergeAsyncCalls(api).length, 0);
    }
});

test("재투입 위임은 재투입 판정일 때만 부른다", async () => {
    const runs = [{ id: 7, name: "Unit Test", html_url: "https://x/runs/7", head_branch: queueBranchPrefix("develop", 1509) + "abc", created_at: "2026-09-04T08:05:00Z" }];
    const jobs = { 7: [{ name: "Run Unit Tests", conclusion: "failure", html_url: "https://x/jobs/71" }] };
    const cases = [
        fakeApi({ live: openLive, runs, jobs }),
        fakeApi({ live: openLive, comments: [{ body: renderRequeueComment({ reason: "X", headSha: HEAD }) }] }),
        fakeApi({ live: { ...openLive, state: "MERGED" } }),
    ];
    for (const api of cases) {
        await handleDequeue({
            api, repository: REPO, number: 1509, reason: "CI_TIMEOUT", logger: silent,
            requeue: async () => assert.fail("재투입 판정이 아니면 위임하지 않는다"),
        });
    }
});

test("dispatchRequeue 는 판정한 head 를 입력으로 재투입 워크플로를 띄운다", async () => {
    const api = fakeApi({ live: openLive });
    await dispatchRequeue(api, REPO, { number: 2208, headSha: HEAD, ref: "develop" });
    assert.deepEqual(dispatches(api).map(({ apiPath, method, body }) => ({ apiPath, method, body })), [{
        apiPath: `/repos/${REPO}/actions/workflows/${REQUEUE_WORKFLOW_FILE}/dispatches`,
        method: "POST",
        body: { ref: "develop", inputs: { pull_request_number: "2208", head_sha: HEAD } },
    }]);
});

test("requeueDispatched 는 판정한 head 그대로 열려 있고 큐 밖일 때만 넣는다", async () => {
    const plain = fakeApi({ live: openLive });
    assert.deepEqual(await requeueDispatched({ api: plain, repository: REPO, number: 2208, headSha: HEAD, logger: silent }), { action: "requeue", enqueueMethod: "graphql" });
    assert.equal(enqueueMutations(plain).length, 1);

    const stacked = fakeApi({ live: stackedLive });
    const stackedResult = await requeueDispatched({ api: stacked, repository: REPO, number: 2123, headSha: HEAD, logger: silent, sleep: immediateSleep });
    assert.equal(stackedResult.enqueueMethod, "merge-async");
    assert.equal(enqueueMutations(stacked).length, 0);

    for (const [live, why] of [
        [{ ...openLive, state: "MERGED" }, "MERGED"],
        [{ ...openLive, mergeQueueEntry: { state: "QUEUED", position: 1 } }, "already-queued"],
        [{ ...openLive, headRefOid: "f".repeat(40) }, "head-changed"],
        [stackUpperLive, "stack-upper"],
    ]) {
        const api = fakeApi({ live });
        assert.deepEqual(await requeueDispatched({ api, repository: REPO, number: 2208, headSha: HEAD, logger: silent }), { action: "none", why });
        assert.equal(enqueueMutations(api).length, 0, why);
        assert.equal(mergeAsyncCalls(api).length, 0, why);
        assert.equal(commentPosts(api).length, 0, "판정·코멘트는 방출 처리 job 몫이다");
    }
});

test("사람 토큰이 없으면 GITHUB_TOKEN 으로 넣지 않고 직접 투입 명령을 안내한다", () => {
    const body = renderMissingQueueTokenComment({ headSha: HEAD, repository: REPO, number: 2208 });
    assert.match(body, /MERGE_QUEUE_TOKEN/);
    assert.match(body, /CI_TIMEOUT/);
    assert.match(body, /gh pr merge 2208 --repo Afternote\/Afternote-FE/);
    assert.ok(!body.includes("<!-- merge-queue-dequeue:requeued"));
});

test("워크플로: 방출 처리는 시크릿 없이 dispatch 만 하고, 재투입은 workflow_dispatch 로만 사람 토큰을 쓴다", () => {
    const dequeue = readFileSync(new URL("../workflows/merge-queue-dequeue.yml", import.meta.url), "utf8");
    assert.doesNotMatch(dequeue, /\bsecrets(?:\.|\[)/);
    assert.match(dequeue, /actions: write/);
    assert.match(dequeue, /DEFAULT_BRANCH: \$\{\{ github\.event\.repository\.default_branch \}\}/);

    const requeue = readFileSync(new URL(`../workflows/${REQUEUE_WORKFLOW_FILE}`, import.meta.url), "utf8");
    assert.match(requeue, /^on:\n  workflow_dispatch:\n/m);
    assert.doesNotMatch(requeue, /^  (pull_request|pull_request_target|merge_group|schedule|push):/m);
    assert.match(requeue, /^permissions: \{\}$/m);
    assert.match(requeue, /MERGE_QUEUE_TOKEN: \$\{\{ secrets\.MERGE_QUEUE_TOKEN \}\}/);
    assert.match(requeue, /MODE: requeue/);
    assert.match(requeue, /ref: \$\{\{ github\.event\.repository\.default_branch \}\}/);
    assert.match(requeue, /persist-credentials: false/);
});
