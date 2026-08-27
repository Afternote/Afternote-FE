import { readFile } from "node:fs/promises";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";

const ISSUE_REFERENCE_RE = /\b(?:close[sd]?|fix(?:e[sd])?|resolve[sd]?|refs?|references?|part\s+of|related\s+to)\s*:?[ \t]+(?:(?:([\w.-]+)\/([\w.-]+))?#(\d+)|https:\/\/github\.com\/([\w.-]+)\/([\w.-]+)\/issues\/(\d+))/gi;

function visibleMarkdown(text) {
    return String(text ?? "")
        .replace(/<!--[\s\S]*?(?:-->|$)/g, "")
        .replace(/```[\s\S]*?(?:```|$)/g, "")
        .replace(/~~~[\s\S]*?(?:~~~|$)/g, "")
        .replace(/`[^`\n]*`/g, "");
}

function requiredString(value, name) {
    if (typeof value !== "string" || value.trim() === "") {
        throw new Error(`${name} 값이 없습니다.`);
    }
    return value.trim();
}

export function extractSameRepositoryIssueNumbers(text, repository) {
    const [expectedOwner, expectedName] = requiredString(repository, "repository").split("/");
    if (!expectedOwner || !expectedName) {
        throw new Error(`repository 형식이 owner/name 이 아닙니다: ${repository}`);
    }

    const issueNumbers = new Set();
    ISSUE_REFERENCE_RE.lastIndex = 0;
    for (const match of visibleMarkdown(text).matchAll(ISSUE_REFERENCE_RE)) {
        const owner = match[1] ?? match[4] ?? expectedOwner;
        const name = match[2] ?? match[5] ?? expectedName;
        const issueNumber = Number(match[3] ?? match[6]);
        if (
            owner.toLowerCase() === expectedOwner.toLowerCase()
            && name.toLowerCase() === expectedName.toLowerCase()
        ) {
            issueNumbers.add(issueNumber);
        }
    }
    return [...issueNumbers];
}

export async function validatePullRequestIssueLink({ pullRequest, repository, loadIssue }) {
    if (typeof loadIssue !== "function") {
        throw new Error("loadIssue 함수가 필요합니다.");
    }
    const pullRequestNumber = pullRequest?.number;
    if (!Number.isInteger(pullRequestNumber)) {
        throw new Error("pull_request.number 값이 없습니다.");
    }

    const references = extractSameRepositoryIssueNumbers(pullRequest.body, repository);
    if (references.length === 0) {
        throw new Error(
            `PR #${pullRequestNumber}에 같은 저장소의 Issue 참조가 없습니다. 관련 기존 Issue를 재사용하고 Refs #N을 추가하세요.`,
        );
    }

    const issues = [];
    const rejected = [];
    for (const issueNumber of references) {
        let issue;
        try {
            issue = await loadIssue(issueNumber);
        } catch (error) {
            rejected.push(`#${issueNumber}: 조회 실패 (${error.message})`);
            continue;
        }
        if (issue?.pull_request) {
            rejected.push(`#${issueNumber}: Issue가 아니라 PR`);
            continue;
        }
        issues.push(issueNumber);
    }

    if (issues.length === 0) {
        throw new Error(
            `PR #${pullRequestNumber}에 연결된 실제 Issue가 없습니다. ${rejected.join(", ")}`,
        );
    }
    return { issues, rejected };
}

async function requestIssue(apiUrl, repository, token, issueNumber) {
    const response = await fetch(`${apiUrl}/repos/${repository}/issues/${issueNumber}`, {
        headers: {
            Accept: "application/vnd.github+json",
            Authorization: `Bearer ${token}`,
            "X-GitHub-Api-Version": "2022-11-28",
        },
    });
    if (!response.ok) {
        const detail = (await response.text()).slice(0, 500);
        throw new Error(`HTTP ${response.status}: ${detail}`);
    }
    return response.json();
}

async function main() {
    const eventPath = requiredString(process.argv[2] ?? process.env.GITHUB_EVENT_PATH, "pull request JSON path");
    const repository = requiredString(process.env.GITHUB_REPOSITORY, "GITHUB_REPOSITORY");
    const token = requiredString(process.env.GH_TOKEN ?? process.env.GITHUB_TOKEN, "GH_TOKEN");
    const apiUrl = requiredString(process.env.GITHUB_API_URL ?? "https://api.github.com", "GITHUB_API_URL");
    const event = JSON.parse(await readFile(eventPath, "utf8"));
    const pullRequest = event.pull_request ?? event;
    if (!Number.isInteger(pullRequest?.number)) throw new Error("pull request JSON이 아닙니다.");

    const result = await validatePullRequestIssueLink({
        pullRequest,
        repository,
        loadIssue: (issueNumber) => requestIssue(apiUrl, repository, token, issueNumber),
    });
    for (const warning of result.rejected) {
        console.log(`::warning::무효 Issue 참조: ${warning}`);
    }
    console.log(`PR #${pullRequest.number} linked issues: ${result.issues.map((number) => `#${number}`).join(", ")}`);
}

const isDirectExecution = process.argv[1]
    && resolve(process.argv[1]) === fileURLToPath(import.meta.url);

if (isDirectExecution) {
    try {
        await main();
    } catch (error) {
        console.error(`::error::${error.message}`);
        process.exitCode = 1;
    }
}
