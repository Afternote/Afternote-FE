#!/usr/bin/env node

// merge queue 에서 방출된 PR 을 자동 처리한다 (#1892).
//
// 큐(#1477)는 투입한 뒤의 결과가 «보러 가야만» 보였다. 방출은 두 갈래다.
// - merge group CI 실패 — PR 화면엔 «removed from the merge queue» 만 남고 실패 job 은 Actions 탭에서
//   event:merge_group 으로 따로 찾아야 했다. → 실패 job 링크를 PR 코멘트로 남긴다. 재투입하지 않는다.
// - 조용한 방출 — 앞 건이 머지되며 base 가 움직이면 실패 흔적 없이 큐에서 빠진다(0830 실측 #1509:
//   mergeQueueEntry null · mergeStateStatus CLEAN · merge_group run 전량 success). → 같은 head SHA 당
//   한 번만 재투입한다. 0902 에 감시 루프가 90초마다 맹목 재투입해 30분을 헛돈 사고(#1638·#1639)가
//   «한 번까지» 규칙의 근거다. 횟수는 이 스크립트가 남기는 마커 코멘트로 센다.
//
// 판정 순서는 «실패 job 이 있는가» 가 먼저다. payload 의 reason 문자열은 GitHub 이 열거값을 문서화하지
// 않아 정본으로 삼지 않는다 — 실패 job 은 데이터로 확인하고, reason 은 «사람이 뺐다·이미 머지됐다» 를
// 거르는 데만 쓴다. 그 밖의 reason 은 전부 조용한 방출로 본다.

import { readFile } from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import { pathToFileURL } from "node:url";

export const REQUEUE_MARKER_PREFIX = "<!-- merge-queue-dequeue:requeued head=";
// payload 의 reason 표기가 문서화돼 있지 않아(timeline 은 failed_checks·merged 소문자) 대문자로 맞춰 비교한다.
export const NO_ACTION_REASONS = new Set(["MANUAL", "ALREADY_MERGED", "MERGE", "MERGED"]);
const FAILED_JOB_CONCLUSIONS = new Set(["failure", "timed_out"]);
const QUEUE_RUNS_TO_INSPECT = 10;

export function queueBranchPrefix(baseRef, number) {
    return `gh-readonly-queue/${baseRef}/pr-${number}-`;
}

/**
 * 이 PR 의 큐 브랜치에서 돈 merge_group run 중 실패·타임아웃으로 끝난 job 만 모은다.
 *
 * [since] 는 마지막 큐 투입 시각이다 — 큐 브랜치 이름은 head 가 아니라 base SHA 를 달고 있어 며칠 전
 * 투입의 실패 run 도 같은 접두어로 잡힌다(0904 #1582 실측). 그 이전 run 은 이번 방출과 무관하다.
 */
export function collectFailedJobs({ runs, jobsByRunId, baseRef, number, since }) {
    const prefix = queueBranchPrefix(baseRef, number);
    const failed = [];
    for (const run of runs) {
        if (typeof run.head_branch !== "string" || !run.head_branch.startsWith(prefix)) continue;
        if (since && typeof run.created_at === "string" && run.created_at < since) continue;
        for (const job of jobsByRunId.get(run.id) ?? []) {
            if (!FAILED_JOB_CONCLUSIONS.has(job.conclusion)) continue;
            failed.push({
                runName: run.name,
                runUrl: run.html_url,
                jobName: job.name,
                jobUrl: job.html_url,
                conclusion: job.conclusion,
            });
        }
    }
    return failed;
}

export function requeueMarker(headSha) {
    return `${REQUEUE_MARKER_PREFIX}${headSha} -->`;
}

/** 같은 head 에서 이미 재투입한 횟수 — 마커 코멘트 수다. 새 커밋이 올라오면 head 가 바뀌어 0 부터다. */
export function countRequeuesForHead(comments, headSha) {
    const marker = requeueMarker(headSha);
    return comments.filter((comment) => typeof comment.body === "string" && comment.body.includes(marker)).length;
}

/**
 * 판정표.
 * 1. 실패 job 이 있다 → comment-failure (재투입 없음)
 * 2. reason 이 MANUAL·ALREADY_MERGED·MERGE → none
 * 3. 같은 head 에서 이미 재투입했다 → comment-give-up
 * 4. 그 밖 → requeue
 */
export function decide({ reason, failedJobs, requeueCount }) {
    if (failedJobs.length > 0) return "comment-failure";
    if (NO_ACTION_REASONS.has(String(reason ?? "").toUpperCase())) return "none";
    if (requeueCount >= 1) return "comment-give-up";
    return "requeue";
}

const shortSha = (sha) => sha.slice(0, 7);

export function renderFailureComment({ reason, headSha, failedJobs, repository, number }) {
    const lines = failedJobs.map((job) => `- [${job.runName} / ${job.jobName}](${job.jobUrl}) — ${job.conclusion}`);
    return [
        "### merge queue 방출 — merge group CI 실패",
        "",
        `사유 \`${reason}\` · head \`${shortSha(headSha)}\`. 재투입하지 않았다.`,
        "",
        "실패한 job:",
        ...lines,
        "",
        `고친 뒤 \`gh pr merge ${number} --repo ${repository}\` 로 다시 투입한다.`,
    ].join("\n");
}

export function renderRequeueComment({ reason, headSha }) {
    return [
        requeueMarker(headSha),
        "### merge queue 방출 — 재투입했다",
        "",
        `사유 \`${reason}\` · head \`${shortSha(headSha)}\` · merge group 실패 job 없음 → 조용한 방출로 보고 한 번 재투입했다.`,
        "같은 head 에서 또 방출되면 재투입하지 않고 여기에 남긴다.",
    ].join("\n");
}

export function renderGiveUpComment({ reason, headSha }) {
    return [
        "### merge queue 방출 — 같은 head 두 번째, 재투입하지 않는다",
        "",
        `사유 \`${reason}\` · head \`${shortSha(headSha)}\`. 실패 job 은 없다.`,
        "`gh run list --event merge_group` 으로 merge group run 을 확인하고 수동으로 투입한다.",
    ].join("\n");
}

export function createApi(token, { fetchImpl = globalThis.fetch } = {}) {
    if (typeof fetchImpl !== "function") {
        throw new TypeError("fetch 구현이 필요합니다.");
    }

    return async function api(apiPath, { method = "GET", body } = {}) {
        const response = await fetchImpl(`https://api.github.com${apiPath}`, {
            method,
            headers: {
                accept: "application/vnd.github+json",
                authorization: `Bearer ${token}`,
                "content-type": "application/json",
                "x-github-api-version": "2022-11-28",
            },
            body: body === undefined ? undefined : JSON.stringify(body),
        });
        if (!response.ok) {
            const detail = await response.text();
            throw new Error(`GitHub API ${method} ${apiPath} 실패: ${response.status} ${detail}`);
        }
        if (response.status === 204) return null;
        return response.json();
    };
}

async function graphql(api, query, variables) {
    const payload = await api("/graphql", { method: "POST", body: { query, variables } });
    if (payload?.errors?.length) {
        throw new Error(`GraphQL 실패: ${JSON.stringify(payload.errors)}`);
    }
    return payload.data;
}

/** 이벤트는 runner 대기 중 순서가 뒤집힐 수 있으므로 payload 가 아니라 live 상태를 정본으로 삼는다. */
export async function fetchLivePullRequest(api, repository, number) {
    const [owner, name] = repository.split("/");
    const data = await graphql(
        api,
        `query($owner: String!, $name: String!, $number: Int!) {
            repository(owner: $owner, name: $name) {
                pullRequest(number: $number) {
                    id
                    state
                    headRefOid
                    baseRefName
                    mergeQueueEntry { state position }
                    timelineItems(last: 5, itemTypes: [ADDED_TO_MERGE_QUEUE_EVENT]) {
                        nodes { ... on AddedToMergeQueueEvent { createdAt } }
                    }
                }
            }
        }`,
        { owner, name, number },
    );
    return data.repository.pullRequest;
}

/** 마지막으로 큐에 들어간 시각 — 그 뒤의 merge_group run 만 이번 방출의 근거다. */
export function lastEnqueuedAt(pullRequest) {
    const stamps = (pullRequest.timelineItems?.nodes ?? []).map((node) => node?.createdAt).filter(Boolean);
    return stamps.length > 0 ? stamps[stamps.length - 1] : undefined;
}

export async function fetchQueueFailedJobs(api, repository, { baseRef, number, since }) {
    const prefix = queueBranchPrefix(baseRef, number);
    const listed = await api(`/repos/${repository}/actions/runs?event=merge_group&per_page=100`);
    const runs = (listed.workflow_runs ?? [])
        .filter((run) => typeof run.head_branch === "string" && run.head_branch.startsWith(prefix))
        .filter((run) => !since || typeof run.created_at !== "string" || run.created_at >= since)
        .slice(0, QUEUE_RUNS_TO_INSPECT);
    const jobsByRunId = new Map();
    for (const run of runs) {
        const jobs = await api(`/repos/${repository}/actions/runs/${run.id}/jobs?per_page=100`);
        jobsByRunId.set(run.id, jobs.jobs ?? []);
    }
    return collectFailedJobs({ runs, jobsByRunId, baseRef, number, since });
}

export async function fetchComments(api, repository, number) {
    const comments = [];
    for (let page = 1; page <= 5; page += 1) {
        const batch = await api(`/repos/${repository}/issues/${number}/comments?per_page=100&page=${page}`);
        comments.push(...batch);
        if (batch.length < 100) break;
    }
    return comments;
}

export async function postComment(api, repository, number, body) {
    await api(`/repos/${repository}/issues/${number}/comments`, { method: "POST", body: { body } });
}

export async function enqueue(api, pullRequestId) {
    const data = await graphql(
        api,
        `mutation($pullRequestId: ID!) {
            enqueuePullRequest(input: { pullRequestId: $pullRequestId }) {
                mergeQueueEntry { state position }
            }
        }`,
        { pullRequestId },
    );
    return data.enqueuePullRequest.mergeQueueEntry;
}

export async function handleDequeue({ api, repository, number, reason, dryRun = false, logger = console }) {
    const live = await fetchLivePullRequest(api, repository, number);
    if (!live) throw new Error(`#${number} 를 찾을 수 없습니다`);
    if (live.state !== "OPEN") {
        logger.log(`#${number} 는 ${live.state} — 아무것도 하지 않는다`);
        return { action: "none", why: live.state };
    }
    if (live.mergeQueueEntry) {
        logger.log(`#${number} 는 이미 큐에 있다(${live.mergeQueueEntry.state}) — 아무것도 하지 않는다`);
        return { action: "none", why: "already-queued" };
    }

    const headSha = live.headRefOid;
    const since = lastEnqueuedAt(live);
    const failedJobs = await fetchQueueFailedJobs(api, repository, { baseRef: live.baseRefName, number, since });
    const comments = await fetchComments(api, repository, number);
    const requeueCount = countRequeuesForHead(comments, headSha);
    const action = decide({ reason, failedJobs, requeueCount });
    logger.log(`#${number} reason=${reason} head=${shortSha(headSha)} 마지막투입=${since ?? "?"} 실패job=${failedJobs.length} 재투입횟수=${requeueCount} → ${action}`);

    if (dryRun) return { action, dryRun: true, failedJobs, requeueCount };

    if (action === "comment-failure") {
        await postComment(api, repository, number, renderFailureComment({ reason, headSha, failedJobs, repository, number }));
    } else if (action === "comment-give-up") {
        await postComment(api, repository, number, renderGiveUpComment({ reason, headSha }));
    } else if (action === "requeue") {
        // 마커를 먼저 남긴다 — 재투입 뒤 코멘트가 실패하면 다음 방출에서 두 번째 재투입이 나간다.
        await postComment(api, repository, number, renderRequeueComment({ reason, headSha }));
        const entry = await enqueue(api, live.id);
        logger.log(`#${number} 재투입 → ${entry?.state ?? "?"} position=${entry?.position ?? "?"}`);
    }
    return { action, failedJobs, requeueCount };
}

async function main() {
    const token = process.env.GITHUB_TOKEN;
    const repository = process.env.GITHUB_REPOSITORY;
    if (!token || !repository) {
        throw new Error("GITHUB_TOKEN·GITHUB_REPOSITORY 가 필요합니다.");
    }
    let number = Number(process.env.PULL_REQUEST_NUMBER);
    let reason = process.env.DEQUEUE_REASON;
    if ((!Number.isInteger(number) || number <= 0 || !reason) && process.env.GITHUB_EVENT_PATH) {
        const event = JSON.parse(await readFile(process.env.GITHUB_EVENT_PATH, "utf8"));
        number = Number.isInteger(number) && number > 0 ? number : Number(event.number ?? event.pull_request?.number);
        reason = reason || event.reason;
    }
    if (!Number.isInteger(number) || number <= 0) {
        throw new Error("PULL_REQUEST_NUMBER 가 양의 정수여야 합니다.");
    }
    reason = reason || "UNKNOWN";

    const api = createApi(token);
    const result = await handleDequeue({ api, repository, number, reason, dryRun: process.env.DRY_RUN === "true" });
    const summary = `merge-queue-dequeue: #${number} reason=${reason} → ${result.action}${result.dryRun ? " (dry-run)" : ""}`;
    console.log(summary);
    if (process.env.GITHUB_STEP_SUMMARY) {
        const { appendFile } = await import("node:fs/promises");
        await appendFile(process.env.GITHUB_STEP_SUMMARY, `${summary}\n`);
    }
}

const invokedPath = process.argv[1] ? pathToFileURL(path.resolve(process.argv[1])).href : "";
if (import.meta.url === invokedPath) {
    main().catch((error) => {
        console.error(error.message);
        process.exitCode = 1;
    });
}
