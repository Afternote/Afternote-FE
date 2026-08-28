import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";
import {
    extractSameRepositoryIssueNumbers,
    extractTitleIssueNumber,
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

test("extracts closing and non-closing Issue references without duplicates", () => {
    assert.deepEqual(
        extractSameRepositoryIssueNumbers(
            "Refs #12, Part of: Afternote/Afternote-FE#13, Related to #14, and closes https://github.com/Afternote/Afternote-FE/issues/12",
            "Afternote/Afternote-FE",
        ),
        [12, 13, 14],
    );
});

test("ignores another repository and the empty PR template", () => {
    assert.deepEqual(
        extractSameRepositoryIssueNumbers(
            "Refs outside/repository#1\n- Refs #",
            "Afternote/Afternote-FE",
        ),
        [],
    );
});

test("ignores Issue references in comments and code examples", () => {
    assert.deepEqual(
        extractSameRepositoryIssueNumbers(
            "<!-- Refs #1 -->\n`Part of #2`\n```md\nCloses #3\n```",
            "Afternote/Afternote-FE",
        ),
        [],
    );
});

test("extracts exactly one representative Issue from the end of the PR title", () => {
    assert.equal(extractTitleIssueNumber("fix(home): correct navigation (#1228)"), 1228);
    assert.equal(extractTitleIssueNumber("fix(home): correct (#1227) and document (#1228)"), null);
    assert.equal(extractTitleIssueNumber("fix(home): correct navigation #1228"), null);
});

test("accepts a same-repository open Issue", async () => {
    const result = await validatePullRequestIssueLink({
        pullRequest: pullRequest({ title: "change (#1228)", body: "Refs #1228" }),
        repository: "Afternote/Afternote-FE",
        loadIssue: issueLoader(new Map([[1228, { number: 1228, state: "open" }]])),
    });
    assert.deepEqual(result, { issues: [1228], rejected: [] });
});

test("allows multiple pull requests to share one Issue regardless of closing behavior or state", async () => {
    const loadIssue = issueLoader(new Map([[601, { number: 601, state: "closed" }]]));
    const results = await Promise.all([
        validatePullRequestIssueLink({
            pullRequest: pullRequest({ number: 7, title: "first change (#601)", body: "Part of #601" }),
            repository: "Afternote/Afternote-FE",
            loadIssue,
        }),
        validatePullRequestIssueLink({
            pullRequest: pullRequest({ number: 8, title: "second change (#601)", body: "Refs #601" }),
            repository: "Afternote/Afternote-FE",
            loadIssue,
        }),
        validatePullRequestIssueLink({
            pullRequest: pullRequest({ number: 9, title: "last change (#601)", body: "Closes #601" }),
            repository: "Afternote/Afternote-FE",
            loadIssue,
        }),
    ]);

    assert.deepEqual(results, [
        { issues: [601], rejected: [] },
        { issues: [601], rejected: [] },
        { issues: [601], rejected: [] },
    ]);
});

test("rejects a PR number used as an Issue reference", async () => {
    await assert.rejects(
        validatePullRequestIssueLink({
            pullRequest: pullRequest({ title: "change (#1227)", body: "Refs #1227" }),
            repository: "Afternote/Afternote-FE",
            loadIssue: issueLoader(new Map([[1227, { number: 1227, state: "closed", pull_request: {} }]])),
        }),
        /Issue가 아니라 PR/,
    );
});

test("accepts a closed Issue while warning about a missing reference", async () => {
    const result = await validatePullRequestIssueLink({
        pullRequest: pullRequest({ title: "change (#2)", body: "Closes #1 and fixes #2" }),
        repository: "Afternote/Afternote-FE",
        loadIssue: issueLoader(new Map([[2, { number: 2, state: "closed" }]])),
    });

    assert.deepEqual(result, {
        issues: [2],
        rejected: ["#1: 조회 실패 (not found)"],
    });
});

test("rejects a missing Issue", async () => {
    await assert.rejects(
        validatePullRequestIssueLink({
            pullRequest: pullRequest({ title: "change (#1)", body: "Closes #1" }),
            repository: "Afternote/Afternote-FE",
            loadIssue: issueLoader(new Map()),
        }),
        /연결된 실제 Issue가 없습니다.*조회 실패/,
    );
});

test("requires an explicit Issue reference rather than an incidental number mention", async () => {
    await assert.rejects(
        validatePullRequestIssueLink({
            pullRequest: pullRequest({ title: "change (#1228)", body: "See #1228 for context" }),
            repository: "Afternote/Afternote-FE",
            loadIssue: issueLoader(new Map([[1228, { number: 1228, state: "open" }]])),
        }),
        /Issue 참조가 없습니다/,
    );
});

test("requires the Issue reference in the PR body as well as the title", async () => {
    await assert.rejects(
        validatePullRequestIssueLink({
            pullRequest: pullRequest({ title: "change (#1228)" }),
            repository: "Afternote/Afternote-FE",
            loadIssue: issueLoader(new Map([[1228, { number: 1228, state: "open" }]])),
        }),
        /Issue 참조가 없습니다/,
    );
});

test("requires the representative Issue at the end of the PR title", async () => {
    await assert.rejects(
        validatePullRequestIssueLink({
            pullRequest: pullRequest({ title: "change", body: "Refs #1228" }),
            repository: "Afternote/Afternote-FE",
            loadIssue: issueLoader(new Map([[1228, { number: 1228, state: "open" }]])),
        }),
        /제목은 대표 Issue 번호 하나로 끝나야 합니다/,
    );
});

test("requires the title Issue to be linked in the PR body", async () => {
    await assert.rejects(
        validatePullRequestIssueLink({
            pullRequest: pullRequest({ title: "change (#1229)", body: "Refs #1228" }),
            repository: "Afternote/Afternote-FE",
            loadIssue: issueLoader(new Map([
                [1228, { number: 1228, state: "open" }],
                [1229, { number: 1229, state: "open" }],
            ])),
        }),
        /대표 Issue #1229를 본문에서도/,
    );
});

test("requires the title Issue itself to be an Issue when other body references are valid", async () => {
    await assert.rejects(
        validatePullRequestIssueLink({
            pullRequest: pullRequest({
                title: "change (#1227)",
                body: "Refs #1227\nRefs #1228",
            }),
            repository: "Afternote/Afternote-FE",
            loadIssue: issueLoader(new Map([
                [1227, { number: 1227, state: "closed", pull_request: {} }],
                [1228, { number: 1228, state: "open" }],
            ])),
        }),
        /대표 Issue #1227가 실제 Issue로 확인되지 않았습니다.*Issue가 아니라 PR/,
    );
});

test("required Repository Quality check runs the issue guard on pull requests", async () => {
    const [workflow, callerWorkflow] = await Promise.all([
        readFile(workflowUrl, "utf8"),
        readFile(callerWorkflowUrl, "utf8"),
    ]);
    assert.match(workflow, /^permissions:\n(?:  .+\n)*  issues: read$/m);
    assert.match(callerWorkflow, /^permissions:\n(?:  .+\n)*  issues: read$/m);
    assert.match(workflow, /- name: Require linked Issue\n\s+if: inputs\.pull_request_number > 0/);
    assert.match(workflow, /GH_TOKEN: \$\{\{ github\.token \}\}/);
    assert.match(workflow, /validate-pr-issue-link\.mjs "\$\{\{ steps\.changed-files\.outputs\.pull_request_json \}\}"/);
    assert.match(
        callerWorkflow,
        /^\s{2}pull_request:\n\s{4}types: \[opened, synchronize, reopened, edited\]$/m,
    );
});

test("the PR template tells authors to reuse an Issue across pull requests", async () => {
    const template = await readFile(templateUrl, "utf8");
    assert.match(template, /관련된 기존 Issue를 재사용/);
    assert.match(template, /PR 제목 끝에 대표 Issue 하나를 `\(#N\)` 형식/);
    assert.match(template, /여러 PR이 같은 Issue를 공유/);
    assert.match(template, /최종 완료하는 PR에서만 Closes\/Fixes\/Resolves/);
    assert.match(template, /^- Refs #$/m);
    assert.doesNotMatch(template, /^- Closes #$/m);
    assert.doesNotMatch(template, /^- closed #$/m);
});
