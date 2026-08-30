#!/usr/bin/env node

// 변경요청을 받은 뒤 작성자가 아무 조치도 하지 않은 PR 에 라벨을 붙인다 (#1552).
//
// `review-debt-guard.yml` 은 설계상 **리뷰어의 빚**만 잡는다 — "재요청이 없거나 최신 변경요청
// 뒤 새 커밋이 없으면 빚이 아니다"(#1136). 공이 작성자에게 있는 PR 로 리뷰어를 벌하지 않는
// 것은 맞지만, 그 결과 **작성자 무조치는 어느 게이트에서도 신호가 나지 않는다.** 가드는 이미
// 이 상태를 식별하고 «변경요청 미반영, 공은 작성자에게» 로 로그에 찍은 뒤 버린다. 없던 판정을
// 새로 만드는 것이 아니라 버려지는 판정을 라벨로 남긴다.
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
// 판단한 PR 에 라벨이 없거나, 반대로 리뷰어 몫인 PR 에 라벨이 붙는다. 두 판정이 공유하는
// 술어는 `awaiting-author-policy.test.mjs` 가 잠근다.

import path from "node:path";
import process from "node:process";
import { pathToFileURL } from "node:url";

export const DEFAULT_LABEL = "awaiting-author";

const LABEL_COLOR = "FBCA04";
const LABEL_DESCRIPTION = "변경요청 뒤 작성자 조치 없음 — 공은 작성자에게 있다";
// 25건 × (리뷰·커밋·코멘트 100) 를 한 번에 물으면 GraphQL 이 시간 안에 못 돌려 504 를 낸다
// (8/30 실측). 페이지를 잘게 끊고, 하위 컬렉션도 «판정 이후» 를 덮을 만큼만 가져온다. 셋 다
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
                    nodes {
                        state
                        submittedAt
                        author { login }
                    }
                }
                commits(last: 50) {
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
                    nodes {
                        createdAt
                        author { login }
                    }
                }
            }
        }
    }
}`;

function sameLogin(a, b) {
    return typeof a === "string" && typeof b === "string" && a.toLowerCase() === b.toLowerCase();
}

/**
 * PR 전체에서 «최신 결정 리뷰» 를 고른다.
 *
 * 리뷰어별로 묶지 않는다. 팀에서 한 사람이 더 늦은 판정을 내렸다면 그것이 PR 의 현재 상태이고,
 * 더 오래된 다른 리뷰어의 변경요청을 다시 살리지 않는다 — 가드의 `sort_by(.t) | last` 와 같다.
 */
export function latestDecision(reviews) {
    const decisions = (reviews ?? [])
        .filter((review) => review?.state === "APPROVED" || review?.state === "CHANGES_REQUESTED")
        .filter((review) => typeof review.submittedAt === "string")
        .sort((a, b) => (a.submittedAt < b.submittedAt ? -1 : a.submittedAt > b.submittedAt ? 1 : 0));

    return decisions.length > 0 ? decisions[decisions.length - 1] : null;
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
 * 함께 제출되므로 `reviews` 에서 잡힌다.
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

    if (fixes > 0 || responses > 0) {
        return {
            awaiting: false,
            reason: `작성자 조치 있음(${fixes}커밋 · 응답 ${responses}건)`,
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
 * 라벨을 붙일 PR 과 뗄 PR 을 한 번에 계산한다.
 *
 * 매 실행이 현재 집합을 다시 세우고 차이만 쓴다. 조치가 들어오면 다음 실행에서 라벨이 떨어지므로
 * 스테일 라벨이 남지 않는다.
 */
export function planAwaitingAuthorLabels({ pullRequests, repository, label = DEFAULT_LABEL }) {
    const toLabel = [];
    const toUnlabel = [];
    const unchanged = [];

    for (const pullRequest of pullRequests ?? []) {
        const labels = (pullRequest?.labels?.nodes ?? []).map((node) => node?.name);
        const labeled = labels.includes(label);
        const verdict = judgeAwaitingAuthor(pullRequest, { repository });
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

function createApi(token) {
    return async function api(apiPath, { method = "GET", body, allowNotFound = false } = {}) {
        const response = await fetch(`https://api.github.com${apiPath}`, {
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
            break;
        }
        pullRequests.push(...(page.nodes ?? []).filter(Boolean));
        if (!page.pageInfo?.hasNextPage) {
            break;
        }
        cursor = page.pageInfo.endCursor;
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

async function main() {
    const token = process.env.GITHUB_TOKEN;
    const repository = process.env.GITHUB_REPOSITORY;
    if (!token || !repository) {
        throw new Error("GITHUB_TOKEN·GITHUB_REPOSITORY 가 필요합니다.");
    }

    const label = process.env.AWAITING_AUTHOR_LABEL ?? DEFAULT_LABEL;
    const dryRun = process.env.DRY_RUN === "true";
    const api = createApi(token);

    const pullRequests = await fetchOpenPullRequests(api, repository);
    const plan = planAwaitingAuthorLabels({ pullRequests, repository, label });

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
