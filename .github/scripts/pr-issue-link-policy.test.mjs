import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";
import {
    extractClosingIssueNumbers,
    extractSameRepositoryIssueNumbers,
    extractTitleIssueNumber,
    validatePullRequestIssueLink,
} from "./validate-pr-issue-link.mjs";

const workflowUrl = new URL("../workflows/repository-quality.yml", import.meta.url);
const callerWorkflowUrl = new URL("../workflows/pr-validation.yml", import.meta.url);
const templateUrl = new URL("../PULL_REQUEST_TEMPLATE.md", import.meta.url);

function pullRequest({
    title = "change",
    body = "",
    number = 7,
    user = { login: "author", type: "User" },
    labels = [],
} = {}) {
    return { number, title, body, user, labels };
}

function assignedIssue(number, overrides = {}) {
    return { number, state: "open", assignees: [{ login: "author" }], ...overrides };
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

test("extracts only the references GitHub auto-closes on merge", () => {
    // #1748 — 대표 Issue 판정은 GitHub 이 실제로 닫는 형태만 센다. 콜론이 끼거나 Refs·Part of 는 제외.
    assert.deepEqual(
        extractClosingIssueNumbers(
            "Refs #12, Part of #13, closes #14, Fixed: #15, resolves Afternote/Afternote-FE#16, fixes https://github.com/Afternote/Afternote-FE/issues/17, Closed outside/repository#18",
            "Afternote/Afternote-FE",
        ),
        [14, 16, 17],
    );
});

test("ignores another repository and the empty PR template", () => {
    assert.deepEqual(
        extractSameRepositoryIssueNumbers(
            "Refs outside/repository#1\n- Closes #",
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
    assert.deepEqual(
        extractClosingIssueNumbers(
            "<!-- Closes #1 -->\n`Fixes #2`\n```md\nCloses #3\n```",
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

test("accepts a same-repository open Issue closed by the PR", async () => {
    const result = await validatePullRequestIssueLink({
        pullRequest: pullRequest({ title: "change (#1228)", body: "Closes #1228" }),
        repository: "Afternote/Afternote-FE",
        loadIssue: issueLoader(new Map([[1228, assignedIssue(1228)]])),
    });
    assert.deepEqual(result, { issues: [1228], rejected: [] });
});

test("rejects a representative Issue that is only referenced, not closed", async () => {
    // #1748 — 대표 Issue 를 Refs·Part of 로 걸면 블로커가 열린 Issue 를 merge-order-guard 밖에서 머지하거나
    // Issue 의 일부만 하고 남는 몫을 산문에 남기는 통로가 된다. 일부만 하면 그 몫을 새 Issue 로 분리한다.
    const loadIssue = issueLoader(new Map([[601, assignedIssue(601)]]));
    for (const body of ["Part of #601", "Refs #601", "Closes: #601"]) {
        await assert.rejects(
            validatePullRequestIssueLink({
                pullRequest: pullRequest({ title: "partial change (#601)", body }),
                repository: "Afternote/Afternote-FE",
                loadIssue,
            }),
            /Refs 로는 대표 Issue를 걸 수 없습니다.*새 Issue로 분리해 대표 Issue로 삼/,
            body,
        );
    }
});

test("accepts every GitHub closing keyword for the representative Issue regardless of Issue state", async () => {
    const loadIssue = issueLoader(new Map([[601, assignedIssue(601, { state: "closed" })]]));
    for (const body of ["Closes #601", "fixes #601", "Resolved #601", "Fix Afternote/Afternote-FE#601"]) {
        const result = await validatePullRequestIssueLink({
            pullRequest: pullRequest({ title: "change (#601)", body }),
            repository: "Afternote/Afternote-FE",
            loadIssue,
        });
        assert.deepEqual(result, { issues: [601], rejected: [] }, body);
    }
});

test("rejects a PR number used as an Issue reference", async () => {
    await assert.rejects(
        validatePullRequestIssueLink({
            pullRequest: pullRequest({ title: "change (#1227)", body: "Closes #1227" }),
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
        loadIssue: issueLoader(new Map([[2, assignedIssue(2, { state: "closed" })]])),
    });

    assert.deepEqual(result, {
        issues: [2],
        rejected: ["#1: 조회 실패 (not found)"],
    });
});

test("rejects an author who is not an assignee of the representative Issue", async () => {
    await assert.rejects(
        validatePullRequestIssueLink({
            pullRequest: pullRequest({ title: "change (#1228)", body: "Closes #1228" }),
            repository: "Afternote/Afternote-FE",
            loadIssue: issueLoader(new Map([
                [1228, assignedIssue(1228, { assignees: [{ login: "someone-else" }] })],
            ])),
        }),
        /작성자 @author는 대표 Issue #1228의 담당자\(@someone-else\)가 아닙니다/,
    );
});

test("rejects an unassigned representative Issue", async () => {
    await assert.rejects(
        validatePullRequestIssueLink({
            pullRequest: pullRequest({ title: "change (#1228)", body: "Closes #1228" }),
            repository: "Afternote/Afternote-FE",
            loadIssue: issueLoader(new Map([
                [1228, assignedIssue(1228, { assignees: [] })],
            ])),
        }),
        /대표 Issue #1228에 담당자가 없습니다.*@author를 어사인/,
    );
});

test("matches the assignee login case-insensitively among multiple assignees", async () => {
    const result = await validatePullRequestIssueLink({
        pullRequest: pullRequest({
            title: "change (#1228)",
            body: "Closes #1228",
            user: { login: "Author", type: "User" },
        }),
        repository: "Afternote/Afternote-FE",
        loadIssue: issueLoader(new Map([
            [1228, assignedIssue(1228, { assignees: [{ login: "someone-else" }, "author"] })],
        ])),
    });
    assert.deepEqual(result, { issues: [1228], rejected: [] });
});

test("keeps Refs for Issues the PR touches but does not close, and checks the assignee only on the representative Issue", async () => {
    const result = await validatePullRequestIssueLink({
        pullRequest: pullRequest({ title: "change (#1228)", body: "Closes #1228\nRefs #1229" }),
        repository: "Afternote/Afternote-FE",
        loadIssue: issueLoader(new Map([
            [1228, assignedIssue(1228)],
            [1229, assignedIssue(1229, { assignees: [{ login: "someone-else" }] })],
        ])),
    });
    assert.deepEqual(result, { issues: [1228, 1229], rejected: [] });
});

test("exempts bot authors from the assignee check and the closing keyword but keeps the Issue link requirement", async () => {
    // 봇 PR 은 사람이 나중에 링크를 붙이는 구조라 대표 Issue 를 Refs 로 거는 현행을 유지한다 (#1748).
    const loadIssue = issueLoader(new Map([[12, assignedIssue(12, { assignees: [] })]]));
    const result = await validatePullRequestIssueLink({
        pullRequest: pullRequest({
            title: "chore(deps): bump upload-artifact (#12)",
            body: "Refs #12",
            user: { login: "dependabot[bot]", type: "Bot" },
        }),
        repository: "Afternote/Afternote-FE",
        loadIssue,
    });
    assert.deepEqual(result, { issues: [12], rejected: [] });

    await assert.rejects(
        validatePullRequestIssueLink({
            pullRequest: pullRequest({
                title: "chore(deps): bump upload-artifact",
                body: "",
                user: { login: "dependabot[bot]", type: "Bot" },
            }),
            repository: "Afternote/Afternote-FE",
            loadIssue,
        }),
        /제목은 대표 Issue 번호 하나로 끝나야 합니다/,
    );
});

test("the issue-assignee-exempt label skips the assignee check but keeps the Issue link and closing requirements", async () => {
    const labels = [{ name: "issue-assignee-exempt" }];
    const loadIssue = issueLoader(new Map([
        [1228, assignedIssue(1228, { assignees: [{ login: "someone-else" }] })],
    ]));
    const result = await validatePullRequestIssueLink({
        pullRequest: pullRequest({ title: "change (#1228)", body: "Closes #1228", labels }),
        repository: "Afternote/Afternote-FE",
        loadIssue,
    });
    assert.deepEqual(result, { issues: [1228], rejected: [] });

    const unassigned = await validatePullRequestIssueLink({
        pullRequest: pullRequest({ title: "change (#12)", body: "Closes #12", labels }),
        repository: "Afternote/Afternote-FE",
        loadIssue: issueLoader(new Map([[12, assignedIssue(12, { assignees: [] })]])),
    });
    assert.deepEqual(unassigned, { issues: [12], rejected: [] });

    await assert.rejects(
        validatePullRequestIssueLink({
            pullRequest: pullRequest({ title: "change (#1228)", body: "Refs #1228", labels }),
            repository: "Afternote/Afternote-FE",
            loadIssue,
        }),
        /Refs 로는 대표 Issue를 걸 수 없습니다/,
    );

    await assert.rejects(
        validatePullRequestIssueLink({
            pullRequest: pullRequest({ title: "change", body: "", labels }),
            repository: "Afternote/Afternote-FE",
            loadIssue,
        }),
        /제목은 대표 Issue 번호 하나로 끝나야 합니다/,
    );
});

test("unrelated labels keep the assignee rejection and point at the exempt label", async () => {
    await assert.rejects(
        validatePullRequestIssueLink({
            pullRequest: pullRequest({
                title: "change (#1228)",
                body: "Closes #1228",
                labels: [{ name: "documentation" }, "bug"],
            }),
            repository: "Afternote/Afternote-FE",
            loadIssue: issueLoader(new Map([
                [1228, assignedIssue(1228, { assignees: [{ login: "someone-else" }] })],
            ])),
        }),
        /담당자\(@someone-else\)가 아닙니다.*`issue-assignee-exempt` 라벨/,
    );
});

test("requires the pull request author login", async () => {
    await assert.rejects(
        validatePullRequestIssueLink({
            pullRequest: pullRequest({ title: "change (#1228)", body: "Closes #1228", user: null }),
            repository: "Afternote/Afternote-FE",
            loadIssue: issueLoader(new Map([[1228, assignedIssue(1228)]])),
        }),
        /pull_request\.user\.login 값이 없습니다/,
    );
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
            pullRequest: pullRequest({ title: "change", body: "Closes #1228" }),
            repository: "Afternote/Afternote-FE",
            loadIssue: issueLoader(new Map([[1228, { number: 1228, state: "open" }]])),
        }),
        /제목은 대표 Issue 번호 하나로 끝나야 합니다/,
    );
});

test("requires the title Issue to be linked in the PR body", async () => {
    await assert.rejects(
        validatePullRequestIssueLink({
            pullRequest: pullRequest({ title: "change (#1229)", body: "Closes #1228" }),
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
                body: "Closes #1227\nRefs #1228",
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

test("the PR template tells authors to close the representative Issue and to split partial work into a new Issue", async () => {
    const template = await readFile(templateUrl, "utf8");
    assert.match(template, /PR 제목 끝에 대표 Issue 하나를 `\(#N\)` 형식/);
    assert.match(template, /대표 Issue.*Closes/);
    assert.match(template, /Refs 로는 걸 수 없/);
    assert.match(template, /일부만.*새 Issue.*분리/);
    assert.match(template, /blocked_by.*merge-order-guard/);
    assert.match(template, /대표 Issue에는 PR 작성자가 담당자로 지정/);
    assert.match(template, /^- Closes #$/m);
    assert.doesNotMatch(template, /^- Refs #$/m);
    assert.doesNotMatch(template, /여러 PR이 같은 Issue를 공유/);
    assert.doesNotMatch(template, /최종 완료하는 PR에서만/);
});
