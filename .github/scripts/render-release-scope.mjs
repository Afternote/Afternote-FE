#!/usr/bin/env node

import fs from "node:fs/promises";
import path from "node:path";
import { pathToFileURL } from "node:url";

const ISSUE_HEADING = "포함 이슈";
const QA_HEADING = "QA 포인트";
const HEADING_PATTERN = /^#{1,6}\s+(.+?)\s*$/;

function unique(values) {
    return [...new Set(values)];
}

export function extractQaPoints(body) {
    const points = [];
    let capturing = false;
    for (const line of String(body ?? "").split(/\r?\n/)) {
        const heading = HEADING_PATTERN.exec(line);
        if (heading) {
            if (capturing) {
                break;
            }
            capturing = /qa\s*포인트/i.test(heading[1]);
            continue;
        }
        if (!capturing) {
            continue;
        }
        const point = line
            .replace(/^\s*[-*]\s*/, "")
            .replace(/^\s*\d+\.\s*/, "")
            .trim();
        if (point && point.toLowerCase() !== "no response") {
            points.push(point);
        }
    }
    return unique(points);
}

export function summarizeReleaseScope(context) {
    const pullRequests = context.pendingPullRequests ?? [];
    const issues = new Map();
    for (const pullRequest of pullRequests) {
        for (const issue of pullRequest.closingIssues ?? []) {
            issues.set(issue.number, issue);
        }
    }
    const includedIssues = [...issues.keys()].sort((left, right) => left - right);
    return {
        alreadyDistributed: context.alreadyDistributed === true,
        pullRequestCount: pullRequests.length,
        includedIssues,
        issueTitles: Object.fromEntries(
            [...issues.entries()].map(([number, issue]) => [number, issue.title]),
        ),
        qaPointsDraft: unique(
            pullRequests.flatMap((pullRequest) => extractQaPoints(pullRequest.body)),
        ),
        baseline: context.baselineDistribution ?? null,
    };
}

export function renderIssueSection(includedIssues) {
    if (includedIssues.length === 0) {
        return "- 연결된 이슈 없음 — 배포 전에 직접 채워 주세요.";
    }
    return includedIssues.map((number) => `- #${number}`).join("\n");
}

export function renderQaDraftSection(scope) {
    if (scope.qaPointsDraft.length > 0) {
        return scope.qaPointsDraft.map((point) => `- ${point}`).join("\n");
    }
    return "- 테스터가 실행할 동작과 기대 결과를 직접 채워 주세요.";
}

function findSection(lines, heading) {
    let start = -1;
    for (const [index, line] of lines.entries()) {
        const match = HEADING_PATTERN.exec(line);
        if (!match) {
            continue;
        }
        if (start === -1) {
            if (match[1] === heading) {
                start = index;
            }
            continue;
        }
        return { start, end: index };
    }
    return start === -1 ? null : { start, end: lines.length };
}

function sectionHasContent(lines, section) {
    return lines
        .slice(section.start + 1, section.end)
        .some((line) => line.trim().length > 0);
}

/**
 * `## 포함 이슈` 는 항상 산출값으로 덮어쓰고, `## QA 포인트` 는 비어 있을 때만 초안을 채운다.
 * 사람이 다시 쓴 QA 문구와 나머지 섹션은 건드리지 않는다.
 */
export function applyReleaseScopeToBody(body, { issueSection, qaSection }) {
    let lines = String(body ?? "").split(/\r?\n/);

    const replaceSection = (heading, content) => {
        const section = findSection(lines, heading);
        if (!section) {
            const tail = lines.length > 0 && lines.at(-1).trim() === "" ? [] : [""];
            lines = [...lines, ...tail, `## ${heading}`, "", ...content.split("\n"), ""];
            return;
        }
        lines = [
            ...lines.slice(0, section.start + 1),
            "",
            ...content.split("\n"),
            "",
            ...lines.slice(section.end),
        ];
    };

    replaceSection(ISSUE_HEADING, issueSection);

    const qa = findSection(lines, QA_HEADING);
    if (!qa || !sectionHasContent(lines, qa)) {
        replaceSection(QA_HEADING, qaSection);
    }

    return lines.join("\n").replace(/\n{3,}/g, "\n\n").trimEnd() + "\n";
}

async function main() {
    const [contextPath, outputPath] = process.argv.slice(2);
    if (!contextPath || !outputPath) {
        throw new Error("context path and scope output path are required");
    }
    const context = JSON.parse(await fs.readFile(contextPath, "utf8"));
    const scope = summarizeReleaseScope(context);
    const result = {
        ...scope,
        body: applyReleaseScopeToBody(context.releasePullRequest?.body, {
            issueSection: renderIssueSection(scope.includedIssues),
            qaSection: renderQaDraftSection(scope),
        }),
    };
    await fs.mkdir(path.dirname(outputPath), { recursive: true });
    await fs.writeFile(outputPath, `${JSON.stringify(result, null, 2)}\n`, "utf8");
}

const invokedPath = process.argv[1] ? pathToFileURL(path.resolve(process.argv[1])).href : "";
if (import.meta.url === invokedPath) {
    main().catch((error) => {
        console.error(error instanceof Error ? error.stack : error);
        process.exitCode = 1;
    });
}
