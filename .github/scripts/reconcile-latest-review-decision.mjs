import { pathToFileURL } from "node:url";

export const DECISIVE_REVIEW_STATES = new Set(["APPROVED", "CHANGES_REQUESTED"]);

const WRITE_PERMISSIONS = new Set(["admin", "maintain", "write"]);
const NON_WRITE_PERMISSIONS = new Set(["triage", "read", "none"]);

function requiredString(value, name) {
    if (typeof value !== "string" || value.trim() === "") {
        throw new Error(`${name} 값이 없습니다.`);
    }
    return value.trim();
}

function requiredPositiveInteger(value, name) {
    const number = typeof value === "number" ? value : Number(value);
    if (!Number.isSafeInteger(number) || number <= 0) {
        throw new Error(`${name} 값이 양의 정수가 아닙니다: ${value}`);
    }
    return number;
}

function normalizeReviewState(value) {
    return requiredString(value, "review.state").toUpperCase();
}

function normalizeRepository(repository) {
    const value = requiredString(repository, "repository");
    if (!/^[A-Za-z0-9_.-]+\/[A-Za-z0-9_.-]+$/.test(value)) {
        throw new Error(`repository 형식이 owner/name 이 아닙니다: ${value}`);
    }
    return value;
}

function normalizeDecisiveReview(review) {
    const state = normalizeReviewState(review?.state);
    if (!DECISIVE_REVIEW_STATES.has(state)) {
        return null;
    }

    const id = requiredPositiveInteger(review?.id, "review.id");
    const reviewer = requiredString(
        review?.user?.login ?? review?.reviewer,
        "review.user.login",
    ).toLowerCase();
    const submittedAt = requiredString(
        review?.submitted_at ?? review?.submittedAt,
        "review.submitted_at",
    );
    const submittedTimestamp = Date.parse(submittedAt);
    if (!Number.isFinite(submittedTimestamp)) {
        throw new Error(`review.submitted_at 값이 올바른 시각이 아닙니다: ${submittedAt}`);
    }

    return { id, reviewer, state, submittedAt, submittedTimestamp };
}

function compareDecisiveReviews(left, right) {
    return left.submittedTimestamp - right.submittedTimestamp || left.id - right.id;
}

export function classifyRepositoryPermission(permission) {
    const value = requiredString(permission, "repository permission").toLowerCase();
    if (WRITE_PERMISSIONS.has(value)) {
        return "write";
    }
    if (NON_WRITE_PERMISSIONS.has(value)) {
        return "read";
    }
    throw new Error(`알 수 없는 repository permission 입니다: ${permission}`);
}

export function selectLatestDecisiveReviewsByReviewer(reviews) {
    if (!Array.isArray(reviews)) {
        throw new Error("리뷰 목록 응답이 배열이 아닙니다.");
    }

    const latestByReviewer = new Map();
    for (const rawReview of reviews) {
        const review = normalizeDecisiveReview(rawReview);
        if (!review) {
            continue;
        }
        const previous = latestByReviewer.get(review.reviewer);
        if (!previous || compareDecisiveReviews(previous, review) <= 0) {
            latestByReviewer.set(review.reviewer, review);
        }
    }

    return [...latestByReviewer.values()].sort(compareDecisiveReviews);
}

export function buildDismissalPlan(reviews, permissionByReviewer) {
    if (!(permissionByReviewer instanceof Map)) {
        throw new Error("permissionByReviewer 는 Map 이어야 합니다.");
    }

    const latestByReviewer = selectLatestDecisiveReviewsByReviewer(reviews);
    const writeReviews = latestByReviewer.filter((review) => {
        if (!permissionByReviewer.has(review.reviewer)) {
            throw new Error(`@${review.reviewer}의 repository permission 이 없습니다.`);
        }
        return classifyRepositoryPermission(permissionByReviewer.get(review.reviewer)) === "write";
    });

    if (writeReviews.length === 0) {
        return { status: "no-write-review", latestReview: null, blockingReviews: [] };
    }

    const latestReview = writeReviews.at(-1);
    if (latestReview.state !== "APPROVED") {
        return { status: "latest-changes-requested", latestReview, blockingReviews: [] };
    }

    const blockingReviews = writeReviews.filter((review) => review.state === "CHANGES_REQUESTED");
    if (blockingReviews.length === 0) {
        return { status: "already-approved", latestReview, blockingReviews: [] };
    }

    return { status: "dismiss", latestReview, blockingReviews };
}

export class GitHubClient {
    constructor({ token, apiUrl = "https://api.github.com", fetchImpl = globalThis.fetch }) {
        this.token = requiredString(token, "GITHUB_TOKEN");
        this.apiUrl = requiredString(apiUrl, "GITHUB_API_URL").replace(/\/+$/, "");
        if (typeof fetchImpl !== "function") {
            throw new Error("fetch 구현이 없습니다.");
        }
        this.fetchImpl = fetchImpl;
    }

    async request(path, { method = "GET", body } = {}) {
        if (typeof path !== "string" || !path.startsWith("/")) {
            throw new Error(`GitHub API path가 올바르지 않습니다: ${path}`);
        }

        const response = await this.fetchImpl(`${this.apiUrl}${path}`, {
            method,
            headers: {
                Accept: "application/vnd.github+json",
                Authorization: `Bearer ${this.token}`,
                "Content-Type": "application/json",
                "X-GitHub-Api-Version": "2022-11-28",
            },
            body: body === undefined ? undefined : JSON.stringify(body),
        });
        const responseText = await response.text();
        let data = null;
        if (responseText !== "") {
            try {
                data = JSON.parse(responseText);
            } catch {
                throw new Error(`GitHub API가 JSON이 아닌 응답을 반환했습니다: ${response.status}`);
            }
        }
        if (!response.ok) {
            const detail = typeof data?.message === "string" ? data.message : responseText.slice(0, 500);
            throw new Error(`GitHub API ${method} ${path} 실패: HTTP ${response.status} ${detail}`);
        }
        return data;
    }

    async paginate(path) {
        const items = [];
        for (let page = 1; ; page += 1) {
            const separator = path.includes("?") ? "&" : "?";
            const response = await this.request(`${path}${separator}per_page=100&page=${page}`);
            if (!Array.isArray(response)) {
                throw new Error(`GitHub API pagination 응답이 배열이 아닙니다: ${path}`);
            }
            items.push(...response);
            if (response.length < 100) {
                return items;
            }
        }
    }
}

export async function planPullRequest(client, repository, pullNumber) {
    const normalizedRepository = normalizeRepository(repository);
    const normalizedPullNumber = requiredPositiveInteger(pullNumber, "pull number");
    const reviews = await client.paginate(
        `/repos/${normalizedRepository}/pulls/${normalizedPullNumber}/reviews`,
    );
    const latestByReviewer = selectLatestDecisiveReviewsByReviewer(reviews);
    const permissionByReviewer = new Map();

    for (const review of latestByReviewer) {
        const response = await client.request(
            `/repos/${normalizedRepository}/collaborators/${encodeURIComponent(review.reviewer)}/permission`,
        );
        permissionByReviewer.set(review.reviewer, response?.permission);
    }

    return {
        pullNumber: normalizedPullNumber,
        ...buildDismissalPlan(reviews, permissionByReviewer),
    };
}

export async function applyDismissalPlan(client, repository, plan) {
    const normalizedRepository = normalizeRepository(repository);
    if (plan?.status !== "dismiss") {
        return [];
    }

    const pullNumber = requiredPositiveInteger(plan.pullNumber, "pull number");
    const approval = normalizeDecisiveReview(plan.latestReview);
    if (!approval || approval.state !== "APPROVED") {
        throw new Error(`#${pullNumber} dismissal plan의 최종 승인이 올바르지 않습니다.`);
    }

    const dismissed = [];
    for (const rawReview of plan.blockingReviews) {
        const blockingReview = normalizeDecisiveReview(rawReview);
        if (!blockingReview || blockingReview.state !== "CHANGES_REQUESTED") {
            throw new Error(`#${pullNumber} dismissal plan에 변경요청이 아닌 리뷰가 있습니다.`);
        }
        if (compareDecisiveReviews(blockingReview, approval) >= 0) {
            throw new Error(`#${pullNumber} 최종 승인보다 늦은 변경요청은 dismiss할 수 없습니다.`);
        }

        const message = [
            `@${approval.reviewer}의 더 늦은 승인(review #${approval.id})을 최종 판정으로 사용합니다.`,
            "저장소 정책은 PR 전체에서 가장 최근의 APPROVED/CHANGES_REQUESTED 판정을 우선합니다.",
        ].join(" ");
        const response = await client.request(
            `/repos/${normalizedRepository}/pulls/${pullNumber}/reviews/${blockingReview.id}/dismissals`,
            { method: "PUT", body: { event: "DISMISS", message } },
        );
        if (normalizeReviewState(response?.state) !== "DISMISSED") {
            throw new Error(`#${pullNumber} review #${blockingReview.id} dismiss 응답이 DISMISSED가 아닙니다.`);
        }
        dismissed.push(blockingReview);
    }
    return dismissed;
}

export async function reconcileLatestReviewDecisions({ client, repository, pullNumbers, logger = console }) {
    if (!Array.isArray(pullNumbers)) {
        throw new Error("pullNumbers 는 배열이어야 합니다.");
    }
    const normalizedPullNumbers = [...new Set(
        pullNumbers.map((pullNumber) => requiredPositiveInteger(pullNumber, "pull number")),
    )];

    // 모든 PR의 조회와 권한 판정을 먼저 끝낸다. 한 PR이라도 근거 조회가 실패하면
    // 다른 PR의 blocking review까지 일부만 dismiss하지 않는다.
    const plans = [];
    for (const pullNumber of normalizedPullNumbers) {
        plans.push(await planPullRequest(client, repository, pullNumber));
    }

    const results = [];
    for (const plan of plans) {
        const dismissed = await applyDismissalPlan(client, repository, plan);
        if (dismissed.length === 0) {
            logger.log(`#${plan.pullNumber}: ${plan.status}`);
        } else {
            logger.log(
                `#${plan.pullNumber}: @${plan.latestReview.reviewer} 승인 뒤 변경요청 ${dismissed.length}건 dismiss`,
            );
        }
        results.push({ plan, dismissed });
    }
    return results;
}

export function parseTargetPullNumber(value) {
    if (value === undefined || value === null || String(value).trim() === "") {
        return null;
    }
    return requiredPositiveInteger(String(value).trim(), "TARGET_PULL_NUMBER");
}

async function main() {
    const repository = normalizeRepository(process.env.GITHUB_REPOSITORY);
    const client = new GitHubClient({
        token: process.env.GITHUB_TOKEN ?? process.env.GH_TOKEN,
        apiUrl: process.env.GITHUB_API_URL ?? "https://api.github.com",
    });
    const targetPullNumber = parseTargetPullNumber(process.env.TARGET_PULL_NUMBER);
    let pullNumbers;

    if (targetPullNumber === null) {
        const openPullRequests = await client.paginate(`/repos/${repository}/pulls?state=open`);
        pullNumbers = openPullRequests.map((pullRequest) =>
            requiredPositiveInteger(pullRequest?.number, "pull_request.number"));
    } else {
        pullNumbers = [targetPullNumber];
    }

    console.log(`Latest review decision reconcile: ${pullNumbers.length} open pull request(s)`);
    await reconcileLatestReviewDecisions({ client, repository, pullNumbers });
}

const isDirectExecution = process.argv[1]
    && import.meta.url === pathToFileURL(process.argv[1]).href;

if (isDirectExecution) {
    try {
        await main();
    } catch (error) {
        console.error(`::error::${error.message}`);
        process.exitCode = 1;
    }
}
