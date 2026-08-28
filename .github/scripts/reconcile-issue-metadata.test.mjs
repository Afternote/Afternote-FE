import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

import {
    ASSIGNEE_BY_MODULE,
    CLASSIFICATION_LABELS,
    GUARD_COMMENT_MARKER,
    LEGACY_ISSUE_MAX,
    inspectIssue,
    readFormSection,
    reconcileIssue,
} from "./reconcile-issue-metadata.mjs";

const repository = "Afternote/Afternote-FE";

function formBody(type = "bug", module = "afternote") {
    return [
        "### 작업 유형",
        "",
        `${type} — 선택한 작업 유형`,
        "",
        "### 주 담당 모듈",
        "",
        `${module} — 선택한 주 담당 모듈`,
        "",
        "### 개요",
        "",
        "재현과 완료 조건",
    ].join("\n");
}

function issue(overrides = {}) {
    return {
        number: LEGACY_ISSUE_MAX + 1,
        state: "open",
        body: formBody(),
        labels: [],
        assignees: [],
        ...overrides,
    };
}

function fakeApi(initialIssue, initialComments = []) {
    let currentIssue = structuredClone(initialIssue);
    const comments = structuredClone(initialComments);
    const calls = [];
    const api = async (apiPath, options = {}) => {
        const method = options.method ?? "GET";
        calls.push({ apiPath, method, body: options.body });
        if (apiPath.endsWith(`/issues/${currentIssue.number}/comments?per_page=100&page=1`)) {
            return structuredClone(comments);
        }
        if (apiPath.endsWith(`/issues/${currentIssue.number}/comments`) && method === "POST") {
            comments.push({ body: options.body.body });
            return { id: comments.length, body: options.body.body };
        }
        if (apiPath.endsWith(`/issues/${currentIssue.number}`) && method === "PATCH") {
            currentIssue = { ...currentIssue, ...structuredClone(options.body) };
            if (options.body.labels) {
                currentIssue.labels = options.body.labels.map((name) => ({ name }));
            }
            if (options.body.assignees) {
                currentIssue.assignees = options.body.assignees.map((login) => ({ login }));
            }
            return structuredClone(currentIssue);
        }
        if (apiPath.endsWith(`/issues/${currentIssue.number}`) && method === "GET") {
            return structuredClone(currentIssue);
        }
        throw new Error(`Unexpected API call: ${method} ${apiPath}`);
    };
    return {
        api,
        calls,
        currentIssue: () => structuredClone(currentIssue),
        comments: () => structuredClone(comments),
    };
}

test("reads exact issue-form sections with CRLF input", () => {
    const body = formBody("enhancement", "setting").replaceAll("\n", "\r\n");
    assert.equal(readFormSection(body, "작업 유형"), "enhancement — 선택한 작업 유형");
    assert.equal(readFormSection(body, "주 담당 모듈"), "setting — 선택한 주 담당 모듈");
});

test("maps every primary module to the repository owner table", () => {
    for (const [module, assignee] of Object.entries(ASSIGNEE_BY_MODULE)) {
        const inspection = inspectIssue(issue({ body: formBody("enhancement", module) }));
        assert.equal(inspection.status, "valid", module);
        assert.equal(inspection.expectedAssignee, assignee, module);
        assert.equal(inspection.expectedLabel, "enhancement", module);
    }
});

test("maps every work type to exactly one classification label", () => {
    for (const label of CLASSIFICATION_LABELS) {
        const inspection = inspectIssue(issue({ body: formBody(label, "core") }));
        assert.equal(inspection.status, "valid", label);
        assert.equal(inspection.expectedLabel, label, label);
        assert.deepEqual(inspection.labels, [label], label);
    }
});

test("replaces wrong category labels and assignees while preserving operational labels", () => {
    const inspection = inspectIssue(issue({
        body: formBody("bug", "timeletter"),
        labels: [{ name: "enhancement" }, { name: "P1" }, { name: "documentation" }],
        assignees: [{ login: "1hyok" }, { login: "Sadturtleman" }],
    }));
    assert.equal(inspection.status, "valid");
    assert.equal(inspection.needsUpdate, true);
    assert.deepEqual(inspection.labels, ["P1", "bug"]);
    assert.deepEqual(inspection.assignees, ["koongmai"]);
});

test("skips legacy issues and rejects new issues without structured metadata", () => {
    assert.deepEqual(
        inspectIssue(issue({ number: LEGACY_ISSUE_MAX, body: null })),
        { status: "skipped", reason: "legacy" },
    );
    assert.deepEqual(
        inspectIssue(issue({ body: "본문만 있음" })),
        {
            status: "invalid",
            reasons: [
                "`작업 유형`이 없거나 허용된 값이 아닙니다.",
                "`주 담당 모듈`이 없거나 허용된 값이 아닙니다.",
            ],
        },
    );
});

test("corrects metadata and verifies the persisted postcondition", async () => {
    const original = issue({
        body: formBody("documentation", "mindrecord"),
        labels: [{ name: "bug" }, { name: "P0" }],
        assignees: [{ login: "koongmai" }],
    });
    const fake = fakeApi(original);
    const result = await reconcileIssue(fake.api, repository, original);

    assert.deepEqual(result, {
        number: original.number,
        action: "corrected",
        label: "documentation",
        assignee: "Sadturtleman",
    });
    assert.deepEqual(fake.currentIssue().labels, [{ name: "P0" }, { name: "documentation" }]);
    assert.deepEqual(fake.currentIssue().assignees, [{ login: "Sadturtleman" }]);
    assert.equal(fake.calls.filter((call) => call.method === "PATCH").length, 1);
});

test("invalid issues receive one guard comment and remain closed on repeated reconciliation", async () => {
    const original = issue({ body: "필수 필드 없음" });
    const fake = fakeApi(original);

    await reconcileIssue(fake.api, repository, original);
    await reconcileIssue(fake.api, repository, fake.currentIssue());

    assert.equal(fake.currentIssue().state, "closed");
    assert.equal(fake.currentIssue().state_reason, "not_planned");
    assert.equal(fake.comments().length, 1);
    assert.match(fake.comments()[0].body, new RegExp(GUARD_COMMENT_MARKER));
    assert.match(fake.comments()[0].body, /issues\/new\/choose/);
});

test("issue form and workflow preserve strict metadata enforcement boundaries", async () => {
    const form = await readFile(new URL("../ISSUE_TEMPLATE/issue.yml", import.meta.url), "utf8");
    const config = await readFile(new URL("../ISSUE_TEMPLATE/config.yml", import.meta.url), "utf8");
    const workflow = await readFile(new URL("../workflows/issue-metadata-guard.yml", import.meta.url), "utf8");

    assert.match(config, /^blank_issues_enabled: false$/m);
    assert.match(form, /label: 작업 유형[\s\S]*validations:\n\s+required: true/);
    assert.match(form, /label: 주 담당 모듈[\s\S]*validations:\n\s+required: true/);
    for (const type of CLASSIFICATION_LABELS) {
        assert.match(form, new RegExp(`- ${type} —`));
    }
    for (const module of Object.keys(ASSIGNEE_BY_MODULE)) {
        assert.match(form, new RegExp(`- ${module} —`));
    }
    assert.match(
        workflow,
        /^\s{4}types: \[opened, reopened, edited, assigned, unassigned, labeled, unlabeled\]$/m,
    );
    assert.match(workflow, /^\s{2}schedule:\n\s{4}- cron:/m);
    assert.match(workflow, /^permissions: \{\}$/m);
    assert.match(workflow, /^\s{6}issues: write$/m);
    assert.match(workflow, /actions\/checkout@[0-9a-f]{40}/);
    assert.match(workflow, /ref: \$\{\{ github\.event\.repository\.default_branch \}\}/);
    assert.match(workflow, /persist-credentials: false/);
});
