#!/usr/bin/env node

// CI Test Plan이 selected/full인 same-repository PR에만 `android-test` 라벨을 유지한다.
// token-authored commit에는 현재 plan digest와 exact HEAD를 묶어 Managed Device workflow를 다시 dispatch한다.

import path from "node:path";
import process from "node:process";
import { pathToFileURL } from "node:url";

import { ciTestPlanDigest, inspectPullRequestCiTestPlan } from "./ci-test-plan.mjs";

export const DEFAULT_LABEL = "android-test";
export const DEFAULT_PENDING_LABEL = "android-test-dispatch-pending";

const LABEL_COLOR = "1D76DB";
const LABEL_DESCRIPTION = "CI Test Plan이 selected/full인 Android Managed Device 검증 대상";
const PENDING_LABEL_COLOR = "FBCA04";
const PENDING_LABEL_DESCRIPTION = "android-test 자동 dispatch 재시도 필요";
const PULL_REQUEST_PAGE_SIZE = 50;

const OPEN_PULL_REQUESTS_QUERY = `
query($owner: String!, $name: String!, $cursor: String, $pageSize: Int!) {
    repository(owner: $owner, name: $name) {
        pullRequests(states: OPEN, first: $pageSize, after: $cursor) {
            pageInfo { hasNextPage endCursor }
            nodes {
                number
                createdAt
                headRefOid
                headRepository { nameWithOwner }
                body
                labels(first: 100) { nodes { name } }
            }
        }
    }
}`;

export function resolveAndroidTestDecision(pullRequest) {
    const inspection = inspectPullRequestCiTestPlan(pullRequest);
    if (!inspection.valid) {
        return { required: true, mode: "full", valid: false, digest: "" };
    }
    const plan = inspection.plan;
    return {
        required: plan.androidTest.mode !== "none",
        mode: plan.androidTest.mode,
        valid: true,
        digest: ciTestPlanDigest(plan),
    };
}

export function planLabelChanges({
    pullRequests,
    repository,
    label = DEFAULT_LABEL,
    pendingLabel = DEFAULT_PENDING_LABEL,
    redispatchHeadSha = "",
}) {
    const toLabel = [];
    const toRetry = [];
    const alreadyLabeled = [];
    const notRequired = [];
    const skippedForks = [];

    for (const pullRequest of pullRequests) {
        const requirement = {
            required: pullRequest.androidTestRequired === true,
            matches: pullRequest.androidTestRequired === true
                ? [{ id: `ci-test-plan-${pullRequest.androidTestMode}`, description: "CI Test Plan", paths: [] }]
                : [],
        };
        const candidate = { ...pullRequest, requirement };
        const hasLabel = (pullRequest.labels ?? []).includes(label);
        if (pullRequest.headRepository !== repository && (requirement.required || hasLabel)) {
            skippedForks.push(candidate);
            continue;
        }

        if (!requirement.required) {
            notRequired.push(pullRequest);
            continue;
        }

        if (hasLabel) {
            if (
                (pullRequest.labels ?? []).includes(pendingLabel) ||
                (redispatchHeadSha && pullRequest.headRefOid === redispatchHeadSha)
            ) {
                toRetry.push(candidate);
            } else {
                alreadyLabeled.push(candidate);
            }
            continue;
        }

        toLabel.push(candidate);
    }

    return { toLabel, toRetry, alreadyLabeled, notRequired, skippedForks };
}

export function renderSummary({ plan, label, dryRun }) {
    return [
        `## Android 계측 테스트 라벨 (\`${label}\`)${dryRun ? " — dry run" : ""}`,
        "",
        `- 라벨 부착: ${plan.toLabel.length}건${formatNumbers(plan.toLabel)}`,
        `- dispatch 재시도: ${(plan.toRetry ?? []).length}건${formatNumbers(plan.toRetry ?? [])}`,
        `- 이미 유지 중: ${plan.alreadyLabeled.length}건${formatNumbers(plan.alreadyLabeled)}`,
        `- plan=none: ${plan.notRequired.length}건${formatNumbers(plan.notRequired)}`,
        `- fork라 실행 불가: ${plan.skippedForks.length}건${formatNumbers(plan.skippedForks)}`,
    ].join("\n");
}

function formatNumbers(pullRequests) {
    if (pullRequests.length === 0) {
        return "";
    }
    return ` — ${pullRequests.map((pullRequest) => `#${pullRequest.number}`).join(", ")}`;
}

function normalizePullRequest(node) {
    const androidTestDecision = resolveAndroidTestDecision({
        number: node.number,
        created_at: node.createdAt,
        body: node.body,
    });
    return {
        number: node.number,
        headRefOid: node.headRefOid,
        headRepository: node.headRepository?.nameWithOwner ?? null,
        labels: (node.labels?.nodes ?? []).map((item) => item.name),
        androidTestRequired: androidTestDecision.required,
        androidTestMode: androidTestDecision.mode,
        planDigest: androidTestDecision.digest,
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

export async function ensureLabelExists(
    api,
    repository,
    label,
    { color = LABEL_COLOR, description = LABEL_DESCRIPTION } = {},
) {
    const existing = await api(`/repos/${repository}/labels/${encodeURIComponent(label)}`, {
        allowNotFound: true,
    });
    if (existing) {
        return;
    }
    await api(`/repos/${repository}/labels`, {
        method: "POST",
        body: { name: label, color, description },
    });
}

export async function fetchCurrentDispatchTarget(api, repository, number, label) {
    const pullRequest = await api(`/repos/${repository}/pulls/${number}`);
    const headBranch = pullRequest?.head?.ref;
    const headSha = pullRequest?.head?.sha;
    const headRepository = pullRequest?.head?.repo?.full_name;
    const labels = (pullRequest?.labels ?? []).map((item) => item.name);
    const decision = resolveAndroidTestDecision(pullRequest);

    if (pullRequest?.state !== "open") {
        throw new Error(`PR이 open 상태가 아닙니다: ${pullRequest?.state ?? "unknown"}`);
    }
    if (headRepository !== repository) {
        throw new Error(`same-repository PR이 아닙니다: ${headRepository ?? "unknown"}`);
    }
    if (!labels.includes(label)) {
        throw new Error(`${label} 라벨이 현재 PR에 없습니다.`);
    }
    if (!decision.valid || !decision.required || !decision.digest) {
        throw new Error("현재 CI Test Plan이 유효한 selected/full 계획이 아닙니다.");
    }
    if (typeof headSha !== "string" || !/^[0-9a-f]{40}$/.test(headSha)) {
        throw new Error(`현재 HEAD SHA가 올바르지 않습니다: ${headSha ?? "unknown"}`);
    }
    if (typeof headBranch !== "string" || headBranch.length === 0) {
        throw new Error(`현재 HEAD branch가 올바르지 않습니다: ${headBranch ?? "unknown"}`);
    }

    return {
        headBranch,
        headSha,
        planDigest: decision.digest,
    };
}

export async function applyPlan(
    api,
    repository,
    plan,
    { label, pendingLabel = DEFAULT_PENDING_LABEL, dryRun, logger = console },
) {
    const failures = [];

    const operations = [
        ...plan.toLabel.map((pullRequest) => ({ pullRequest, addLabel: true })),
        ...(plan.toRetry ?? []).map((pullRequest) => ({ pullRequest, addLabel: false })),
    ];

    for (const { pullRequest, addLabel } of operations) {
        try {
            if (dryRun) {
                logger.log(
                    `[dry-run] #${pullRequest.number} ${
                        addLabel ? `${label} 라벨 부착` : "현재 HEAD dispatch"
                    }`,
                );
                continue;
            }
            if (addLabel) {
                // pull_request:labeled가 현재 HEAD의 Managed Device를 직접 실행한다. 별도
                // workflow_dispatch를 만들면 오래된 PR branch의 입력 schema와 충돌하고 같은
                // HEAD를 중복 실행하므로, 최초 부착은 감사 표식만 쓴다.
                await api(`/repos/${repository}/issues/${pullRequest.number}/labels`, {
                    method: "POST",
                    body: { labels: [label] },
                });
                logger.log(`#${pullRequest.number} ${label} 라벨 부착`);
                continue;
            }

            if (!(pullRequest.labels ?? []).includes(pendingLabel)) {
                // GITHUB_TOKEN이 만든 commit은 pull_request:synchronize를 만들지 않는다. bridge가
                // 요청한 exact-HEAD dispatch가 실패하면 다음 reconcile이 다시 시도하도록 표시한다.
                await api(`/repos/${repository}/issues/${pullRequest.number}/labels`, {
                    method: "POST",
                    body: { labels: [pendingLabel] },
                });
            }

            // bridge가 관찰한 뒤 HEAD가 움직인 경합을 닫기 위해 dispatch 직전에 PR을 다시 읽는다.
            const {
                headBranch: currentHeadBranch,
                headSha: currentHeadSha,
                planDigest,
            } = await fetchCurrentDispatchTarget(api, repository, pullRequest.number, label);
            await api(
                `/repos/${repository}/actions/workflows/android-managed-device.yml/dispatches`,
                {
                    method: "POST",
                    body: {
                        // workflow_dispatch의 GITHUB_SHA/cache scope를 PR head branch에 결박한다.
                        ref: currentHeadBranch,
                        inputs: {
                            pull_request_number: String(pullRequest.number),
                            expected_head_sha: currentHeadSha,
                            expected_plan_digest: planDigest,
                        },
                    },
                },
            );
            await api(
                `/repos/${repository}/issues/${pullRequest.number}/labels/${encodeURIComponent(pendingLabel)}`,
                { method: "DELETE", allowNotFound: true },
            );
            logger.log(`#${pullRequest.number} 현재 HEAD dispatch`);
        } catch (error) {
            failures.push(
                `#${pullRequest.number} 자동 처리 실패(다음 reconcile에서 재시도): ${error.message}`,
            );
        }
    }

    return failures;
}

async function main() {
    const token = process.env.GITHUB_TOKEN;
    const repository = process.env.GITHUB_REPOSITORY;
    if (!token || !repository) {
        throw new Error("GITHUB_TOKEN, GITHUB_REPOSITORY가 필요합니다.");
    }

    const label = process.env.ANDROID_TEST_LABEL ?? DEFAULT_LABEL;
    const pendingLabel = process.env.ANDROID_TEST_PENDING_LABEL ?? DEFAULT_PENDING_LABEL;
    const redispatchHeadSha = process.env.ANDROID_TEST_REDISPATCH_HEAD_SHA ?? "";
    if (redispatchHeadSha && !/^[0-9a-f]{40}$/.test(redispatchHeadSha)) {
        throw new Error("ANDROID_TEST_REDISPATCH_HEAD_SHA는 비어 있거나 40자리 lowercase SHA여야 합니다.");
    }
    const dryRun = process.env.DRY_RUN === "true";
    const api = createApi(token);
    const pullRequests = await fetchOpenPullRequests(api, repository);
    const plan = planLabelChanges({
        pullRequests,
        repository,
        label,
        pendingLabel,
        redispatchHeadSha,
    });

    if (!dryRun && plan.toLabel.length > 0) {
        await ensureLabelExists(api, repository, label);
    }
    if (!dryRun && plan.toRetry.length > 0) {
        await ensureLabelExists(api, repository, pendingLabel, {
            color: PENDING_LABEL_COLOR,
            description: PENDING_LABEL_DESCRIPTION,
        });
    }

    const failures = await applyPlan(api, repository, plan, {
        label,
        pendingLabel,
        dryRun,
    });
    const summary = renderSummary({ plan, label, dryRun });
    console.log(summary);

    if (process.env.GITHUB_STEP_SUMMARY) {
        const { appendFile } = await import("node:fs/promises");
        await appendFile(process.env.GITHUB_STEP_SUMMARY, `${summary}\n`);
    }

    if (plan.skippedForks.length > 0) {
        failures.push(
            `fork PR은 현재 Managed Device 권한 경계에서 실행할 수 없습니다:${formatNumbers(plan.skippedForks)}`,
        );
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
