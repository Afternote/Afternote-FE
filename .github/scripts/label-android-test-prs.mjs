#!/usr/bin/env node

// 모든 same-repository PR 에 `android-test` 라벨을 유지한다.
//
// 라벨은 Android Managed Device 실행 스위치다. 한 번 성공한 뒤 사람이 라벨을 제거하고 head 를
// 갱신하면 새 SHA 는 계측 테스트 없이 남는다. 이 스크립트는 default branch 의 신뢰된 워크플로에서
// 열린 PR 전체를 다시 읽고 라벨이 없는 same-repository PR 에 추가한다. 경로 규칙과 구조화 QA 결정은
// 실행 여부가 아니라 추가 위험 근거를 기록하는 데 유지한다. 자동 제거는 하지 않는다.

import path from "node:path";
import process from "node:process";
import { pathToFileURL } from "node:url";

import { inspectQaMetadata } from "./qa-metadata.mjs";

export const DEFAULT_LABEL = "android-test";
export const DEFAULT_PENDING_LABEL = "android-test-dispatch-pending";

const LABEL_COLOR = "1D76DB";
const LABEL_DESCRIPTION = "모든 same-repository PR head에서 Android Managed Device 계측 테스트 실행";
const PENDING_LABEL_COLOR = "FBCA04";
const PENDING_LABEL_DESCRIPTION = "android-test 자동 dispatch 재시도 필요";
const PULL_REQUEST_PAGE_SIZE = 50;
const CHANGED_FILE_PAGE_SIZE = 100;
const MAX_CHANGED_FILE_PAGES = 30;
const LEGACY_ANDROID_TEST_EXCLUDED_SCOPES = new Set(["ci-only", "covered-by-ci"]);

const REQUIREMENT_RULES = [
    {
        id: "androidTest-source",
        description: "계측 테스트 소스",
        matches: (filePath) => /(^|\/)src\/androidTest\//.test(filePath),
    },
    {
        id: "android-manifest",
        description: "Android manifest",
        matches: (filePath) => /(^|\/)src\/main\/AndroidManifest\.xml$/.test(filePath),
    },
    {
        id: "runtime-navigation",
        description: "앱 런타임 navigation",
        matches: (filePath) =>
            /^(app|feature\/[^/]+\/presentation)\/src\/main\/(java|kotlin)\/.+\/navigation\//.test(filePath),
    },
    {
        id: "runtime-presentation-source",
        description: "사용자에게 보이는 presentation 런타임 소스",
        qaExcludable: true,
        matches: (filePath) =>
            /^feature\/[^/]+\/presentation\/src\/main\/(java|kotlin)\//.test(filePath) &&
            !/\/navigation\//.test(filePath),
    },
    {
        id: "app-entry-point",
        description: "앱 진입점",
        matches: (filePath) =>
            /^app\/src\/main\/(java|kotlin)\/.+\/(MainActivity|AfternoteApplication)\.kt$/.test(filePath),
    },
    {
        id: "app-build-config",
        description: "앱 Android 빌드 설정",
        matches: (filePath) => filePath === "app/build.gradle.kts",
    },
    {
        id: "android-build-system",
        description: "Android 빌드 시스템",
        qaExcludable: true,
        matches: (filePath) =>
            filePath === "build.gradle.kts" ||
            filePath === "settings.gradle.kts" ||
            filePath === "gradle.properties" ||
            filePath === "gradle/libs.versions.toml" ||
            filePath.startsWith("gradle/wrapper/") ||
            filePath.startsWith("build-logic/") ||
            (filePath !== "app/build.gradle.kts" && filePath.endsWith("/build.gradle.kts")),
    },
    {
        id: "managed-device-config",
        description: "Managed Device 실행 설정",
        matches: (filePath) =>
            filePath === ".github/workflows/android-managed-device.yml" ||
            filePath.startsWith(".github/actions/setup-ci-config/"),
    },
];

const OPEN_PULL_REQUESTS_QUERY = `
query($owner: String!, $name: String!, $cursor: String, $pageSize: Int!) {
    repository(owner: $owner, name: $name) {
        pullRequests(states: OPEN, first: $pageSize, after: $cursor) {
            pageInfo { hasNextPage endCursor }
            nodes {
                number
                headRefOid
                headRepository { nameWithOwner }
                body
                labels(first: 100) { nodes { name } }
            }
        }
    }
}`;

export function classifyAndroidTestRequirement(
    filePaths,
    { androidTestRequired = false, androidTestExcluded = false } = {},
) {
    const matches = [];

    if (androidTestRequired) {
        matches.push({
            id: "qa-metadata-decision",
            description: "PR QA 메타데이터의 명시적 계측 테스트 결정",
            paths: [],
        });
    }

    for (const rule of REQUIREMENT_RULES) {
        // 화면·빌드 시스템 변경은 실제 Android 경계를 건드릴 가능성이 높아 기본적으로 실행한다.
        // 다만 구조화 QA가 동일 input·boundary·observation의 CI 근거로 제외를 증명한 경우에만
        // 이 soft rule을 건너뛴다. manifest/navigation/계측 소스 같은 hard rule은 제외할 수 없다.
        if (rule.qaExcludable === true && androidTestExcluded) {
            continue;
        }
        const matchedPaths = filePaths.filter((filePath) => rule.matches(filePath));
        if (matchedPaths.length > 0) {
            matches.push({
                id: rule.id,
                description: rule.description,
                paths: matchedPaths,
            });
        }
    }

    return {
        required: matches.length > 0,
        matches,
    };
}

export function resolveAndroidTestDecision(body, { pullRequestNumber = "?" } = {}) {
    const qaInspection = inspectQaMetadata(body, { pullRequestNumber });
    if (!qaInspection.valid) {
        return { required: false, excluded: false };
    }

    const explicitDecision = qaInspection.metadata.androidTest;
    return {
        required: explicitDecision?.required === true,
        excluded:
            explicitDecision?.required === false ||
            (explicitDecision === undefined &&
                LEGACY_ANDROID_TEST_EXCLUDED_SCOPES.has(qaInspection.metadata.scope)),
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
        const pathRequirement = classifyAndroidTestRequirement(pullRequest.files ?? [], {
            androidTestRequired: pullRequest.androidTestRequired === true,
            androidTestExcluded: pullRequest.androidTestExcluded === true,
        });
        const requirement = {
            required: true,
            matches: [
                {
                    id: "all-pull-requests",
                    description: "모든 same-repository PR의 필수 계측 테스트",
                    paths: [],
                },
                ...pathRequirement.matches,
            ],
        };
        const candidate = { ...pullRequest, requirement };
        const hasLabel = (pullRequest.labels ?? []).includes(label);
        if (pullRequest.headRepository !== repository && (requirement.required || hasLabel)) {
            skippedForks.push(candidate);
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

        if (!requirement.required) {
            notRequired.push(pullRequest);
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
        `- 경로 규칙 비대상: ${plan.notRequired.length}건${formatNumbers(plan.notRequired)}`,
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
    const androidTestDecision = resolveAndroidTestDecision(node.body, {
        pullRequestNumber: node.number,
    });
    return {
        number: node.number,
        headRefOid: node.headRefOid,
        headRepository: node.headRepository?.nameWithOwner ?? null,
        labels: (node.labels?.nodes ?? []).map((item) => item.name),
        androidTestRequired: androidTestDecision.required,
        androidTestExcluded: androidTestDecision.excluded,
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

export async function fetchChangedFiles(
    api,
    repository,
    number,
    { pageSize = CHANGED_FILE_PAGE_SIZE, maxPages = MAX_CHANGED_FILE_PAGES } = {},
) {
    const files = [];

    for (let page = 1; page <= maxPages; page += 1) {
        const result = await api(
            `/repos/${repository}/pulls/${number}/files?per_page=${pageSize}&page=${page}`,
        );
        if (!Array.isArray(result)) {
            throw new Error(`#${number} changed files 응답이 배열이 아닙니다.`);
        }
        files.push(...result.map((item) => item.filename));
        if (result.length < pageSize) {
            return files;
        }
    }

    throw new Error(`#${number} changed files가 ${maxPages * pageSize}개를 넘어 판정을 중단합니다.`);
}

export async function collectPullRequestsWithFiles(api, repository, { fetchPullRequests = fetchOpenPullRequests } = {}) {
    const pullRequests = await fetchPullRequests(api, repository);
    const collected = [];

    for (const pullRequest of pullRequests) {
        collected.push({
            ...pullRequest,
            files: await fetchChangedFiles(api, repository, pullRequest.number),
        });
    }

    return collected;
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
    const qaInspection = inspectQaMetadata(pullRequest?.body, { pullRequestNumber: number });

    if (pullRequest?.state !== "open") {
        throw new Error(`PR이 open 상태가 아닙니다: ${pullRequest?.state ?? "unknown"}`);
    }
    if (headRepository !== repository) {
        throw new Error(`same-repository PR이 아닙니다: ${headRepository ?? "unknown"}`);
    }
    if (!labels.includes(label)) {
        throw new Error(`${label} 라벨이 현재 PR에 없습니다.`);
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
        androidTestRef:
            qaInspection.valid && qaInspection.metadata?.androidTest?.required === true
                ? qaInspection.metadata.androidTest.testRef
                : "",
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
                    `[dry-run] #${pullRequest.number} ${addLabel ? `${label} 라벨 부착 + ` : ""}` +
                        "현재 HEAD dispatch",
                );
                continue;
            }
            if (addLabel) {
                // 이 표식은 자동화가 소유한다. 프로세스가 어느 단계에서 끊겨도 다음 reconcile이
                // dispatch를 재시도하며, 사람이나 다른 job이 붙인 안전 라벨은 절대 지우지 않는다.
                await api(`/repos/${repository}/issues/${pullRequest.number}/labels`, {
                    method: "POST",
                    body: { labels: [pendingLabel] },
                });
                await api(`/repos/${repository}/issues/${pullRequest.number}/labels`, {
                    method: "POST",
                    body: { labels: [label] },
                });
            }

            // changed-files를 읽은 뒤 push가 먼저 발생하고 라벨이 나중에 붙는 경합을 닫는다.
            // 라벨을 붙인 다음 현재 PR을 다시 읽으면, 그 전에 끝난 push는 새 SHA로 dispatch하고
            // 그 뒤의 push는 이미 라벨이 있으므로 synchronize 이벤트가 Managed Device를 실행한다.
            const {
                headBranch: currentHeadBranch,
                headSha: currentHeadSha,
                androidTestRef,
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
                            expected_test_ref: androidTestRef,
                        },
                    },
                },
            );
            await api(
                `/repos/${repository}/issues/${pullRequest.number}/labels/${encodeURIComponent(pendingLabel)}`,
                { method: "DELETE", allowNotFound: true },
            );
            logger.log(`#${pullRequest.number} ${label} 라벨 부착 + 현재 HEAD dispatch`);
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
    const pullRequests = await collectPullRequestsWithFiles(api, repository);
    const plan = planLabelChanges({
        pullRequests,
        repository,
        label,
        pendingLabel,
        redispatchHeadSha,
    });

    if (!dryRun && (plan.toLabel.length > 0 || plan.toRetry.length > 0)) {
        await ensureLabelExists(api, repository, label);
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
