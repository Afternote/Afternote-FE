#!/usr/bin/env node

// 열린 PR 이 있는 이슈에 라벨을 붙여 «이미 누가 하고 있다» 를 이슈 쪽에서 보이게 한다 (#1532).
//
// GitHub 은 closing 키워드를 기본 브랜치를 target 하는 PR 에서만 실제 링크로 만든다. base 가
// 다른 스택 PR 은 본문에 `Closes #N` 이 있어도 `closingIssuesReferences` 가 비고, 이슈의
// Development 칸에 아무것도 뜨지 않는다. 남는 것은 타임라인의 «mentioned this issue» 한 줄뿐이라
// 다른 참조들 사이에 묻히고, 이슈만 본 사람은 미착수로 읽는다.
//
// 머지 시점의 같은 공백은 `auto-close-issue.yml` 이 이미 메웠다(#294). 비어 있는 것은 PR 이
// 열려 있는 동안이고, #686 에서 확정한 스택 순서(상위를 develop 에 먼저 머지 → 하위 base 를
// develop 로 리타겟)를 따르면 머지 시점의 base 는 늘 기본 브랜치이므로 그 우회로는 닫히지 않는다.
//
// 이벤트가 아니라 리컨사일러인 이유 둘. (1) `pull_request` 로 돌면 dependabot 이 연 PR 은 토큰이
// read-only 라 라벨을 못 붙인다. (2) 이벤트마다 조회하면 호출이 PR 수만큼 곱해진다 — 이 스크립트는
// 열린 PR 1회 + 라벨 보유 이슈 1회 + 대상 종류 확인 1회를 읽고 차이만 쓴다 (#1465).

import path from "node:path";
import process from "node:process";
import { pathToFileURL } from "node:url";

import { extractTitleIssueNumber } from "./validate-pr-issue-link.mjs";

export const DEFAULT_LABEL = "pr-open";
export const COMMENT_MARKER_PREFIX = "<!-- issue-pr-open:";

const LABEL_COLOR = "1D76DB";
const LABEL_DESCRIPTION = "이 이슈를 구현하는 PR 이 열려 있다 — base 가 기본 브랜치가 아니어도 드러낸다";
const PULL_REQUEST_PAGE_SIZE = 50;
const ISSUE_PAGE_SIZE = 100;

const OPEN_PULL_REQUESTS_QUERY = `
query($owner: String!, $name: String!, $cursor: String, $pageSize: Int!) {
    repository(owner: $owner, name: $name) {
        pullRequests(states: OPEN, first: $pageSize, after: $cursor) {
            pageInfo { hasNextPage endCursor }
            nodes {
                number
                title
                baseRefName
                isDraft
            }
        }
    }
}`;

export function commentMarker(pullRequestNumber) {
    return `${COMMENT_MARKER_PREFIX}${pullRequestNumber} -->`;
}

/**
 * 열린 PR 목록과 현재 라벨 보유 이슈로부터 라벨·코멘트 계획을 세운다.
 *
 * 대표 이슈 판정은 `validate-pr-issue-link.mjs` 의 파서를 그대로 쓴다. Repository Quality 가
 * 모든 PR 제목에 `(#N)` 하나를 강제하므로 여기서 새 규칙을 만들면 두 판정이 갈린다.
 *
 * 제목이 그 형식이 아닌 PR 은 건너뛴다 — 게이트가 이미 그 PR 을 막고 있고, 여기서 본문의 느슨한
 * `Refs #N` 까지 주우면 «참고로 언급한 이슈» 에까지 라벨이 붙어 라벨의 뜻이 무너진다.
 */
export function planIssueLabelChanges({
    pullRequests,
    labeledIssueNumbers,
    defaultBranch,
    label = DEFAULT_LABEL,
}) {
    const byIssue = new Map();
    const skipped = [];

    for (const pullRequest of pullRequests) {
        const issueNumber = extractTitleIssueNumber(pullRequest.title);
        if (issueNumber === null) {
            skipped.push(pullRequest);
            continue;
        }
        if (!byIssue.has(issueNumber)) {
            byIssue.set(issueNumber, []);
        }
        byIssue.get(issueNumber).push(pullRequest);
    }

    const labeled = new Set(labeledIssueNumbers);
    const toLabel = [];
    const toUnlabel = [];
    const comments = [];

    for (const [issueNumber, issuePullRequests] of [...byIssue].sort((a, b) => a[0] - b[0])) {
        if (!labeled.has(issueNumber)) {
            toLabel.push({ issueNumber, pullRequests: issuePullRequests });
        }
        // 코멘트는 GitHub 이 아무것도 보여 주지 않는 PR 에만 단다. base 가 기본 브랜치면
        // Development 칸이 이미 그 PR 을 띄우므로 같은 말을 한 번 더 하는 소음이 된다.
        for (const pullRequest of issuePullRequests) {
            if (pullRequest.baseRefName !== defaultBranch) {
                comments.push({ issueNumber, pullRequest });
            }
        }
    }

    for (const issueNumber of [...labeled].sort((a, b) => a - b)) {
        if (!byIssue.has(issueNumber)) {
            toUnlabel.push(issueNumber);
        }
    }

    return { label, toLabel, toUnlabel, comments, skipped };
}

/**
 * 코멘트는 PR 상태를 글로 적지 않는다.
 *
 * GitHub 은 본문의 `#1511` 을 렌더할 때 open·merged·closed 배지를 함께 그린다. 상태를 문장으로
 * 박아 두면 PR 이 닫힌 뒤 코멘트만 거짓말을 하게 되므로, 상태는 그 배지에 맡기고 여기서는
 * «어느 PR 이 이 이슈를 맡았는가» 와 «왜 Development 칸이 비어 있는가» 만 남긴다.
 */
export function renderLinkComment({ pullRequest, defaultBranch }) {
    return [
        commentMarker(pullRequest.number),
        `이 이슈를 맡은 PR 은 **#${pullRequest.number}** 다 (base \`${pullRequest.baseRefName}\`).`,
        "",
        `base 가 기본 브랜치(\`${defaultBranch}\`)가 아니라서 GitHub 은 이 이슈의 Development 칸에`,
        "아무것도 표시하지 않는다. 본문에 `Closes #N` 이 있어도 마찬가지다 — 그 링크는 기본 브랜치를",
        "target 하는 PR 에서만 만들어진다.",
        "",
        "PR 이 닫히거나 머지되면 이 이슈의 `pr-open` 라벨은 다음 리컨사일에서 떨어진다.",
    ].join("\n");
}

export function renderSummary({ plan, dryRun }) {
    return [
        `## 열린 PR 이 있는 이슈 라벨 (\`${plan.label}\`)${dryRun ? " — dry run" : ""}`,
        "",
        `- 라벨 부착: ${plan.toLabel.length}건${formatIssues(plan.toLabel.map((entry) => entry.issueNumber))}`,
        `- 라벨 제거: ${plan.toUnlabel.length}건${formatIssues(plan.toUnlabel)}`,
        `- 링크 코멘트 대상(base≠기본 브랜치): ${plan.comments.length}건${formatIssues(plan.comments.map((entry) => entry.issueNumber))}`,
        `- 대표 이슈 미판정: ${plan.skipped.length}건${formatIssues(plan.skipped.map((pullRequest) => pullRequest.number), "#")}`,
    ].join("\n");
}

function formatIssues(numbers, prefix = "#") {
    if (numbers.length === 0) {
        return "";
    }
    return ` — ${numbers.map((number) => `${prefix}${number}`).join(", ")}`;
}

function normalizePullRequest(node) {
    return {
        number: node.number,
        title: node.title,
        baseRefName: node.baseRefName,
        isDraft: node.isDraft,
    };
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
        const page = data.repository.pullRequests;
        pullRequests.push(...page.nodes.map(normalizePullRequest));

        if (!page.pageInfo.hasNextPage) {
            return pullRequests;
        }
        cursor = page.pageInfo.endCursor;
    }
}

/**
 * 라벨을 들고 있는 이슈를 모은다.
 *
 * 닫힌 이슈까지 보는 이유: PR 이 살아 있는 채로 이슈가 먼저 닫히면 라벨이 남는데, `state=open`
 * 만 보면 그 이슈가 목록에 없어 «뗄 것이 없다» 로 읽혀 라벨이 영구히 붙어 있게 된다.
 */
export async function fetchLabeledIssueNumbers(api, repository, label) {
    const numbers = [];
    for (let page = 1; ; page += 1) {
        const items = await api(
            `/repos/${repository}/issues?labels=${encodeURIComponent(label)}&state=all&per_page=${ISSUE_PAGE_SIZE}&page=${page}`,
        );
        const batch = items ?? [];
        for (const item of batch) {
            // 라벨은 PR 에도 붙일 수 있고 이 엔드포인트는 PR 도 함께 돌려준다. PR 은 이 스크립트의
            // 관리 대상이 아니므로 계획에서 빼야 «뗄 대상» 으로 오인되지 않는다.
            if (!item.pull_request) {
                numbers.push(item.number);
            }
        }
        if (batch.length < ISSUE_PAGE_SIZE) {
            return numbers;
        }
    }
}

/**
 * 라벨을 붙이기 전에 대상이 정말 이슈인지 한 번에 확인한다.
 *
 * 제목의 `(#N)` 이 이슈가 아니라 PR 번호일 수 있다 — Repository Quality 가 그런 PR 을 막지만
 * 리컨사일러는 게이트를 통과하지 못한 PR 까지 훑는다. 번호마다 REST 로 물으면 첫 실행에서만
 * 수십 번이 되므로 alias 를 묶어 한 번에 판정한다.
 */
export async function resolveIssueNumbers(api, repository, numbers) {
    if (numbers.length === 0) {
        return new Set();
    }
    const [owner, name] = repository.split("/");
    const aliases = numbers
        .map((number) => `n${number}: issueOrPullRequest(number: ${number}) { __typename }`)
        .join("\n        ");
    const data = await graphql(
        api,
        `query($owner: String!, $name: String!) {
    repository(owner: $owner, name: $name) {
        ${aliases}
    }
}`,
        { owner, name },
    );

    const issues = new Set();
    for (const number of numbers) {
        if (data?.repository?.[`n${number}`]?.__typename === "Issue") {
            issues.add(number);
        }
    }
    return issues;
}

export async function ensureLabelExists(api, repository, label) {
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

async function hasLinkComment(api, repository, issueNumber, pullRequestNumber) {
    const marker = commentMarker(pullRequestNumber);
    for (let page = 1; ; page += 1) {
        const comments = await api(
            `/repos/${repository}/issues/${issueNumber}/comments?per_page=${ISSUE_PAGE_SIZE}&page=${page}`,
        );
        const batch = comments ?? [];
        if (batch.some((comment) => (comment.body ?? "").includes(marker))) {
            return true;
        }
        if (batch.length < ISSUE_PAGE_SIZE) {
            return false;
        }
    }
}

// `logger` 를 받는 이유는 label-conflicting-prs 와 같다 — 테스트가 실제 조작처럼 보이는 줄을
// CI 로그에 남기지 않게 하기 위해서다.
export async function applyPlan(
    api,
    repository,
    plan,
    { defaultBranch, dryRun, realIssues, logger = console },
) {
    const failures = [];
    const isIssue = (number) => realIssues === undefined || realIssues.has(number);

    for (const entry of plan.toLabel) {
        const numbers = entry.pullRequests.map((pullRequest) => `#${pullRequest.number}`).join(", ");
        if (!isIssue(entry.issueNumber)) {
            logger.log(`::warning::#${entry.issueNumber} 는 이슈가 아니다 — 라벨을 붙이지 않는다 (${numbers})`);
            continue;
        }
        try {
            if (dryRun) {
                logger.log(`[dry-run] #${entry.issueNumber} 라벨 부착 (${numbers})`);
                continue;
            }
            await api(`/repos/${repository}/issues/${entry.issueNumber}/labels`, {
                method: "POST",
                body: { labels: [plan.label] },
            });
            logger.log(`#${entry.issueNumber} 라벨 부착 (${numbers})`);
        } catch (error) {
            failures.push(`#${entry.issueNumber} 라벨 부착 실패: ${error.message}`);
        }
    }

    for (const issueNumber of plan.toUnlabel) {
        try {
            if (dryRun) {
                logger.log(`[dry-run] #${issueNumber} 라벨 제거`);
                continue;
            }
            await api(
                `/repos/${repository}/issues/${issueNumber}/labels/${encodeURIComponent(plan.label)}`,
                { method: "DELETE", allowNotFound: true },
            );
            logger.log(`#${issueNumber} 라벨 제거`);
        } catch (error) {
            failures.push(`#${issueNumber} 라벨 제거 실패: ${error.message}`);
        }
    }

    for (const { issueNumber, pullRequest } of plan.comments) {
        if (!isIssue(issueNumber)) {
            continue;
        }
        try {
            if (dryRun) {
                logger.log(`[dry-run] #${issueNumber} 링크 코멘트 (#${pullRequest.number})`);
                continue;
            }
            // 실행마다 같은 말이 쌓이면 이슈가 못 읽게 된다. PR 마다 마커를 하나 두고 있으면 건너뛴다.
            if (await hasLinkComment(api, repository, issueNumber, pullRequest.number)) {
                continue;
            }
            await api(`/repos/${repository}/issues/${issueNumber}/comments`, {
                method: "POST",
                body: { body: renderLinkComment({ pullRequest, defaultBranch }) },
            });
            logger.log(`#${issueNumber} 링크 코멘트 (#${pullRequest.number})`);
        } catch (error) {
            failures.push(`#${issueNumber} 링크 코멘트 실패: ${error.message}`);
        }
    }

    return failures;
}

async function main() {
    const token = process.env.GITHUB_TOKEN;
    const repository = process.env.GITHUB_REPOSITORY;
    const defaultBranch = process.env.GITHUB_DEFAULT_BRANCH;
    if (!token || !repository || !defaultBranch) {
        throw new Error("GITHUB_TOKEN·GITHUB_REPOSITORY·GITHUB_DEFAULT_BRANCH 가 필요합니다.");
    }

    const label = process.env.ISSUE_PR_OPEN_LABEL ?? DEFAULT_LABEL;
    const dryRun = process.env.DRY_RUN === "true";
    const api = createApi(token);

    const pullRequests = await fetchOpenPullRequests(api, repository);
    const labeledIssueNumbers = await fetchLabeledIssueNumbers(api, repository, label);
    const plan = planIssueLabelChanges({ pullRequests, labeledIssueNumbers, defaultBranch, label });

    const candidates = [...new Set(plan.toLabel.map((entry) => entry.issueNumber))];
    const realIssues = await resolveIssueNumbers(api, repository, candidates);

    if (!dryRun && plan.toLabel.some((entry) => realIssues.has(entry.issueNumber))) {
        await ensureLabelExists(api, repository, label);
    }

    const failures = await applyPlan(api, repository, plan, { defaultBranch, dryRun, realIssues });
    const summary = renderSummary({ plan, dryRun });
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
