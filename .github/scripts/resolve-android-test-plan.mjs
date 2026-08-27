#!/usr/bin/env node

import fs from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import { pathToFileURL } from "node:url";

import {
    ciTestPlanDigest,
    inspectPullRequestCiTestPlan,
} from "./ci-test-plan.mjs";

export function resolveAndroidTestPlan(pullRequest) {
    const inspection = inspectPullRequestCiTestPlan(pullRequest);
    if (!inspection.valid) {
        throw new Error(inspection.errors.join("\n"));
    }
    const plan = inspection.plan;
    return {
        plan,
        digest: ciTestPlanDigest(plan),
    };
}

async function main() {
    const payloadPath = process.argv[2];
    if (!payloadPath) {
        throw new Error("pull request JSON 경로가 필요합니다.");
    }
    const payload = JSON.parse(await fs.readFile(path.resolve(payloadPath), "utf8"));
    const pullRequest = payload?.pull_request ?? payload;
    process.stdout.write(`${JSON.stringify(resolveAndroidTestPlan(pullRequest))}\n`);
}

const invokedPath = process.argv[1] ? pathToFileURL(path.resolve(process.argv[1])).href : "";
if (import.meta.url === invokedPath) {
    main().catch((error) => {
        console.error(error instanceof Error ? error.message : error);
        process.exitCode = 1;
    });
}
