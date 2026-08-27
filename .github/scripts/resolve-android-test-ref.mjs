#!/usr/bin/env node

import fs from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import { pathToFileURL } from "node:url";

import { inspectQaMetadata } from "./qa-metadata.mjs";

export function resolveAndroidTestRef(body, pullRequestNumber = "?") {
    const inspection = inspectQaMetadata(body, { pullRequestNumber });
    if (!inspection.valid || inspection.metadata?.androidTest?.required !== true) {
        return "";
    }
    return inspection.metadata.androidTest.testRef;
}

async function main() {
    const pullRequestPath = process.argv[2];
    if (!pullRequestPath) {
        throw new Error("pull request JSON path가 필요합니다.");
    }
    const pullRequest = JSON.parse(await fs.readFile(pullRequestPath, "utf8"));
    process.stdout.write(resolveAndroidTestRef(pullRequest.body, pullRequest.number));
}

const invokedPath = process.argv[1] ? pathToFileURL(path.resolve(process.argv[1])).href : "";
if (import.meta.url === invokedPath) {
    main().catch((error) => {
        console.error(error.message);
        process.exitCode = 1;
    });
}
