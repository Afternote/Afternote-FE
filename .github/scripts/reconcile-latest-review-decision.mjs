import { pathToFileURL } from "node:url";

export const DECISIVE_REVIEW_STATES = new Set(["APPROVED", "CHANGES_REQUESTED"]);
export const REVIEW_RESPONSE_COMMAND = "/review-response";

const WRITE_PERMISSIONS = new Set(["admin", "maintain", "write"]);
const NON_WRITE_PERMISSIONS = new Set(["triage", "read", "none"]);
const AUTHOR_ACTION_KINDS = new Set(["commit", "issue-comment", "review-reply"]);

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

function normalizeLogin(value, name) {
    return requiredString(value, name).toLowerCase();
}

function normalizeTimestamp(value, name) {
    const timestampText = requiredString(value, name);
    const timestamp = Date.parse(timestampText);
    if (!Number.isFinite(timestamp)) {
        throw new Error(`${name} 값이 올바른 시각이 아닙니다: ${timestampText}`);
    }
    return { timestampText, timestamp };
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
    const reviewer = normalizeLogin(
        review?.user?.login ?? review?.reviewer,
        "review.user.login",
    );
    const { timestampText: submittedAt, timestamp: submittedTimestamp } = normalizeTimestamp(
        review?.submitted_at ?? review?.submittedAt,
        "review.submitted_at",
    );

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

function isSubstantiveReviewResponse(body) {
    if (typeof body !== "string") {
        return false;
    }
    const trimmed = body.trim();
    if (!trimmed.toLowerCase().startsWith(REVIEW_RESPONSE_COMMAND)) {
        return false;
    }
    const remainder = trimmed.slice(REVIEW_RESPONSE_COMMAND.length);
    return /^(?:[ \t]+|\r?\n)[\s\S]*\S$/.test(remainder);
}

function normalizeAuthorAction({ kind, id, actedAt, reviewer = null }) {
    const normalizedKind = requiredString(kind, "author action kind");
    if (!AUTHOR_ACTION_KINDS.has(normalizedKind)) {
        throw new Error(`알 수 없는 author action kind 입니다: ${normalizedKind}`);
    }
    const normalizedId = requiredString(
        typeof id === "number" ? String(id) : id,
        "author action id",
    );
    const { timestampText, timestamp } = normalizeTimestamp(actedAt, "author action actedAt");
    return {
        kind: normalizedKind,
        id: normalizedId,
        actedAt: timestampText,
        actedTimestamp: timestamp,
        reviewer: reviewer === null ? null : normalizeLogin(reviewer, "author action reviewer"),
    };
}

function compareAuthorActions(left, right) {
    return left.actedTimestamp - right.actedTimestamp
        || left.kind.localeCompare(right.kind)
        || left.id.localeCompare(right.id);
}

export function collectAuthorActions({
    pullAuthor,
    reviews,
    commits = [],
    issueComments = [],
    reviewComments = [],
}) {
    const author = normalizeLogin(pullAuthor, "pull_request.user.login");
    for (const [name, value] of Object.entries({ commits, issueComments, reviewComments })) {
        if (!Array.isArray(value)) {
            throw new Error(`${name} 응답이 배열이 아닙니다.`);
        }
    }

    const decisiveReviews = reviews
        .map((review) => normalizeDecisiveReview(review))
        .filter(Boolean);
    const changeRequestById = new Map(
        decisiveReviews
            .filter((review) => review.state === "CHANGES_REQUESTED")
            .map((review) => [review.id, review]),
    );
    const actions = [];

    for (const commit of commits) {
        const commitAuthors = [commit?.author?.login, commit?.committer?.login]
            .filter((login) => typeof login === "string")
            .map((login) => login.toLowerCase());
        if (!commitAuthors.includes(author)) {
            continue;
        }
        if (!Array.isArray(commit?.parents)) {
            throw new Error("commit.parents 응답이 배열이 아닙니다.");
        }
        if (commit.parents.length !== 1) {
            continue;
        }
        actions.push(normalizeAuthorAction({
            kind: "commit",
            id: requiredString(commit?.sha, "commit.sha"),
            actedAt: commit?.commit?.committer?.date ?? commit?.commit?.author?.date,
        }));
    }

    for (const comment of issueComments) {
        const commenter = comment?.user?.login;
        if (typeof commenter !== "string" || commenter.toLowerCase() !== author) {
            continue;
        }
        if (!isSubstantiveReviewResponse(comment?.body)) {
            continue;
        }
        actions.push(normalizeAuthorAction({
            kind: "issue-comment",
            id: requiredPositiveInteger(comment?.id, "issue comment.id"),
            actedAt: comment?.created_at,
        }));
    }

    const reviewCommentById = new Map();
    for (const comment of reviewComments) {
        reviewCommentById.set(requiredPositiveInteger(comment?.id, "review comment.id"), comment);
    }
    for (const comment of reviewComments) {
        const commenter = comment?.user?.login;
        if (typeof commenter !== "string" || commenter.toLowerCase() !== author) {
            continue;
        }
        if (comment?.in_reply_to_id === undefined || comment?.in_reply_to_id === null) {
            continue;
        }
        const rootCommentId = requiredPositiveInteger(
            comment.in_reply_to_id,
            "review comment.in_reply_to_id",
        );
        const rootComment = reviewCommentById.get(rootCommentId);
        if (!rootComment) {
            throw new Error(`review comment #${comment.id}의 원본 댓글 #${rootCommentId}이 없습니다.`);
        }
        const rootReviewId = requiredPositiveInteger(
            rootComment?.pull_request_review_id,
            "review comment.pull_request_review_id",
        );
        const changeRequest = changeRequestById.get(rootReviewId);
        if (!changeRequest) {
            continue;
        }
        actions.push(normalizeAuthorAction({
            kind: "review-reply",
            id: requiredPositiveInteger(comment?.id, "review comment.id"),
            actedAt: comment?.created_at,
            reviewer: changeRequest.reviewer,
        }));
    }

    return actions.sort(compareAuthorActions);
}

function findReviewedResponseEvidence({
    reviews,
    blockingReview,
    approval,
    authorActions,
}) {
    const reviewerReviews = reviews
        .map((review) => normalizeDecisiveReview(review))
        .filter((review) => review && review.reviewer === blockingReview.reviewer)
        .sort(compareDecisiveReviews);
    const relevantActions = authorActions.filter((action) =>
        (action.reviewer === null || action.reviewer === blockingReview.reviewer)
        && action.actedTimestamp < approval.submittedTimestamp);
    const action = relevantActions.at(-1);
    if (!action || action.actedTimestamp >= blockingReview.submittedTimestamp) {
        return null;
    }

    const priorReview = reviewerReviews
        .filter((review) => review.submittedTimestamp < action.actedTimestamp)
        .at(-1);
    if (!priorReview || priorReview.state !== "CHANGES_REQUESTED") {
        return null;
    }

    const reviewsAfterResponse = reviewerReviews.filter((review) =>
        review.submittedTimestamp > action.actedTimestamp
        && compareDecisiveReviews(review, approval) <= 0);
    if (reviewsAfterResponse.length !== 1 || reviewsAfterResponse[0].id !== blockingReview.id) {
        return null;
    }

    return {
        reviewer: blockingReview.reviewer,
        priorBlockingReview: priorReview,
        action,
    };
}

export function buildDismissalPlan(reviews, permissionByReviewer, context = {}) {
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
        return {
            status: "no-write-review",
            latestReview: null,
            blockingReviews: [],
            responseEvidence: [],
            pendingReviewers: [],
        };
    }

    const latestReview = writeReviews.at(-1);
    if (latestReview.state !== "APPROVED") {
        return {
            status: "latest-changes-requested",
            latestReview,
            blockingReviews: [],
            responseEvidence: [],
            pendingReviewers: [],
        };
    }

    const blockingReviews = writeReviews.filter((review) => review.state === "CHANGES_REQUESTED");
    if (blockingReviews.length === 0) {
        return {
            status: "already-approved",
            latestReview,
            blockingReviews: [],
            responseEvidence: [],
            pendingReviewers: [],
        };
    }

    const pullAuthor = normalizeLogin(context?.pullAuthor, "pull_request.user.login");
    const authorActions = collectAuthorActions({
        pullAuthor,
        reviews,
        commits: context?.commits,
        issueComments: context?.issueComments,
        reviewComments: context?.reviewComments,
    });
    const responseEvidence = [];
    const pendingReviewers = [];
    for (const blockingReview of blockingReviews) {
        const evidence = findReviewedResponseEvidence({
            reviews,
            blockingReview,
            approval: latestReview,
            authorActions,
        });
        if (evidence) {
            responseEvidence.push(evidence);
        } else {
            pendingReviewers.push(blockingReview.reviewer);
        }
    }

    if (pendingReviewers.length > 0) {
        return {
            status: "awaiting-reviewed-author-response",
            latestReview,
            blockingReviews,
            responseEvidence,
            pendingReviewers,
            pullAuthor,
        };
    }

    return {
        status: "dismiss",
        latestReview,
        blockingReviews,
        responseEvidence,
        pendingReviewers: [],
        pullAuthor,
    };
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
    const pullRequest = await client.request(
        `/repos/${normalizedRepository}/pulls/${normalizedPullNumber}`,
    );
    const reviews = await client.paginate(
        `/repos/${normalizedRepository}/pulls/${normalizedPullNumber}/reviews`,
    );
    const commits = await client.paginate(
        `/repos/${normalizedRepository}/pulls/${normalizedPullNumber}/commits`,
    );
    const issueComments = await client.paginate(
        `/repos/${normalizedRepository}/issues/${normalizedPullNumber}/comments`,
    );
    const reviewComments = await client.paginate(
        `/repos/${normalizedRepository}/pulls/${normalizedPullNumber}/comments`,
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
        ...buildDismissalPlan(reviews, permissionByReviewer, {
            pullAuthor: pullRequest?.user?.login,
            commits,
            issueComments,
            reviewComments,
        }),
    };
}

export async function applyDismissalPlan(client, repository, plan) {
    const normalizedRepository = normalizeRepository(repository);
    if (plan?.status !== "dismiss") {
        return [];
    }

    const pullNumber = requiredPositiveInteger(plan.pullNumber, "pull number");
    const pullAuthor = normalizeLogin(plan.pullAuthor, "pull_request.user.login");
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
        const evidence = plan.responseEvidence?.find(
            (candidate) => candidate?.reviewer === blockingReview.reviewer,
        );
        if (!evidence) {
            throw new Error(`#${pullNumber} @${blockingReview.reviewer}의 검토된 작성자 대응 근거가 없습니다.`);
        }
        const priorBlockingReview = normalizeDecisiveReview(evidence?.priorBlockingReview);
        const action = normalizeAuthorAction(evidence.action);
        if (!priorBlockingReview || priorBlockingReview.state !== "CHANGES_REQUESTED") {
            throw new Error(`#${pullNumber} @${blockingReview.reviewer}의 이전 변경요청 근거가 없습니다.`);
        }
        if (priorBlockingReview.reviewer !== blockingReview.reviewer
            || !(priorBlockingReview.submittedTimestamp < action.actedTimestamp)
            || !(action.actedTimestamp < blockingReview.submittedTimestamp)) {
            throw new Error(`#${pullNumber} @${blockingReview.reviewer}의 작성자 대응·재리뷰 순서가 올바르지 않습니다.`);
        }

        const message = [
            `작성자 @${pullAuthor}의 대응(${action.kind} ${action.id}) 뒤`,
            `@${blockingReview.reviewer}와 @${approval.reviewer}가 모두 재검토했습니다.`,
            `더 늦은 승인(review #${approval.id})을 최종 판정으로 사용합니다.`,
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
