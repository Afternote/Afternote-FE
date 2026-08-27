#!/usr/bin/env node

import process from "node:process";
import { pathToFileURL } from "node:url";

const PAGE_SIZE = 100;
const UPDATE_ATTEMPTS = 30;
const UPDATE_DELAY_MS = 2000;
const REQUIRED_WORKFLOWS = ["pr-validation.yml", "codeql.yml", "merge-order-guard.yml"];

function requirePositiveInteger(value, name) {
    const parsed = Number(value);
    if (!Number.isSafeInteger(parsed) || parsed < 1) {
        throw new Error(`${name} must be a positive integer`);
    }
    return parsed;
}

function assertSameRepositoryPullRequest(pullRequest, repository) {
    if (pullRequest.state !== "open") {
        throw new Error(`PR #${pullRequest.number} is not open`);
    }
    if (pullRequest.head?.repo?.full_name !== repository) {
        throw new Error(`PR #${pullRequest.number} is a fork PR`);
    }
    if (!pullRequest.head.ref || !pullRequest.base?.ref || !/^[0-9a-f]{40}$/.test(pullRequest.head.sha ?? "")) {
        throw new Error(`PR #${pullRequest.number} has incomplete branch metadata`);
    }
}

/**
 * root 의 head branch 를 base 로 삼는 열린 PR 을 한 단계씩 따라간다.
 * 분기 스택은 자동으로 어느 자식을 먼저 갱신할지 결정할 수 없으므로 쓰기 전에 거절한다.
 */
export function buildLinearStack(pullRequests, rootNumber, repository, maxDepth) {
    const root = pullRequests.find((pullRequest) => pullRequest.number === rootNumber);
    if (!root) {
        throw new Error(`Open PR #${rootNumber} was not found`);
    }

    const stack = [];
    const seen = new Set();
    let current = root;
    for (;;) {
        assertSameRepositoryPullRequest(current, repository);
        if (seen.has(current.number)) {
            throw new Error(`Cycle detected at PR #${current.number}`);
        }
        seen.add(current.number);
        stack.push(current);
        if (stack.length > maxDepth) {
            throw new Error(`Stack exceeds max depth ${maxDepth}`);
        }

        const children = pullRequests.filter(
            (pullRequest) => pullRequest.number !== current.number && pullRequest.base?.ref === current.head.ref,
        );
        if (children.length > 1) {
            throw new Error(
                `PR #${current.number} has multiple open children: ${children.map((child) => `#${child.number}`).join(", ")}`,
            );
        }
        if (children.length === 0) {
            return stack;
        }
        current = children[0];
    }
}

function encodeRef(ref) {
    return encodeURIComponent(ref);
}

const sleep = (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds));

async function waitForUpdatedHead(
    api,
    repository,
    pullRequest,
    { attempts = UPDATE_ATTEMPTS, delayMs = UPDATE_DELAY_MS, wait = sleep } = {},
) {
    for (let attempt = 1; attempt <= attempts; attempt += 1) {
        await wait(delayMs);
        const latest = await api(`/repos/${repository}/pulls/${pullRequest.number}`);
        if (latest.head.sha !== pullRequest.head.sha) {
            return latest.head.sha;
        }
    }
    throw new Error(`Timed out waiting for PR #${pullRequest.number} branch update`);
}

/** 부모부터 update-branch 를 호출한다. expected_head_sha 가 동시 push 를 fail-closed 로 막는다. */
export async function refreshStack(api, repository, stack, options = {}) {
    const results = [];
    const dryRun = options.dryRun ?? false;

    for (const discovered of stack) {
        const current = await api(`/repos/${repository}/pulls/${discovered.number}`);
        assertSameRepositoryPullRequest(current, repository);
        if (
            current.head.sha !== discovered.head.sha ||
            current.head.ref !== discovered.head.ref ||
            current.base.ref !== discovered.base.ref
        ) {
            throw new Error(`PR #${current.number} moved after stack discovery; rerun the workflow`);
        }

        const comparison = await api(
            `/repos/${repository}/compare/${encodeRef(current.base.ref)}...${encodeRef(current.head.ref)}`,
        );
        if (comparison.behind_by === 0) {
            results.push({ number: current.number, branch: current.head.ref, sha: current.head.sha, updated: false });
            continue;
        }
        if (dryRun) {
            results.push({ number: current.number, branch: current.head.ref, sha: current.head.sha, updated: true });
            continue;
        }

        await api(`/repos/${repository}/pulls/${current.number}/update-branch`, {
            method: "PUT",
            body: { expected_head_sha: current.head.sha },
        });
        const sha = await waitForUpdatedHead(api, repository, current, options);
        results.push({ number: current.number, branch: current.head.ref, sha, updated: true });
    }

    return results;
}

/** GITHUB_TOKEN push 는 pull_request 를 재발행하지 않으므로 새 SHA 에 필수 workflow 를 수동 부착한다. */
export async function dispatchRequiredChecks(api, repository, results, { dryRun = false } = {}) {
    if (dryRun) {
        return;
    }
    for (const result of results) {
        for (const workflow of REQUIRED_WORKFLOWS) {
            const body = { ref: result.branch };
            if (workflow === "pr-validation.yml" || workflow === "merge-order-guard.yml") {
                body.inputs = { pull_request_number: String(result.number) };
            }
            await api(`/repos/${repository}/actions/workflows/${workflow}/dispatches`, {
                method: "POST",
                body,
            });
        }
    }
}

export function renderSummary(stack, results, dryRun) {
    const byNumber = new Map(results.map((result) => [result.number, result]));
    const lines = [
        `## PR stack refresh${dryRun ? " — dry run" : ""}`,
        "",
        ...stack.map((pullRequest, index) => {
            const result = byNumber.get(pullRequest.number);
            const state = result?.updated ? (dryRun ? "would update" : "updated") : "already current";
            return `${index + 1}. #${pullRequest.number} \`${pullRequest.base.ref}\` → \`${pullRequest.head.ref}\` — ${state}`;
        }),
    ];
    if (!dryRun) {
        lines.push("", "각 PR 현재 head 에 필수 validation, CodeQL, merge-order guard 를 요청했습니다.");
    }
    return lines.join("\n");
}

function createApi(token) {
    return async function api(apiPath, { method = "GET", body } = {}) {
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
        if (!response.ok) {
            const detail = await response.text();
            throw new Error(`GitHub API ${method} ${apiPath} failed: ${response.status} ${detail}`);
        }
        if (response.status === 204) {
            return null;
        }
        return response.json();
    };
}

async function listOpenPullRequests(api, repository) {
    const pullRequests = [];
    for (let page = 1; ; page += 1) {
        const batch = await api(`/repos/${repository}/pulls?state=open&per_page=${PAGE_SIZE}&page=${page}`);
        pullRequests.push(...batch);
        if (batch.length < PAGE_SIZE) {
            return pullRequests;
        }
    }
}

async function main() {
    const token = process.env.GITHUB_TOKEN;
    const repository = process.env.GITHUB_REPOSITORY;
    if (!token || !repository) {
        throw new Error("GITHUB_TOKEN and GITHUB_REPOSITORY are required");
    }
    const rootNumber = requirePositiveInteger(process.env.ROOT_PULL_REQUEST, "ROOT_PULL_REQUEST");
    const maxDepth = requirePositiveInteger(process.env.MAX_DEPTH ?? "5", "MAX_DEPTH");
    const dryRun = process.env.DRY_RUN === "true";
    const api = createApi(token);
    const pullRequests = await listOpenPullRequests(api, repository);
    const stack = buildLinearStack(pullRequests, rootNumber, repository, maxDepth);
    const results = await refreshStack(api, repository, stack, { dryRun });
    await dispatchRequiredChecks(api, repository, results, { dryRun });

    const summary = renderSummary(stack, results, dryRun);
    console.log(summary);
    if (process.env.GITHUB_STEP_SUMMARY) {
        const { appendFile } = await import("node:fs/promises");
        await appendFile(process.env.GITHUB_STEP_SUMMARY, `${summary}\n`);
    }
}

if (import.meta.url === pathToFileURL(process.argv[1] ?? "").href) {
    main().catch((error) => {
        console.error(error);
        process.exitCode = 1;
    });
}
