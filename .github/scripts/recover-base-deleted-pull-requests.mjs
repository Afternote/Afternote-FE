#!/usr/bin/env node

// base 브랜치 삭제로 자동 폐쇄된 PR 을 되살린다 (#2076).
//
// 브랜치를 지우면 그 브랜치를 base 로 하던 열린 PR 이 GitHub 정책에 따라 즉시 닫힌다. 닫는
// 코멘트도 리뷰도 없이 base_ref_deleted 다음 closed 두 이벤트만 남는다. 2026-09-11 22:17:52Z 에
// feat/1762-draft-resume 가 지워지며 1초 뒤 승인까지 받은 #1626 이 그렇게 사라졌고, 그 위에
// 쌓여 있던 일곱 건이 develop 에 도달할 경로를 잃은 채 이틀 동안 아무에게도 보이지 않았다.
//
// 판정 기준은 "닫힌 시각이 삭제와 가깝다" 가 아니라 "그 닫힘의 원인이 base 삭제다" 이다. PR
// 타임라인에서 마지막 closed 바로 앞에 base_ref_deleted 가 붙어 있는 경우만 되살린다. 사람이
// 손으로 닫은 PR, 이미 머지된 PR, 다른 base 의 PR 은 어느 것도 이 서명을 갖지 않는다.
//
// 되살릴 수 없는 경우(같은 이름의 브랜치가 다시 올라오지 않아 base 가 없는 경우)에는 조용히
// 지나가지 않고 추적 이슈를 열어 끊긴 사슬을 기록한다.

import path from "node:path";
import process from "node:process";
import { pathToFileURL } from "node:url";

/** base_ref_deleted 와 closed 사이의 허용 간격. 실측은 1초였다. */
export const AUTO_CLOSE_WINDOW_SECONDS = 120;
/** 이번 삭제와 무관한 옛 폐쇄를 되살리지 않도록 두는 조회 창. */
export const RECENT_CLOSE_WINDOW_MINUTES = 60;
export const REOPEN_MARKER_PREFIX = "<!-- base-delete-recovery:reopened ref=";
export const TRACKING_MARKER_PREFIX = "<!-- base-delete-recovery:unrecoverable ref=";
export const TRACKING_ISSUE_LABELS = ["maintenance", "area:platform"];
// 이슈 양식(.github/ISSUE_TEMPLATE/issue.yml)의 선택지 원문이다. issue-metadata-guard 는 이 두
// 줄을 읽어 라벨과 담당자를 정하고, 읽지 못하면 이슈를 자동으로 닫는다. 추적 기록이 그렇게
// 닫히면 기록을 남긴 의미가 없어서 양식을 그대로 채운다.
export const TRACKING_ISSUE_TYPE_OPTION = "maintenance — CI·빌드·의존성·저장소 운영 유지보수";
export const TRACKING_ISSUE_MODULE_OPTION = "platform — CI·빌드·릴리스·저장소 운영";

const TIMELINE_PAGE_LIMIT = 20;
const ISSUE_PAGE_LIMIT = 3;
const AUTO_CLOSE_SETTLE_ATTEMPTS = 3;
const AUTO_CLOSE_SETTLE_DELAY_MS = 15_000;
const BASE_REF_RESTORE_ATTEMPTS = 5;
const BASE_REF_RESTORE_DELAY_MS = 20_000;

export function reopenMarker(ref) {
    return `${REOPEN_MARKER_PREFIX}${ref} -->`;
}

export function trackingMarker(ref) {
    return `${TRACKING_MARKER_PREFIX}${ref} -->`;
}

function parseTime(value) {
    const parsed = Date.parse(String(value ?? ""));
    return Number.isFinite(parsed) ? parsed : null;
}

/**
 * 이 PR 이 이번 base 삭제로 닫혔는가.
 *
 * 네 가지를 모두 만족해야 한다. base 가 지워진 그 브랜치이고, 머지되지 않았고, 지금 닫혀 있고,
 * 마지막 closed 이벤트 바로 앞이 base_ref_deleted 다. 마지막 조건이 이 판정의 본체다. 나머지
 * 셋만으로는 브랜치를 지우기 직전에 사람이 손으로 닫은 PR 과 구별되지 않는다.
 */
export function autoClosedByBaseDeletion({ pullRequest, timeline = [], ref, observedAt }) {
    const observed = parseTime(observedAt);
    if (observed === null) {
        throw new Error("observedAt 이 ISO 8601 시각이어야 합니다.");
    }
    if (pullRequest?.base?.ref !== ref) {
        return { eligible: false, reason: `base 가 ${pullRequest?.base?.ref ?? "없음"}` };
    }
    if (pullRequest.merged_at) {
        return { eligible: false, reason: "이미 머지된 PR" };
    }
    if (pullRequest.state !== "closed") {
        return { eligible: false, reason: `상태가 ${pullRequest.state}` };
    }

    const closedAt = parseTime(pullRequest.closed_at);
    if (closedAt === null) {
        return { eligible: false, reason: "closed_at 이 없음" };
    }
    if (closedAt < observed - RECENT_CLOSE_WINDOW_MINUTES * 60_000) {
        return { eligible: false, reason: "이번 삭제와 무관한 옛 폐쇄" };
    }

    // committed 같은 일부 타임라인 항목에는 created_at 이 없다. 시각으로 줄을 세울 수 없는
    // 항목은 순서 판정에서 뺀다.
    const events = timeline
        .filter((item) => item && typeof item.event === "string" && parseTime(item.created_at) !== null)
        .map((item) => ({ event: item.event, at: parseTime(item.created_at) }))
        .sort((left, right) => left.at - right.at);

    const closedIndex = events.findLastIndex((item) => item.event === "closed");
    if (closedIndex < 0) {
        return { eligible: false, reason: "closed 이벤트가 없음" };
    }
    const deletion = events[closedIndex - 1];
    if (!deletion || deletion.event !== "base_ref_deleted") {
        return { eligible: false, reason: "base 삭제가 아닌 폐쇄" };
    }
    if (events[closedIndex].at - deletion.at > AUTO_CLOSE_WINDOW_SECONDS * 1000) {
        return { eligible: false, reason: "base 삭제와 폐쇄 사이가 너무 멀다" };
    }

    return { eligible: true, reason: "base 삭제로 자동 폐쇄됨" };
}

export function renderReopenComment({ ref, deletedBy, runUrl }) {
    return [
        reopenMarker(ref),
        "### base 브랜치 삭제로 닫혔던 PR 을 다시 열었다",
        "",
        `base 브랜치 \`${ref}\` 가 삭제되면서 이 PR 이 GitHub 정책에 따라 자동으로 닫혔다. 그 닫힘에는 리뷰도 코멘트도 남지 않는다.`,
        "같은 이름의 브랜치가 다시 올라와 base 가 살아 있어 이 워크플로가 PR 을 다시 열었다.",
        "",
        "리뷰 승인과 스택 연결은 닫히기 전 그대로가 아닐 수 있다. 머지 전에 base 와 스택 순서를 확인한다.",
        "이 reopen 은 GITHUB_TOKEN 이 했다. 그 이벤트로는 새 워크플로 run 이 만들어지지 않으니, reopened 에 걸린 알림이나 체크가 필요하면 커밋을 다시 밀거나 run 을 재실행한다.",
        "",
        `삭제한 사람: @${deletedBy}`,
        `복구 run: ${runUrl}`,
    ].join("\n");
}

function renderLostPullRequest({ pullRequest, dependents }) {
    const lines = [
        `- #${pullRequest.number} ${pullRequest.title}`,
        `  - head \`${pullRequest.head?.ref ?? "?"}\` · 닫힌 시각 \`${pullRequest.closed_at}\``,
        `  - 이 PR 이 기록한 base SHA \`${pullRequest.base?.sha ?? "?"}\``,
    ];
    if (dependents.length > 0) {
        lines.push(`  - 이 PR 의 head 를 base 로 하던 열린 PR: ${dependents.map((number) => `#${number}`).join(", ")}`);
    }
    return lines;
}

export function renderTrackingIssue({ ref, repository, deletedBy, runUrl, lost }) {
    const numbers = lost.map(({ pullRequest }) => `#${pullRequest.number}`).join(", ");
    const restoreSha = lost[0]?.pullRequest?.base?.sha ?? "<복구할 SHA>";
    const reopenCommands = lost.map(
        ({ pullRequest }) => `gh pr reopen ${pullRequest.number} --repo ${repository}`,
    );

    return {
        title: `maintenance(platform): 삭제된 base 브랜치 ${ref} 의 PR 을 되살리지 못했다`,
        labels: TRACKING_ISSUE_LABELS,
        body: [
            trackingMarker(ref),
            "### 작업 유형",
            "",
            TRACKING_ISSUE_TYPE_OPTION,
            "",
            "### 주 담당 모듈",
            "",
            TRACKING_ISSUE_MODULE_OPTION,
            "",
            "### 개요",
            "",
            `브랜치 \`${ref}\` 가 삭제되면서 이 브랜치를 base 로 하던 PR 이 자동으로 닫혔다. 대상: ${numbers}`,
            "같은 이름의 브랜치가 다시 올라오지 않아 base 가 없고, base 없이는 reopen 이 거절된다. 그래서 자동 복구 대신 이 기록을 남긴다.",
            "",
            "#### 끊긴 지점",
            "",
            ...lost.flatMap(renderLostPullRequest),
            "",
            "#### 되살리는 법",
            "",
            "1. 같은 이름으로 브랜치를 다시 올린다. 아래 SHA 는 닫힌 PR 이 마지막으로 기록한 base 라 브랜치의 최종 상태보다 뒤처져 있을 수 있으니 먼저 확인한다.",
            "",
            "```",
            `git push origin ${restoreSha}:refs/heads/${ref}`,
            "```",
            "",
            "2. 닫힌 PR 을 다시 연다.",
            "",
            "```",
            ...reopenCommands,
            "```",
            "",
            "3. 스택이 있었다면 `gh stack link --base develop <아래 PR> <위 PR>` 로 순서를 다시 건다.",
            "",
            "#### 완료 조건",
            "",
            "닫힌 PR 이 다시 열려 원래 사슬로 돌아가거나, 되살리지 않기로 하고 그 결정이 이 이슈에 남는다.",
            "",
            "### 참고",
            "",
            `삭제한 사람: @${deletedBy}`,
            `감지 run: ${runUrl}`,
        ].join("\n"),
    };
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
            const error = new Error(`GitHub API ${method} ${apiPath} 실패: ${response.status} ${detail}`);
            error.status = response.status;
            throw error;
        }
        if (response.status === 204) return null;
        return response.json();
    };
}

function encodeRef(ref) {
    return ref.split("/").map(encodeURIComponent).join("/");
}

// unref 를 걸면 이 타이머가 이벤트 루프를 붙잡지 않아 대기 중에 프로세스가 조용히 끝난다.
const defaultSleep = (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds));

export async function fetchPullRequestsForBase(api, repository, ref) {
    // 방금 닫힌 PR 이 앞에 오도록 최근 갱신순으로 받는다.
    return api(
        `/repos/${repository}/pulls?state=all&base=${encodeRef(ref)}&per_page=100&sort=updated&direction=desc`,
    );
}

export async function fetchTimeline(api, repository, number) {
    const events = [];
    for (let page = 1; page <= TIMELINE_PAGE_LIMIT; page += 1) {
        const batch = await api(`/repos/${repository}/issues/${number}/timeline?per_page=100&page=${page}`);
        events.push(...batch);
        if (batch.length < 100) break;
    }
    return events;
}

/** 이 PR 의 head 를 base 로 하던 열린 PR. 삭제로 끊긴 사슬의 위쪽이다. */
export async function fetchDependents(api, repository, headRef) {
    if (!headRef) return [];
    const dependents = await api(`/repos/${repository}/pulls?state=open&base=${encodeRef(headRef)}&per_page=100`);
    return dependents.map((pullRequest) => pullRequest.number);
}

/**
 * base 삭제로 닫힌 PR 을 모은다.
 *
 * 삭제 이벤트가 자동 폐쇄보다 먼저 도착할 수 있다. 같은 base 의 PR 이 아직 열려 있는 동안에는
 * 폐쇄가 아직 반영되지 않았다고 보고 짧게 다시 본다.
 */
export async function collectAutoClosedPullRequests({
    api,
    repository,
    ref,
    observedAt,
    sleep = defaultSleep,
    logger = console,
}) {
    let lost = [];
    for (let attempt = 1; attempt <= AUTO_CLOSE_SETTLE_ATTEMPTS; attempt += 1) {
        const pullRequests = await fetchPullRequestsForBase(api, repository, ref);
        lost = [];
        for (const pullRequest of pullRequests) {
            if (pullRequest.base?.ref !== ref || pullRequest.state !== "closed" || pullRequest.merged_at) {
                continue;
            }
            const timeline = await fetchTimeline(api, repository, pullRequest.number);
            const verdict = autoClosedByBaseDeletion({ pullRequest, timeline, ref, observedAt });
            logger.log(`#${pullRequest.number}: ${verdict.reason}`);
            if (verdict.eligible) {
                lost.push({ pullRequest, dependents: await fetchDependents(api, repository, pullRequest.head?.ref) });
            }
        }

        const stillOpen = pullRequests.filter(
            (pullRequest) => pullRequest.base?.ref === ref && pullRequest.state === "open",
        );
        if (lost.length > 0 || stillOpen.length === 0 || attempt === AUTO_CLOSE_SETTLE_ATTEMPTS) {
            break;
        }
        logger.log(`base \`${ref}\` 의 PR ${stillOpen.length}건이 아직 열려 있다. 자동 폐쇄 반영을 기다린다`);
        await sleep(AUTO_CLOSE_SETTLE_DELAY_MS);
    }
    return lost;
}

/**
 * base 가 같은 이름으로 다시 올라왔는가.
 *
 * 이름을 옮기는 중이었다면 삭제 직후에 같은 이름이 다시 밀린다. 그 push 가 이 워크플로보다
 * 늦을 수 있어 한 번만 보고 없다고 판정하지 않는다. 404 가 아닌 실패는 그대로 던진다.
 */
export async function waitForBaseRef({
    api,
    repository,
    ref,
    attempts = BASE_REF_RESTORE_ATTEMPTS,
    delayMs = BASE_REF_RESTORE_DELAY_MS,
    sleep = defaultSleep,
    logger = console,
}) {
    for (let attempt = 1; attempt <= attempts; attempt += 1) {
        try {
            await api(`/repos/${repository}/git/ref/heads/${encodeRef(ref)}`);
            return true;
        } catch (error) {
            if (error.status !== 404) throw error;
        }
        if (attempt < attempts) {
            logger.log(`base \`${ref}\` 가 아직 없다. 다시 올라오기를 기다린다 (${attempt}/${attempts})`);
            await sleep(delayMs);
        }
    }
    return false;
}

async function findTrackingIssue(api, repository, ref) {
    const marker = trackingMarker(ref);
    for (let page = 1; page <= ISSUE_PAGE_LIMIT; page += 1) {
        const batch = await api(`/repos/${repository}/issues?state=open&per_page=100&page=${page}`);
        const found = batch.find((issue) => !issue.pull_request && String(issue.body ?? "").includes(marker));
        if (found) return found;
        if (batch.length < 100) break;
    }
    return null;
}

export async function recoverBaseDeletedPullRequests({
    api,
    repository,
    ref,
    refType,
    deletedBy,
    runUrl,
    observedAt = new Date().toISOString(),
    sleep = defaultSleep,
    logger = console,
    dryRun = false,
}) {
    if (refType !== "branch") {
        logger.log(`${refType} 삭제는 대상이 아니다`);
        return { action: "none", why: "ref-type" };
    }

    const lost = await collectAutoClosedPullRequests({ api, repository, ref, observedAt, sleep, logger });
    if (lost.length === 0) {
        logger.log(`base \`${ref}\` 삭제로 닫힌 PR 없음`);
        return { action: "none", why: "no-auto-closed-pull-request" };
    }

    const numbers = lost.map(({ pullRequest }) => pullRequest.number);
    const baseRestored = await waitForBaseRef({ api, repository, ref, sleep, logger });
    if (dryRun) {
        return { action: baseRestored ? "reopen" : "track", dryRun: true, pullRequests: numbers };
    }

    if (baseRestored) {
        const comment = renderReopenComment({ ref, deletedBy, runUrl });
        for (const number of numbers) {
            // 코멘트보다 reopen 이 먼저다. 코멘트가 실패해도 PR 은 열려 있어야 한다.
            await api(`/repos/${repository}/pulls/${number}`, { method: "PATCH", body: { state: "open" } });
            await api(`/repos/${repository}/issues/${number}/comments`, { method: "POST", body: { body: comment } });
            logger.log(`#${number} 를 다시 열고 경위를 남겼다`);
        }
        return { action: "reopen", pullRequests: numbers };
    }

    const existing = await findTrackingIssue(api, repository, ref);
    const issue = renderTrackingIssue({ ref, repository, deletedBy, runUrl, lost });
    if (existing) {
        await api(`/repos/${repository}/issues/${existing.number}`, {
            method: "PATCH",
            body: { title: issue.title, body: issue.body },
        });
        logger.log(`추적 이슈 #${existing.number} 를 갱신했다`);
        return { action: "track", issueNumber: existing.number, pullRequests: numbers, reused: true };
    }

    const created = await api(`/repos/${repository}/issues`, {
        method: "POST",
        body: { title: issue.title, body: issue.body, labels: issue.labels },
    });
    logger.log(`추적 이슈 #${created.number} 를 열었다`);
    return { action: "track", issueNumber: created.number, pullRequests: numbers };
}

async function main() {
    const token = process.env.GITHUB_TOKEN;
    const repository = process.env.GITHUB_REPOSITORY;
    const ref = process.env.DELETED_REF;
    const refType = process.env.DELETED_REF_TYPE;
    if (!token || !repository || !ref || !refType) {
        throw new Error("GITHUB_TOKEN·GITHUB_REPOSITORY·DELETED_REF·DELETED_REF_TYPE 가 필요합니다.");
    }

    const result = await recoverBaseDeletedPullRequests({
        api: createApi(token),
        repository,
        ref,
        refType,
        deletedBy: process.env.DELETED_BY || "unknown",
        runUrl: process.env.RUN_URL || "",
        dryRun: process.env.DRY_RUN === "true",
    });

    const summary = `base-delete-recovery: ${ref} → ${result.action}${
        result.pullRequests?.length ? ` (${result.pullRequests.map((number) => `#${number}`).join(", ")})` : ""
    }${result.issueNumber ? ` 추적 이슈 #${result.issueNumber}` : ""}`;
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
