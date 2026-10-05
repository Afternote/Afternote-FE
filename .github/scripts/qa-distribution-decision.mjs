#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import fs from "node:fs/promises";
import path from "node:path";
import { pathToFileURL } from "node:url";

import { sourceShaForDistributionRun, sortDistributionRuns } from "./collect-release-scope-context.mjs";

// QA 배포 권고 기준이다. 배포 승인이나 빌드 성공을 뜻하지 않는다.
export const POLICY = Object.freeze({ runtimePullRequests: 5, moduleGroups: 3, pendingDays: 7 });
const SHA = /^[a-f0-9]{40}$/;
const DAY = 86_400_000;
const DELIVERY_PATH = /^\.github\/(?:workflows\/release-distribution\.ya?ml|actions\/(?:setup-release-config|cleanup-release-config)\/|scripts\/(?:(?:resolve-firebase-version-code|version-code-bands|verify-release-attestation)\.mjs|render-distribution-release-notes\.sh)$)/;
const NEUTRAL_PATH = /^(?:\.github\/|\.agents\/|\.claude\/|\.codex\/|docs\/|konsist\/|baselineprofile\/|scripts\/|\.idea\/)|(?:^|\/)src\/(?:test[^/]*|androidTest[^/]*|screenshotTest[^/]*|debug)\/|(?:^|\/)(?:README[^/]*|AGENTS\.md|CLAUDE\.md|LICENSE[^/]*|\.gitignore|\.editorconfig|\.gitattributes|\.mcp\.json)$|\.(?:md|mdx)$/i;

class CollectionError extends Error {}

export function requiresAppQa(file) {
    return DELIVERY_PATH.test(file) || !NEUTRAL_PATH.test(file);
}

function moduleGroup(file) {
    if (DELIVERY_PATH.test(file)) return "release";
    const feature = /^feature\/([^/]+)\//.exec(file);
    if (feature) return `feature/${feature[1]}`;
    if (/^(?:gradle\/|build-logic\/|[^/]*gradle[^/]*$)/.test(file)) return "build";
    return file.split("/")[0];
}

export function decideQaDistribution(context, now = Date.now()) {
    if (!Number.isFinite(now)) throw new CollectionError("Invalid evaluation time");
    const runtimePaths = context.changedPaths.filter(requiresAppQa);
    const result = {
        ...context,
        evaluatedAt: new Date(now).toISOString(),
        policy: POLICY,
        runtimePaths,
        decision: "hold",
        reasons: [],
        readyToDistribute: "not_evaluated",
    };
    if (runtimePaths.length === 0) {
        result.reasons.push("마지막 배포 이후 앱·배포 산출물에 영향을 줄 파일 차이가 없습니다.");
        return result;
    }
    if (context.unmappedCommits?.length || !context.runtimePullRequests?.length) {
        result.decision = "unknown";
        result.reasons.push("앱 변경과 머지 PR의 연결을 완전히 확인하지 못했습니다.");
        return result;
    }
    const timestamps = context.runtimePullRequests.map((pr) => Date.parse(pr.mergedAt));
    if (timestamps.some((date) => !Number.isFinite(date) || date > now)) {
        throw new CollectionError("Invalid merge time in pending PRs");
    }
    result.oldestPendingDays = Math.floor((now - Math.min(...timestamps)) / DAY);
    const immediate = context.runtimePullRequests.filter((pr) => /^(?:fix|feat|security)(?:\([^)]*\))?!?:/i.test(pr.title));
    if (immediate.length) {
        result.reasons.push(`앱 변경이 남아 있는 기능·버그·보안 PR ${immediate.length}건이 있습니다.`);
    }
    // 경로로 위험도를 추측하지 않는다. 실제 릴리스 조립 설정 변경은 산출물 QA 대상으로 표시한다.
    if (runtimePaths.some((file) => DELIVERY_PATH.test(file))) {
        result.reasons.push("실제 Firebase 릴리스 조립 경로가 변경됐습니다.");
    }
    if (context.runtimePullRequests.length >= POLICY.runtimePullRequests) {
        result.reasons.push(`미배포 앱 변경 PR ${context.runtimePullRequests.length}건이 묶음 기준 ${POLICY.runtimePullRequests}건에 도달했습니다.`);
    }
    result.moduleGroups = [...new Set(runtimePaths.map(moduleGroup))].sort();
    if (result.moduleGroups.length >= POLICY.moduleGroups) {
        result.reasons.push(`앱 영향 모듈군 ${result.moduleGroups.length}개가 묶음 기준 ${POLICY.moduleGroups}개에 도달했습니다.`);
    }
    if (result.oldestPendingDays >= POLICY.pendingDays) {
        result.reasons.push(`가장 오래된 미배포 앱 변경이 ${result.oldestPendingDays}일 대기했습니다.`);
    }
    if (result.reasons.length) result.decision = "deploy";
    else result.reasons.push("앱 변경이 있으나 즉시 권고·누적량·최대 대기 기준에 아직 도달하지 않았습니다.");
    return result;
}

function command(binary, args) {
    try {
        return execFileSync(binary, args, { encoding: "utf8", maxBuffer: 128 * 1024 * 1024, stdio: ["ignore", "pipe", "pipe"] }).trim();
    } catch (error) {
        const status = Number.isInteger(error.status) ? error.status : null;
        throw Object.assign(new CollectionError(`${binary === "gh" ? "GitHub API" : "Git history"} read failed (exit ${status ?? "unknown"})`), { status });
    }
}

function githubApi(endpoint, paginate = false) {
    const args = ["api", ...(paginate ? ["--paginate", "--slurp"] : []), endpoint];
    return JSON.parse(command("gh", args));
}

function pages(items, field) {
    if (!Array.isArray(items)) throw new CollectionError("Missing paginated response");
    return items.flatMap((page) => {
        const values = field ? page[field] : page;
        if (!Array.isArray(values)) throw new CollectionError("Invalid paginated response");
        return values;
    });
}

function gitText(args) {
    return command("git", args);
}

function isAncestor(ancestor, head, git) {
    if (!SHA.test(ancestor ?? "") || !SHA.test(head ?? "")) throw new CollectionError("Invalid commit SHA");
    try {
        git(["merge-base", "--is-ancestor", ancestor, head]);
        return true;
    } catch (error) {
        if (error.status === 1) return false;
        throw error;
    }
}

function changedPaths(base, head, git) {
    return git(["diff", "--no-renames", "--name-only", "-z", base, head, "--"]).split("\0").filter(Boolean);
}

function changedBlobs(base, head, git) {
    const fields = git(["diff", "--raw", "--no-abbrev", "--no-renames", "-z", base, head, "--"]).split("\0").filter(Boolean);
    if (fields.length % 2) throw new CollectionError("Incomplete raw Git diff");
    const entries = [];
    for (let i = 0; i < fields.length; i += 2) {
        const match = /^:(\d{6}) (\d{6}) ([a-f0-9]{40}) ([a-f0-9]{40}) [AMDT]$/.exec(fields[i]);
        if (!match) throw new CollectionError("Unsupported raw Git diff");
        entries.push({ file: fields[i + 1], before: `${match[1]}:${match[3]}`, after: `${match[2]}:${match[4]}` });
    }
    return entries;
}

export async function collectQaDistributionContext({ repository, headSha, api = githubApi, git = gitText }) {
    if (!/^[\w.-]+\/[\w.-]+$/.test(repository ?? "") || !SHA.test(headSha ?? "")) {
        throw new CollectionError("Repository and exact develop SHA are required");
    }
    git(["cat-file", "-e", `${headSha}^{commit}`]);
    const root = `repos/${repository}`;
    const runs = pages(await api(`${root}/actions/workflows/release-distribution.yml/runs?status=success&branch=main&event=push&per_page=100`, true), "workflow_runs");
    const latest = sortDistributionRuns(runs.filter((run) => run.conclusion === "success" && run.head_branch === "main" && run.event === "push"))[0];
    if (!latest) throw new CollectionError("No successful main Firebase distribution baseline");
    const associated = pages(await api(`${root}/commits/${latest.head_sha}/pulls?per_page=100`, true));
    const sourceSha = sourceShaForDistributionRun(latest, associated);
    if (!SHA.test(sourceSha ?? "")) throw new CollectionError("Successful distribution has no develop release PR source SHA");
    if (!isAncestor(sourceSha, headSha, git)) throw new CollectionError("Deployed source is not an ancestor of this develop revision");
    const context = {
        repository,
        headSha,
        baseline: { runId: latest.id, sourceSha, mainSha: latest.head_sha, completedAt: latest.updated_at, url: latest.html_url },
        changedPaths: changedPaths(sourceSha, headSha, git),
        pendingPullRequestCount: null,
        runtimePullRequests: [],
        unmappedCommits: [],
    };
    const remaining = new Set(context.changedPaths.filter(requiresAppQa));
    if (remaining.size === 0) return context;

    // gh follows every Link page. No 50-PR or 1,000-closed-PR truncation.
    const pulls = pages(await api(`${root}/pulls?state=closed&base=develop&sort=updated&direction=desc&per_page=100`, true));
    const firstParentHistory = git(["rev-list", "--first-parent", headSha]).split("\n").filter(Boolean);
    const boundary = firstParentHistory.indexOf(sourceSha);
    if (boundary < 0) throw new CollectionError("Deployed source is outside develop first-parent history");
    const directCommits = firstParentHistory.slice(0, boundary).reverse();
    const direct = new Set(directCommits);
    const pending = pulls.filter((pr) => pr.merged_at && pr.base?.ref === "develop" && direct.has(pr.merge_commit_sha));
    if (new Set(pending.map((pr) => pr.number)).size !== pending.length) throw new CollectionError("Duplicate PRs in paginated response");
    context.pendingPullRequestCount = pending.length;
    const byCommit = new Map(pending.map((pr) => [pr.merge_commit_sha, pr]));
    const baselineBlobs = new Map();
    const contributors = new Map();
    for (const sha of directCommits) {
        if (!SHA.test(sha)) throw new CollectionError("Invalid commit in develop history");
        for (const entry of changedBlobs(`${sha}^1`, sha, git)) {
            if (!remaining.has(entry.file)) continue;
            if (!baselineBlobs.has(entry.file)) baselineBlobs.set(entry.file, entry.before);
            const history = contributors.get(entry.file) ?? [];
            // A full return to the deployed blob removes earlier feature/revert history.
            contributors.set(entry.file, entry.after === baselineBlobs.get(entry.file) ? [] : [...history, sha]);
        }
    }
    if ([...remaining].some((file) => !contributors.get(file)?.length)) throw new CollectionError("Net app diff has no complete contributing history");
    const pathsByCommit = new Map();
    for (const [file, commits] of contributors) for (const sha of commits) {
        pathsByCommit.set(sha, [...(pathsByCommit.get(sha) ?? []), file]);
    }
    const runtimeByPr = new Map();
    for (const [sha, changed] of pathsByCommit) {
        let pr = byCommit.get(sha);
        if (!pr) {
            // Rebase merges keep several first-parent commits, but merge_commit_sha names only the last.
            const related = pages(await api(`${root}/commits/${sha}/pulls?per_page=100`, true))
                .filter((candidate) => candidate.merged_at && candidate.base?.ref === "develop" && direct.has(candidate.merge_commit_sha));
            if (related.length === 1) pr = related[0];
        }
        if (!pr) {
            context.unmappedCommits.push(sha);
            continue;
        }
        const prior = runtimeByPr.get(pr.number);
        runtimeByPr.set(pr.number, { number: pr.number, title: pr.title, mergedAt: pr.merged_at, mergeSha: pr.merge_commit_sha,
            paths: [...new Set([...(prior?.paths ?? []), ...changed])] });
    }
    context.runtimePullRequests = [...runtimeByPr.values()].sort((left, right) => left.number - right.number);
    return context;
}

const LABELS = { deploy: "배포 필요", hold: "묶음 대기", unknown: "판정 불가" };

export function renderQaDistributionSummary(result) {
    const lines = [
        `## QA 배포 판정: ${LABELS[result.decision]}`,
        "",
        ...result.reasons.map((reason) => `- ${reason}`),
        "",
        `- 판정 대상: \`${result.headSha ?? "확인 실패"}\``,
        `- 마지막 배포의 develop SHA: \`${result.baseline?.sourceSha ?? "확인 실패"}\``,
        `- 앱 변경 PR: ${result.runtimePullRequests?.length ?? "확인 실패"}건`,
        `- 순 변경 파일: ${result.changedPaths?.length ?? "확인 실패"}개`,
        "",
        "이 결과는 QA 배포 필요성에 대한 권고입니다. 배포 가능 여부는 검사하지 않았습니다. 릴리스 PR의 필수 CI와 QA를 통과한 뒤 main으로 승격합니다.",
        "PR 유형과 파일 단위 변경 이력을 사용합니다. 실제 사용자 영향·위험도를 의미 분석하거나 부분 revert의 줄 단위 기여를 확정하지 않습니다.",
        "PR·이슈 댓글 작성과 자동 배포는 수행하지 않습니다.",
    ];
    if (result.baseline) lines.push("", `[기준 배포 실행](${result.baseline.url})`);
    if (result.runtimePullRequests?.length) {
        lines.push("", `앱 변경 PR: ${result.runtimePullRequests.map((pr) => `[#${pr.number}](https://github.com/${result.repository}/pull/${pr.number})`).join(", ")}`);
    }
    return `${lines.join("\n")}\n`;
}

export async function runQaDistributionDecision({ env = process.env, collect = collectQaDistributionContext, now = Date.now() } = {}) {
    const outputDirectory = env.QA_DISTRIBUTION_OUTPUT_DIR;
    if (!outputDirectory) throw new CollectionError("QA_DISTRIBUTION_OUTPUT_DIR is required");
    let result;
    try {
        result = decideQaDistribution(await collect({ repository: env.GITHUB_REPOSITORY, headSha: env.QA_DISTRIBUTION_HEAD_SHA }), now);
    } catch (error) {
        // gh/git errors may include response bodies. Keep public artifacts free of raw API/token data.
        result = { decision: "unknown", headSha: SHA.test(env.QA_DISTRIBUTION_HEAD_SHA ?? "") ? env.QA_DISTRIBUTION_HEAD_SHA : null,
            evaluatedAt: new Date(now).toISOString(), readyToDistribute: "not_evaluated",
            reasons: [error instanceof CollectionError ? error.message : "배포 기준점 또는 전체 변경 범위 수집에 실패했습니다. API 접근·전체 Git 이력·배포 PR 연결을 확인하세요."] };
    }
    await fs.mkdir(outputDirectory, { recursive: true });
    const summary = renderQaDistributionSummary(result);
    await fs.writeFile(path.join(outputDirectory, "decision.json"), `${JSON.stringify(result, null, 2)}\n`);
    await fs.writeFile(path.join(outputDirectory, "summary.md"), summary);
    if (env.GITHUB_STEP_SUMMARY) await fs.appendFile(env.GITHUB_STEP_SUMMARY, summary);
    console.log(`QA 배포 판정: ${LABELS[result.decision]}`);
    return result;
}

if (import.meta.url === (process.argv[1] ? pathToFileURL(path.resolve(process.argv[1])).href : "")) {
    runQaDistributionDecision().then((result) => {
        if (result.decision === "unknown") process.exitCode = 1;
    }).catch((error) => { console.error(error.message); process.exitCode = 1; });
}
