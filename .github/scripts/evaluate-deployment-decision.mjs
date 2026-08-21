#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import fs from "node:fs/promises";
import path from "node:path";
import { pathToFileURL } from "node:url";

import { inspectQaMetadata, isRunnableQaScope } from "./qa-semantic-audit.mjs";

const DEPLOYMENT_CONTROL_PATHS = [
    /^\.github\/workflows\/(?:release-distribution|deployment-decision)\.ya?ml$/,
    /^\.github\/scripts\/(?:collect-deployment-decision-context|evaluate-deployment-decision|qa-semantic-audit|validate-pr-qa-metadata)\.mjs$/,
    /^\.github\/scripts\/render-distribution-release-notes\.sh$/,
];
const HIGH_RISK_PATHS = [
    /(^|\/)(auth|login|onboarding|signup|signing|keystore)(\/|\.|$)/i,
    /(^|\/)(data|network|api)(\/|$)/i,
    /(^|\/)(build\.gradle(?:\.kts)?|settings\.gradle(?:\.kts)?|gradle\.properties)$/,
    /^gradle\/libs\.versions\.toml$/,
];
const HIGH_RISK_TEXT =
    /로그인|회원가입|인증|온보딩|데이터\s*(?:손실|삭제)|API\s*계약|서명|배포|release|signing|authentication|data[ -]?loss/i;
const RUNTIME_NEUTRAL_PATHS = [
    /^\.github\/(?:workflows|scripts)\//,
    /(^|\/)README(?:\.[^/]*)?$/i,
    /(^|\/)docs?\//i,
    /\.md$/i,
    /(^|\/)(test|tests|androidTest)\//,
    /(?:Test|Tests)\.(?:kt|java)$/,
];

function unique(values) {
    return [...new Set(values)];
}

function parseTitle(title) {
    const match = /^([a-z]+)(?:\(([^)]+)\))?:/i.exec(String(title ?? ""));
    return {
        type: match?.[1]?.toLowerCase() ?? "",
        scope: match?.[2]?.toLowerCase() ?? "",
    };
}

function bodyWithoutQaMetadata(body) {
    const kept = [];
    let skipping = false;
    for (const line of String(body ?? "").split(/\r?\n/)) {
        const heading = /^#{1,6}\s+(.+?)\s*$/.exec(line);
        if (heading) {
            if (/qa\s*(?:메타데이터|metadata)/i.test(heading[1])) {
                skipping = true;
                continue;
            }
            skipping = false;
        }
        if (!skipping) {
            kept.push(line);
        }
    }
    return kept.join("\n");
}

function issueMap(context) {
    const issues = new Map();
    const pullRequests = [context.targetPullRequest, ...(context.pendingPullRequests ?? [])];
    for (const pullRequest of pullRequests) {
        for (const issue of pullRequest?.closingIssues ?? []) {
            issues.set(issue.number, issue);
        }
    }
    return issues;
}

function structuredQaSourceCount(pullRequests) {
    return pullRequests.filter((pullRequest) => {
        const inspection = inspectQaMetadata(pullRequest.body, {
            pullRequestNumber: pullRequest.number,
        });
        return inspection.valid && isRunnableQaScope(inspection.metadata.scope);
    }).length;
}

function isHighRiskFile(file) {
    return HIGH_RISK_PATHS.some((pattern) => pattern.test(file));
}

function isDeploymentControlFile(file) {
    return DEPLOYMENT_CONTROL_PATHS.some((pattern) => pattern.test(file));
}

function isRuntimeNeutralFile(file) {
    return RUNTIME_NEUTRAL_PATHS.some((pattern) => pattern.test(file));
}

export function evaluateDeploymentDecision(context, changedFiles) {
    const pullRequests = context.pendingPullRequests ?? [];
    const issues = issueMap(context);
    const includedIssues = [...issues.keys()].sort((left, right) => left - right);
    const structuredQaSources = structuredQaSourceCount(pullRequests);
    const result = (decision, risk, reason) => ({
        decision,
        risk,
        reason,
        includedIssues,
        changedFiles,
        qaPoints: [],
    });

    if (context.targetCoveredBySuccessfulDistribution) {
        return result(
            "hold",
            "low",
            "대상 머지 커밋이 이미 성공한 배포에 포함되어 추가 배포가 필요하지 않습니다.",
        );
    }

    if (!context.baselineDistribution) {
        return result(
            "deploy",
            "normal",
            "성공한 배포 기준점이 없어 현재 상태를 첫 QA 기준 빌드로 배포해야 합니다.",
        );
    }

    const deploymentControlFiles = changedFiles.filter(isDeploymentControlFile);
    const highRiskPullRequests = pullRequests.filter((pullRequest) =>
        HIGH_RISK_TEXT.test(
            `${pullRequest.title}\n${bodyWithoutQaMetadata(pullRequest.body)}`,
        ),
    );
    if (deploymentControlFiles.length > 0) {
        const evidence = [
            ...deploymentControlFiles.slice(0, 3),
            ...highRiskPullRequests.slice(0, 3).map((pullRequest) => `PR #${pullRequest.number}`),
        ];
        return result(
            "deploy",
            "high",
            `고위험 변경이 포함됐습니다: ${unique(evidence).join(", ")}. 다른 변경을 기다리지 않고 QA 배포합니다.`,
        );
    }

    const runtimeNeutral = changedFiles.length > 0 && changedFiles.every(isRuntimeNeutralFile);
    if (runtimeNeutral) {
        return result(
            "hold",
            "low",
            "문서·테스트·일반 CI 등 앱 런타임에 영향을 주지 않는 변경만 있어 다음 사용자 노출 변경과 함께 배포합니다.",
        );
    }

    const highRiskFiles = changedFiles.filter(
        (file) => isHighRiskFile(file) && !isRuntimeNeutralFile(file),
    );
    if (highRiskFiles.length > 0 || highRiskPullRequests.length > 0) {
        const evidence = [
            ...highRiskFiles.slice(0, 3),
            ...highRiskPullRequests.slice(0, 3).map((pullRequest) => `PR #${pullRequest.number}`),
        ];
        return result(
            "deploy",
            "high",
            `고위험 변경이 포함됐습니다: ${unique(evidence).join(", ")}. 다른 변경을 기다리지 않고 QA 배포합니다.`,
        );
    }

    const parsedTitles = pullRequests.map((pullRequest) => ({
        pullRequest,
        ...parseTitle(pullRequest.title),
    }));
    const visibleFixes = parsedTitles.filter(({ type }) => type === "fix");
    if (visibleFixes.length > 0) {
        return result(
            "deploy",
            "normal",
            `테스터 확인이 필요한 결함 수정이 머지됐습니다: ${visibleFixes.map(({ pullRequest }) => `PR #${pullRequest.number}`).join(", ")}.`,
        );
    }

    const completedFeatures = parsedTitles.filter(({ type }) => type === "feat");
    if (completedFeatures.length > 0) {
        return result(
            "deploy",
            "normal",
            `새 사용자 흐름을 검증할 기능 변경이 완료됐습니다: ${completedFeatures.map(({ pullRequest }) => `PR #${pullRequest.number}`).join(", ")}.`,
        );
    }

    if (includedIssues.length >= 3) {
        return result(
            "deploy",
            "normal",
            `마지막 배포 이후 독립 이슈 ${includedIssues.length}건이 누적돼 회귀 원인 분리 전에 배포합니다.`,
        );
    }

    if (structuredQaSources >= 6) {
        return result(
            "deploy",
            "normal",
            `구조화된 QA 원천이 ${structuredQaSources}개로 배포 경계에 도달했습니다.`,
        );
    }

    const scopes = unique(parsedTitles.map(({ scope }) => scope).filter(Boolean));
    if (pullRequests.length >= 5 || scopes.length >= 3) {
        return result(
            "deploy",
            "normal",
            `누적 PR ${pullRequests.length}건·영향 스코프 ${scopes.length}개로 더 쌓이면 회귀 원인 분리가 어려워집니다.`,
        );
    }

    return result(
        "hold",
        "low",
        `현재 누적 변경은 PR ${pullRequests.length}건·이슈 ${includedIssues.length}건으로 즉시 QA가 필요한 위험 또는 묶음 크기에 도달하지 않았습니다.`,
    );
}

function changedFilesFor(context) {
    const baseSha = context.baselineDistribution?.headSha;
    const headSha = context.targetPullRequest?.mergeCommitSha;
    if (!headSha || context.targetCoveredBySuccessfulDistribution) {
        return [];
    }
    if (!baseSha) {
        return unique(
            (context.pendingPullRequests ?? []).flatMap(
                (pullRequest) => pullRequest.changedFiles ?? [],
            ),
        );
    }
    const output = execFileSync("git", ["diff", "--name-only", `${baseSha}..${headSha}`], {
        encoding: "utf8",
    });
    return unique(output.split(/\r?\n/).filter(Boolean));
}

async function main() {
    const [contextPath, outputPath] = process.argv.slice(2);
    if (!contextPath || !outputPath) {
        throw new Error("context path and decision output path are required");
    }
    const context = JSON.parse(await fs.readFile(contextPath, "utf8"));
    const decision = evaluateDeploymentDecision(context, changedFilesFor(context));
    await fs.mkdir(path.dirname(outputPath), { recursive: true });
    await fs.writeFile(outputPath, `${JSON.stringify(decision, null, 2)}\n`, "utf8");
}

const invokedPath = process.argv[1] ? pathToFileURL(path.resolve(process.argv[1])).href : "";
if (import.meta.url === invokedPath) {
    main().catch((error) => {
        console.error(error instanceof Error ? error.stack : error);
        process.exitCode = 1;
    });
}
