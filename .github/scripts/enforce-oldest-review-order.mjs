#!/usr/bin/env node

// 오래된 PR 을 내버려 둔 채 새 PR 만 리뷰하는 순서를 막는다.
//
// GitHub 은 리뷰 제출 버튼 자체를 가로막을 수 없다. 따라서 결정 리뷰가 제출되면
// 아직 판정이 필요한 더 오래된 PR 을 조회하고, 그런 PR 이 있으면 방금 리뷰를
// dismiss 한다. 판정 근거를 전부 읽기 전에는 쓰기 API 를 호출하지 않는다.

import fs from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import { pathToFileURL } from "node:url";

export const DECISIVE_REVIEW_STATES = new Set(["APPROVED", "CHANGES_REQUESTED"]);
export const WRITE_CAPABLE_PERMISSIONS = new Set(["write", "maintain", "admin"]);

const NON_WRITE_PERMISSIONS = new Set(["none", "read", "triage"]);
const PAGE_SIZE = 100;

function requiredString(value, label) {
    if (typeof value !== "string" || value.length === 0) {
        throw new Error(`${label} 값이 없거나 올바르지 않습니다.`);
    }
    return value;
}

function requiredInteger(value, label) {
    if (!Number.isInteger(value) || value < 1) {
        throw new Error(`${label} 값이 없거나 올바르지 않습니다.`);
    }
    return value;
}

function parseTimestamp(value, label) {
    const timestamp = Date.parse(requiredString(value, label));
    if (Number.isNaN(timestamp)) {
        throw new Error(`${label} 시각을 해석할 수 없습니다: ${value}`);
    }
    return timestamp;
}

function validateRepository(repository) {
    const value = requiredString(repository, "repository");
    if (!/^[^/\s]+\/[^/\s]+$/.test(value)) {
        throw new Error(`repository 형식이 올바르지 않습니다: ${value}`);
    }
    return value;
}

export function normalizeReviewState(state) {
    return requiredString(state, "review.state").toUpperCase();
}

export function isBotReviewer(user) {
    const login = requiredString(user?.login, "review.user.login");
    return user?.type?.toLowerCase() === "bot" || /\[bot\]$/i.test(login) ||
        login === "dependabot" || login === "github-actions";
}

/**
 * 웹훅만으로 안전하게 판단할 수 있는 제외 사유를 먼저 가른다.
 * 팀 권한은 author_association 이 아니라 REST permission 응답으로 별도 확인한다.
 */
export function classifyReviewEvent(event, repository) {
    validateRepository(repository);
    if (!event || typeof event !== "object") {
        throw new Error("GitHub 이벤트 payload 가 올바르지 않습니다.");
    }
    if (requiredString(event.action, "event.action") !== "submitted") {
        return { status: "skipped", reason: "submitted 이벤트가 아님" };
    }

    const payloadRepository = event.repository?.full_name;
    if (payloadRepository && payloadRepository !== repository) {
        throw new Error(`이벤트 저장소(${payloadRepository})와 실행 저장소(${repository})가 다릅니다.`);
    }

    const pullRequest = event.pull_request;
    const review = event.review;
    if (!pullRequest || !review) {
        throw new Error("pull_request 또는 review payload 가 없습니다.");
    }

    const number = requiredInteger(pullRequest.number, "pull_request.number");
    const createdAt = requiredString(pullRequest.created_at, "pull_request.created_at");
    parseTimestamp(createdAt, "pull_request.created_at");
    const headRepository = requiredString(
        pullRequest.head?.repo?.full_name,
        "pull_request.head.repo.full_name",
    );
    const reviewer = requiredString(review.user?.login, "review.user.login");
    const reviewId = requiredInteger(review.id, "review.id");
    const state = normalizeReviewState(review.state);

    if (!DECISIVE_REVIEW_STATES.has(state)) {
        return { status: "skipped", reason: `결정 리뷰가 아님(${state})` };
    }
    if (isBotReviewer(review.user)) {
        return { status: "skipped", reason: `봇 리뷰어(${reviewer})` };
    }
    if (headRepository !== repository) {
        return { status: "skipped", reason: `fork PR(${headRepository})` };
    }
    if (pullRequest.draft === true) {
        return { status: "skipped", reason: "draft PR" };
    }

    return {
        status: "eligible",
        context: { number, createdAt, reviewer, reviewId, state },
    };
}

export function classifyRepositoryPermission(permission) {
    const value = requiredString(permission, "repository permission").toLowerCase();
    if (WRITE_CAPABLE_PERMISSIONS.has(value)) {
        return "team";
    }
    if (NON_WRITE_PERMISSIONS.has(value)) {
        return "non-team";
    }
    throw new Error(`알 수 없는 repository permission 입니다: ${permission}`);
}

function normalizeOpenPullRequest(pullRequest) {
    if (!pullRequest || typeof pullRequest !== "object") {
        throw new Error("열린 PR 응답이 올바르지 않습니다.");
    }
    if (typeof pullRequest.draft !== "boolean") {
        throw new Error(`PR #${pullRequest.number ?? "?"}의 draft 값이 올바르지 않습니다.`);
    }
    if (!Array.isArray(pullRequest.requested_reviewers)) {
        throw new Error(`PR #${pullRequest.number ?? "?"}의 requested_reviewers 값이 올바르지 않습니다.`);
    }

    const createdAt = requiredString(pullRequest.created_at, "pull_request.created_at");
    const createdTimestamp = parseTimestamp(createdAt, "pull_request.created_at");
    return {
        number: requiredInteger(pullRequest.number, "pull_request.number"),
        title: requiredString(pullRequest.title, "pull_request.title"),
        author: requiredString(pullRequest.user?.login, "pull_request.user.login"),
        draft: pullRequest.draft,
        requestedReviewers: pullRequest.requested_reviewers.map((requested, index) =>
            requiredString(
                requested?.login,
                `pull_request.requested_reviewers[${index}].login`,
            ).toLowerCase()
        ),
        createdAt,
        createdTimestamp,
    };
}

export function comparePullRequestsOldestFirst(left, right) {
    const leftTimestamp = left.createdTimestamp ??
        parseTimestamp(left.createdAt ?? left.created_at, "pull_request.created_at");
    const rightTimestamp = right.createdTimestamp ??
        parseTimestamp(right.createdAt ?? right.created_at, "pull_request.created_at");
    if (leftTimestamp !== rightTimestamp) {
        return leftTimestamp - rightTimestamp;
    }
    return requiredInteger(left.number, "pull_request.number") -
        requiredInteger(right.number, "pull_request.number");
}

/** 현재 리뷰 대상보다 오래됐고, 리뷰어 자신이 작성하지 않은 ready PR 만 남긴다. */
export function selectOlderPullRequests(openPullRequests, currentPullRequest, reviewer) {
    if (!Array.isArray(openPullRequests)) {
        throw new Error("열린 PR 목록 응답이 배열이 아닙니다.");
    }
    const current = {
        number: requiredInteger(currentPullRequest?.number, "current pull_request.number"),
        createdTimestamp: parseTimestamp(
            currentPullRequest?.createdAt ?? currentPullRequest?.created_at,
            "current pull_request.created_at",
        ),
    };
    const reviewerLogin = requiredString(reviewer, "reviewer").toLowerCase();

    return openPullRequests
        .map(normalizeOpenPullRequest)
        .filter((pullRequest) => !pullRequest.draft)
        .filter((pullRequest) => pullRequest.number !== current.number)
        .filter((pullRequest) => pullRequest.author.toLowerCase() !== reviewerLogin)
        .filter((pullRequest) => comparePullRequestsOldestFirst(pullRequest, current) < 0)
        .sort(comparePullRequestsOldestFirst);
}

/**
 * review-debt-guard 와 같은 방식으로 리뷰어별 최신 결정만 남긴다.
 * 누군가의 최신 결정이 CHANGES_REQUESTED 면 그중 가장 늦은 시각을 기준으로 삼는다.
 */
export function analyzeDecisiveReviews(reviews) {
    if (!Array.isArray(reviews)) {
        throw new Error("리뷰 목록 응답이 배열이 아닙니다.");
    }

    const latestByReviewer = new Map();
    for (const review of reviews) {
        const state = normalizeReviewState(review?.state);
        if (!DECISIVE_REVIEW_STATES.has(state)) {
            continue;
        }
        const reviewer = requiredString(review.user?.login, "review.user.login").toLowerCase();
        const submittedAt = requiredString(review.submitted_at, "review.submitted_at");
        const submittedTimestamp = parseTimestamp(submittedAt, "review.submitted_at");
        const previous = latestByReviewer.get(reviewer);
        if (!previous || submittedTimestamp >= previous.submittedTimestamp) {
            latestByReviewer.set(reviewer, {
                reviewer,
                state,
                submittedAt,
                submittedTimestamp,
            });
        }
    }

    if (latestByReviewer.size === 0) {
        return { kind: "no-decisive-review", debt: true, blockedAt: null };
    }

    const outstanding = [...latestByReviewer.values()]
        .filter((review) => review.state === "CHANGES_REQUESTED")
        .sort((left, right) => left.submittedTimestamp - right.submittedTimestamp);
    if (outstanding.length === 0) {
        return { kind: "resolved", debt: false, blockedAt: null };
    }

    const latest = outstanding.at(-1);
    return {
        kind: "changes-requested",
        debt: false,
        blockedAt: latest.submittedAt,
        blockedTimestamp: latest.submittedTimestamp,
        outstandingReviews: outstanding,
    };
}

/**
 * 변경요청을 낸 뒤 현재 다시 요청받은 리뷰어만 고른다.
 * GitHub는 리뷰 제출 시 그 사람의 요청을 제거하므로, 같은 리뷰어가 현재 요청 목록에
 * 다시 있다면 마지막 결정 뒤 작성자가 명시적으로 재리뷰를 요청한 것이다.
 */
export function selectRequestedOutstandingReview(reviewState, requestedReviewers) {
    if (!Array.isArray(requestedReviewers)) {
        throw new Error("requested_reviewers 응답이 배열이 아닙니다.");
    }
    const active = new Set(requestedReviewers.map((reviewer, index) =>
        requiredString(reviewer, `requested_reviewers[${index}]`).toLowerCase()
    ));
    if (reviewState?.kind !== "changes-requested") {
        return null;
    }
    if (!Array.isArray(reviewState.outstandingReviews)) {
        throw new Error("미해소 변경요청 목록이 올바르지 않습니다.");
    }
    return reviewState.outstandingReviews
        .filter((review) => active.has(review.reviewer))
        .sort((left, right) => left.submittedTimestamp - right.submittedTimestamp)
        .at(-1) ?? null;
}

/** 병합 커밋(parents 2개 이상)은 변경요청 반영 커밋으로 세지 않는다. */
export function hasSubstantiveCommitAfter(commits, blockedAt) {
    if (!Array.isArray(commits)) {
        throw new Error("커밋 목록 응답이 배열이 아닙니다.");
    }
    const blockedTimestamp = parseTimestamp(blockedAt, "changes_requested.submitted_at");
    let found = false;

    // 응답 전체를 검증한 다음 결과를 돌려준다. 중간의 깨진 항목을 건너뛰고 dismiss 하면 안 된다.
    for (const commit of commits) {
        if (!Array.isArray(commit?.parents)) {
            throw new Error(`커밋 ${commit?.sha ?? "?"}의 parents 응답이 올바르지 않습니다.`);
        }
        if (commit.parents.length >= 2) {
            continue;
        }
        const committedAt = parseTimestamp(
            commit.commit?.committer?.date,
            `커밋 ${commit.sha ?? "?"}의 committer.date`,
        );
        if (committedAt > blockedTimestamp) {
            found = true;
        }
    }
    return found;
}

export function reviewDebtStatus(reviews, commits, requestedReviewers = []) {
    const reviewState = analyzeDecisiveReviews(reviews);
    if (reviewState.kind === "no-decisive-review") {
        return { debt: true, reason: "no-decisive-review" };
    }
    if (reviewState.kind === "resolved") {
        return { debt: false, reason: "resolved" };
    }
    const requestedReview = selectRequestedOutstandingReview(reviewState, requestedReviewers);
    if (!requestedReview) {
        return {
            debt: false,
            reason: "changes-requested-not-rerequested",
            blockedAt: reviewState.blockedAt,
        };
    }
    if (commits === undefined) {
        throw new Error("변경요청 이후 커밋 판정에 필요한 커밋 목록이 없습니다.");
    }
    const fixed = hasSubstantiveCommitAfter(commits, requestedReview.submittedAt);
    return {
        debt: fixed,
        reason: fixed
            ? "changes-requested-fixed-rerequested"
            : "changes-requested-rerequested-not-fixed",
        blockedAt: requestedReview.submittedAt,
        reviewer: requestedReview.reviewer,
    };
}

export function nextPageUrl(linkHeader) {
    if (linkHeader == null || linkHeader === "") {
        return null;
    }
    if (typeof linkHeader !== "string") {
        throw new Error("GitHub Link 헤더가 문자열이 아닙니다.");
    }
    for (const part of linkHeader.split(",")) {
        if (!/;\s*rel="?next"?(?:\s*;|\s*$)/i.test(part)) {
            continue;
        }
        const match = part.match(/<([^>]+)>/);
        if (!match) {
            throw new Error(`GitHub next Link 헤더를 해석할 수 없습니다: ${part.trim()}`);
        }
        return match[1];
    }
    return null;
}

function responseHeader(headers, name) {
    if (!headers) {
        return null;
    }
    if (typeof headers.get === "function") {
        return headers.get(name);
    }
    return headers[name] ?? headers[name.toLowerCase()] ?? null;
}

/** Link 헤더가 끝날 때까지 모든 REST collection 페이지를 읽는다. */
export async function paginateRest(client, apiPath) {
    const items = [];
    const seen = new Set();
    let next = requiredString(apiPath, "REST collection path");

    while (next) {
        if (seen.has(next)) {
            throw new Error(`GitHub REST 페이지네이션이 순환합니다: ${next}`);
        }
        seen.add(next);
        const response = await client.request(next, { method: "GET" });
        if (!Array.isArray(response?.data)) {
            throw new Error(`GitHub REST collection 응답이 배열이 아닙니다: ${next}`);
        }
        items.push(...response.data);
        next = nextPageUrl(responseHeader(response.headers, "link"));
    }
    return items;
}

function truncate(value, limit = 2_000) {
    const text = String(value ?? "");
    return text.length <= limit ? text : `${text.slice(0, limit)}...[truncated]`;
}

export function createGitHubClient({
    token,
    apiUrl = "https://api.github.com",
    fetchImpl = globalThis.fetch,
}) {
    requiredString(token, "GITHUB_TOKEN");
    if (typeof fetchImpl !== "function") {
        throw new Error("fetch 구현이 없습니다.");
    }
    const base = new URL(requiredString(apiUrl, "GITHUB_API_URL").replace(/\/+$/, "") + "/");

    return {
        async request(apiPath, { method = "GET", body } = {}) {
            const rawPath = requiredString(apiPath, "GitHub API path");
            const url = /^https?:\/\//i.test(rawPath)
                ? new URL(rawPath)
                : new URL(rawPath.replace(/^\/+/, ""), base);
            if (url.origin !== base.origin) {
                throw new Error(`GitHub API 페이지가 다른 origin 을 가리킵니다: ${url.origin}`);
            }

            const response = await fetchImpl(url, {
                method,
                headers: {
                    accept: "application/vnd.github+json",
                    authorization: `Bearer ${token}`,
                    "content-type": "application/json",
                    "x-github-api-version": "2022-11-28",
                },
                body: body === undefined ? undefined : JSON.stringify(body),
            });
            const text = await response.text();
            if (!response.ok) {
                throw new Error(
                    `GitHub API ${method} ${url.pathname} 실패: ${response.status} ${truncate(text)}`,
                );
            }

            let data = null;
            if (text.length > 0) {
                try {
                    data = JSON.parse(text);
                } catch (error) {
                    throw new Error(`GitHub API ${url.pathname} JSON 해석 실패: ${error.message}`);
                }
            }
            return { data, headers: response.headers };
        },
    };
}

export async function findOldestReviewDebt(client, repository, currentPullRequest, reviewer) {
    validateRepository(repository);
    const openPullRequests = await paginateRest(
        client,
        `/repos/${repository}/pulls?state=open&sort=created&direction=asc&per_page=${PAGE_SIZE}`,
    );
    const candidates = selectOlderPullRequests(openPullRequests, currentPullRequest, reviewer);

    for (const pullRequest of candidates) {
        const reviews = await paginateRest(
            client,
            `/repos/${repository}/pulls/${pullRequest.number}/reviews?per_page=${PAGE_SIZE}`,
        );
        const reviewState = analyzeDecisiveReviews(reviews);
        if (reviewState.kind === "no-decisive-review") {
            return { ...pullRequest, debtReason: "no-decisive-review" };
        }
        if (reviewState.kind === "resolved") {
            continue;
        }

        const requestedReview = selectRequestedOutstandingReview(
            reviewState,
            pullRequest.requestedReviewers,
        );
        if (!requestedReview) {
            continue;
        }

        const commits = await paginateRest(
            client,
            `/repos/${repository}/pulls/${pullRequest.number}/commits?per_page=${PAGE_SIZE}`,
        );
        if (hasSubstantiveCommitAfter(commits, requestedReview.submittedAt)) {
            return {
                ...pullRequest,
                debtReason: "changes-requested-fixed-rerequested",
                blockedAt: requestedReview.submittedAt,
                requestedReviewer: requestedReview.reviewer,
            };
        }
    }
    return null;
}

function oneLineTitle(title) {
    const normalized = requiredString(title, "pull_request.title").replace(/\s+/g, " ").trim();
    return normalized.length <= 100 ? normalized : `${normalized.slice(0, 97)}...`;
}

export function renderDismissalMessage(oldestPullRequest) {
    const number = requiredInteger(oldestPullRequest?.number, "oldest pull_request.number");
    const title = oneLineTitle(oldestPullRequest?.title);
    return `오래된 PR부터 리뷰해야 합니다. 가장 오래된 미처리 PR #${number} (${title})을 먼저 리뷰해 주세요. 이 리뷰는 순서 위반으로 자동 취소되었습니다.`;
}

/**
 * 조회가 하나라도 실패하면 예외가 그대로 전파되고 PUT 은 호출되지 않는다.
 * oldestDebt 를 확정한 뒤에만 dismiss API 를 한 번 호출한다.
 */
export async function enforceOldestReviewOrder({
    event,
    repository,
    client,
    dryRun = false,
    logger = console,
}) {
    const classification = classifyReviewEvent(event, repository);
    if (classification.status === "skipped") {
        logger.log(`건너뜀: ${classification.reason}`);
        return classification;
    }

    const context = classification.context;
    const permissionResponse = await client.request(
        `/repos/${repository}/collaborators/${encodeURIComponent(context.reviewer)}/permission`,
        { method: "GET" },
    );
    const permission = permissionResponse?.data?.permission;
    if (classifyRepositoryPermission(permission) === "non-team") {
        const result = { status: "skipped", reason: `쓰기 권한 없는 리뷰어(${permission})` };
        logger.log(`건너뜀: ${result.reason}`);
        return result;
    }

    const oldestDebt = await findOldestReviewDebt(
        client,
        repository,
        { number: context.number, createdAt: context.createdAt },
        context.reviewer,
    );
    if (!oldestDebt) {
        const result = { status: "allowed", reason: "더 오래된 미처리 PR 없음" };
        logger.log(result.reason);
        return result;
    }

    const message = renderDismissalMessage(oldestDebt);
    if (dryRun) {
        const result = {
            status: "dry-run",
            reason: `[dry-run] ${message}`,
            message,
            oldestDebt,
        };
        logger.log(result.reason);
        return result;
    }
    const dismissalResponse = await client.request(
        `/repos/${repository}/pulls/${context.number}/reviews/${context.reviewId}/dismissals`,
        { method: "PUT", body: { message, event: "DISMISS" } },
    );
    const dismissedState = normalizeReviewState(dismissalResponse?.data?.state);
    if (dismissedState !== "DISMISSED") {
        throw new Error(`review dismissal 응답 상태가 DISMISSED가 아닙니다: ${dismissedState}`);
    }
    logger.log(message);
    return { status: "dismissed", message, oldestDebt };
}

export function renderRunSummary(result) {
    if (result.status === "dismissed") {
        return `## 오래된 PR 리뷰 순서\n\n❌ ${result.message}`;
    }
    const marker = result.status === "allowed" ? "✅" : "ℹ️";
    return `## 오래된 PR 리뷰 순서\n\n${marker} ${result.reason}`;
}

function escapeWorkflowCommand(value) {
    return String(value).replaceAll("%", "%25").replaceAll("\r", "%0D").replaceAll("\n", "%0A");
}

async function appendStepSummary(summary) {
    if (!process.env.GITHUB_STEP_SUMMARY) {
        return;
    }
    try {
        await fs.appendFile(process.env.GITHUB_STEP_SUMMARY, `${summary}\n`);
    } catch (error) {
        console.error(`::warning::${escapeWorkflowCommand(`Step summary 작성 실패: ${error.message}`)}`);
    }
}

async function main() {
    const token = requiredString(process.env.GITHUB_TOKEN, "GITHUB_TOKEN");
    const repository = validateRepository(process.env.GITHUB_REPOSITORY);
    const eventPath = requiredString(process.env.GITHUB_EVENT_PATH, "GITHUB_EVENT_PATH");

    let event;
    try {
        event = JSON.parse(await fs.readFile(eventPath, "utf8"));
    } catch (error) {
        throw new Error(`GitHub 이벤트를 읽지 못했습니다: ${error.message}`);
    }

    const client = createGitHubClient({
        token,
        apiUrl: process.env.GITHUB_API_URL ?? "https://api.github.com",
    });
    const result = await enforceOldestReviewOrder({
        event,
        repository,
        client,
        dryRun: process.env.DRY_RUN === "true",
    });
    const summary = renderRunSummary(result);
    console.log(summary);
    await appendStepSummary(summary);

    if (result.status === "dismissed") {
        // 리뷰 취소를 성공시킨 뒤 job 도 실패시켜 순서 위반이 Checks 에 선명하게 남게 한다.
        throw new Error(result.message);
    }
}

const invokedPath = process.argv[1] ? pathToFileURL(path.resolve(process.argv[1])).href : "";
if (import.meta.url === invokedPath) {
    main().catch((error) => {
        console.error(`::error::${escapeWorkflowCommand(error.message)}`);
        process.exitCode = 1;
    });
}
