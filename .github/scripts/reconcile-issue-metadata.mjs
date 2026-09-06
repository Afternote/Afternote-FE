#!/usr/bin/env node

import fs from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import { pathToFileURL } from "node:url";

// #1342가 이 정책을 도입하는 추적 이슈다. 그 이전 백로그는 새 필수 필드가 없으므로
// 정기 재검사에서 소급 종료하지 않고, 이후 생성된 이슈부터 fail-closed로 관리한다.
export const LEGACY_ISSUE_MAX = 1342;

export const TYPE_LABELS = Object.freeze([
    "bug",
    "enhancement",
    "documentation",
    "maintenance",
    "refactor",
    "release",
    "security",
    "test",
]);

export const TYPE_LABEL_BY_KEY = Object.freeze({
    bug: "bug",
    enhancement: "enhancement",
    documentation: "documentation",
    maintenance: "maintenance",
    refactor: "refactor",
    release: "release",
    security: "security",
    test: "test",
});

export const AREA_LABEL_BY_MODULE = Object.freeze({
    afternote: "area:afternote",
    onboarding: "area:onboarding",
    core: "area:core",
    receiver: "area:receiver",
    setting: "area:setting",
    timeletter: "area:timeletter",
    mindrecord: "area:mindrecord",
    home: "area:home",
    platform: "area:platform",
});

export const ASSIGNEE_BY_MODULE = Object.freeze({
    afternote: "1hyok",
    onboarding: "Sadturtleman",
    core: "1hyok",
    receiver: "1hyok",
    setting: "1hyok",
    timeletter: "koongmai",
    mindrecord: "Sadturtleman",
    home: "Sadturtleman",
    platform: "1hyok",
});

// #1910: 담당 이관은 모듈마다 소급 범위가 다르다. 설정은 이미 열려 있는 이슈까지 곧바로 옮기고,
// 온보딩은 이 결정 전에 열린 이슈를 옮기지 않는다.
//
// 소급 여부가 왜 선택지인가. issue-metadata-guard 의 스케줄 실행은 열린 이슈 전체를 훑으므로,
// 지도만 바꾸면 진행 중인 이슈까지 전부 새 담당자에게 넘어간다. 그러면 그 이슈로 열어 둔 PR 이
// validate-pr-issue-link 에서 "작성자가 대표 이슈의 담당자가 아니다" 로 빨개지고, 옮겨 받은
// 사람은 남이 절반쯤 짜 둔 브랜치를 떠안는다. 그래서 소급은 기본값이 아니라 결정 사항이다.
//
// fromIssue 미만의 이슈는 before 가 계속 맡는다. 경계는 결정을 적은 이슈 번호이고, 그보다 큰
// 번호는 전부 결정 이후에 열린 이슈다.
export const HANDOVER_BY_MODULE = Object.freeze({
    onboarding: Object.freeze({ before: "1hyok", fromIssue: 1910 }),
});

/** 이관 경계를 반영한 담당자. 경계가 없는 모듈은 지도 그대로다. */
export function assigneeForIssue(moduleKey, issueNumber) {
    const handover = HANDOVER_BY_MODULE[moduleKey];
    if (!handover) {
        return ASSIGNEE_BY_MODULE[moduleKey];
    }
    const number = Number(issueNumber);
    if (!Number.isSafeInteger(number)) {
        // 번호를 모르면 경계를 판정할 수 없다. 어느 쪽으로도 접지 않고 실행을 실패시킨다.
        throw new Error(`이관 경계를 판정할 이슈 번호가 없습니다: ${moduleKey} / ${issueNumber}`);
    }
    return number < handover.fromIssue ? handover.before : ASSIGNEE_BY_MODULE[moduleKey];
}

export const GUARD_COMMENT_MARKER = "<!-- issue-metadata-guard:v1 -->";

// #1407: bug 이슈는 조직 이슈 필드 Priority 로 심각도를 지정해야 한다. Issue Form 은 필드 값을
// 자동 부착하지 못하므로 빈 값을 안내 코멘트로 알린다.
//
// #1534: 비어 있다고 이슈를 닫지는 않는다. Priority 는 이슈의 내용이 아니라 분류 메타데이터고,
// 그것이 비었다는 이유로 실재하는 결함 보고가 닫히면 「열린 이슈 목록 = 지금 할 일」 쪽이 거짓이
// 된다 — 할 일은 그대로인데 목록에서만 사라진다. 판정 불가 메타데이터(작업 유형·주 담당 모듈)로
// 닫는 경로는 그대로다. 그쪽은 담당자도 라벨도 정할 수 없는 상태라 성격이 다르다.
export const PRIORITY_FIELD_NAME = "Priority";
export const PRIORITY_COMMENT_MARKER = "<!-- issue-metadata-guard:priority:v1 -->";

export const PRIORITY_OPTION_GUIDE = Object.freeze([
    "Urgent: 치명 — 크래시·데이터 소실·보안 무력화·핵심 경로 차단·가짜 성공",
    "High: 심각 — 실패·틀린 값의 정상 위장, 핵심 기능 무동작",
    "Medium: 불편 — 우회로가 있는 오동작·틀린 표시, 핵심 흐름은 유지",
    "Low: 경미 — 문구·정렬·마감 품질 흠, 기능 영향 없음",
]);

function unique(values) {
    return [...new Set(values)];
}

function labelNames(issue) {
    return unique((issue.labels ?? [])
        .map((label) => typeof label === "string" ? label : label?.name)
        .filter(Boolean));
}

function assigneeLogins(issue) {
    return unique((issue.assignees ?? [])
        .map((assignee) => typeof assignee === "string" ? assignee : assignee?.login)
        .filter(Boolean));
}

function sameStringSet(left, right) {
    return left.length === right.length && left.every((value) => right.includes(value));
}

export function readFormSection(body, label) {
    const lines = String(body ?? "").replaceAll("\r\n", "\n").split("\n");
    const heading = `### ${label}`;
    const start = lines.findIndex((line) => line.trim() === heading);
    if (start < 0) {
        return null;
    }
    const valueLines = [];
    for (let index = start + 1; index < lines.length; index += 1) {
        if (/^###\s+/.test(lines[index].trim())) {
            break;
        }
        valueLines.push(lines[index]);
    }
    const value = valueLines.join("\n").trim();
    if (!value || /^_?No response_?$/i.test(value)) {
        return null;
    }
    return value;
}

function optionKey(value) {
    return /^([a-z][a-z0-9-]*)\s+—\s+/.exec(value ?? "")?.[1] ?? null;
}

export function expectedMetadata(issue) {
    const typeValue = readFormSection(issue.body, "작업 유형");
    const moduleValue = readFormSection(issue.body, "주 담당 모듈");
    const typeKey = optionKey(typeValue);
    const moduleKey = optionKey(moduleValue);
    const expectedLabel = TYPE_LABEL_BY_KEY[typeKey];
    const expectedAreaLabel = AREA_LABEL_BY_MODULE[moduleKey];
    const mappedAssignee = ASSIGNEE_BY_MODULE[moduleKey];
    const reasons = [];

    if (!expectedLabel) {
        reasons.push("`작업 유형`이 없거나 허용된 값이 아닙니다.");
    }
    if (!mappedAssignee || !expectedAreaLabel) {
        reasons.push("`주 담당 모듈`이 없거나 허용된 값이 아닙니다.");
    }
    if (reasons.length > 0) {
        return { valid: false, reasons };
    }
    return {
        valid: true,
        typeKey,
        moduleKey,
        expectedLabel,
        expectedAreaLabel,
        expectedAssignee: assigneeForIssue(moduleKey, issue.number),
    };
}

export function inspectIssue(issue) {
    if (issue.pull_request) {
        return { status: "skipped", reason: "pull-request" };
    }
    if (!Number.isSafeInteger(Number(issue.number)) || Number(issue.number) <= LEGACY_ISSUE_MAX) {
        return { status: "skipped", reason: "legacy" };
    }

    const expected = expectedMetadata(issue);
    if (!expected.valid) {
        return { status: "invalid", reasons: expected.reasons };
    }

    const currentLabels = labelNames(issue);
    const currentAssignees = assigneeLogins(issue);
    const targetLabels = unique([
        ...currentLabels.filter((label) => !TYPE_LABELS.includes(label) &&
            label !== "internal" && !label.startsWith("area:")),
        expected.expectedLabel,
        expected.expectedAreaLabel,
    ]);
    const targetAssignees = [expected.expectedAssignee];
    const needsUpdate = !sameStringSet(currentLabels, targetLabels) ||
        !sameStringSet(currentAssignees, targetAssignees);

    return {
        status: "valid",
        needsUpdate,
        labels: targetLabels,
        assignees: targetAssignees,
        expectedLabel: expected.expectedLabel,
        expectedAreaLabel: expected.expectedAreaLabel,
        expectedAssignee: expected.expectedAssignee,
        moduleKey: expected.moduleKey,
        typeKey: expected.typeKey,
    };
}

export function priorityFieldState(issue) {
    const values = issue.issue_field_values;
    if (!Array.isArray(values)) {
        return { status: "unknown" };
    }
    const set = values.some((value) => value?.issue_field_name === PRIORITY_FIELD_NAME &&
        value?.single_select_option?.name);
    return { status: set ? "set" : "missing" };
}

export function renderPriorityComment() {
    return [
        PRIORITY_COMMENT_MARKER,
        "bug 이슈는 사이드바 Fields > Priority 로 심각도를 지정해야 합니다. 판정 기준:",
        "",
        ...PRIORITY_OPTION_GUIDE.map((line) => `- ${line}`),
        "",
        "값이 비어 있어도 이슈를 닫지는 않습니다. 이 안내는 이슈당 한 번만 답니다.",
    ].join("\n");
}

export function renderInvalidComment(repository, reasons) {
    return [
        GUARD_COMMENT_MARKER,
        "이 이슈는 필수 메타데이터를 판정할 수 없어 자동으로 닫았습니다.",
        "",
        ...reasons.map((reason) => `- ${reason}`),
        "",
        `[이슈 등록 양식](https://github.com/${repository}/issues/new/choose)에 맞게 본문을 보완한 뒤 다시 열어 주세요.`,
    ].join("\n");
}

async function listIssueComments(api, repository, issueNumber) {
    const comments = [];
    for (let page = 1; ; page += 1) {
        const batch = await api(
            `/repos/${repository}/issues/${issueNumber}/comments?per_page=100&page=${page}`,
        );
        comments.push(...batch);
        if (batch.length < 100) {
            return comments;
        }
    }
}

async function assertReconciled(api, repository, issueNumber, expected) {
    const latest = await api(`/repos/${repository}/issues/${issueNumber}`);
    const inspection = inspectIssue(latest);
    if (inspection.status !== "valid" || inspection.needsUpdate ||
        inspection.expectedLabel !== expected.expectedLabel ||
        inspection.expectedAreaLabel !== expected.expectedAreaLabel ||
        inspection.expectedAssignee !== expected.expectedAssignee) {
        throw new Error(`Issue #${issueNumber} metadata postcondition failed`);
    }
}

async function reconcilePriorityField(api, repository, issue) {
    let state = priorityFieldState(issue);
    if (state.status === "unknown") {
        // 이벤트 페이로드에는 issue_field_values 가 없을 수 있다. 단건 재조회로 보강한다.
        state = priorityFieldState(await api(`/repos/${repository}/issues/${issue.number}`));
    }
    if (state.status === "unknown") {
        throw new Error(`Issue #${issue.number}: API 응답에 issue_field_values 가 없습니다 — ` +
            "토큰의 이슈 필드 지원 여부를 확인해야 합니다");
    }
    if (state.status === "set") {
        return { status: "set" };
    }

    const comments = await listIssueComments(api, repository, issue.number);
    if (!comments.some((comment) => comment.body?.includes(PRIORITY_COMMENT_MARKER))) {
        await api(`/repos/${repository}/issues/${issue.number}/comments`, {
            method: "POST",
            body: { body: renderPriorityComment() },
        });
    }
    return { status: "reminded" };
}

export async function reconcileIssue(api, repository, issue) {
    const inspection = inspectIssue(issue);
    if (inspection.status === "skipped") {
        return { number: issue.number, action: "skipped", reason: inspection.reason };
    }

    if (inspection.status === "invalid") {
        const comments = await listIssueComments(api, repository, issue.number);
        if (!comments.some((comment) => comment.body?.includes(GUARD_COMMENT_MARKER))) {
            await api(`/repos/${repository}/issues/${issue.number}/comments`, {
                method: "POST",
                body: { body: renderInvalidComment(repository, inspection.reasons) },
            });
        }
        if (issue.state !== "closed") {
            await api(`/repos/${repository}/issues/${issue.number}`, {
                method: "PATCH",
                body: { state: "closed", state_reason: "not_planned" },
            });
        }
        const latest = await api(`/repos/${repository}/issues/${issue.number}`);
        if (latest.state !== "closed") {
            throw new Error(`Issue #${issue.number} invalid metadata close postcondition failed`);
        }
        return { number: issue.number, action: "closed-invalid", reasons: inspection.reasons };
    }

    if (inspection.needsUpdate) {
        await api(`/repos/${repository}/issues/${issue.number}`, {
            method: "PATCH",
            body: {
                labels: inspection.labels,
                assignees: inspection.assignees,
            },
        });
    }
    await assertReconciled(api, repository, issue.number, inspection);
    const priority = inspection.typeKey === "bug"
        ? await reconcilePriorityField(api, repository, issue)
        : null;
    const result = {
        number: issue.number,
        action: inspection.needsUpdate ? "corrected" : "unchanged",
        label: inspection.expectedLabel,
        areaLabel: inspection.expectedAreaLabel,
        assignee: inspection.expectedAssignee,
    };
    if (priority) {
        result.priority = priority.status;
    }
    return result;
}

function createApi(token) {
    return async function api(apiPath, { method = "GET", body } = {}) {
        const response = await fetch(`https://api.github.com${apiPath}`, {
            method,
            headers: {
                accept: "application/vnd.github+json",
                authorization: `Bearer ${token}`,
                "content-type": "application/json",
                "user-agent": "Afternote-issue-metadata-guard",
                "x-github-api-version": "2022-11-28",
            },
            body: body === undefined ? undefined : JSON.stringify(body),
        });
        const text = await response.text();
        const payload = text ? JSON.parse(text) : null;
        if (!response.ok) {
            throw new Error(`GitHub API ${method} ${apiPath} failed: ${response.status} ${payload?.message ?? text}`);
        }
        return payload;
    };
}

async function listOpenIssues(api, repository) {
    const issues = [];
    for (let page = 1; ; page += 1) {
        const batch = await api(`/repos/${repository}/issues?state=open&per_page=100&page=${page}`);
        issues.push(...batch.filter((issue) => !issue.pull_request));
        if (batch.length < 100) {
            return issues;
        }
    }
}

async function main() {
    const token = process.env.GITHUB_TOKEN;
    const repository = process.env.GITHUB_REPOSITORY;
    const eventName = process.env.GITHUB_EVENT_NAME;
    if (!token || !repository || !eventName) {
        throw new Error("GITHUB_TOKEN, GITHUB_REPOSITORY, and GITHUB_EVENT_NAME are required");
    }
    const api = createApi(token);
    let issues;
    if (eventName === "issues") {
        const event = JSON.parse(await fs.readFile(process.env.GITHUB_EVENT_PATH, "utf8"));
        if (!event.issue) {
            throw new Error("issues event payload is missing issue");
        }
        issues = [event.issue];
    } else if (eventName === "schedule" || eventName === "workflow_dispatch") {
        issues = await listOpenIssues(api, repository);
    } else {
        throw new Error(`Unsupported event: ${eventName}`);
    }

    for (const issue of issues) {
        const result = await reconcileIssue(api, repository, issue);
        console.log(`#${result.number}: ${result.action}`);
    }
}

const invokedPath = process.argv[1] ? pathToFileURL(path.resolve(process.argv[1])).href : "";
if (import.meta.url === invokedPath) {
    main().catch((error) => {
        console.error(error instanceof Error ? error.stack : error);
        process.exitCode = 1;
    });
}
