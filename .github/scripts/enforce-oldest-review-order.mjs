#!/usr/bin/env node

// 오래된 PR 을 내버려 둔 채 새 PR 만 리뷰하는 순서를 막는다.
//
// 리뷰 자체는 취소하지 않는다. trusted default branch workflow가 현재 head SHA에
// 고정 commit status를 기록하고, 상태 변동 때 열린 PR 전체를 다시 계산한다.

import fs from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import { pathToFileURL } from "node:url";

export const DECISIVE_REVIEW_STATES = new Set(["APPROVED", "CHANGES_REQUESTED"]);
export const WRITE_CAPABLE_PERMISSIONS = new Set(["write", "maintain", "admin"]);
export const STATUS_CONTEXT = "oldest-review-order";
export const REPLAY_MARKER_PREFIX = "<!-- oldest-review-order-replay:review_id=";

const NON_WRITE_PERMISSIONS = new Set(["none", "read", "triage"]);
const PAGE_SIZE = 100;
const POLICY_DISMISSAL_PREFIX = "오래된 PR부터 리뷰해야 합니다. 가장 오래된 미처리 PR #";
const POLICY_DISMISSAL_SUFFIX = "을 먼저 리뷰해 주세요. 이 리뷰는 순서 위반으로 자동 취소되었습니다.";
const HISTORICAL_REPLAY_ALLOWLIST = new Map([
    [5032388275, {
        pullRequestNumber: 821,
        author: "koongmai",
        dismissalEventId: 30053730355,
        predecessorPullRequestNumber: 741,
    }],
]);

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

/** review-debt-guard 와 같이 PR 전체에서 가장 최근에 제출된 결정만 남긴다. */
export function analyzeDecisiveReviews(reviews) {
    if (!Array.isArray(reviews)) {
        throw new Error("리뷰 목록 응답이 배열이 아닙니다.");
    }

    let latest = null;
    for (const review of reviews) {
        const state = normalizeReviewState(review?.state);
        if (!DECISIVE_REVIEW_STATES.has(state)) {
            continue;
        }
        const reviewer = requiredString(review.user?.login, "review.user.login").toLowerCase();
        const submittedAt = requiredString(review.submitted_at, "review.submitted_at");
        const submittedTimestamp = parseTimestamp(submittedAt, "review.submitted_at");
        if (!latest || submittedTimestamp >= latest.submittedTimestamp) {
            latest = {
                reviewer,
                state,
                submittedAt,
                submittedTimestamp,
            };
        }
    }

    if (!latest) {
        return { kind: "no-decisive-review", debt: true, blockedAt: null };
    }

    if (latest.state === "APPROVED") {
        return { kind: "resolved", debt: false, blockedAt: null };
    }

    return {
        kind: "changes-requested",
        debt: false,
        blockedAt: latest.submittedAt,
        blockedTimestamp: latest.submittedTimestamp,
        outstandingReviews: [latest],
    };
}

function replayReviewIdFromBody(body) {
    if (typeof body !== "string") return null;
    const lines = body.split(/\r?\n/).filter((line) => line.includes("oldest-review-order-replay:"));
    if (lines.length === 0) return null;
    if (lines.length !== 1) throw new Error("replay marker가 정확히 하나가 아닙니다.");
    const match = lines[0].match(/^<!-- oldest-review-order-replay:review_id=([1-9]\d*) -->$/);
    if (!match) throw new Error("replay marker 형식이 올바르지 않습니다.");
    return Number(match[1]);
}

function historicalReplaySpec(reviewId) {
    const spec = HISTORICAL_REPLAY_ALLOWLIST.get(reviewId);
    if (!spec) {
        throw new Error(`review_id ${reviewId}는 검증된 historical replay allowlist에 없습니다.`);
    }
    return spec;
}

/** reviewer별 최신 활성 human 결정과 검증된 replay proxy를 모두 고른다. */
export function selectLatestActiveDecisiveReviewsByReviewer(reviews) {
    if (!Array.isArray(reviews)) {
        throw new Error("리뷰 목록 응답이 배열이 아닙니다.");
    }
    const latestByReviewer = new Map();
    for (const review of reviews) {
        const state = normalizeReviewState(review?.state);
        if (!DECISIVE_REVIEW_STATES.has(state)) continue;
        let reviewer = requiredString(review.user?.login, "review.user.login");
        let proxyReviewId = null;
        if (isBotReviewer(review.user)) {
            if (reviewer !== "github-actions[bot]" || state !== "APPROVED") continue;
            proxyReviewId = replayReviewIdFromBody(review.body);
            if (proxyReviewId == null) continue;
            const replaySpec = historicalReplaySpec(proxyReviewId);
            const originals = reviews.filter((candidate) => candidate?.id === proxyReviewId);
            if (originals.length !== 1 || normalizeReviewState(originals[0].state) !== "DISMISSED") {
                throw new Error(`replay marker의 원 review_id ${proxyReviewId}가 유효한 DISMISSED 리뷰가 아닙니다.`);
            }
            if (isBotReviewer(originals[0].user)) {
                throw new Error(`replay marker의 원 review_id ${proxyReviewId} 작성자가 human이 아닙니다.`);
            }
            reviewer = requiredString(originals[0].user?.login, "original review.user.login");
            if (reviewer.toLowerCase() !== replaySpec.author.toLowerCase()) {
                throw new Error(`replay marker의 원 review_id ${proxyReviewId} 작성자가 allowlist와 다릅니다.`);
            }
        }
        const id = requiredInteger(review.id, "review.id");
        const submittedAt = requiredString(review.submitted_at, "review.submitted_at");
        const submittedTimestamp = parseTimestamp(submittedAt, "review.submitted_at");
        const key = reviewer.toLowerCase();
        const latest = latestByReviewer.get(key);
        if (!latest || submittedTimestamp > latest.submittedTimestamp ||
            (submittedTimestamp === latest.submittedTimestamp && id > latest.id)) {
            latestByReviewer.set(key, {
                id,
                reviewer,
                state,
                submittedAt,
                submittedTimestamp,
                proxyReviewId,
            });
        }
    }
    return [...latestByReviewer.values()].sort((left, right) =>
        left.submittedTimestamp - right.submittedTimestamp || left.id - right.id
    );
}

/** 기존 단일 selector 호환: reviewer별 결정을 모은 뒤 전체 최신을 반환한다. */
export function selectLatestActiveHumanDecisiveReview(reviews) {
    return selectLatestActiveDecisiveReviewsByReviewer(reviews).at(-1) ?? null;
}

/**
 * PR 전체의 최신 변경요청을 낸 뒤 현재 다시 요청받은 리뷰어만 고른다.
 * GitHub는 리뷰 제출 시 그 사람의 요청을 제거하므로, 같은 리뷰어가 현재 요청 목록에
 * 다시 있다면 최신 결정 뒤 작성자가 명시적으로 재리뷰를 요청한 것이다.
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

async function cachedPaginateRest(client, cache, key, apiPath) {
    if (!cache) return paginateRest(client, apiPath);
    if (!cache.has(key)) cache.set(key, paginateRest(client, apiPath));
    return cache.get(key);
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

export async function findOldestReviewDebt(
    client,
    repository,
    currentPullRequest,
    reviewer,
    providedOpenPullRequests,
    reviewCache,
) {
    validateRepository(repository);
    const openPullRequests = providedOpenPullRequests ?? await paginateRest(
        client,
        `/repos/${repository}/pulls?state=open&sort=created&direction=asc&per_page=${PAGE_SIZE}`,
    );
    const candidates = selectOlderPullRequests(openPullRequests, currentPullRequest, reviewer);

    for (const pullRequest of candidates) {
        const reviews = await cachedPaginateRest(
            client,
            reviewCache,
            pullRequest.number,
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
    return `${POLICY_DISMISSAL_PREFIX}${number} (${title})${POLICY_DISMISSAL_SUFFIX}`;
}

export function parsePolicyDismissalMessage(message) {
    const value = requiredString(message, "dismissal_message");
    if (!value.startsWith(POLICY_DISMISSAL_PREFIX) || !value.endsWith(POLICY_DISMISSAL_SUFFIX)) {
        throw new Error("oldest-review-order의 정확한 dismissal message가 아닙니다.");
    }
    const middle = value.slice(POLICY_DISMISSAL_PREFIX.length, -POLICY_DISMISSAL_SUFFIX.length);
    const match = middle.match(/^([1-9]\d*) \(([^\r\n]+)\)$/);
    if (!match || renderDismissalMessage({ number: Number(match[1]), title: match[2] }) !== value) {
        throw new Error("oldest-review-order의 정확한 dismissal message가 아닙니다.");
    }
    return { number: Number(match[1]), title: match[2] };
}

function normalizeStatusPullRequest(pullRequest) {
    const normalized = normalizeOpenPullRequest(pullRequest);
    const state = requiredString(pullRequest.state, "pull_request.state").toLowerCase();
    if (state !== "open" && state !== "closed") {
        throw new Error(`PR #${normalized.number} state가 올바르지 않습니다: ${state}`);
    }
    const headSha = requiredString(pullRequest.head?.sha, "pull_request.head.sha");
    if (!/^[0-9a-f]{40,64}$/i.test(headSha)) {
        throw new Error(`PR #${normalized.number} head SHA가 올바르지 않습니다.`);
    }
    return {
        ...normalized,
        state,
        headSha,
        headRepository: requiredString(
            pullRequest.head?.repo?.full_name,
            "pull_request.head.repo.full_name",
        ),
    };
}

function statusDescription(value) {
    return Array.from(requiredString(value, "status description").replace(/\s+/g, " "))
        .slice(0, 140)
        .join("");
}

export async function postOrderStatus({ client, repository, sha, state, description, targetUrl }) {
    if (!new Set(["pending", "success", "failure"]).has(state)) {
        throw new Error(`지원하지 않는 status state입니다: ${state}`);
    }
    const body = { state, context: STATUS_CONTEXT, description: statusDescription(description) };
    if (targetUrl) body.target_url = targetUrl;
    const response = await client.request(`/repos/${validateRepository(repository)}/statuses/${sha}`, {
        method: "POST",
        body,
    });
    if (response?.data?.state !== state || response?.data?.context !== STATUS_CONTEXT) {
        throw new Error("commit status 응답이 요청과 다릅니다.");
    }
    return response.data;
}

export async function evaluatePullRequestOrder({
    client,
    repository,
    pullRequest,
    openPullRequests,
    reviewCache,
}) {
    const current = normalizeStatusPullRequest(pullRequest);
    if (current.state === "closed") {
        return { state: "success", reason: "닫힌 PR은 리뷰 순서 검사 대상이 아닙니다." };
    }
    if (current.draft) {
        return { state: "success", reason: "draft PR은 리뷰 순서 검사 대상이 아닙니다." };
    }
    if (current.headRepository !== repository) {
        return { state: "success", reason: "fork PR은 리뷰 순서 검사 대상이 아닙니다." };
    }
    const reviews = await cachedPaginateRest(
        client,
        reviewCache,
        current.number,
        `/repos/${repository}/pulls/${current.number}/reviews?per_page=${PAGE_SIZE}`,
    );
    const activeReviews = selectLatestActiveDecisiveReviewsByReviewer(reviews);
    if (activeReviews.length === 0) {
        return {
            state: "success",
            reason: "활성 human 결정 리뷰가 없습니다. 승인 여부는 네이티브 규칙이 확인합니다.",
        };
    }
    const teamReviews = [];
    const violations = [];
    for (const activeReview of activeReviews) {
        const permissionResponse = await client.request(
            `/repos/${repository}/collaborators/${encodeURIComponent(activeReview.reviewer)}/permission`,
            { method: "GET" },
        );
        if (classifyRepositoryPermission(permissionResponse?.data?.permission) === "non-team") {
            if (activeReview.proxyReviewId != null) {
                return {
                    state: "failure",
                    reason: `이관 승인 원 작성자 @${activeReview.reviewer}에게 현재 write 권한이 없습니다.`,
                    invalidProxyReviewId: activeReview.proxyReviewId,
                };
            }
            continue;
        }
        teamReviews.push(activeReview);
        const oldestDebt = await findOldestReviewDebt(
            client,
            repository,
            current,
            activeReview.reviewer,
            openPullRequests,
            reviewCache,
        );
        if (oldestDebt) violations.push({ activeReview, oldestDebt });
    }
    if (teamReviews.length === 0) {
        return { state: "success", reason: "활성 결정 리뷰 작성자에게 현재 write 권한이 없습니다." };
    }
    if (violations.length === 0) {
        return { state: "success", reason: "모든 팀 리뷰어에게 더 오래된 미처리 PR이 없습니다." };
    }
    violations.sort((left, right) => comparePullRequestsOldestFirst(left.oldestDebt, right.oldestDebt));
    const { activeReview, oldestDebt } = violations[0];
    return {
        state: "failure",
        reason: `@${activeReview.reviewer}은 먼저 #${oldestDebt.number} (${oneLineTitle(oldestDebt.title)})을 리뷰해야 합니다.`,
        oldestDebt,
    };
}

async function getPullRequest(client, repository, number) {
    const response = await client.request(`/repos/${repository}/pulls/${requiredInteger(number, "pull number")}`);
    if (!response?.data || Array.isArray(response.data)) {
        throw new Error(`PR #${number} 응답이 올바르지 않습니다.`);
    }
    return response.data;
}

/** pending을 먼저 기록하고, 모든 판정 read가 성공한 뒤에만 final status를 쓴다. */
export async function recalculatePullRequests({
    client,
    repository,
    scope = "all",
    affectedPullRequestNumber,
    eventPullRequestNumber,
    targetUrl,
    logger = console,
}) {
    const openPullRequests = await paginateRest(
        client,
        `/repos/${repository}/pulls?state=open&sort=created&direction=asc&per_page=${PAGE_SIZE}`,
    );
    for (const pullRequest of openPullRequests) normalizeStatusPullRequest(pullRequest);
    let targets = [...openPullRequests];
    if (scope === "affected") {
        const affected = Number(affectedPullRequestNumber);
        if (!Number.isInteger(affected) || affected < 1) throw new Error("affected PR 번호가 필요합니다.");
        const current = targets.find((pullRequest) => pullRequest.number === affected) ??
            await getPullRequest(client, repository, affected);
        const normalized = normalizeStatusPullRequest(current);
        targets = targets.filter((pullRequest) =>
            comparePullRequestsOldestFirst(normalizeStatusPullRequest(pullRequest), normalized) >= 0
        );
        if (!targets.some((pullRequest) => pullRequest.number === affected)) targets.push(current);
    } else if (scope !== "all") {
        throw new Error(`지원하지 않는 재계산 범위입니다: ${scope}`);
    }
    if (eventPullRequestNumber) {
        const eventNumber = Number(eventPullRequestNumber);
        if (!targets.some((pullRequest) => pullRequest.number === eventNumber)) {
            targets.push(await getPullRequest(client, repository, eventNumber));
        }
    }
    targets = [...new Map(targets.map((pullRequest) => [pullRequest.number, pullRequest])).values()]
        .sort(comparePullRequestsOldestFirst);

    for (const pullRequest of targets) {
        const current = normalizeStatusPullRequest(pullRequest);
        await postOrderStatus({
            client, repository, sha: current.headSha, state: "pending",
            description: "오래된 PR 리뷰 순서를 재계산하는 중입니다.", targetUrl,
        });
    }
    const decisions = [];
    const reviewCache = new Map();
    for (const pullRequest of targets) {
        decisions.push({
            pullRequest,
            decision: await evaluatePullRequestOrder({
                client, repository, pullRequest, openPullRequests, reviewCache,
            }),
        });
    }
    for (const { pullRequest } of decisions) {
        const before = normalizeStatusPullRequest(pullRequest);
        const after = normalizeStatusPullRequest(await getPullRequest(client, repository, before.number));
        if (before.headSha !== after.headSha) {
            await postOrderStatus({
                client, repository, sha: after.headSha, state: "pending",
                description: "PR head가 바뀌어 다음 재계산을 기다립니다.", targetUrl,
            });
            throw new Error(`PR #${before.number}의 head가 재계산 중 변경됐습니다.`);
        }
        if (before.state !== after.state || before.draft !== after.draft ||
            before.headRepository !== after.headRepository) {
            throw new Error(`PR #${before.number}의 상태가 재계산 중 변경됐습니다.`);
        }
    }
    const results = [];
    for (const { pullRequest, decision } of decisions) {
        const current = normalizeStatusPullRequest(pullRequest);
        await postOrderStatus({
            client, repository, sha: current.headSha, state: decision.state,
            description: decision.reason, targetUrl,
        });
        results.push({ number: current.number, headSha: current.headSha, ...decision });
        logger.log(`#${current.number}: ${decision.state} — ${decision.reason}`);
    }
    return { status: "recalculated", scope, results };
}

export function replayMarker(reviewId) {
    const id = Number(reviewId);
    if (!Number.isInteger(id) || id < 1) throw new Error("review_id가 올바르지 않습니다.");
    return `${REPLAY_MARKER_PREFIX}${id} -->`;
}

function hasExactMarker(body, marker) {
    return typeof body === "string" && body.split(/\r?\n/).includes(marker);
}

/** 과거 정책이 취소한 APPROVED 하나만 provenance 검증 뒤 bot 승인으로 이관한다. */
export async function replayHistoricalApproval({
    client,
    repository,
    reviewId,
    pullRequestNumber,
    logger = console,
}) {
    const id = Number(reviewId);
    const marker = replayMarker(id);
    const replaySpec = historicalReplaySpec(id);
    const requestedPullNumber = pullRequestNumber == null ? null : Number(pullRequestNumber);
    if (requestedPullNumber != null && (!Number.isInteger(requestedPullNumber) || requestedPullNumber < 1)) {
        throw new Error("pull_number가 올바르지 않습니다.");
    }
    if (requestedPullNumber != null && requestedPullNumber !== replaySpec.pullRequestNumber) {
        throw new Error(`review_id ${id}의 검증된 PR은 #${replaySpec.pullRequestNumber}입니다.`);
    }
    const openPullRequests = await paginateRest(
        client,
        `/repos/${repository}/pulls?state=open&sort=created&direction=asc&per_page=${PAGE_SIZE}`,
    );
    const matches = [];
    const reviewCache = new Map();
    for (const pullRequest of openPullRequests) {
        const current = normalizeStatusPullRequest(pullRequest);
        const reviews = await cachedPaginateRest(
            client,
            reviewCache,
            current.number,
            `/repos/${repository}/pulls/${current.number}/reviews?per_page=${PAGE_SIZE}`,
        );
        const original = reviews.find((review) => review?.id === id);
        if (original) matches.push({ pullRequest, reviews, original });
    }
    if (matches.length !== 1) {
        throw new Error(`열린 PR에서 review_id ${id}를 정확히 하나 찾지 못했습니다.`);
    }
    const { pullRequest, reviews, original } = matches[0];
    const current = normalizeStatusPullRequest(pullRequest);
    if (current.number !== replaySpec.pullRequestNumber) {
        throw new Error(`review_id ${id}가 allowlist의 PR #${replaySpec.pullRequestNumber}에 속하지 않습니다.`);
    }
    if (requestedPullNumber != null && current.number !== requestedPullNumber) {
        throw new Error(`review_id ${id}가 요청한 PR #${requestedPullNumber}에 속하지 않습니다.`);
    }
    if (current.draft || current.headRepository !== repository) {
        throw new Error("draft 또는 fork PR의 historical approval은 재생하지 않습니다.");
    }
    if (normalizeReviewState(original.state) !== "DISMISSED") {
        throw new Error("원 리뷰의 현재 상태가 DISMISSED가 아닙니다.");
    }
    const author = requiredString(original.user?.login, "original review.user.login");
    if (isBotReviewer(original.user)) throw new Error("원 리뷰 작성자가 human이 아닙니다.");
    if (author.toLowerCase() !== replaySpec.author.toLowerCase()) {
        throw new Error(`원 리뷰 작성자 @${author}가 allowlist와 다릅니다.`);
    }

    const timeline = await paginateRest(
        client,
        `/repos/${repository}/issues/${current.number}/timeline?per_page=${PAGE_SIZE}`,
    );
    const dismissals = timeline.filter((event) =>
        event?.event === "review_dismissed" && String(event.dismissed_review?.review_id) === String(id)
    );
    if (dismissals.length !== 1) {
        throw new Error(`review_id ${id}의 dismissal event를 정확히 하나 찾지 못했습니다.`);
    }
    const dismissal = dismissals[0];
    if (dismissal.id !== replaySpec.dismissalEventId) {
        throw new Error(`review_id ${id}의 dismissal event가 allowlist와 다릅니다.`);
    }
    if (dismissal.actor?.login !== "github-actions[bot]") {
        throw new Error("dismissal actor가 github-actions[bot]이 아닙니다.");
    }
    if (String(dismissal.dismissed_review?.state).toLowerCase() !== "approved") {
        throw new Error("dismissal 직전 상태가 APPROVED가 아닙니다. CHANGES_REQUESTED는 재생하지 않습니다.");
    }
    const provenance = parsePolicyDismissalMessage(dismissal.dismissed_review?.dismissal_message);
    if (provenance.number !== replaySpec.predecessorPullRequestNumber) {
        throw new Error(`review_id ${id}의 선행 PR provenance가 allowlist와 다릅니다.`);
    }

    const originalLink = `https://github.com/${repository}/pull/${current.number}#pullrequestreview-${id}`;
    const body = [
        marker,
        `과거 oldest-review-order 정책이 자동 취소한 @${author}의 [원 승인 리뷰](${originalLink})를 1회 이관했습니다.`,
        `검증된 당시 선행 PR: #${provenance.number} (${provenance.title})`,
    ].join("\n\n");
    const existing = reviews.filter((review) => hasExactMarker(review?.body, marker));
    if (existing.length > 0) {
        if (existing.length !== 1 || existing[0].user?.login !== "github-actions[bot]" ||
            normalizeReviewState(existing[0].state) !== "APPROVED" || existing[0].body !== body) {
            throw new Error("동일 replay marker가 검증된 bot 승인에 있지 않습니다.");
        }
        logger.log(`review_id ${id}는 이미 이관됐습니다.`);
        return { status: "already-replayed", pullRequestNumber: current.number, marker };
    }

    const permission = await client.request(
        `/repos/${repository}/collaborators/${encodeURIComponent(author)}/permission`,
    );
    if (classifyRepositoryPermission(permission?.data?.permission) !== "team") {
        throw new Error(`원 리뷰 작성자 @${author}에게 현재 write 권한이 없습니다.`);
    }
    const originalTime = parseTimestamp(original.submitted_at, "original review.submitted_at");
    const newer = reviews.find((review) => {
        const state = normalizeReviewState(review?.state);
        if (!DECISIVE_REVIEW_STATES.has(state) && state !== "DISMISSED") return false;
        if (review?.id === id || review.user?.login?.toLowerCase() !== author.toLowerCase()) {
            return false;
        }
        const submittedTimestamp = parseTimestamp(review.submitted_at, "newer review.submitted_at");
        return submittedTimestamp > originalTime ||
            (submittedTimestamp === originalTime && requiredInteger(review.id, "newer review.id") > id);
    });
    if (newer) throw new Error(`@${author}의 더 새로운 결정 리뷰가 있습니다.`);

    const debt = await findOldestReviewDebt(
        client,
        repository,
        current,
        author,
        openPullRequests,
        reviewCache,
    );
    if (debt) throw new Error(`@${author}의 현재 가장 오래된 리뷰 빚은 #${debt.number}입니다.`);

    const refreshed = normalizeStatusPullRequest(await getPullRequest(client, repository, current.number));
    if (refreshed.state !== "open" || refreshed.headSha !== current.headSha) {
        throw new Error("PR 상태 또는 head가 검증 중 변경됐습니다.");
    }
    const created = await client.request(`/repos/${repository}/pulls/${current.number}/reviews`, {
        method: "POST",
        body: { event: "APPROVE", commit_id: current.headSha, body },
    });
    if (normalizeReviewState(created?.data?.state) !== "APPROVED" ||
        created?.data?.user?.login !== "github-actions[bot]" ||
        !hasExactMarker(created?.data?.body, marker)) {
        throw new Error("historical approval 생성 응답을 검증하지 못했습니다.");
    }
    logger.log(`review_id ${id}를 PR #${current.number}에 이관했습니다.`);
    return { status: "replayed", pullRequestNumber: current.number, marker, author, originalLink };
}

export function renderRunSummary(result) {
    if (result.status === "recalculated") {
        const blocked = result.results.filter((item) => item.state === "failure").length;
        return `## 오래된 PR 리뷰 순서 재계산\n\n- 대상: ${result.results.length}개\n- 차단: ${blocked}개`;
    }
    const replayed = result.status === "replayed" ? "이관 완료" : "이미 이관됨 — 쓰기 생략";
    return `## 과거 승인 리뷰 이관\n\n✅ PR #${result.pullRequestNumber}: ${replayed}`;
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
    if (event.repository?.full_name && event.repository.full_name !== repository) {
        throw new Error(`이벤트 저장소(${event.repository.full_name})와 실행 저장소(${repository})가 다릅니다.`);
    }

    const client = createGitHubClient({
        token,
        apiUrl: process.env.GITHUB_API_URL ?? "https://api.github.com",
    });
    const targetUrl = process.env.GITHUB_RUN_ID
        ? `${(process.env.GITHUB_SERVER_URL ?? "https://github.com").replace(/\/+$/, "")}/${repository}/actions/runs/${process.env.GITHUB_RUN_ID}`
        : null;
    const mode = process.env.MODE ?? "recalculate";
    if (mode === "replay") {
        const replay = await replayHistoricalApproval({
            client,
            repository,
            reviewId: process.env.REVIEW_ID,
            pullRequestNumber: process.env.REPLAY_PULL_NUMBER,
        });
        const replaySummary = renderRunSummary(replay);
        console.log(replaySummary);
        await appendStepSummary(replaySummary);
        const recalculated = await recalculatePullRequests({
            client,
            repository,
            scope: "affected",
            affectedPullRequestNumber: replay.pullRequestNumber,
            targetUrl,
        });
        const recalculationSummary = renderRunSummary(recalculated);
        console.log(recalculationSummary);
        await appendStepSummary(recalculationSummary);
        return;
    }
    if (mode !== "recalculate") throw new Error(`지원하지 않는 실행 mode입니다: ${mode}`);
    const result = await recalculatePullRequests({
        client,
        repository,
        scope: process.env.RECALCULATE_SCOPE ?? "all",
        affectedPullRequestNumber: process.env.AFFECTED_PULL_REQUEST,
        eventPullRequestNumber: event.pull_request?.number,
        targetUrl,
    });
    const summary = renderRunSummary(result);
    console.log(summary);
    await appendStepSummary(summary);
}

const invokedPath = process.argv[1] ? pathToFileURL(path.resolve(process.argv[1])).href : "";
if (import.meta.url === invokedPath) {
    main().catch((error) => {
        console.error(`::error::${escapeWorkflowCommand(`재계산 실패 — pending을 유지합니다: ${error.message}`)}`);
        process.exitCode = 1;
    });
}
