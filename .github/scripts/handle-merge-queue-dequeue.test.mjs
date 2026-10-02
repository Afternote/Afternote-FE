import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

import {
    NO_ACTION_REASONS,
    collectFailedJobs,
    countRequeuesForHead,
    decide,
    handleDequeue,
    lastEnqueuedAt,
    queueBranchPrefix,
    renderFailureComment,
    renderRequeueComment,
    requeueMarker,
} from "./handle-merge-queue-dequeue.mjs";

const REPO = "Afternote/Afternote-FE";
const HEAD = "0123456789abcdef0123456789abcdef01234567";
const silent = { log() {} };

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

// ---- handleDequeue 흐름: live 상태가 정본, 쓰기 순서 ----

function fakeApi({ live, runs = [], jobs = {}, comments = [] }) {
    const calls = [];
    const api = async (apiPath, options = {}) => {
        calls.push({ apiPath, method: options.method ?? "GET", body: options.body });
        if (apiPath === "/graphql") {
            const query = options.body.query;
            if (query.includes("enqueuePullRequest")) {
                return { data: { enqueuePullRequest: { mergeQueueEntry: { state: "QUEUED", position: 1 } } } };
            }
            return { data: { repository: { pullRequest: live } } };
        }
        if (apiPath.startsWith(`/repos/${REPO}/actions/runs?`)) return { workflow_runs: runs };
        const jobsMatch = /\/actions\/runs\/(\d+)\/jobs/.exec(apiPath);
        if (jobsMatch) return { jobs: jobs[jobsMatch[1]] ?? [] };
        if (apiPath.includes("/comments") && (options.method ?? "GET") === "GET") return comments;
        if (apiPath.includes("/comments") && options.method === "POST") return { id: 1 };
        throw new Error(`unexpected ${apiPath}`);
    };
    api.calls = calls;
    return api;
}

const openLive = {
    id: "PR_1",
    state: "OPEN",
    headRefOid: HEAD,
    baseRefName: "develop",
    mergeQueueEntry: null,
    timelineItems: { nodes: [{ createdAt: "2026-09-04T08:00:00Z" }] },
};

test("MERGED·CLOSED 거나 이미 큐에 있으면 payload 와 무관하게 아무것도 쓰지 않는다", async () => {
    for (const live of [
        { ...openLive, state: "MERGED" },
        { ...openLive, state: "CLOSED" },
        { ...openLive, mergeQueueEntry: { state: "QUEUED", position: 2 } },
    ]) {
        const api = fakeApi({ live });
        const result = await handleDequeue({ api, repository: REPO, number: 1509, reason: "UNKNOWN_REMOVAL_REASON", logger: silent });
        assert.equal(result.action, "none");
        assert.equal(api.calls.filter((call) => call.method === "POST" && call.apiPath !== "/graphql").length, 0);
        assert.ok(!api.calls.some((call) => call.body?.query?.includes("enqueuePullRequest")));
    }
});

test("조용한 방출: 마커 코멘트를 먼저 남기고 그 다음 enqueuePullRequest 를 쏜다", async () => {
    const api = fakeApi({ live: openLive });
    const result = await handleDequeue({ api, repository: REPO, number: 1509, reason: "UNKNOWN_REMOVAL_REASON", logger: silent });
    assert.equal(result.action, "requeue");
    const writes = api.calls.filter((call) => call.method === "POST" && (call.apiPath !== "/graphql" || call.body.query.includes("enqueuePullRequest")));
    assert.equal(writes.length, 2);
    assert.match(writes[0].apiPath, /\/issues\/1509\/comments$/);
    assert.ok(writes[0].body.body.startsWith(requeueMarker(HEAD)));
    assert.equal(writes[1].apiPath, "/graphql");
});

test("같은 head 의 두 번째 방출은 코멘트만 남기고 enqueuePullRequest 를 쏘지 않는다", async () => {
    const api = fakeApi({ live: openLive, comments: [{ body: renderRequeueComment({ reason: "X", headSha: HEAD }) }] });
    const result = await handleDequeue({ api, repository: REPO, number: 1509, reason: "UNKNOWN_REMOVAL_REASON", logger: silent });
    assert.equal(result.action, "comment-give-up");
    assert.ok(!api.calls.some((call) => call.body?.query?.includes("enqueuePullRequest")));
    assert.equal(api.calls.filter((call) => call.method === "POST" && call.apiPath.endsWith("/comments")).length, 1);
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

test("dry-run 은 판정만 하고 아무것도 쓰지 않는다", async () => {
    const api = fakeApi({ live: openLive });
    const result = await handleDequeue({ api, repository: REPO, number: 1509, reason: "ROLL_BACK", dryRun: true, logger: silent });
    assert.equal(result.action, "requeue");
    assert.equal(api.calls.filter((call) => call.method === "POST" && call.apiPath !== "/graphql").length, 0);
    assert.ok(!api.calls.some((call) => call.body?.query?.includes("enqueuePullRequest")));
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
