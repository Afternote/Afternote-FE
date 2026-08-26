import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";
import {
    extractSameRepositoryClosingIssueNumbers,
    validatePullRequestIssueLink,
} from "./validate-pr-issue-link.mjs";

const workflowUrl = new URL("../workflows/repository-quality.yml", import.meta.url);
const callerWorkflowUrl = new URL("../workflows/pr-validation.yml", import.meta.url);
const templateUrl = new URL("../PULL_REQUEST_TEMPLATE.md", import.meta.url);

function pullRequest({ title = "change", body = "", number = 7 } = {}) {
    return { number, title, body };
}

function issueLoader(items) {
    return async (number) => {
        if (!items.has(number)) {
            throw new Error("not found");
        }
        return items.get(number);
    };
}

test("extracts local and fully qualified closing issue references without duplicates", () => {
    assert.deepEqual(
        extractSameRepositoryClosingIssueNumbers(
            "Closes #12, fixes: Afternote/Afternote-FE#13 and resolves https://github.com/Afternote/Afternote-FE/issues/12",
            "Afternote/Afternote-FE",
        ),
        [12, 13],
    );
});

test("ignores another repository and the empty PR template", () => {
    assert.deepEqual(
        extractSameRepositoryClosingIssueNumbers(
            "Closes outside/repository#1\n- Closes #",
            "Afternote/Afternote-FE",
        ),
        [],
    );
});

test("ignores closing references in comments and code examples", () => {
    assert.deepEqual(
        extractSameRepositoryClosingIssueNumbers(
            "<!-- Closes #1 -->\n`Fixes #2`\n```md\nResolves #3\n```",
            "Afternote/Afternote-FE",
        ),
        [],
    );
});

test("accepts a same-repository open Issue", async () => {
    const result = await validatePullRequestIssueLink({
        pullRequest: pullRequest({ body: "Closes #1228" }),
        repository: "Afternote/Afternote-FE",
        loadIssue: issueLoader(new Map([[1228, { number: 1228, state: "open" }]])),
    });
    assert.deepEqual(result, { openIssues: [1228], rejected: [] });
});

test("rejects a PR number used as a closing Issue", async () => {
    await assert.rejects(
        validatePullRequestIssueLink({
            pullRequest: pullRequest({ body: "Closes #1227" }),
            repository: "Afternote/Afternote-FE",
            loadIssue: issueLoader(new Map([[1227, { number: 1227, state: "closed", pull_request: {} }]])),
        }),
        /Issue가 아니라 PR/,
    );
});

test("rejects missing and closed Issues", async () => {
    await assert.rejects(
        validatePullRequestIssueLink({
            pullRequest: pullRequest({ body: "Closes #1 and fixes #2" }),
            repository: "Afternote/Afternote-FE",
            loadIssue: issueLoader(new Map([[2, { number: 2, state: "closed" }]])),
        }),
        /조회 실패.*#2: closed/,
    );
});

test("requires a closing reference rather than an incidental issue mention", async () => {
    await assert.rejects(
        validatePullRequestIssueLink({
            pullRequest: pullRequest({ body: "Related to #1228" }),
            repository: "Afternote/Afternote-FE",
            loadIssue: issueLoader(new Map([[1228, { number: 1228, state: "open" }]])),
        }),
        /closing issue가 없습니다/,
    );
});

test("requires the closing reference in the PR body rather than the title", async () => {
    await assert.rejects(
        validatePullRequestIssueLink({
            pullRequest: pullRequest({ title: "Closes #1228" }),
            repository: "Afternote/Afternote-FE",
            loadIssue: issueLoader(new Map([[1228, { number: 1228, state: "open" }]])),
        }),
        /closing issue가 없습니다/,
    );
});

test("required Repository Quality check runs the issue guard on pull requests", async () => {
    const [workflow, callerWorkflow] = await Promise.all([
        readFile(workflowUrl, "utf8"),
        readFile(callerWorkflowUrl, "utf8"),
    ]);
    assert.match(workflow, /^permissions:\n(?:  .+\n)*  issues: read$/m);
    assert.match(callerWorkflow, /^permissions:\n(?:  .+\n)*  issues: read$/m);
    assert.match(workflow, /- name: Require linked open issue\n\s+if: github\.event_name == 'pull_request'/);
    assert.match(workflow, /GH_TOKEN: \$\{\{ github\.token \}\}/);
    assert.match(workflow, /run: node \.github\/scripts\/validate-pr-issue-link\.mjs/);
});

test("the PR template tells authors to create and close an actual Issue", async () => {
    const template = await readFile(templateUrl, "utf8");
    assert.match(template, /PR 생성 전에 이슈를 먼저 만들고/);
    assert.match(template, /^- Closes #$/m);
    assert.doesNotMatch(template, /^- closed #$/m);
});
