#!/usr/bin/env node

import fs from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import { pathToFileURL } from "node:url";

export const STATUS_CONTEXT = "Base Freshness";
const SHA_PATTERN = /^[0-9a-f]{40}$/i;
const ZERO_SHA = "0".repeat(40);

function requireString(value, name) {
    if (typeof value !== "string" || value.length === 0) {
        throw new Error(`${name} must be a non-empty string`);
    }
    return value;
}

export function requireSha(value, name = "sha") {
    const sha = requireString(value, name).toLowerCase();
    if (!SHA_PATTERN.test(sha)) {
        throw new Error(`${name} must be a full 40-character commit SHA: ${value}`);
    }
    return sha;
}

export function branchFromRef(ref) {
    const prefix = "refs/heads/";
    if (typeof ref !== "string" || !ref.startsWith(prefix) || ref.length === prefix.length) {
        throw new Error(`expected a branch ref, received: ${ref ?? "<empty>"}`);
    }
    return ref.slice(prefix.length);
}

export function freshnessFromComparisonStatus(status) {
    switch (status) {
        case "ahead":
        case "identical":
            return true;
        case "behind":
        case "diverged":
            return false;
        default:
            throw new Error(`unexpected GitHub comparison status: ${status ?? "<empty>"}`);
    }
}

export function statusDescription({ baseRef, baseSha, fresh }) {
    const shortSha = requireSha(baseSha, "base sha").slice(0, 12);
    const description = fresh
        ? `Up to date with ${baseRef}@${shortSha}`
        : `Missing ${baseRef}@${shortSha}; merge or rebase the current base`;
    if (description.length > 140) {
        return fresh ? `Up to date with base@${shortSha}` : `Missing base@${shortSha}; merge or rebase`;
    }
    return description;
}

class GitHubApi {
    constructor({ apiUrl, repository, token }) {
        this.apiUrl = requireString(apiUrl, "GITHUB_API_URL").replace(/\/$/, "");
        this.repository = requireString(repository, "GITHUB_REPOSITORY");
        this.token = requireString(token, "GITHUB_TOKEN");
    }

    async request(endpoint, { method = "GET", body, query } = {}) {
        const url = new URL(`${this.apiUrl}/repos/${this.repository}/${endpoint}`);
        for (const [key, value] of Object.entries(query ?? {})) {
            if (value !== undefined && value !== null && value !== "") {
                url.searchParams.set(key, String(value));
            }
        }
        const response = await fetch(url, {
            method,
            headers: {
                accept: "application/vnd.github+json",
                authorization: `Bearer ${this.token}`,
                "x-github-api-version": "2022-11-28",
            },
            body: body === undefined ? undefined : JSON.stringify(body),
        });
        const text = await response.text();
        if (!response.ok) {
            throw new Error(`${method} ${url.pathname} failed (${response.status}): ${text.slice(0, 500)}`);
        }
        return text.length === 0 ? null : JSON.parse(text);
    }

    async paginate(endpoint, query = {}) {
        const values = [];
        for (let page = 1; ; page += 1) {
            const batch = await this.request(endpoint, {
                query: { ...query, page, per_page: 100 },
            });
            if (!Array.isArray(batch)) {
                throw new Error(`paginated endpoint ${endpoint} did not return an array`);
            }
            values.push(...batch);
            if (batch.length < 100) {
                return values;
            }
        }
    }

    getPullRequest(number) {
        return this.request(`pulls/${number}`);
    }

    listPullRequests(query) {
        return this.paginate("pulls", { state: "open", ...query });
    }

    async getBranchHead(branch) {
        const ref = await this.request(`git/ref/heads/${encodeURIComponent(branch)}`);
        return requireSha(ref?.object?.sha, `head of ${branch}`);
    }

    compare(baseSha, headSha) {
        return this.request(`compare/${requireSha(baseSha, "base sha")}...${requireSha(headSha, "head sha")}`);
    }

    setStatus(sha, { state, description, targetUrl }) {
        if (!["error", "failure", "pending", "success"].includes(state)) {
            throw new Error(`invalid commit status state: ${state}`);
        }
        return this.request(`statuses/${requireSha(sha, "status sha")}`, {
            method: "POST",
            body: {
                state,
                context: STATUS_CONTEXT,
                description,
                target_url: targetUrl,
            },
        });
    }
}

function validatePullRequest(pullRequest, repository) {
    const number = Number(pullRequest?.number);
    if (!Number.isSafeInteger(number) || number < 1) {
        throw new Error("pull request response has no valid number");
    }
    if (pullRequest.state !== "open") {
        return { kind: "closed", number };
    }
    if (pullRequest.base?.repo?.full_name !== repository) {
        throw new Error(`PR #${number} targets an unexpected repository: ${pullRequest.base?.repo?.full_name}`);
    }
    return {
        kind: "open",
        number,
        baseRef: requireString(pullRequest.base?.ref, `PR #${number} base ref`),
        headSha: requireSha(pullRequest.head?.sha, `PR #${number} head sha`),
    };
}

export async function evaluatePullRequestFreshness(api, {
    number,
    repository,
    targetUrl,
    expectedHeadSha,
    maxAttempts = 3,
}) {
    const expected = expectedHeadSha ? requireSha(expectedHeadSha, "expected PR head sha") : null;
    let lastCurrent;

    for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
        const current = validatePullRequest(await api.getPullRequest(number), repository);
        if (current.kind === "closed") {
            return current;
        }
        lastCurrent = current;
        if (expected !== null && current.headSha !== expected) {
            return { kind: "obsolete", number: current.number, currentHeadSha: current.headSha };
        }

        const baseSha = await api.getBranchHead(current.baseRef);
        await api.setStatus(current.headSha, {
            state: "pending",
            description: `Checking current ${current.baseRef}@${baseSha.slice(0, 12)}`,
            targetUrl,
        });
        const comparison = await api.compare(baseSha, current.headSha);
        const [verifiedPullRequest, verifiedBaseSha] = await Promise.all([
            api.getPullRequest(number),
            api.getBranchHead(current.baseRef),
        ]);
        const verified = validatePullRequest(verifiedPullRequest, repository);
        if (verified.kind === "closed") {
            return verified;
        }
        if (
            verified.headSha !== current.headSha ||
            verified.baseRef !== current.baseRef ||
            verifiedBaseSha !== baseSha
        ) {
            continue;
        }

        const fresh = freshnessFromComparisonStatus(comparison?.status);
        await api.setStatus(current.headSha, {
            state: fresh ? "success" : "failure",
            description: statusDescription({ baseRef: current.baseRef, baseSha, fresh }),
            targetUrl,
        });

        // Status 기록과 live ref 조회는 GitHub API에서 원자적이지 않다. 기록 직후에도
        // 같은 base/head인지 확인하고, 사이에 움직였다면 pending부터 다시 평가해
        // 방금 쓴 오래된 결론을 그대로 남기지 않는다. 별도 publisher 실행끼리는
        // workflow concurrency로 직렬화한다.
        const [finalPullRequest, finalBaseSha] = await Promise.all([
            api.getPullRequest(number),
            api.getBranchHead(current.baseRef),
        ]);
        const finalCurrent = validatePullRequest(finalPullRequest, repository);
        if (finalCurrent.kind === "closed") {
            return finalCurrent;
        }
        if (
            finalCurrent.headSha !== current.headSha ||
            finalCurrent.baseRef !== current.baseRef ||
            finalBaseSha !== baseSha
        ) {
            continue;
        }
        return {
            kind: fresh ? "fresh" : "stale",
            number: current.number,
            baseRef: current.baseRef,
            baseSha,
            headSha: current.headSha,
        };
    }

    if (lastCurrent) {
        await api.setStatus(lastCurrent.headSha, {
            state: "error",
            description: "Base or PR head kept changing; rerun freshness evaluation",
            targetUrl,
        });
    }
    throw new Error(`PR #${number} changed during every freshness evaluation attempt`);
}

async function evaluatePullRequests(api, pullRequests, context) {
    const results = [];
    const errors = [];
    for (const pullRequest of pullRequests) {
        const number = Number(pullRequest?.number ?? pullRequest);
        try {
            results.push(await evaluatePullRequestFreshness(api, { ...context, number }));
        } catch (error) {
            const message = error instanceof Error ? error.message : String(error);
            errors.push(new Error(`PR #${number} freshness evaluation failed: ${message}`, { cause: error }));
        }
    }
    if (errors.length > 0) {
        throw new AggregateError(
            errors,
            `${errors.length} of ${pullRequests.length} pull request freshness evaluations failed`,
        );
    }
    return results;
}

export async function publishFromEvent(api, event, eventName, targetUrl) {
    if (eventName === "push") {
        // A deleted branch push can replace another pending run in the shared concurrency
        // group, so it must still perform the same complete live sweep.
        if (event.deleted !== true && event.after !== ZERO_SHA) {
            branchFromRef(event.ref);
        }
    } else if (eventName === "workflow_run") {
        const workflowRun = event.workflow_run;
        if (workflowRun?.name !== "Base Freshness Probe") {
            throw new Error(`unexpected source workflow: ${workflowRun?.name ?? "<empty>"}`);
        }
    } else {
        throw new Error(`unsupported publisher event: ${eventName}`);
    }

    // GitHub concurrency keeps at most one pending run per group. A newer unrelated
    // signal can therefore replace a pending base-push signal. Every surviving run
    // sweeps all open PRs from live API state so coalescing cannot leave a stale green.
    const pullRequests = await api.listPullRequests();
    return evaluatePullRequests(api, pullRequests, {
        repository: api.repository,
        targetUrl,
    });
}

export async function evaluateMergeGroupFreshness(api, mergeGroup, { maxAttempts = 3 } = {}) {
    const baseRef = branchFromRef(requireString(mergeGroup?.base_ref, "merge_group base_ref"));
    const headSha = requireSha(mergeGroup?.head_sha, "merge_group head_sha");
    for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
        const baseSha = await api.getBranchHead(baseRef);
        const comparison = await api.compare(baseSha, headSha);
        if ((await api.getBranchHead(baseRef)) !== baseSha) {
            continue;
        }
        const fresh = freshnessFromComparisonStatus(comparison?.status);
        if (!fresh) {
            throw new Error(
                `merge group ${headSha} does not contain current ${baseRef}@${baseSha}; GitHub must rebuild the group`,
            );
        }
        return { baseRef, baseSha, headSha };
    }
    throw new Error(`base ${baseRef} changed during every merge-group freshness evaluation attempt`);
}

async function appendSummary(results) {
    if (!process.env.GITHUB_STEP_SUMMARY || results.length === 0) {
        return;
    }
    const lines = ["### Base Freshness", ""];
    for (const result of results) {
        if (result.number) {
            lines.push(`- PR #${result.number}: ${result.kind}`);
        } else {
            lines.push(`- merge group: fresh against \`${result.baseRef}@${result.baseSha.slice(0, 12)}\``);
        }
    }
    lines.push("");
    await fs.appendFile(process.env.GITHUB_STEP_SUMMARY, `${lines.join("\n")}\n`, "utf8");
}

async function main() {
    const eventPath = requireString(process.env.GITHUB_EVENT_PATH, "GITHUB_EVENT_PATH");
    const event = JSON.parse(await fs.readFile(eventPath, "utf8"));
    const api = new GitHubApi({
        apiUrl: process.env.GITHUB_API_URL,
        repository: process.env.GITHUB_REPOSITORY,
        token: process.env.GITHUB_TOKEN,
    });
    const mode = process.env.BASE_FRESHNESS_MODE ?? "publish";
    let results;
    if (mode === "merge-group") {
        results = [await evaluateMergeGroupFreshness(api, event.merge_group)];
    } else if (mode === "publish") {
        const targetUrl = `${requireString(process.env.GITHUB_SERVER_URL, "GITHUB_SERVER_URL")}/${api.repository}/actions/runs/${requireString(process.env.GITHUB_RUN_ID, "GITHUB_RUN_ID")}`;
        results = await publishFromEvent(api, event, process.env.GITHUB_EVENT_NAME, targetUrl);
    } else {
        throw new Error(`unsupported BASE_FRESHNESS_MODE: ${mode}`);
    }
    await appendSummary(results);
    for (const result of results) {
        console.log(JSON.stringify(result));
    }
}

const invokedPath = process.argv[1] ? pathToFileURL(path.resolve(process.argv[1])).href : "";
if (import.meta.url === invokedPath) {
    main().catch((error) => {
        console.error(error instanceof Error ? error.stack : error);
        process.exitCode = 1;
    });
}
