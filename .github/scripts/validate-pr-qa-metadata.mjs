#!/usr/bin/env node

import fs from "node:fs/promises";
import path from "node:path";
import { pathToFileURL } from "node:url";

import { inspectQaMetadata } from "./qa-semantic-audit.mjs";

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
    const number = event.pull_request.number ?? event.number ?? "?";
    const inspection = inspectQaMetadata(event.pull_request.body, {
        pullRequestNumber: number,
    });
    return {
        skipped: false,
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
