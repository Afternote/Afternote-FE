import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

import {
    AREA_LABEL_BY_MODULE,
    ASSIGNEE_BY_MODULE,
    GUARD_COMMENT_MARKER,
    LEGACY_ISSUE_MAX,
    PRIORITY_COMMENT_MARKER,
    PRIORITY_FIELD_NAME,
    PRIORITY_GRACE_MS,
    PRIORITY_OPTION_GUIDE,
    TYPE_LABELS,
    inspectIssue,
    priorityFieldState,
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
        assert.equal(inspection.expectedAreaLabel, AREA_LABEL_BY_MODULE[module], module);
    }
});

test("maps every work type to exactly one classification label", () => {
    for (const label of TYPE_LABELS) {
        const inspection = inspectIssue(issue({ body: formBody(label, "core") }));
        assert.equal(inspection.status, "valid", label);
        assert.equal(inspection.expectedLabel, label, label);
        assert.deepEqual(inspection.labels, [label, "area:core"], label);
    }
});

test("replaces wrong type, area, legacy internal labels and assignees while preserving operational labels", () => {
    const inspection = inspectIssue(issue({
        body: formBody("bug", "timeletter"),
        labels: [
            { name: "enhancement" },
            { name: "P1" },
            { name: "documentation" },
            { name: "internal" },
            { name: "area:core" },
            { name: "area:home" },
        ],
        assignees: [{ login: "1hyok" }, { login: "Sadturtleman" }],
    }));
    assert.equal(inspection.status, "valid");
    assert.equal(inspection.needsUpdate, true);
    assert.deepEqual(inspection.labels, ["P1", "bug", "area:timeletter"]);
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
        areaLabel: "area:mindrecord",
        assignee: "Sadturtleman",
    });
    assert.deepEqual(
        fake.currentIssue().labels,
        [{ name: "P0" }, { name: "documentation" }, { name: "area:mindrecord" }],
    );
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
    for (const type of TYPE_LABELS) {
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

function priorityValue(option = "High") {
    return {
        data_type: "single_select",
        issue_field_name: PRIORITY_FIELD_NAME,
        single_select_option: { name: option },
    };
}

test("priorityFieldState distinguishes unknown, set, missing, and overdue", () => {
    const now = Date.now();
    assert.equal(priorityFieldState(issue()).status, "unknown");
    assert.equal(priorityFieldState(issue({
        issue_field_values: [priorityValue("Urgent")],
    })).status, "set");
    assert.equal(priorityFieldState(issue({
        issue_field_values: [{ issue_field_name: "Effort", single_select_option: { name: "High" } }],
        created_at: new Date(now).toISOString(),
    }), now).status, "missing");
    assert.equal(priorityFieldState(issue({
        issue_field_values: [],
        created_at: new Date(now - PRIORITY_GRACE_MS - 60_000).toISOString(),
    }), now).status, "missing-overdue");
    assert.equal(priorityFieldState(issue({ issue_field_values: [] }), now).status, "missing");
});

test("fresh bug issue missing priority gets exactly one reminder and stays open", async () => {
    const original = issue({
        issue_field_values: [],
        created_at: new Date().toISOString(),
    });
    const fake = fakeApi(original);

    const result = await reconcileIssue(fake.api, repository, original);
    await reconcileIssue(fake.api, repository, fake.currentIssue());

    assert.equal(result.action, "corrected");
    assert.equal(result.priority, "reminded");
    assert.equal(fake.currentIssue().state, "open");
    assert.equal(fake.comments().length, 1);
    assert.match(fake.comments()[0].body, new RegExp(PRIORITY_COMMENT_MARKER));
    for (const line of PRIORITY_OPTION_GUIDE) {
        assert.ok(fake.comments()[0].body.includes(`- ${line}`), line);
    }
});

test("bug issue past the grace period is closed as not planned", async () => {
    const original = issue({
        issue_field_values: [],
        created_at: new Date(Date.now() - PRIORITY_GRACE_MS - 60_000).toISOString(),
    });
    const fake = fakeApi(original);

    const result = await reconcileIssue(fake.api, repository, original);

    assert.equal(result.action, "closed-priority-missing");
    assert.equal(result.priority, "closed");
    assert.equal(fake.currentIssue().state, "closed");
    assert.equal(fake.currentIssue().state_reason, "not_planned");
    assert.equal(fake.comments().length, 1);
});

test("bug issue with priority set and non-bug issues pass without comments", async () => {
    const withPriority = issue({ issue_field_values: [priorityValue()] });
    const fakeBug = fakeApi(withPriority);
    const bugResult = await reconcileIssue(fakeBug.api, repository, withPriority);
    assert.equal(bugResult.priority, "set");
    assert.equal(fakeBug.comments().length, 0);

    const nonBug = issue({ body: formBody("refactor", "core") });
    const fakeRefactor = fakeApi(nonBug);
    const refactorResult = await reconcileIssue(fakeRefactor.api, repository, nonBug);
    assert.equal("priority" in refactorResult, false);
    assert.equal(fakeRefactor.comments().length, 0);
});

test("event payload without field values falls back to a fresh fetch", async () => {
    const stored = issue({ issue_field_values: [priorityValue()] });
    const fake = fakeApi(stored);
    const eventPayload = structuredClone(stored);
    delete eventPayload.issue_field_values;

    const result = await reconcileIssue(fake.api, repository, eventPayload);

    assert.equal(result.priority, "set");
    assert.equal(fake.comments().length, 0);
});

test("missing field support in every response fails loudly", async () => {
    const original = issue();
    const fake = fakeApi(original);
    await assert.rejects(
        () => reconcileIssue(fake.api, repository, original),
        /issue_field_values/,
    );
});
