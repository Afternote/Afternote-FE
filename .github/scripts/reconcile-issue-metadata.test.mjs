import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

import {
    AREA_LABEL_BY_MODULE,
    ASSIGNEE_BY_MODULE,
    GUARD_COMMENT_MARKER,
    HANDOVER_BY_MODULE,
    LEGACY_ISSUE_MAX,
    PRIORITY_COMMENT_MARKER,
    PRIORITY_FIELD_NAME,
    PRIORITY_OPTION_GUIDE,
    TYPE_LABELS,
    assigneeForIssue,
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

// 이관 경계 위의 번호. 지도는 앞으로의 담당을 말하므로 경계를 넘긴 이슈로 대조한다.
const AFTER_HANDOVER = Math.max(
    LEGACY_ISSUE_MAX,
    ...Object.values(HANDOVER_BY_MODULE).map((handover) => handover.fromIssue),
) + 1;

test("maps every primary module to the repository owner table", () => {
    for (const [module, assignee] of Object.entries(ASSIGNEE_BY_MODULE)) {
        const inspection = inspectIssue(issue({
            number: AFTER_HANDOVER,
            body: formBody("enhancement", module),
        }));
        assert.equal(inspection.status, "valid", module);
        assert.equal(inspection.expectedAssignee, assignee, module);
        assert.equal(inspection.expectedLabel, "enhancement", module);
        assert.equal(inspection.expectedAreaLabel, AREA_LABEL_BY_MODULE[module], module);
    }
});

test("a module handover leaves issues opened before the decision with the previous assignee", () => {
    // #1910: 온보딩은 이 결정 전에 열린 이슈를 옮기지 않는다. 옮기면 그 이슈로 열어 둔 PR 이
    // validate-pr-issue-link 에서 작성자와 담당자가 다르다는 이유로 빨개진다.
    for (const [module, handover] of Object.entries(HANDOVER_BY_MODULE)) {
        assert.ok(ASSIGNEE_BY_MODULE[module], `${module} 이 지도에 없다`);
        assert.notEqual(handover.before, ASSIGNEE_BY_MODULE[module], module);
        assert.ok(handover.fromIssue > LEGACY_ISSUE_MAX, `${module} 경계가 legacy 스킵 안에 있다`);

        assert.equal(
            assigneeForIssue(module, handover.fromIssue - 1),
            handover.before,
            module,
        );
        assert.equal(
            assigneeForIssue(module, handover.fromIssue),
            ASSIGNEE_BY_MODULE[module],
            module,
        );
        // 경계는 이슈 하나가 아니라 판정 전체를 통과해야 한다.
        assert.equal(
            inspectIssue(issue({
                number: handover.fromIssue - 1,
                body: formBody("enhancement", module),
            })).expectedAssignee,
            handover.before,
            module,
        );
    }
});

test("a module without a handover entry moves its open issues right away", () => {
    // 설정은 경계를 두지 않는다. 이미 열려 있는 이슈도 다음 리컨사일에서 새 담당자로 옮겨진다.
    assert.equal(HANDOVER_BY_MODULE.setting, undefined);
    assert.equal(assigneeForIssue("setting", LEGACY_ISSUE_MAX + 1), ASSIGNEE_BY_MODULE.setting);
    const inspection = inspectIssue(issue({
        number: LEGACY_ISSUE_MAX + 1,
        body: formBody("enhancement", "setting"),
        assignees: [{ login: "koongmai" }],
    }));
    assert.equal(inspection.needsUpdate, true);
    assert.deepEqual(inspection.assignees, [ASSIGNEE_BY_MODULE.setting]);
});

test("an unknown issue number fails the handover judgement instead of guessing", () => {
    // 경계 판정을 못 하면 어느 쪽으로도 접지 않는다. 접으면 옛 이슈가 조용히 새 담당자에게 간다.
    const module = Object.keys(HANDOVER_BY_MODULE)[0];
    assert.throws(() => assigneeForIssue(module, undefined), /이슈 번호/);
    assert.throws(() => assigneeForIssue(module, "1910번"), /이슈 번호/);
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

test("priorityFieldState distinguishes unknown, set, and missing", () => {
    // 등록 시각은 더 이상 판정에 들어가지 않는다 — 유예가 끝나도 달라지는 동작이 없다 (#1534).
    assert.equal(priorityFieldState(issue()).status, "unknown");
    assert.equal(priorityFieldState(issue({
        issue_field_values: [priorityValue("Urgent")],
    })).status, "set");
    assert.equal(priorityFieldState(issue({
        issue_field_values: [{ issue_field_name: "Effort", single_select_option: { name: "High" } }],
    })).status, "missing");
    assert.equal(priorityFieldState(issue({ issue_field_values: [] })).status, "missing");
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

test("an aged bug issue missing priority still stays open", async () => {
    // Priority 는 이슈의 내용이 아니라 분류 메타데이터다. 비었다고 결함 보고를 닫으면 할 일은
    // 그대로인데 열린 이슈 목록에서만 사라진다 (#1534). Issue Form 이 조직 필드를 자동으로
    // 채우지 못하므로, 닫으면 정상 등록된 이슈가 조용히 사라지는 경로가 된다.
    const original = issue({
        issue_field_values: [],
        created_at: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString(),
    });
    const fake = fakeApi(original);

    const result = await reconcileIssue(fake.api, repository, original);
    await reconcileIssue(fake.api, repository, fake.currentIssue());

    assert.equal(result.priority, "reminded");
    assert.notEqual(result.action, "closed-priority-missing");
    assert.equal(fake.currentIssue().state, "open");
    assert.equal(fake.comments().length, 1, "안내는 이슈당 한 번만 단다");
});

test("the priority reminder does not threaten an automatic close", async () => {
    // 닫지 않게 된 뒤에도 예고 문구가 남으면 코멘트가 거짓말을 한다.
    const original = issue({ issue_field_values: [] });
    const fake = fakeApi(original);

    await reconcileIssue(fake.api, repository, original);

    assert.doesNotMatch(fake.comments()[0].body, /자동으로 닫습니다|다시 열어 주세요/);
});

test("issues invalid for metadata are still closed", async () => {
    // 이번 변경은 Priority 경로 하나다. 작업 유형·주 담당 모듈을 판정할 수 없는 이슈는 담당자도
    // 라벨도 정할 수 없어 성격이 다르므로 닫는 경로를 그대로 둔다.
    const original = issue({ body: "구조화되지 않은 본문" });
    const fake = fakeApi(original);

    const result = await reconcileIssue(fake.api, repository, original);

    assert.equal(result.action, "closed-invalid");
    assert.equal(fake.currentIssue().state, "closed");
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
