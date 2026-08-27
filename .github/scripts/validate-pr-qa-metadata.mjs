#!/usr/bin/env node

import fs from "node:fs/promises";
import path from "node:path";
import { pathToFileURL } from "node:url";

import { hasQaMetadataSection, inspectQaMetadata } from "./qa-metadata.mjs";

// 게이트 도입 전에 생성된 PR은 리베이스로 이 워크플로를 받아도 소급 차단하지 않는다.
// 섹션을 채워 넣은 순간부터는 생성 시각과 무관하게 검증한다.
//
// 이 값은 게이트가 develop 에 들어가는 날보다 뒤여야 한다. 앞서면 그 사이에 열린 PR 들이
// 본문을 고치기 전까지 리베이스하는 순간 막힌다 — 8/24 로 두는 동안 열린 PR 22건이,
// 8/26 00:00Z 로 두는 동안 #1194 가 그 상태였다. 머지가 이 날짜를 넘기면 넘긴 만큼 다시 민다.
export const QA_METADATA_GATE_CUTOFF = "2026-08-27T00:00:00Z";
// 기존 구조화 QA 본문을 소급 차단하지 않는다. 도입 감사 시점에 열려 있던 가장 최근 PR
// (#1265, 2026-08-27T06:44:06Z) 뒤로 경계를 고정하고 이후 PR은 명시적 결정을 요구한다.
export const ANDROID_TEST_DECISION_GATE_CUTOFF = "2026-08-27T08:00:00Z";

function isGrandfathered(pullRequest) {
    const createdAt = Date.parse(pullRequest?.created_at ?? "");
    return Number.isFinite(createdAt) && createdAt < Date.parse(QA_METADATA_GATE_CUTOFF);
}

function isAndroidTestDecisionGrandfathered(pullRequest) {
    const createdAt = Date.parse(pullRequest?.created_at ?? "");
    return (
        Number.isFinite(createdAt) &&
        createdAt < Date.parse(ANDROID_TEST_DECISION_GATE_CUTOFF)
    );
}

function escapeWorkflowCommand(value) {
    return String(value)
        .replaceAll("%", "%25")
        .replaceAll("\r", "%0D")
        .replaceAll("\n", "%0A");
}

export async function validateAndroidTestReference(metadata, { root = process.cwd() } = {}) {
    if (metadata?.androidTest?.required !== true) {
        return;
    }
    const testRef = metadata.androidTest.testRef;
    const [relativePath, testName] = testRef.split("#", 2);
    const absolutePath = path.resolve(root, relativePath);
    const relativeToRoot = path.relative(root, absolutePath);
    if (relativeToRoot.startsWith("..") || path.isAbsolute(relativeToRoot)) {
        throw new Error(`androidTest.testRef가 저장소 밖을 가리킵니다: ${testRef}`);
    }

    let stat;
    try {
        stat = await fs.stat(absolutePath);
    } catch {
        throw new Error(`androidTest.testRef 파일이 현재 PR revision에 없습니다: ${testRef}`);
    }
    if (!stat.isFile()) {
        throw new Error(`androidTest.testRef가 파일이 아닙니다: ${testRef}`);
    }

    const source = await fs.readFile(absolutePath, "utf8");
    const escapedTestName = testName.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    const testDeclaration = new RegExp(
        `@Test(?:\\s*\\([^)]*\\))?\\s*` +
            `(?:@[\\w:.]+(?:\\([^\\n]*\\))?\\s*)*` +
            `fun\\s+${escapedTestName}\\s*\\(`,
    );
    if (!testDeclaration.test(source)) {
        throw new Error(`androidTest.testRef의 @Test 메서드가 현재 PR revision에 없습니다: ${testRef}`);
    }
}

export function validatePullRequestEvent(event) {
    if (!event?.pull_request) {
        return { skipped: true, valid: true, errors: [] };
    }
    if (isGrandfathered(event.pull_request) && !hasQaMetadataSection(event.pull_request.body)) {
        return { skipped: false, grandfathered: true, valid: true, errors: [] };
    }
    const number = event.pull_request.number ?? event.number ?? "?";
    const inspection = inspectQaMetadata(event.pull_request.body, {
        pullRequestNumber: number,
        requireAndroidTestDecision: !isAndroidTestDecisionGrandfathered(event.pull_request),
    });
    return {
        skipped: false,
        grandfathered: false,
        valid: inspection.valid,
        errors: inspection.errors,
        metadata: inspection.metadata,
    };
}

async function main() {
    const eventPath = process.argv[2] || process.env.GITHUB_EVENT_PATH;
    if (!eventPath) {
        throw new Error("GitHub pull_request event path is required");
    }
    const event = JSON.parse(await fs.readFile(eventPath, "utf8"));
    const validation = validatePullRequestEvent(event);
    if (validation.skipped) {
        console.log("pull_request event가 아니므로 QA 메타데이터 검증을 건너뜁니다.");
        return;
    }
    if (validation.grandfathered) {
        console.log(
            `::notice title=구조화 QA 메타데이터::${escapeWorkflowCommand(
                `게이트 도입(${QA_METADATA_GATE_CUTOFF}) 전에 생성된 PR이므로 검증을 건너뜁니다. ` +
                    "`QA 메타데이터` 섹션을 채우면 다음 push부터 검증됩니다.",
            )}`,
        );
        return;
    }
    if (!validation.valid) {
        for (const error of validation.errors) {
            console.error(
                `::error title=구조화 QA 메타데이터 오류::${escapeWorkflowCommand(error)}`,
            );
        }
        process.exitCode = 1;
        return;
    }
    try {
        await validateAndroidTestReference(validation.metadata);
    } catch (error) {
        console.error(
            `::error title=구조화 QA 메타데이터 오류::${escapeWorkflowCommand(error.message)}`,
        );
        process.exitCode = 1;
        return;
    }
    console.log(`QA 메타데이터 검증 완료: ${validation.metadata.scope}`);
}

const invokedPath = process.argv[1] ? pathToFileURL(path.resolve(process.argv[1])).href : "";
if (import.meta.url === invokedPath) {
    main().catch((error) => {
        console.error(error instanceof Error ? error.stack : error);
        process.exitCode = 1;
    });
}
