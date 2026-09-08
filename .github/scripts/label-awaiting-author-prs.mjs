#!/usr/bin/env node

// 변경요청을 받은 뒤 작성자가 아무 조치도 하지 않은 PR 에 라벨을 붙인다 (#1552).
//
// `review-debt-guard.yml` 은 리뷰어의 빚과 작성자의 빚을 서로 다른 입장 조건으로 다룬다.
// 리뷰어 빚 판정에서는 "재요청이 없거나 최신 변경요청 뒤 작성자 조치가 없으면 리뷰어 빚이
// 아니다"(#1136)를 유지한다. 대신 `check-author-debt.mjs` 가 이 파일의 같은 판정을 재사용해,
// 공이 작성자에게 남은 자기 PR 이 있으면 새 PR 을 닫는다. 라벨 리컨사일러는 현재 상태를 열린
// PR 목록에서 바로 볼 수 있게 만드는 관찰 가능성 경로이고, check CLI 는 새 PR 입장 게이트다.
//
// 8/30 실측: 열린 non-draft 29건 중 최신 판정이 CHANGES_REQUESTED 인 9건이 9건 모두 작성자
// 무조치였고, 그중 #440 은 49일 · #767·#771 은 22일째였다. 오래 열려 있을수록 develop 이
// 전진해 충돌 비용이 커진다.
//
// 이벤트가 아니라 리컨사일러인 이유. 붙이는 경로와 떼는 경로를 따로 만들면 떼는 쪽이 새고
// 스테일 라벨이 남아 신호가 죽는다. 여기서는 매 실행이 «지금 무조치인 PR 집합» 을 다시 계산해
// 차이만 쓰므로 붙이기와 떼기가 갈라질 수 없다. 호출도 PR 수만큼 곱해지지 않는다 (#1465).
//
// 판정 기준은 `review-debt-guard.yml` 과 같아야 한다. 한쪽만 바뀌면 가드가 «작성자 몫» 이라고
// 판단한 PR 에 라벨이 없거나, 반대로 리뷰어 몫인 PR 에 라벨이 붙는다. 커밋·응답·본문 편집을
// 포함해 두 판정이 공유하는 술어는 `awaiting-author-policy.test.mjs` 가 잠근다.

import { readFile } from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import { pathToFileURL } from "node:url";

export const DEFAULT_LABEL = "awaiting-author";

const LABEL_COLOR = "FBCA04";
const LABEL_DESCRIPTION = "변경요청 뒤 작성자 조치 없음 — 공은 작성자에게 있다";
// 25건 × (리뷰·커밋·코멘트·본문 편집 100) 를 한 번에 물으면 GraphQL 이 시간 안에 못 돌려
// 504 를 낸다
// (8/30 실측). 페이지를 잘게 끊고, 하위 컬렉션도 «판정 이후» 를 덮을 만큼만 가져온다. 모두
// `last` 인 것은 필요한 쪽이 최근이기 때문이다 — 판정은 최신 결정 리뷰이고, 반영·응답은 그
// 시각 이후만 센다. `first` 로 앞부분을 가져오면 정작 볼 구간이 잘린다.
const PULL_REQUEST_PAGE_SIZE = 10;

const OPEN_PULL_REQUESTS_QUERY = `
query($owner: String!, $name: String!, $cursor: String, $pageSize: Int!) {
    repository(owner: $owner, name: $name) {
        pullRequests(states: OPEN, first: $pageSize, after: $cursor) {
            pageInfo { hasNextPage endCursor }
            nodes {
                number
                title
                isDraft
                createdAt
                author { login }
                headRepository { nameWithOwner }
                labels(first: 50) { nodes { name } }
                reviews(last: 50) {
                    pageInfo { hasPreviousPage }
                    nodes {
                        state
                        submittedAt
                        authorCanPushToRepository
                        author { login }
                    }
                }
                commits(last: 50) {
                    pageInfo { hasPreviousPage }
                    nodes {
                        commit {
                            committedDate
                            changedFilesIfAvailable
                            parents(first: 2) { totalCount }
                            author { email user { login } }
                        }
                    }
                }
                comments(last: 50) {
                    pageInfo { hasPreviousPage }
                    nodes {
                        createdAt
                        author { login }
                    }
                }
                userContentEdits(last: 50) {
                    pageInfo { hasPreviousPage }
                    nodes {
                        editedAt
                        editor { login }
                    }
                }
            }
        }
    }
}`;

const OPEN_PULL_REQUESTS_BY_AUTHOR_QUERY = `
query($searchQuery: String!, $cursor: String, $pageSize: Int!) {
    search(query: $searchQuery, type: ISSUE, first: $pageSize, after: $cursor) {
        pageInfo { hasNextPage endCursor }
        nodes {
            ... on PullRequest {
                number
                title
                isDraft
                createdAt
                author { login }
                headRepository { nameWithOwner }
                labels(first: 50) { nodes { name } }
                reviews(last: 50) {
                    pageInfo { hasPreviousPage }
                    nodes {
                        state
                        submittedAt
                        authorCanPushToRepository
                        author { login }
                    }
                }
                commits(last: 50) {
                    pageInfo { hasPreviousPage }
                    nodes {
                        commit {
                            committedDate
                            changedFilesIfAvailable
                            parents(first: 2) { totalCount }
                            author { email user { login } }
                        }
                    }
                }
                comments(last: 50) {
                    pageInfo { hasPreviousPage }
                    nodes {
                        createdAt
                        author { login }
                    }
                }
                userContentEdits(last: 50) {
                    pageInfo { hasPreviousPage }
                    nodes {
                        editedAt
                        editor { login }
                    }
                }
            }
        }
    }
}`;

// 판정 함수가 각 connection 에서 읽는 시각 필드. reviews 는 판정 기준 시각 자체를 구하는
// 근거라 여기 넣지 않는다.
const ACTIVITY_TIMESTAMP_BY_CONNECTION = Object.freeze({
    commits: (node) => node?.commit?.committedDate,
    comments: (node) => node?.createdAt,
    userContentEdits: (node) => node?.editedAt,
});

function sameLogin(a, b) {
    return typeof a === "string" && typeof b === "string" && a.toLowerCase() === b.toLowerCase();
}

/**
 * PR 전체에서 «최신 결정 리뷰» 를 고른다.
 *
 * 쓰기 권한이 있는 팀원의 판정만 인정하고 리뷰어별로 묶지 않는다. 외부인의 변경요청으로 팀원의
 * 새 PR 을 막거나, 외부인의 승인으로 팀 변경요청을 지울 수 없다. 팀에서 한 사람이 더 늦은 판정을
 * 내렸다면 그것이 PR 의 현재 상태이고, 더 오래된 다른 팀원의 변경요청을 다시 살리지 않는다.
 */
export function latestDecision(reviews) {
    const decisions = (reviews ?? [])
        .filter((review) => review?.state === "APPROVED" || review?.state === "CHANGES_REQUESTED")
        .filter((review) => review?.authorCanPushToRepository === true)
        .filter((review) => typeof review.submittedAt === "string")
        .sort((a, b) => (a.submittedAt < b.submittedAt ? -1 : a.submittedAt > b.submittedAt ? 1 : 0));

    return decisions.length > 0 ? decisions[decisions.length - 1] : null;
}

/**
 * `last: 50` 이 판정에 필요한 범위를 덮는지 본다.
 *
 * 판정은 최신 결정 시각 이후 항목만 세므로, 가져온 것 중 가장 오래된 항목이 그 시각보다 앞서면
 * 그 이후는 전부 들어와 있다. `reviews` 는 그 시각 자체를 구하는 근거라 여기서 제외한다.
 */
export function isCoveredSinceDecision(connectionName, nodes, since) {
    const readTimestamp = ACTIVITY_TIMESTAMP_BY_CONNECTION[connectionName];
    if (!readTimestamp) {
        return false;
    }
    // 최신 결정이 없거나 변경요청이 아니면 judgeAwaitingAuthor 가 이 connection 을 읽지 않는다.
    if (since === null || since === undefined) {
        return true;
    }
    if (!Array.isArray(nodes) || nodes.length === 0) {
        return false;
    }
    const oldest = readTimestamp(nodes[0]);
    return typeof oldest === "string" && oldest <= since;
}

/**
 * 판정 시각 이후 작성자가 올린 «실질» 커밋 수를 센다.
 *
 * 병합 커밋은 빼고(base 를 끌어온 변경이 반영분과 구별되지 않는다), 바뀐 파일이 0건인 커밋도
 * 뺀다. 누가 올렸는지를 보지 않으면 리뷰어가 미는 CI 재트리거 빈 커밋이 «작성자가 반영했다» 가
 * 되어 가짜 판정이 된다 — 8/28 에 그 한 건으로 PR 3건이 빚으로 잡혔다 (#1459).
 *
 * 계정이 연결되지 않은 커밋은 login 이 비어 가릴 수 없다. 같은 PR 에서 작성자 것으로 확인된
 * 커밋의 이메일을 폴백 신원으로 쓴다.
 *
 * 변경 파일 수를 알 수 없으면(`changedFilesIfAvailable` 가 null) 반영으로 센다. 조회 공백을
 * «빈 커밋» 으로 접으면 라벨이 없는 근거로 둔갑한다 — 놓치는 쪽이 잘못 붙이는 쪽보다 안전하다.
 */
export function countAuthorFixes({ commits, author, since }) {
    const nodes = (commits ?? []).map((node) => node?.commit).filter(Boolean);

    const ownEmails = new Set();
    for (const commit of nodes) {
        if (sameLogin(commit.author?.user?.login, author) && commit.author?.email) {
            ownEmails.add(commit.author.email.toLowerCase());
        }
    }

    let fixes = 0;
    for (const commit of nodes) {
        if ((commit.parents?.totalCount ?? 0) >= 2) {
            continue;
        }
        if (!(typeof commit.committedDate === "string" && commit.committedDate > since)) {
            continue;
        }

        const login = commit.author?.user?.login;
        const email = commit.author?.email?.toLowerCase();
        const isAuthor = login ? sameLogin(login, author) : Boolean(email && ownEmails.has(email));
        if (!isAuthor) {
            continue;
        }

        const changed = commit.changedFilesIfAvailable;
        if (changed === 0) {
            continue;
        }
        fixes += 1;
    }

    return fixes;
}

/**
 * 판정 시각 이후 작성자가 남긴 응답 수를 센다.
 *
 * 커밋만 세면 base 를 merge 로 끌어와 반영한 PR 이 «무조치» 가 된다 — #1316 은 리뷰가 지목한
 * 파일이 실제로 바뀌고 응답 코멘트도 2건 달린 채 빚에서 빠져 있었다 (#1450).
 *
 * 라인 코멘트는 따로 조회하지 않는다. 작성자가 라인 코멘트를 남기면 그 코멘트를 담은 리뷰가
 * 함께 제출되므로 `reviews` 에서 잡힌다. PR 본문 편집은 별도 이력이므로 아래
 * `countAuthorBodyEdits` 에서 센다.
 */
export function countAuthorResponses({ comments, reviews, author, since }) {
    const issueComments = (comments ?? []).filter(
        (comment) =>
            sameLogin(comment?.author?.login, author) &&
            typeof comment?.createdAt === "string" &&
            comment.createdAt > since,
    ).length;

    const authorReviews = (reviews ?? []).filter(
        (review) =>
            sameLogin(review?.author?.login, author) &&
            typeof review?.submittedAt === "string" &&
            review.submittedAt > since,
    ).length;

    return issueComments + authorReviews;
}

/**
 * 판정 시각 이후 PR 작성자가 직접 고친 본문 편집 수를 센다.
 *
 * PR 본문의 CI Test Plan 같이 커밋 밖에 있는 리뷰 지적은 본문 편집만으로 반영될 수 있다.
 * `updatedAt` 은 라벨·리뷰어 등 다른 메타데이터 변경도 섞이므로 쓰지 않고, GitHub 가 남기는
 * `userContentEdits` 의 실제 편집자와 시각만 본다. 리뷰어의 대리 편집이나 편집자 정보가 사라진
 * 기록은 작성자 조치로 추정하지 않는다.
 */
export function countAuthorBodyEdits({ userContentEdits, author, since }) {
    return (userContentEdits ?? []).filter(
        (edit) =>
            sameLogin(edit?.editor?.login, author) &&
            typeof edit?.editedAt === "string" &&
            edit.editedAt > since,
    ).length;
}

/**
 * PR 한 건이 «변경요청 뒤 작성자 무조치» 인지 판정한다.
 *
 * 제외 축은 가드와 같다. draft 는 리뷰를 받을 상태가 아니고, 봇 PR 은 사람이 반영할 주체가
 * 없으며, fork PR 은 팀 리뷰 적체와 무관하다.
 */
export function judgeAwaitingAuthor(pullRequest, { repository } = {}) {
    const author = pullRequest?.author?.login ?? "";

    if (pullRequest?.isDraft) {
        return { awaiting: false, reason: "draft" };
    }
    if (!author) {
        return { awaiting: false, reason: "작성자 없음" };
    }
    if (/\[bot\]$/i.test(author) || author === "dependabot" || author === "github-actions") {
        return { awaiting: false, reason: "봇 PR" };
    }

    const headRepository = pullRequest?.headRepository?.nameWithOwner;
    if (repository && headRepository && headRepository !== repository) {
        return { awaiting: false, reason: `fork PR(${headRepository})` };
    }

    const decision = latestDecision(pullRequest?.reviews?.nodes);
    if (!decision) {
        return { awaiting: false, reason: "결정 리뷰 없음" };
    }
    if (decision.state !== "CHANGES_REQUESTED") {
        return { awaiting: false, reason: "최신 판정이 변경요청이 아님" };
    }

    const since = decision.submittedAt;
    const fixes = countAuthorFixes({
        commits: pullRequest?.commits?.nodes,
        author,
        since,
    });
    const responses = countAuthorResponses({
        comments: pullRequest?.comments?.nodes,
        reviews: pullRequest?.reviews?.nodes,
        author,
        since,
    });
    const bodyEdits = countAuthorBodyEdits({
        userContentEdits: pullRequest?.userContentEdits?.nodes,
        author,
        since,
    });

    if (fixes > 0 || responses > 0 || bodyEdits > 0) {
        return {
            awaiting: false,
            reason: `작성자 조치 있음(${fixes}커밋 · 응답 ${responses}건 · 본문 편집 ${bodyEdits}건)`,
            decidedAt: since,
        };
    }

    return {
        awaiting: true,
        reason: "변경요청 뒤 작성자 조치 없음",
        decidedAt: since,
        reviewer: decision.author?.login ?? "",
    };
}

/**
 * 새 PR 작성자가 먼저 처리해야 할 다른 열린 PR 을 고른다.
 *
 * 열린 PR 목록은 호출자가 한 번 live 조회해 넘긴다. 현재 PR 은 이벤트 직후 그 목록에 이미
 * 포함되므로 번호로 명시적으로 제외한다. 작성자 login 은 GitHub 의 대소문자 비구분 규약에
 * 맞춰 비교하고, 실제 무조치 여부는 라벨이 아니라 `judgeAwaitingAuthor` 로 매번 다시 판정한다.
 */
export function findAuthorDebts({ pullRequests, repository, author, currentPullRequestNumber }) {
    if (!Array.isArray(pullRequests)) {
        throw new TypeError("pullRequests 배열이 필요합니다.");
    }
    if (typeof author !== "string" || author.trim() === "") {
        throw new TypeError("author 가 필요합니다.");
    }

    const currentNumber = Number(currentPullRequestNumber);
    if (!Number.isSafeInteger(currentNumber) || currentNumber <= 0) {
        throw new TypeError("currentPullRequestNumber 는 양의 정수여야 합니다.");
    }

    const debts = [];
    for (const pullRequest of pullRequests) {
        if (Number(pullRequest?.number) === currentNumber) {
            continue;
        }
        if (!sameLogin(pullRequest?.author?.login, author)) {
            continue;
        }

        const verdict = judgeAwaitingAuthor(pullRequest, { repository });
        if (!verdict.awaiting) {
            continue;
        }

        debts.push({
            number: pullRequest.number,
            createdDate:
                typeof pullRequest.createdAt === "string" ? pullRequest.createdAt.slice(0, 10) : "",
            reviewer: verdict.reviewer ?? "",
            title: pullRequest.title ?? "",
        });
    }

    return debts;
}

/**
 * 라벨을 붙일 PR 과 뗄 PR 을 한 번에 계산한다.
 *
 * 매 실행이 현재 집합을 다시 세우고 차이만 쓴다. 조치가 들어오면 다음 실행에서 라벨이 떨어지므로
 * 스테일 라벨이 남지 않는다.
 */
export function planAwaitingAuthorLabels({ pullRequests, repository, label = DEFAULT_LABEL, exemptAuthors = [] }) {
    const toLabel = [];
    const toUnlabel = [];
    const unchanged = [];

    for (const pullRequest of pullRequests ?? []) {
        const labels = (pullRequest?.labels?.nodes ?? []).map((node) => node?.name);
        const labeled = labels.includes(label);
        const exempt = exemptAuthors.some((login) => sameLogin(login, pullRequest?.author?.login));
        const verdict = exempt
            ? { awaiting: false, reason: "리뷰 게이트 면제 작성자" }
            : judgeAwaitingAuthor(pullRequest, { repository });
        const entry = {
            number: pullRequest.number,
            title: pullRequest.title,
            author: pullRequest?.author?.login ?? "",
            reason: verdict.reason,
            decidedAt: verdict.decidedAt,
            reviewer: verdict.reviewer,
        };

        if (verdict.awaiting && !labeled) {
            toLabel.push(entry);
        } else if (!verdict.awaiting && labeled) {
            toUnlabel.push(entry);
        } else {
            unchanged.push({ ...entry, labeled });
        }
    }

    return { toLabel, toUnlabel, unchanged };
}

export function renderSummary({ plan, dryRun, label = DEFAULT_LABEL }) {
    const lines = [`## \`${label}\` 리컨사일`];
    if (dryRun) {
        lines.push("", "> DRY_RUN — 라벨을 쓰지 않고 계획만 출력합니다.");
    }

    lines.push("", `- 붙임 ${plan.toLabel.length}건 · 뗌 ${plan.toUnlabel.length}건`);

    if (plan.toLabel.length > 0) {
        lines.push("", "| PR | 작성자 | 변경요청 | 리뷰어 |", "|---|---|---|---|");
        for (const entry of plan.toLabel) {
            lines.push(
                `| #${entry.number} | @${entry.author} | ${entry.decidedAt?.slice(0, 10) ?? "-"} | @${entry.reviewer || "-"} |`,
            );
        }
    }

    if (plan.toUnlabel.length > 0) {
        lines.push("", "**뗀 PR**", "");
        for (const entry of plan.toUnlabel) {
            lines.push(`- #${entry.number} — ${entry.reason}`);
        }
    }

    return lines.join("\n");
}

export function createApi(token, { fetchImpl = globalThis.fetch } = {}) {
    if (typeof fetchImpl !== "function") {
        throw new TypeError("fetch 구현이 필요합니다.");
    }

    return async function api(apiPath, { method = "GET", body, allowNotFound = false } = {}) {
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

        if (allowNotFound && response.status === 404) {
            return null;
        }
        if (!response.ok) {
            const detail = await response.text();
            throw new Error(`GitHub API ${method} ${apiPath} 실패: ${response.status} ${detail}`);
        }
        if (response.status === 204) {
            return null;
        }
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

export async function fetchOpenPullRequests(api, repository) {
    const [owner, name] = repository.split("/");
    if (!owner || !name || repository.split("/").length !== 2) {
        throw new TypeError("repository 는 owner/name 형식이어야 합니다.");
    }

    const pullRequests = [];
    let cursor = null;

    for (;;) {
        const data = await graphql(api, OPEN_PULL_REQUESTS_QUERY, {
            owner,
            name,
            cursor,
            pageSize: PULL_REQUEST_PAGE_SIZE,
        });
        const page = data?.repository?.pullRequests;
        if (!page) {
            throw new Error("GraphQL 응답에 열린 PR 목록이 없습니다.");
        }
        if (!Array.isArray(page.nodes)) {
            throw new Error("GraphQL 열린 PR nodes 가 배열이 아닙니다.");
        }
        pullRequests.push(...(page.nodes ?? []).filter(Boolean));
        if (!page.pageInfo?.hasNextPage) {
            break;
        }
        cursor = page.pageInfo.endCursor;
        if (!cursor) {
            throw new Error("GraphQL 다음 페이지 cursor 가 없습니다.");
        }
    }

    return pullRequests;
}

/**
 * 입장 게이트용으로 한 작성자의 열린 PR 만 조회한다.
 *
 * 라벨 리컨사일러는 전체 열린 PR 이 필요하지만, 새 PR 입장마다 그 전체의 하위 리뷰·커밋·
 * 코멘트를 다시 읽을 필요는 없다. GitHub search 로 작성자를 서버에서 먼저 제한한다.
 */
export async function fetchOpenPullRequestsByAuthor(api, repository, author) {
    const [owner, name] = repository.split("/");
    if (!owner || !name || repository.split("/").length !== 2) {
        throw new TypeError("repository 는 owner/name 형식이어야 합니다.");
    }
    if (typeof author !== "string" || author.trim() === "") {
        throw new TypeError("author 가 필요합니다.");
    }

    const pullRequests = [];
    let cursor = null;

    for (;;) {
        const data = await graphql(api, OPEN_PULL_REQUESTS_BY_AUTHOR_QUERY, {
            searchQuery: `repo:${owner}/${name} is:pr is:open author:${author}`,
            cursor,
            pageSize: PULL_REQUEST_PAGE_SIZE,
        });
        const page = data?.search;
        if (!page) {
            throw new Error("GraphQL 응답에 작성자의 열린 PR 검색 결과가 없습니다.");
        }
        if (!Array.isArray(page.nodes)) {
            throw new Error("GraphQL 작성자 PR nodes 가 배열이 아닙니다.");
        }

        for (const pullRequest of page.nodes) {
            if (!pullRequest || !Number.isSafeInteger(pullRequest.number)) {
                throw new Error("GraphQL 작성자 PR 검색 결과가 불완전합니다.");
            }

            // `last: 50` 앞에 더 많은 항목이 있으면 최신 결정 시각 이후의 작성자 이메일·응답·
            // 본문 편집을 완전하게 판정할 수 없다. 라벨은 다음 리컨사일에서 복구할 수 있지만,
            // 입장 가드가 불완전한 근거로 PR 을 닫는 것은 되돌리기 비용이 있으므로 판정 자체를
            // 중단한다.
            //
            // 다만 완전성의 기준은 «전체 이력» 이 아니라 «판정이 실제로 읽는 범위» 다. 세 판정
            // 함수는 모두 최신 결정 시각(since) 이후 항목만 세고, judgeAwaitingAuthor 는 최신
            // 결정이 없거나 변경요청이 아니면 그 셋을 아예 부르지 않는다. `last: 50` 은 최신
            // 50건이므로 가져온 것 중 가장 오래된 항목이 since 보다 앞서면 since 이후는 전부
            // 확보한 것이고 판정은 완전하다 — 커밋이 몇백 건이든 상관없다. 이 구분이 없어서
            // 커밋 403건짜리 릴리스 PR(develop → main) 하나가 그 작성자의 새 PR 을 전부 막았다
            // (#1787). reviews 는 since 를 구하는 근거라 시각으로 대체 판정할 수 없어 그대로 둔다.
            const decision = latestDecision(pullRequest.reviews?.nodes);
            const since = decision?.state === "CHANGES_REQUESTED" ? decision.submittedAt : null;
            for (const connectionName of ["reviews", "commits", "comments", "userContentEdits"]) {
                const connection = pullRequest[connectionName];
                if (!connection || !Array.isArray(connection.nodes)) {
                    throw new Error(`GraphQL 작성자 PR ${connectionName} 응답이 불완전합니다.`);
                }
                if (connection.pageInfo?.hasPreviousPage === false) {
                    continue;
                }
                if (isCoveredSinceDecision(connectionName, connection.nodes, since)) {
                    continue;
                }
                throw new Error(
                    `GraphQL 작성자 PR ${connectionName} 최근 50건이 완전하지 않습니다.`,
                );
            }
            pullRequests.push(pullRequest);
        }

        if (!page.pageInfo?.hasNextPage) {
            break;
        }
        cursor = page.pageInfo.endCursor;
        if (!cursor) {
            throw new Error("GraphQL 작성자 PR 다음 페이지 cursor 가 없습니다.");
        }
    }

    return pullRequests;
}

export async function ensureLabelExists(api, repository, label = DEFAULT_LABEL) {
    const existing = await api(`/repos/${repository}/labels/${encodeURIComponent(label)}`, {
        allowNotFound: true,
    });
    if (existing) {
        return;
    }
    await api(`/repos/${repository}/labels`, {
        method: "POST",
        body: { name: label, color: LABEL_COLOR, description: LABEL_DESCRIPTION },
    });
}

export async function applyPlan(api, repository, plan, { dryRun = false, label = DEFAULT_LABEL, logger = console } = {}) {
    const failures = [];

    for (const entry of plan.toLabel) {
        if (dryRun) {
            logger.log(`(dry-run) #${entry.number} ${label} 부착`);
            continue;
        }
        try {
            await api(`/repos/${repository}/issues/${entry.number}/labels`, {
                method: "POST",
                body: { labels: [label] },
            });
            logger.log(`#${entry.number} ${label} 부착 — ${entry.reason}`);
        } catch (error) {
            failures.push(`#${entry.number} 라벨 부착 실패: ${error.message}`);
        }
    }

    for (const entry of plan.toUnlabel) {
        if (dryRun) {
            logger.log(`(dry-run) #${entry.number} ${label} 제거`);
            continue;
        }
        try {
            await api(
                `/repos/${repository}/issues/${entry.number}/labels/${encodeURIComponent(label)}`,
                { method: "DELETE", allowNotFound: true },
            );
            logger.log(`#${entry.number} ${label} 제거 — ${entry.reason}`);
        } catch (error) {
            failures.push(`#${entry.number} 라벨 제거 실패: ${error.message}`);
        }
    }

    return failures;
}

/** 가드의 한 줄 환경변수 선언을 읽는다. 형식이 달라지면 빈 면제 목록으로 숨기지 않는다. */
export function parseReviewGateExemptAuthors(workflow) {
    const lines = workflow.split(/\r?\n/);
    const declarations = lines.flatMap((line, index) => {
        const match = /^([ \t]+)REVIEW_GATE_EXEMPT_AUTHORS:[ \t]*(.*)$/.exec(line);
        return match ? [{ index, indent: match[1].length, value: match[2].trim() }] : [];
    });
    if (declarations.length !== 1) {
        throw new Error("REVIEW_GATE_EXEMPT_AUTHORS 선언이 정확히 한 개 필요합니다.");
    }
    const { index, indent, value } = declarations[0];
    const nextLine = lines.slice(index + 1).find((line) => line.trim() && !line.trimStart().startsWith("#"));
    const continued = nextLine && /^[ \t]*/.exec(nextLine)[0].length > indent;
    if (continued || !/^[a-zA-Z0-9-]+(?:[ \t]+[a-zA-Z0-9-]+)*$/.test(value)) {
        throw new Error("REVIEW_GATE_EXEMPT_AUTHORS는 인용·주석 없는 한 줄의 공백 구분 로그인 목록이어야 합니다.");
    }
    return value.split(/\s+/).map((login) => login.toLowerCase());
}

async function main() {
    const token = process.env.GITHUB_TOKEN;
    const repository = process.env.GITHUB_REPOSITORY;
    if (!token || !repository) {
        throw new Error("GITHUB_TOKEN·GITHUB_REPOSITORY 가 필요합니다.");
    }

    const label = process.env.AWAITING_AUTHOR_LABEL ?? DEFAULT_LABEL;
    const dryRun = process.env.DRY_RUN === "true";
    const api = createApi(token);

    // 전체 checkout을 쓰는 라벨 CLI에서만 읽는다. 가드의 scripts-only checkout에서도
    // 상태 판정 함수를 import할 수 있어야 한다. 면제 목록의 정본은 기존 가드 선언이다.
    const exemptAuthors = parseReviewGateExemptAuthors(
        await readFile(new URL("../workflows/review-debt-guard.yml", import.meta.url), "utf8"),
    );
    const pullRequests = await fetchOpenPullRequests(api, repository);
    const plan = planAwaitingAuthorLabels({ pullRequests, repository, label, exemptAuthors });

    if (!dryRun && plan.toLabel.length > 0) {
        await ensureLabelExists(api, repository, label);
    }

    const failures = await applyPlan(api, repository, plan, { dryRun, label });
    const summary = renderSummary({ plan, dryRun, label });
    console.log(summary);

    if (process.env.GITHUB_STEP_SUMMARY) {
        const { appendFile } = await import("node:fs/promises");
        await appendFile(process.env.GITHUB_STEP_SUMMARY, `${summary}\n`);
    }

    if (failures.length > 0) {
        throw new Error(failures.join("\n"));
    }
}

const invokedPath = process.argv[1] ? pathToFileURL(path.resolve(process.argv[1])).href : "";
if (import.meta.url === invokedPath) {
    main().catch((error) => {
        console.error(error.message);
        process.exitCode = 1;
    });
}
