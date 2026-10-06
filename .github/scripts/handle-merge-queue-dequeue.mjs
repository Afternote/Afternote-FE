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
// 예외는 증명된 인프라 실패다(#2250). GMD 분류기(classify-android-managed-device-failure.mjs)가 테스트가 돌기 전에
// 죽었다고 판정하면 그 run 에 재시도 마커 아티팩트를 남긴다. 실패 job 이 전부 그런 job 이면 PR 코드가 실행되기
// 전에 끝난 것이므로 조용한 방출과 같이 같은 head 당 한 번 재투입한다. 머지 큐는 실패한 필수 체크로 바로 방출하므로
// job 재실행(android-managed-device-retry.yml)으로는 복구되지 않고 재투입만이 수단이다.
//
// 판정 순서는 «실패 job 이 있는가» 가 먼저다. payload 의 reason 문자열은 GitHub 이 열거값을 문서화하지
// 않아 정본으로 삼지 않는다 — 실패 job 은 데이터로 확인하고, reason 은 «사람이 뺐다·이미 머지됐다» 를
// 거르는 데만 쓴다. 그 밖의 reason 은 전부 조용한 방출로 본다.
//
// 재투입 수단은 PR 이 GitHub 네이티브 스택의 일원인지로 갈린다(#2177). 스택 PR 은 GraphQL enqueuePullRequest 가
// UNPROCESSABLE("part of a stack and must be enqueued using the asynchronous merge REST API")로 거부되므로
// (0919 #2053, 0926 #2123 실측) REST merge-async 에 merge_action=merge_queue 로 넣는다. 판정표는 같다.
// merge-async 는 202 pending 으로 받으므로 결과(enqueued·merged·failed)를 폴링해 확인하고, failed·미확정은 job 을
// red 로 남긴다. 스택 위쪽 PR 은 자동 재투입하지 않는다 — merge-async 가 downstack 의 열린 PR 까지 함께 넣어
// 아래 PR 의 판정(실패 job 이면 재투입 안 함)을 덮기 때문이다. 위쪽 PR 에는 마커 없는 안내 코멘트만 남긴다.
//
// 재투입은 이 job 이 직접 하지 않고 merge-queue-requeue.yml 에 넘긴다(#2254). GITHUB_TOKEN 으로 큐에 넣으면 GitHub 이
// 그 이벤트로 워크플로를 띄우지 않아 merge_group CI 가 하나도 돌지 않고, 큐 맨 앞을 60분(check timeout) 붙잡은 뒤
// CI_TIMEOUT 으로 다시 빠지면서 뒤의 PR 들까지 다시 검증하게 만든다(1005 #2208 실측: 봇이 넣은 그룹만 체크 0개).
// 투입에는 사람 토큰(MERGE_QUEUE_TOKEN)이 필요한데 이 워크플로는 pull_request_target 브리지라 시크릿을 쓰지 않는다.
// workflow_dispatch 는 GITHUB_TOKEN 으로 보내도 실행되는 예외라서, 판정과 마커는 여기서 끝내고 투입만 dispatch 로 넘긴다.

import { readFile } from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import { pathToFileURL } from "node:url";

export const REQUEUE_MARKER_PREFIX = "<!-- merge-queue-dequeue:requeued head=";
// payload 의 reason 표기가 문서화돼 있지 않아(timeline 은 failed_checks·merged 소문자) 대문자로 맞춰 비교한다.
export const NO_ACTION_REASONS = new Set(["MANUAL", "ALREADY_MERGED", "MERGE", "MERGED"]);
const FAILED_JOB_CONCLUSIONS = new Set(["failure", "timed_out"]);
const QUEUE_RUNS_TO_INSPECT = 10;
// 재시도 마커 아티팩트 이름의 device → 그 마커가 증명하는 job 이름. android-managed-device-retry.yml 과 같은 표다.
export const MANAGED_DEVICE_RETRY_JOBS = new Map([
    ["api30", "Pixel 2 API 30 androidTest"],
    ["api34", "Pixel 2 API 34 accessibility smoke"],
]);
const defaultSleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

export function queueBranchPrefix(baseRef, number) {
    return `gh-readonly-queue/${baseRef}/pr-${number}-`;
}

/**
 * 이 PR 의 큐 브랜치에서 돈 merge_group run 중 실패·타임아웃으로 끝난 job 만 모은다.
 *
 * [since] 는 마지막 큐 투입 시각이다 — 큐 브랜치 이름은 head 가 아니라 base SHA 를 달고 있어 며칠 전
 * 투입의 실패 run 도 같은 접두어로 잡힌다(0904 #1582 실측). 그 이전 run 은 이번 방출과 무관하다.
 */
export function collectFailedJobs({ runs, jobsByRunId, artifactsByRunId = new Map(), baseRef, number, since }) {
    const prefix = queueBranchPrefix(baseRef, number);
    const failed = [];
    for (const run of runs) {
        if (typeof run.head_branch !== "string" || !run.head_branch.startsWith(prefix)) continue;
        if (since && typeof run.created_at === "string" && run.created_at < since) continue;
        const provenJobNames = provenInfrastructureJobNames(run, artifactsByRunId.get(run.id) ?? []);
        for (const job of jobsByRunId.get(run.id) ?? []) {
            if (!FAILED_JOB_CONCLUSIONS.has(job.conclusion)) continue;
            failed.push({
                infrastructure: provenJobNames.has(job.name),
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

/**
 * run 의 재시도 마커 아티팩트가 증명하는 job 이름들. 마커는 분류기가 retryable 로 판정했을 때만 올라가고
 * 이름에 run id·attempt 가 박혀 있어, 같은 run·attempt 의 이름만 받는다.
 */
export function provenInfrastructureJobNames(run, artifacts) {
    const names = new Set();
    for (const [device, jobName] of MANAGED_DEVICE_RETRY_JOBS) {
        const expected = `android-managed-device-retry-${device}-${run.id}-${run.run_attempt ?? 1}`;
        if (artifacts.some((artifact) => artifact.name === expected && !artifact.expired)) names.add(jobName);
    }
    return names;
}

/** 실패 job 이 하나 이상이고 전부 증명된 인프라 실패인가. */
export function onlyInfrastructureFailures(failedJobs) {
    return failedJobs.length > 0 && failedJobs.every((job) => job.infrastructure === true);
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
 * 1. 실패 job 이 있고 그중 하나라도 인프라로 증명되지 않았다 → comment-failure (재투입 없음)
 * 2. reason 이 MANUAL·ALREADY_MERGED·MERGE → none
 * 3. 실패 job 이 전부 증명된 인프라 실패다 → 같은 head 첫 번째면 requeue, 이미 재투입했으면 comment-failure
 * 4. 같은 head 에서 이미 재투입했다 → comment-give-up
 * 5. 그 밖 → requeue
 */
export function decide({ reason, failedJobs, requeueCount }) {
    if (failedJobs.length > 0 && !onlyInfrastructureFailures(failedJobs)) return "comment-failure";
    if (NO_ACTION_REASONS.has(String(reason ?? "").toUpperCase())) return "none";
    if (failedJobs.length > 0) return requeueCount >= 1 ? "comment-failure" : "requeue";
    if (requeueCount >= 1) return "comment-give-up";
    return "requeue";
}

const shortSha = (sha) => sha.slice(0, 7);

/**
 * 스택 PR 은 gh pr merge 가 "Auto-merge is not supported for stacked pull requests" 로 거부하므로 gh stack merge 를 안내한다.
 * gh stack 은 --repo 플래그가 없어(gh 2.85 실측) 저장소 체크아웃 안에서 번호로 부른다.
 */
export function manualEnqueueCommand({ repository, number, stack = false }) {
    return stack ? `gh stack merge ${number} --yes` : `gh pr merge ${number} --repo ${repository}`;
}

export function renderFailureComment({ reason, headSha, failedJobs, repository, number, stack = false }) {
    const lines = failedJobs.map((job) => `- [${job.runName} / ${job.jobName}](${job.jobUrl}) — ${job.conclusion}${job.infrastructure ? " (인프라 실패로 분류됨)" : ""}`);
    return [
        "### merge queue 방출 — merge group CI 실패",
        "",
        `사유 \`${reason}\` · head \`${shortSha(headSha)}\`. 재투입하지 않았다.`,
        "",
        "실패한 job:",
        ...lines,
        "",
        `고친 뒤 \`${manualEnqueueCommand({ repository, number, stack })}\` 로 다시 투입한다.`,
    ].join("\n");
}

export function renderRequeueComment({ reason, headSha, failedJobs = [] }) {
    const why = failedJobs.length > 0
        ? "merge group 실패 job 이 전부 테스트 전 인프라 실패로 분류됨 → 한 번 재투입했다."
        : "merge group 실패 job 없음 → 조용한 방출로 보고 한 번 재투입했다.";
    return [
        requeueMarker(headSha),
        "### merge queue 방출 — 재투입했다",
        "",
        `사유 \`${reason}\` · head \`${shortSha(headSha)}\` · ${why}`,
        ...failedJobs.map((job) => `- [${job.runName} / ${job.jobName}](${job.jobUrl}) — ${job.conclusion}`),
        "같은 head 에서 또 방출되면 재투입하지 않고 여기에 남긴다.",
    ].join("\n");
}

export function renderGiveUpComment({ reason, headSha, repository, number, stack = false }) {
    return [
        "### merge queue 방출 — 같은 head 두 번째, 재투입하지 않는다",
        "",
        `사유 \`${reason}\` · head \`${shortSha(headSha)}\`. 실패 job 은 없다.`,
        "`gh run list --event merge_group` 으로 merge group run 을 확인하고 수동으로 투입한다.",
        `확인한 뒤 \`${manualEnqueueCommand({ repository, number, stack })}\` 로 다시 투입한다.`,
    ].join("\n");
}

export function renderStackUpperComment({ reason, headSha, repository, number }) {
    return [
        "### merge queue 방출: 스택 위쪽 PR",
        "",
        `사유 \`${reason}\` · head \`${shortSha(headSha)}\`. 스택 위쪽 PR 이라 자동 재투입하지 않았다.`,
        "이 PR 을 재투입하면 아래의 열린 PR 까지 큐에 다시 넣어 아래 PR 의 판정을 덮는다.",
        `스택 밑단부터 확인한 뒤 \`${manualEnqueueCommand({ repository, number, stack: true })}\` 로 다시 투입한다.`,
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
                    stack { number baseRefName }
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

/** GitHub 네이티브 스택의 일원인가. 비스택 PR 은 stack 이 null 로 온다(0926 #2176, #2171 실측). */
export function isStackMember(pullRequest) {
    return pullRequest?.stack != null;
}

/** 스택의 기준 브랜치를 base 로 삼는 PR 만 밑단이다(2026-09-27 #2177). */
export function isStackBottom(pullRequest) {
    return isStackMember(pullRequest) && pullRequest.baseRefName === pullRequest.stack.baseRefName;
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
    const artifactsByRunId = new Map();
    for (const run of runs) {
        const jobs = await api(`/repos/${repository}/actions/runs/${run.id}/jobs?per_page=100`);
        jobsByRunId.set(run.id, jobs.jobs ?? []);
        // 마커를 남길 수 있는 run 만 아티팩트를 본다 — 실패한 GMD job 이 없으면 볼 필요가 없다.
        const hasFailedManagedDeviceJob = (jobs.jobs ?? []).some((job) =>
            FAILED_JOB_CONCLUSIONS.has(job.conclusion) && [...MANAGED_DEVICE_RETRY_JOBS.values()].includes(job.name));
        if (hasFailedManagedDeviceJob) {
            const artifacts = await api(`/repos/${repository}/actions/runs/${run.id}/artifacts?per_page=100`);
            artifactsByRunId.set(run.id, artifacts.artifacts ?? []);
        }
    }
    return collectFailedJobs({ runs, jobsByRunId, artifactsByRunId, baseRef, number, since });
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

/**
 * 스택 PR 의 큐 투입. GraphQL enqueuePullRequest 는 스택 PR 을 거부하므로 REST 비동기 머지에
 * merge_action=merge_queue 를 준다. 요청한 PR 과 downstack 의 열린 PR 을 모두 함께 넣는다(2026-09-27 #2177).
 * 새 요청은 202 pending 으로 받아 details.uuid 를 GET 폴링해 enqueued·merged·failed 를 확인한다.
 * 이미 큐에 있거나 머지됐으면 200 enqueued·merged, 같은 PR 의 다른 비동기 요청이 pending 이면 409 다.
 * sha 는 판정한 head 로 고정한다. head 가 어긋나면 요청이 거부되거나 배경에서 취소돼 failed 로 끝난다.
 * failed 는 예외로 올리고, uuid 가 없거나 폴링 상한까지 미확정이면 live 큐 상태를 다시 확인하게 한다.
 */
export async function enqueueStacked(api, repository, number, headSha, { sleep = defaultSleep, pollIntervalMs = 5000, maxPolls = 24 } = {}) {
    const apiPath = `/repos/${repository}/pulls/${number}/merge-async`;
    let result = await api(apiPath, {
        method: "PUT",
        body: { merge_action: "merge_queue", sha: headSha },
    });
    const uuid = result?.details?.uuid;
    for (let polls = 0; ; polls += 1) {
        if (result?.status === "enqueued" || result?.status === "merged") return result;
        if (result?.status === "failed") {
            throw new Error(`merge-async 실패: ${JSON.stringify(result)}`);
        }
        if (result?.status !== "pending" || !uuid || polls >= maxPolls) {
            return { ...result, unresolved: true };
        }
        await sleep(pollIntervalMs);
        result = await api(`${apiPath}/${uuid}`);
    }
}

export const REQUEUE_WORKFLOW_FILE = "merge-queue-requeue.yml";

/** 재투입 워크플로를 띄운다. 입력의 head 로 고정해 그사이 새 커밋이 올라오면 그쪽이 투입하지 않는다. */
export async function dispatchRequeue(api, repository, { number, headSha, ref }) {
    await api(`/repos/${repository}/actions/workflows/${REQUEUE_WORKFLOW_FILE}/dispatches`, {
        method: "POST",
        body: { ref, inputs: { pull_request_number: String(number), head_sha: headSha } },
    });
}

/** 큐 투입 자체. 스택 PR 은 merge-async, 나머지는 GraphQL 이다(#2177). [api] 의 토큰 주인이 투입 주체가 된다. */
export async function performRequeue({ api, repository, number, live, logger = console, sleep = defaultSleep }) {
    const headSha = live.headRefOid;
    if (isStackMember(live)) {
        const enqueueResult = await enqueueStacked(api, repository, number, headSha, { sleep });
        if (enqueueResult.unresolved) {
            const refreshed = await fetchLivePullRequest(api, repository, number);
            if (!refreshed?.mergeQueueEntry) {
                throw new Error(`merge-async 결과 미확정: ${JSON.stringify(enqueueResult)} 큐에도 없다`);
            }
            logger.log(`#${number} 스택 ${live.stack?.number ?? "?"} 재투입(merge-async) → live 재조회로 큐 진입 확인(${refreshed.mergeQueueEntry.state})`);
        } else {
            logger.log(`#${number} 스택 ${live.stack?.number ?? "?"} 재투입(merge-async) → ${enqueueResult?.status ?? "?"} ${enqueueResult?.details?.message ?? ""}`.trimEnd());
        }
        return "merge-async";
    }
    const entry = await enqueue(api, live.id);
    logger.log(`#${number} 재투입 → ${entry?.state ?? "?"} position=${entry?.position ?? "?"}`);
    return "graphql";
}

/**
 * merge-queue-requeue.yml 쪽. dispatch 를 받은 뒤 live 상태를 다시 보고, 판정한 head 그대로 열려 있고 큐 밖일 때만 넣는다.
 * 판정·마커는 방출 처리 job 이 이미 끝냈으므로 여기서는 코멘트를 쓰지 않는다.
 */
export async function requeueDispatched({ api, repository, number, headSha, logger = console, sleep = defaultSleep }) {
    const live = await fetchLivePullRequest(api, repository, number);
    if (!live) throw new Error(`#${number} 를 찾을 수 없습니다`);
    let skip = null;
    if (live.state !== "OPEN") skip = live.state;
    else if (live.mergeQueueEntry) skip = "already-queued";
    else if (live.headRefOid !== headSha) skip = "head-changed";
    else if (isStackMember(live) && !isStackBottom(live)) skip = "stack-upper";
    if (skip) {
        logger.log(`#${number} 재투입 생략(${skip}) — 판정 head ${shortSha(headSha)}, 지금 head ${shortSha(live.headRefOid ?? "")}`);
        return { action: "none", why: skip };
    }
    const enqueueMethod = await performRequeue({ api, repository, number, live, logger, sleep });
    return { action: "requeue", enqueueMethod };
}

export function renderMissingQueueTokenComment({ headSha, repository, number, stack = false }) {
    return [
        "### merge queue 방출: 재투입하지 못했다",
        "",
        `head \`${shortSha(headSha)}\`. \`MERGE_QUEUE_TOKEN\` 시크릿이 없다.`,
        "GITHUB_TOKEN 으로 넣으면 merge_group CI 가 돌지 않아 60분 뒤 CI_TIMEOUT 으로 다시 빠지므로 넣지 않았다.",
        `\`${manualEnqueueCommand({ repository, number, stack })}\` 로 직접 투입한다.`,
    ].join("\n");
}

export async function handleDequeue({
    api,
    repository,
    number,
    reason,
    dryRun = false,
    logger = console,
    sleep = defaultSleep,
    requeue = null,
}) {
    const live = await fetchLivePullRequest(api, repository, number);
    if (!live) throw new Error(`#${number} 를 찾을 수 없습니다`);
    if (live.state !== "OPEN") {
        logger.log(`#${number} 는 ${live.state} — 아무것도 하지 않는다`);
        return { action: "none", why: live.state, stack: live.stack?.number ?? null };
    }
    if (live.mergeQueueEntry) {
        logger.log(`#${number} 는 이미 큐에 있다(${live.mergeQueueEntry.state}) — 아무것도 하지 않는다`);
        return { action: "none", why: "already-queued", stack: live.stack?.number ?? null };
    }

    const headSha = live.headRefOid;
    const stack = isStackMember(live);
    const since = lastEnqueuedAt(live);
    const failedJobs = await fetchQueueFailedJobs(api, repository, { baseRef: live.baseRefName, number, since });
    const comments = await fetchComments(api, repository, number);
    const requeueCount = countRequeuesForHead(comments, headSha);
    let action = decide({ reason, failedJobs, requeueCount });
    // 위쪽 PR 의 재투입이 아래 PR 의 실패 판정을 덮지 않게 밑단에서만 자동 재투입한다(2026-09-27 #2177).
    if (action === "requeue" && stack && !isStackBottom(live)) action = "comment-stack-upper";
    logger.log(`#${number} reason=${reason} head=${shortSha(headSha)} 스택=${stack ? live.stack?.number ?? "?" : "없음"} 마지막투입=${since ?? "?"} 실패job=${failedJobs.length} 재투입횟수=${requeueCount} → ${action}`);

    const result = { action, failedJobs, requeueCount, stack: live.stack?.number ?? null };
    if (dryRun) return { ...result, dryRun: true };

    if (action === "comment-failure") {
        await postComment(api, repository, number, renderFailureComment({ reason, headSha, failedJobs, repository, number, stack }));
    } else if (action === "comment-give-up") {
        await postComment(api, repository, number, renderGiveUpComment({ reason, headSha, repository, number, stack }));
    } else if (action === "comment-stack-upper") {
        await postComment(api, repository, number, renderStackUpperComment({ reason, headSha, repository, number }));
    } else if (action === "requeue") {
        // 마커를 먼저 남긴다 — 재투입 뒤 코멘트가 실패하면 다음 방출에서 두 번째 재투입이 나간다.
        await postComment(api, repository, number, renderRequeueComment({ reason, headSha, failedJobs }));
        result.enqueueMethod = requeue
            ? await requeue({ number, headSha, live })
            : await performRequeue({ api, repository, number, live, logger, sleep });
    }
    return result;
}

function requiredEnv(name) {
    const value = process.env[name];
    if (!value) throw new Error(`${name} 가 필요합니다.`);
    return value;
}

async function requeueMain(repository) {
    const commentApi = createApi(requiredEnv("GITHUB_TOKEN"));
    const number = Number(process.env.PULL_REQUEST_NUMBER);
    const headSha = requiredEnv("HEAD_SHA");
    if (!Number.isInteger(number) || number <= 0) {
        throw new Error("PULL_REQUEST_NUMBER 가 양의 정수여야 합니다.");
    }
    const queueToken = process.env.MERGE_QUEUE_TOKEN;
    if (!queueToken) {
        const live = await fetchLivePullRequest(commentApi, repository, number);
        await postComment(commentApi, repository, number, renderMissingQueueTokenComment({ headSha, repository, number, stack: isStackMember(live) }));
        throw new Error("MERGE_QUEUE_TOKEN 시크릿이 없어 재투입하지 못했다.");
    }
    const result = await requeueDispatched({ api: createApi(queueToken), repository, number, headSha });
    console.log(`merge-queue-requeue: #${number} head=${shortSha(headSha)} → ${result.action}${result.why ? ` (${result.why})` : ""}${result.enqueueMethod ? ` (${result.enqueueMethod})` : ""}`);
}

async function main() {
    const token = process.env.GITHUB_TOKEN;
    const repository = process.env.GITHUB_REPOSITORY;
    if (!token || !repository) {
        throw new Error("GITHUB_TOKEN·GITHUB_REPOSITORY 가 필요합니다.");
    }
    if (process.env.MODE === "requeue") {
        await requeueMain(repository);
        return;
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
    const ref = requiredEnv("DEFAULT_BRANCH");
    const requeue = async ({ headSha }) => {
        await dispatchRequeue(api, repository, { number, headSha, ref });
        return "dispatch";
    };
    const result = await handleDequeue({ api, repository, number, reason, dryRun: process.env.DRY_RUN === "true", requeue });
    const summary = `merge-queue-dequeue: #${number} reason=${reason} → ${result.action}${result.dryRun ? " (dry-run)" : ""}${result.stack !== null ? ` (스택 ${result.stack}, ${result.enqueueMethod ?? "판정만"})` : ""}`;
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
