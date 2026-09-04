#!/usr/bin/env node

// PR 작성자가 담당하지 않는 모듈의 «프로덕션 파일» 을 바꿨는지 검사한다 (#1889).
//
// «남의 담당 모듈은 내 PR 에 프로덕션 코드 0줄» 은 0830 에 확정된 규칙인데, 지금까지 지도
// (`reconcile-issue-metadata.mjs` 의 `ASSIGNEE_BY_MODULE`) 는 이슈 어사인에만 쓰였고 PR 의 변경
// 파일과는 대조된 적이 없다. 대표 Issue 담당자 대조(#1366)는 «이슈» 단위라, 담당 이슈로 열어 놓고
// 남의 모듈 파일을 끼워 넣는 것은 못 잡는다 — #1354 가 그렇게 setting 파일을 실었다.
//
// 지도는 한 곳이다 — 여기에 담당 표를 다시 적지 않는다.
import { readFile } from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import { fileURLToPath } from "node:url";

import { ASSIGNEE_BY_MODULE } from "./reconcile-issue-metadata.mjs";
import { changedPathsFromGithubFiles } from "./resolve-pr-impact.mjs";
import { hasIssueAssigneeExemptLabel } from "./validate-pr-issue-link.mjs";

// 경로 → 모듈 키. `feature/<x>/` 는 x, `core/` 는 core, 나머지 저장소 운영 경로는 platform.
// 수신자 홈은 #1724 가 `feature/home/…/receiver/` 로 옮겨 경로가 곧 담당이다 — 예외 표가 없다.
const PLATFORM_PREFIXES = ["app/", "build-logic/", "konsist/", ".github/", "gradle/", "scripts/", "git-hooks/"];
const PLATFORM_FILES = new Set([
    "build.gradle.kts", "settings.gradle.kts", "gradle.properties", "gradlew", "gradlew.bat",
    ".editorconfig", "Dockerfile.screenshot",
]);
// 소유자가 없는 것 — 누구나 고친다.
const UNOWNED_PREFIXES = ["docs/"];
const UNOWNED_FILES = new Set(["README.md", ".gitignore", ".dockerignore", ".mcp.json"]);
// 테스트 소스셋은 남의 모듈이라도 무관하다 (0830 확정). testFixtures·screenshotTest 도 같은 쪽이다.
const TEST_SOURCE_SET_MARKERS = ["/src/test/", "/src/androidTest/", "/src/testFixtures/", "/src/screenshotTest/"];

export const ISSUE_ASSIGNEE_EXEMPT_LABEL = "issue-assignee-exempt";

function normalize(filePath) {
    return String(filePath ?? "").replaceAll("\\", "/").replace(/^\.\//, "").replace(/^\/+/, "");
}

/** 경로가 속한 모듈 키. 소유자 없는 경로는 null. */
export function moduleKeyOf(filePath) {
    const p = normalize(filePath);
    if (!p) return null;
    if (UNOWNED_FILES.has(p) || UNOWNED_PREFIXES.some((prefix) => p.startsWith(prefix))) return null;
    const feature = /^feature\/([a-z]+)\//.exec(p);
    if (feature) return feature[1];
    if (p.startsWith("core/")) return "core";
    if (PLATFORM_FILES.has(p) || PLATFORM_PREFIXES.some((prefix) => p.startsWith(prefix))) return "platform";
    return null;
}

/** 프로덕션 파일인가 — 테스트 소스셋은 제외, 그 밖의 모듈 파일(빌드 스크립트·리소스·매니페스트)은 포함. */
export function isProductionPath(filePath) {
    const p = `/${normalize(filePath)}`;
    return !TEST_SOURCE_SET_MARKERS.some((marker) => p.includes(marker));
}

function isBotAuthor(user, login) {
    // validate-pr-issue-link.mjs 와 같은 경계 — 봇은 담당자가 될 수 없다.
    return user?.type === "Bot" || login.endsWith("[bot]");
}

/**
 * @returns {{ skipped?: string, violations: Array<{ path: string, module: string, owner: string }> }}
 */
export function validatePullRequestModuleOwner({ pullRequest, changedPaths, owners = ASSIGNEE_BY_MODULE }) {
    const login = String(pullRequest?.user?.login ?? "");
    if (!login) throw new Error("pull_request.user.login 값이 없습니다.");
    if (isBotAuthor(pullRequest.user, login)) return { skipped: "봇 작성자", violations: [] };
    if (hasIssueAssigneeExemptLabel(pullRequest)) return { skipped: `${ISSUE_ASSIGNEE_EXEMPT_LABEL} 라벨`, violations: [] };

    const violations = [];
    for (const raw of changedPaths) {
        const filePath = normalize(raw);
        if (!isProductionPath(filePath)) continue;
        const module = moduleKeyOf(filePath);
        if (!module) continue;
        const owner = owners[module];
        if (!owner) continue; // 지도에 없는 모듈은 판정하지 않는다 — 지도 갱신은 reconcile 쪽 일이다.
        if (owner.toLowerCase() !== login.toLowerCase()) violations.push({ path: filePath, module, owner });
    }
    return { violations };
}

export function formatViolations(pullRequest, violations) {
    const byModule = new Map();
    for (const v of violations) byModule.set(v.module, [...(byModule.get(v.module) ?? []), v]);
    const lines = [...byModule.entries()].map(([module, items]) =>
        `  ${module}(담당 @${items[0].owner}) ${items.length}건: ${items.map((i) => i.path).join(", ")}`);
    return [
        `PR #${pullRequest.number} 작성자 @${pullRequest.user.login} 가 담당하지 않는 모듈의 프로덕션 파일을 바꿨습니다.`,
        ...lines,
        `테스트 소스셋은 무관하고, 프로덕션 파일은 떼어 담당자에게 인계하세요. 인계를 기다릴 수 없는 긴급 수선이면 \`${ISSUE_ASSIGNEE_EXEMPT_LABEL}\` 라벨을 붙인 뒤 재검증(본문 수정 또는 push)을 트리거하세요.`,
    ].join("\n");
}

async function main() {
    const [pullRequestPath, filesPath] = process.argv.slice(2);
    if (!pullRequestPath || !filesPath) throw new Error("사용법: validate-pr-module-owner.mjs <pull-request.json> <files.json>");
    const event = JSON.parse(await readFile(pullRequestPath, "utf8"));
    const pullRequest = event.pull_request ?? event;
    if (!Number.isInteger(pullRequest?.number)) throw new Error("pull request JSON이 아닙니다.");
    const changedPaths = changedPathsFromGithubFiles(JSON.parse(await readFile(filesPath, "utf8")));

    const result = validatePullRequestModuleOwner({ pullRequest, changedPaths });
    if (result.skipped) {
        console.log(`${result.skipped} — 모듈 담당 대조 건너뜀`);
        return;
    }
    if (result.violations.length > 0) throw new Error(formatViolations(pullRequest, result.violations));
    console.log(`PR #${pullRequest.number} 변경 파일 ${changedPaths.length}개 — 모듈 담당 대조 통과`);
}

const isDirectExecution = process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);
if (isDirectExecution) {
    try {
        await main();
    } catch (error) {
        console.error(`::error::${error.message}`);
        process.exitCode = 1;
    }
}
