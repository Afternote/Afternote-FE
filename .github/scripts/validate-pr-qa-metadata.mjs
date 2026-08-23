#!/usr/bin/env node

import fs from "node:fs/promises";
import path from "node:path";
import { pathToFileURL } from "node:url";

import { hasQaMetadataSection, inspectQaMetadata } from "./qa-semantic-audit.mjs";

// 게이트 도입 전에 생성된 PR은 리베이스로 이 워크플로를 받아도 소급 차단하지 않는다.
// 섹션을 채워 넣은 순간부터는 생성 시각과 무관하게 검증한다.
export const QA_METADATA_GATE_CUTOFF = "2026-08-24T00:00:00Z";

function isGrandfathered(pullRequest) {
    const createdAt = Date.parse(pullRequest?.created_at ?? "");
    return Number.isFinite(createdAt) && createdAt < Date.parse(QA_METADATA_GATE_CUTOFF);
}

function escapeWorkflowCommand(value) {
    return String(value)
        .replaceAll("%", "%25")
        .replaceAll("\r", "%0D")
        .replaceAll("\n", "%0A");
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
    console.log(`QA 메타데이터 검증 완료: ${validation.metadata.scope}`);
}

const invokedPath = process.argv[1] ? pathToFileURL(path.resolve(process.argv[1])).href : "";
if (import.meta.url === invokedPath) {
    main().catch((error) => {
        console.error(error instanceof Error ? error.stack : error);
        process.exitCode = 1;
    });
}
