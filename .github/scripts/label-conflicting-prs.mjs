#!/usr/bin/env node

// base 와 충돌한 PR 에 라벨을 붙여 «CI 가 돌지 않는 상태» 를 드러낸다 (#1028).
//
// `pull_request` 워크플로는 base 와 head 를 합친 가상 머지 커밋(`refs/pull/<N>/merge`)에서
// 실행되는데, 충돌이면 GitHub 이 그 ref 를 만들지 못해 워크플로가 **트리거조차 되지 않는다.**
// 체크가 실패하는 게 아니라 아예 생성되지 않으므로, 저자도 리뷰어도 「아직 안 끝났나」로 읽고
// 최신 커밋이 미검증인 채 남는다. 머지 자체는 mergeable_state=dirty 와 required check 의
// pending 이 막으므로, 이 스크립트가 고치는 것은 게이트가 아니라 가시성이다.

import path from "node:path";
import process from "node:process";
import { pathToFileURL } from "node:url";

export const DEFAULT_LABEL = "conflict";
export const COMMENT_MARKER = "<!-- conflict-label -->";

const LABEL_COLOR = "e99695";
const LABEL_DESCRIPTION = "base 와 충돌 — CI 가 트리거되지 않는다. base 를 병합하면 자동으로 떨어진다";
const PULL_REQUEST_PAGE_SIZE = 50;
const UNKNOWN_RESOLVE_ATTEMPTS = 3;
const UNKNOWN_RESOLVE_DELAY_MS = 2000;

const OPEN_PULL_REQUESTS_QUERY = `
query($owner: String!, $name: String!, $cursor: String, $pageSize: Int!) {
    repository(owner: $owner, name: $name) {
        pullRequests(states: OPEN, first: $pageSize, after: $cursor) {
            pageInfo { hasNextPage endCursor }
            nodes {
                number
                mergeable
                baseRefName
                headRefName
                isDraft
                labels(first: 30) { nodes { name } }
            }
        }
    }
}`;

/**
 * 조회 결과를 라벨 조작 계획으로 바꾼다.
 *
 * `UNKNOWN` 은 GitHub 이 mergeable 을 아직 계산하지 못한 상태다. 충돌로 단정하면 멀쩡한 PR 에
 * 라벨이 붙고, 그 오탐은 라벨이 없는 것보다 나쁘다 — 건드리지 않고 다음 실행에 맡긴다.
 */
export function planLabelChanges({ pullRequests, label = DEFAULT_LABEL }) {
    const toLabel = [];
    const toUnlabel = [];
    const skipped = [];

    for (const pullRequest of pullRequests) {
        const labels = pullRequest.labels ?? [];
        const labeled = labels.includes(label);

        if (pullRequest.mergeable === "CONFLICTING") {
            // 이미 붙어 있으면 아무것도 하지 않는다 — 코멘트가 실행마다 쌓이면 소음이 된다.
            if (!labeled) {
                toLabel.push(pullRequest);
            }
            continue;
        }

        if (pullRequest.mergeable === "MERGEABLE") {
            if (labeled) {
                toUnlabel.push(pullRequest);
            }
            continue;
        }

        skipped.push(pullRequest);
    }

    return { toLabel, toUnlabel, skipped };
}

export function renderConflictComment({ baseRefName }) {
    return [
        COMMENT_MARKER,
        `이 PR 은 \`${baseRefName}\` 와 충돌 상태이고, **그래서 CI 가 돌지 않는다.**`,
        "",
        "`pull_request` 워크플로는 base 와 head 를 합친 가상 머지 커밋(`refs/pull/<N>/merge`)에서",
        "실행되는데, 충돌이면 GitHub 이 그 ref 를 만들지 못해 워크플로가 트리거조차 되지 않는다.",
        "체크가 실패하는 것이 아니라 아예 생성되지 않으므로, 새 커밋을 올려도 `no checks reported`",
        "인 채 **최신 커밋이 한 번도 검증되지 않은 상태**로 남는다 (#1028).",
        "",
        `\`${baseRefName}\` 를 병합해 충돌을 풀면 검증이 다시 돈다. 해소되면 이 라벨은 자동으로 떨어진다.`,
    ].join("\n");
}

export function renderSummary({ plan, label, dryRun }) {
    const lines = [
        `## 충돌 PR 라벨 (\`${label}\`)${dryRun ? " — dry run" : ""}`,
        "",
        `- 라벨 부착: ${plan.toLabel.length}건${formatNumbers(plan.toLabel)}`,
        `- 라벨 제거: ${plan.toUnlabel.length}건${formatNumbers(plan.toUnlabel)}`,
        `- 판정 보류(UNKNOWN): ${plan.skipped.length}건${formatNumbers(plan.skipped)}`,
    ];
    return lines.join("\n");
}

function formatNumbers(pullRequests) {
    if (pullRequests.length === 0) {
        return "";
    }
    return ` — ${pullRequests.map((pullRequest) => `#${pullRequest.number}`).join(", ")}`;
}

function normalizePullRequest(node) {
    return {
        number: node.number,
        mergeable: node.mergeable,
        baseRefName: node.baseRefName,
        headRefName: node.headRefName,
        isDraft: node.isDraft,
        labels: (node.labels?.nodes ?? []).map((item) => item.name),
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

async function fetchOpenPullRequests(api, repository) {
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

const sleep = (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds));

/**
 * `UNKNOWN` 이 남아 있으면 다시 조회한다.
 *
 * GitHub 은 mergeable 을 비동기로 계산하고, 조회 자체가 계산을 촉발한다. 첫 응답에서 UNKNOWN 이
 * 흔하므로 몇 번 더 물어본 뒤에 판정한다.
 */
export async function resolveMergeStates(
    api,
    repository,
    { attempts = UNKNOWN_RESOLVE_ATTEMPTS, delayMs = UNKNOWN_RESOLVE_DELAY_MS, wait = sleep, fetchPage = fetchOpenPullRequests } = {},
) {
    let pullRequests = await fetchPage(api, repository);

    for (let attempt = 1; attempt < attempts; attempt += 1) {
        const unknown = pullRequests.filter((pullRequest) => pullRequest.mergeable === "UNKNOWN");
        if (unknown.length === 0) {
            break;
        }
        await wait(delayMs * attempt);
        pullRequests = await fetchPage(api, repository);
    }

    return pullRequests;
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

async function hasConflictComment(api, repository, number) {
    const comments = await api(`/repos/${repository}/issues/${number}/comments?per_page=100`);
    return (comments ?? []).some((comment) => (comment.body ?? "").includes(COMMENT_MARKER));
}

// `logger` 를 받는 이유: 테스트가 실제 동작처럼 보이는 줄을 CI 로그에 찍지 않게 하기 위해서다.
// 워크플로의 «Run script tests» 단계에 «#40 라벨 부착» 이 섞이면 무엇이 실제 조작인지 흐려진다.
export async function applyPlan(api, repository, plan, { label, dryRun, logger = console }) {
    const failures = [];

    for (const pullRequest of plan.toLabel) {
        try {
            if (dryRun) {
                logger.log(`[dry-run] #${pullRequest.number} 라벨 부착 + 코멘트`);
                continue;
            }
            await api(`/repos/${repository}/issues/${pullRequest.number}/labels`, {
                method: "POST",
                body: { labels: [label] },
            });
            // 사람이 라벨을 손으로 뗐다 붙였다 하는 경우까지 감안해 마커로 한 번 더 확인한다.
            if (!(await hasConflictComment(api, repository, pullRequest.number))) {
                await api(`/repos/${repository}/issues/${pullRequest.number}/comments`, {
                    method: "POST",
                    body: { body: renderConflictComment(pullRequest) },
                });
            }
            logger.log(`#${pullRequest.number} 라벨 부착 (base ${pullRequest.baseRefName})`);
        } catch (error) {
            failures.push(`#${pullRequest.number} 라벨 부착 실패: ${error.message}`);
        }
    }

    for (const pullRequest of plan.toUnlabel) {
        try {
            if (dryRun) {
                logger.log(`[dry-run] #${pullRequest.number} 라벨 제거`);
                continue;
            }
            await api(`/repos/${repository}/issues/${pullRequest.number}/labels/${encodeURIComponent(label)}`, {
                method: "DELETE",
                allowNotFound: true,
            });
            logger.log(`#${pullRequest.number} 라벨 제거`);
        } catch (error) {
            failures.push(`#${pullRequest.number} 라벨 제거 실패: ${error.message}`);
        }
    }

    return failures;
}

async function main() {
    const token = process.env.GITHUB_TOKEN;
    const repository = process.env.GITHUB_REPOSITORY;
    if (!token || !repository) {
        throw new Error("GITHUB_TOKEN 과 GITHUB_REPOSITORY 가 필요합니다.");
    }

    const label = process.env.CONFLICT_LABEL ?? DEFAULT_LABEL;
    const dryRun = process.env.DRY_RUN === "true";
    const api = createApi(token);

    const pullRequests = await resolveMergeStates(api, repository);
    const plan = planLabelChanges({ pullRequests, label });

    if (!dryRun && plan.toLabel.length > 0) {
        await ensureLabelExists(api, repository, label);
    }

    const failures = await applyPlan(api, repository, plan, { label, dryRun });
    const summary = renderSummary({ plan, label, dryRun });
    console.log(summary);

    if (process.env.GITHUB_STEP_SUMMARY) {
        const { appendFile } = await import("node:fs/promises");
        await appendFile(process.env.GITHUB_STEP_SUMMARY, `${summary}\n`);
    }

    if (failures.length > 0) {
        // 개별 실패가 나머지 PR 처리를 막지 않게 모아 두었다가 여기서 한 번에 드러낸다.
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
