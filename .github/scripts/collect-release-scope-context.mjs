#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import fs from "node:fs/promises";
import path from "node:path";
import { pathToFileURL } from "node:url";

const MAX_PULL_REQUEST_PAGES = 10;
const MAX_PULL_REQUESTS = 50;
const MAX_ISSUES_PER_PULL_REQUEST = 20;
const MAX_BODY_LENGTH = 8_000;

function truncate(value, limit = MAX_BODY_LENGTH) {
    const text = String(value ?? "");
    return text.length <= limit ? text : `${text.slice(0, limit)}\n...[truncated]`;
}

export function assertReleasePullRequest(pullRequest) {
    if (pullRequest?.base?.ref !== "main") {
        throw new Error(
            `PR #${pullRequest?.number ?? "?"} targets ${pullRequest?.base?.ref ?? "<unknown>"}, not main`,
        );
    }
    if (pullRequest.head?.ref !== "develop") {
        throw new Error(
            `PR #${pullRequest.number} ships ${pullRequest.head?.ref ?? "<unknown>"}, not develop`,
        );
    }
    if (!pullRequest.head?.sha) {
        throw new Error(`PR #${pullRequest.number} has no head SHA`);
    }
}

export function extractClosingIssueNumbers(body) {
    const numbers = new Set();
    const pattern = /\b(?:close[sd]?|fix(?:e[sd])?|resolve[sd]?)\s*:?[ \t]*(?:[\w.-]+\/[\w.-]+)?#([1-9][0-9]*)/gi;
    let match;
    while ((match = pattern.exec(String(body ?? ""))) !== null) {
        numbers.add(Number(match[1]));
    }
    return [...numbers];
}

export function selectMergedPullRequestsByAncestry(
    pullRequests,
    baselineSha,
    targetSha,
    isAncestor,
) {
    if (!targetSha || typeof isAncestor !== "function") {
        throw new Error("targetSha and isAncestor are required");
    }
    return pullRequests
        .filter((pullRequest) => {
            if (
                !pullRequest.merged_at ||
                !pullRequest.merge_commit_sha ||
                pullRequest.base?.ref !== "develop"
            ) {
                return false;
            }
            if (!isAncestor(pullRequest.merge_commit_sha, targetSha)) {
                return false;
            }
            return !baselineSha || !isAncestor(pullRequest.merge_commit_sha, baselineSha);
        })
        .sort((left, right) => Date.parse(left.merged_at) - Date.parse(right.merged_at));
}

export function isTargetCoveredByCompareStatus(status) {
    return status === "ahead" || status === "identical";
}

export function distributionRelationForCompareStatus(status) {
    if (isTargetCoveredByCompareStatus(status)) {
        return "covered";
    }
    return status === "behind" ? "baseline" : null;
}

function distributionRunTime(run) {
    for (const value of [run.updated_at, run.run_started_at, run.created_at]) {
        const timestamp = Date.parse(value ?? "");
        if (!Number.isNaN(timestamp)) {
            return timestamp;
        }
    }
    return Number.NEGATIVE_INFINITY;
}

export function sortDistributionRuns(runs) {
    return [...runs].sort(
        (left, right) => distributionRunTime(right) - distributionRunTime(left),
    );
}

export function sourceShaForDistributionRun(run, associatedPullRequests = []) {
    if (!run?.head_sha) {
        return null;
    }
    if (run.head_branch === "develop") {
        return run.head_sha;
    }
    if (run.head_branch !== "main") {
        return null;
    }

    const releasePullRequest = associatedPullRequests
        .filter(
            (pullRequest) =>
                pullRequest.merged_at &&
                pullRequest.base?.ref === "main" &&
                pullRequest.head?.ref === "develop" &&
                pullRequest.head?.sha,
        )
        .sort((left, right) => Date.parse(right.merged_at) - Date.parse(left.merged_at))[0];
    return releasePullRequest?.head?.sha ?? null;
}

function normalizeIssue(issue) {
    return {
        number: issue.number,
        title: issue.title,
        body: truncate(issue.body),
        url: issue.html_url ?? issue.url,
        labels: (issue.labels?.nodes ?? issue.labels ?? []).map((label) =>
            typeof label === "string" ? label : label.name,
        ),
    };
}

function normalizePullRequest(pullRequest, issues) {
    return {
        number: pullRequest.number,
        title: pullRequest.title,
        body: truncate(pullRequest.body),
        url: pullRequest.html_url,
        mergedAt: pullRequest.merged_at,
        mergeCommitSha: pullRequest.merge_commit_sha,
        author: pullRequest.user?.login,
        labels: (pullRequest.labels ?? []).map((label) => label.name),
        closingIssues: issues.map(normalizeIssue),
    };
}

async function requestJson(url, token, options = {}) {
    const response = await fetch(url, {
        ...options,
        headers: {
            Accept: "application/vnd.github+json",
            Authorization: `Bearer ${token}`,
            "X-GitHub-Api-Version": "2022-11-28",
            ...options.headers,
        },
    });
    if (!response.ok) {
        const body = truncate(await response.text(), 2_000);
        throw new Error(`${options.method ?? "GET"} ${url} failed (${response.status}): ${body}`);
    }
    return response.json();
}

async function listClosedDevelopPullRequests(apiUrl, repository, token) {
    const pullRequests = [];
    for (let page = 1; page <= MAX_PULL_REQUEST_PAGES; page += 1) {
        const query = new URLSearchParams({
            state: "closed",
            base: "develop",
            sort: "updated",
            direction: "desc",
            per_page: "100",
            page: String(page),
        });
        const pageItems = await requestJson(
            `${apiUrl}/repos/${repository}/pulls?${query.toString()}`,
            token,
        );
        pullRequests.push(...pageItems);
        if (pageItems.length < 100) {
            return pullRequests;
        }
    }
    throw new Error(
        `More than ${MAX_PULL_REQUEST_PAGES * 100} closed develop PRs require pagination`,
    );
}

async function loadPullRequestsForCommit(apiUrl, repository, commitSha, token) {
    return requestJson(
        `${apiUrl}/repos/${repository}/commits/${commitSha}/pulls`,
        token,
    );
}

async function loadClosingIssues(graphqlUrl, repository, pullRequest, token) {
    const [owner, name] = repository.split("/");
    const query = `
        query($owner: String!, $name: String!, $number: Int!) {
          repository(owner: $owner, name: $name) {
            pullRequest(number: $number) {
              closingIssuesReferences(first: ${MAX_ISSUES_PER_PULL_REQUEST}) {
                nodes {
                  number
                  title
                  body
                  url
                  labels(first: 20) { nodes { name } }
                }
              }
            }
          }
        }
    `;
    const result = await requestJson(graphqlUrl, token, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ query, variables: { owner, name, number: pullRequest.number } }),
    });
    if (result.errors?.length) {
        throw new Error(`GraphQL closing issue query failed: ${JSON.stringify(result.errors)}`);
    }

    const issues = result.data?.repository?.pullRequest?.closingIssuesReferences?.nodes ?? [];
    if (issues.length > 0) {
        return issues;
    }

    const fallbackNumbers = extractClosingIssueNumbers(pullRequest.body).slice(
        0,
        MAX_ISSUES_PER_PULL_REQUEST,
    );
    const fallbackIssues = [];
    for (const issueNumber of fallbackNumbers) {
        const issue = await requestJson(
            `${process.env.GITHUB_API_URL ?? "https://api.github.com"}/repos/${repository}/issues/${issueNumber}`,
            token,
        );
        if (!issue.pull_request) {
            fallbackIssues.push(issue);
        }
    }
    return fallbackIssues;
}

async function compareCommits(apiUrl, repository, baseSha, headSha, token) {
    if (baseSha === headSha) {
        return "identical";
    }
    const comparison = await requestJson(
        `${apiUrl}/repos/${repository}/compare/${baseSha}...${headSha}`,
        token,
    );
    return comparison.status;
}

async function resolveDistributionSourceSha(apiUrl, repository, run, token) {
    if (run.head_branch === "develop") {
        return sourceShaForDistributionRun(run);
    }
    if (run.head_branch !== "main") {
        return null;
    }

    const pullRequests = await loadPullRequestsForCommit(
        apiUrl,
        repository,
        run.head_sha,
        token,
    );
    const sourceSha = sourceShaForDistributionRun(run, pullRequests);
    if (!sourceSha) {
        throw new Error(
            `Successful main distribution run ${run.id} has no merged develop -> main PR`,
        );
    }
    return sourceSha;
}

async function findRelevantDistribution(apiUrl, repository, runs, targetSha, token) {
    for (const run of sortDistributionRuns(runs)) {
        const sourceSha = await resolveDistributionSourceSha(apiUrl, repository, run, token);
        if (!sourceSha) {
            continue;
        }
        const compareStatus = await compareCommits(
            apiUrl,
            repository,
            targetSha,
            sourceSha,
            token,
        );
        const relation = distributionRelationForCompareStatus(compareStatus);
        if (relation) {
            return { run, sourceSha, relation };
        }
    }
    return null;
}

function gitIsAncestor(ancestorSha, descendantSha) {
    try {
        execFileSync("git", ["merge-base", "--is-ancestor", ancestorSha, descendantSha], {
            stdio: "ignore",
        });
        return true;
    } catch (error) {
        if (error?.status === 1) {
            return false;
        }
        if (error?.status === 128 && !gitCommitExists(ancestorSha)) {
            return false;
        }
        throw error;
    }
}

function gitCommitExists(commitSha) {
    try {
        execFileSync("git", ["cat-file", "-e", `${commitSha}^{commit}`], {
            stdio: "ignore",
        });
        return true;
    } catch (error) {
        if (error?.status === 1 || error?.status === 128) {
            return false;
        }
        throw error;
    }
}

async function main() {
    const token = process.env.GITHUB_TOKEN;
    const repository = process.env.GITHUB_REPOSITORY;
    const targetPullRequestNumber = Number(process.env.RELEASE_PR_NUMBER);
    const outputPath = process.env.RELEASE_SCOPE_CONTEXT_PATH;
    const apiUrl = process.env.GITHUB_API_URL ?? "https://api.github.com";
    const graphqlUrl = process.env.GITHUB_GRAPHQL_URL ?? "https://api.github.com/graphql";

    if (!token || !repository || !targetPullRequestNumber || !outputPath) {
        throw new Error(
            "GITHUB_TOKEN, GITHUB_REPOSITORY, RELEASE_PR_NUMBER, and RELEASE_SCOPE_CONTEXT_PATH are required",
        );
    }

    const targetPullRequest = await requestJson(
        `${apiUrl}/repos/${repository}/pulls/${targetPullRequestNumber}`,
        token,
    );
    assertReleasePullRequest(targetPullRequest);
    const releaseHeadSha = targetPullRequest.head.sha;

    const workflowRunsQuery = new URLSearchParams({ status: "success", per_page: "100" });
    const workflowRuns = await requestJson(
        `${apiUrl}/repos/${repository}/actions/workflows/release-distribution.yml/runs?${workflowRunsQuery.toString()}`,
        token,
    );
    const successfulRuns = workflowRuns.workflow_runs ?? [];
    const relevantDistribution = await findRelevantDistribution(
        apiUrl,
        repository,
        successfulRuns,
        releaseHeadSha,
        token,
    );
    const covered = relevantDistribution?.relation === "covered";
    const baselineSha = relevantDistribution?.sourceSha ?? null;

    const allPullRequests = covered
        ? []
        : await listClosedDevelopPullRequests(apiUrl, repository, token);
    const selectedPullRequests = covered
        ? []
        : selectMergedPullRequestsByAncestry(
              allPullRequests,
              baselineSha,
              releaseHeadSha,
              gitIsAncestor,
          );
    if (selectedPullRequests.length > MAX_PULL_REQUESTS) {
        throw new Error(
            `${selectedPullRequests.length} PRs accumulated since the baseline; refusing a truncated scope`,
        );
    }

    const normalizedPullRequests = [];
    for (const pullRequest of selectedPullRequests) {
        const issues = await loadClosingIssues(graphqlUrl, repository, pullRequest, token);
        normalizedPullRequests.push(normalizePullRequest(pullRequest, issues));
    }

    const scopeContext = {
        repository,
        releasePullRequest: {
            number: targetPullRequest.number,
            headSha: releaseHeadSha,
            body: truncate(targetPullRequest.body),
        },
        alreadyDistributed: covered,
        baselineDistribution: relevantDistribution
            ? {
                  id: relevantDistribution.run.id,
                  headSha: relevantDistribution.sourceSha,
                  runHeadSha: relevantDistribution.run.head_sha,
                  event: relevantDistribution.run.event,
                  headBranch: relevantDistribution.run.head_branch,
                  runStartedAt: relevantDistribution.run.run_started_at,
                  completedAt: relevantDistribution.run.updated_at,
                  url: relevantDistribution.run.html_url,
              }
            : null,
        pendingPullRequests: normalizedPullRequests,
    };

    await fs.mkdir(path.dirname(outputPath), { recursive: true });
    await fs.writeFile(outputPath, `${JSON.stringify(scopeContext, null, 2)}\n`, "utf8");
}

const invokedPath = process.argv[1] ? pathToFileURL(path.resolve(process.argv[1])).href : "";
if (import.meta.url === invokedPath) {
    main().catch((error) => {
        console.error(error instanceof Error ? error.stack : error);
        process.exitCode = 1;
    });
}
