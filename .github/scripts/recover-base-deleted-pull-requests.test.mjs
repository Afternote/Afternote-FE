import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

import { ASSIGNEE_BY_MODULE, inspectIssue } from "./reconcile-issue-metadata.mjs";
import {
    RECENT_CLOSE_WINDOW_MINUTES,
    TRACKING_ISSUE_LABELS,
    TRACKING_ISSUE_MODULE_OPTION,
    TRACKING_ISSUE_TYPE_OPTION,
    autoClosedByBaseDeletion,
    recoverBaseDeletedPullRequests,
    renderReopenComment,
    renderTrackingIssue,
    reopenMarker,
    trackingMarker,
    waitForBaseRef,
} from "./recover-base-deleted-pull-requests.mjs";

const REPO = "Afternote/Afternote-FE";
const REF = "feat/1762-draft-resume";
// 2026-09-11 실사고의 시각. 삭제 1초 뒤에 closed 가 붙었다.
const DELETED_AT = "2026-09-11T22:17:52Z";
const CLOSED_AT = "2026-09-11T22:17:53Z";
const OBSERVED_AT = "2026-09-11T22:18:30Z";
const RUN_URL = `https://github.com/${REPO}/actions/runs/900`;
const silent = { log() {} };
const noWait = async () => {};

function timeline(...events) {
    return events.map(([event, created_at]) => ({ event, created_at }));
}

const AUTO_CLOSED_TIMELINE = timeline(
    ["assigned", "2026-09-11T07:27:16Z"],
    ["added_to_stack", "2026-09-11T08:00:46Z"],
    // committed 항목에는 created_at 이 없다. 순서 판정이 여기서 멈추면 안 된다.
    ["committed", undefined],
    ["base_ref_deleted", DELETED_AT],
    ["closed", CLOSED_AT],
);

function lostPullRequest(overrides = {}) {
    return {
        number: 1626,
        title: "feat: 임시 저장 이어쓰기",
        state: "closed",
        merged_at: null,
        closed_at: CLOSED_AT,
        base: { ref: REF, sha: "1d16dcd67e2ab4f1c0d5f9a8b7c6d5e4f3a2b1c0" },
        head: { ref: "feat/1762-draft-resume-editor" },
        ...overrides,
    };
}

function fakeApi({
    pullRequests = [],
    timelines = {},
    dependents = {},
    baseRefExists = false,
    baseRefError,
    issues = [],
} = {}) {
    const calls = [];
    const api = async (apiPath, options = {}) => {
        const method = options.method ?? "GET";
        calls.push({ apiPath, method, body: options.body });

        const timelineMatch = /\/issues\/(\d+)\/timeline/.exec(apiPath);
        if (timelineMatch) return timelines[timelineMatch[1]] ?? [];
        if (method === "GET" && apiPath.startsWith(`/repos/${REPO}/pulls?state=all`)) return pullRequests;
        if (method === "GET" && apiPath.startsWith(`/repos/${REPO}/pulls?state=open&base=`)) {
            const base = decodeURIComponent(/base=([^&]+)/.exec(apiPath)[1]);
            return (dependents[base] ?? []).map((number) => ({ number }));
        }
        if (apiPath.startsWith(`/repos/${REPO}/git/ref/heads/`)) {
            if (baseRefError) throw baseRefError;
            if (baseRefExists) return { ref: `refs/heads/${REF}` };
            const notFound = new Error("Not Found");
            notFound.status = 404;
            throw notFound;
        }
        if (method === "PATCH" && /\/pulls\/\d+$/.test(apiPath)) return { number: 1626, state: "open" };
        if (method === "POST" && /\/issues\/\d+\/comments$/.test(apiPath)) return { id: 7 };
        if (method === "GET" && apiPath.startsWith(`/repos/${REPO}/issues?state=open`)) return issues;
        if (method === "POST" && apiPath === `/repos/${REPO}/issues`) return { number: 2099 };
        if (method === "PATCH" && /\/issues\/\d+$/.test(apiPath)) return { number: 2050 };
        throw new Error(`unexpected ${method} ${apiPath}`);
    };
    api.calls = calls;
    api.writes = () => calls.filter((call) => call.method !== "GET");
    return api;
}

function recover(api, overrides = {}) {
    return recoverBaseDeletedPullRequests({
        api,
        repository: REPO,
        ref: REF,
        refType: "branch",
        deletedBy: "1hyok",
        runUrl: RUN_URL,
        observedAt: OBSERVED_AT,
        sleep: noWait,
        logger: silent,
        ...overrides,
    });
}

// ---- 판정: base_ref_deleted 다음 closed 라는 서명 (#2076) ----

test("base 삭제 직후 닫힌 미병합 PR 만 되살릴 대상이다", () => {
    const verdict = autoClosedByBaseDeletion({
        pullRequest: lostPullRequest(),
        timeline: AUTO_CLOSED_TIMELINE,
        ref: REF,
        observedAt: OBSERVED_AT,
    });

    assert.equal(verdict.eligible, true);
});

test("이미 머지된 PR 의 base 삭제는 정상 정리다. 되살리지 않는다", () => {
    // 머지 뒤 브랜치를 지우는 일상적인 청소가 이 워크플로를 깨우지 않아야 한다.
    const merged = lostPullRequest({
        merged_at: "2026-09-11T20:00:00Z",
        closed_at: "2026-09-11T20:00:00Z",
    });
    const verdict = autoClosedByBaseDeletion({
        pullRequest: merged,
        timeline: timeline(
            ["merged", "2026-09-11T20:00:00Z"],
            ["closed", "2026-09-11T20:00:00Z"],
            ["base_ref_deleted", DELETED_AT],
        ),
        ref: REF,
        observedAt: OBSERVED_AT,
    });

    assert.equal(verdict.eligible, false);
    assert.match(verdict.reason, /머지/);
});

test("사람이 닫은 PR 은 그 뒤에 브랜치가 지워져도 되살리지 않는다", () => {
    const verdict = autoClosedByBaseDeletion({
        pullRequest: lostPullRequest({ closed_at: "2026-09-11T22:17:40Z" }),
        timeline: timeline(["closed", "2026-09-11T22:17:40Z"], ["base_ref_deleted", DELETED_AT]),
        ref: REF,
        observedAt: OBSERVED_AT,
    });

    assert.equal(verdict.eligible, false);
    assert.match(verdict.reason, /base 삭제가 아닌 폐쇄/);
});

test("다른 base 의 PR, 아직 열린 PR, 창 밖의 옛 폐쇄는 모두 대상이 아니다", () => {
    const cases = [
        [lostPullRequest({ base: { ref: "develop", sha: "0" } }), AUTO_CLOSED_TIMELINE, /base 가 develop/],
        [lostPullRequest({ state: "open", closed_at: null }), AUTO_CLOSED_TIMELINE, /상태가 open/],
        [
            lostPullRequest({ closed_at: "2026-09-11T20:00:00Z" }),
            timeline(["base_ref_deleted", "2026-09-11T19:59:59Z"], ["closed", "2026-09-11T20:00:00Z"]),
            /옛 폐쇄/,
        ],
    ];

    for (const [pullRequest, events, reason] of cases) {
        const verdict = autoClosedByBaseDeletion({
            pullRequest,
            timeline: events,
            ref: REF,
            observedAt: OBSERVED_AT,
        });
        assert.equal(verdict.eligible, false, reason.source);
        assert.match(verdict.reason, reason);
    }
    assert.equal(RECENT_CLOSE_WINDOW_MINUTES, 60);
});

test("base 삭제와 폐쇄 사이가 벌어져 있으면 같은 원인으로 보지 않는다", () => {
    const verdict = autoClosedByBaseDeletion({
        pullRequest: lostPullRequest({ closed_at: "2026-09-11T22:25:00Z" }),
        timeline: timeline(["base_ref_deleted", DELETED_AT], ["closed", "2026-09-11T22:25:00Z"]),
        ref: REF,
        observedAt: OBSERVED_AT,
    });

    assert.equal(verdict.eligible, false);
    assert.match(verdict.reason, /너무 멀다/);
});

// ---- 분기 1: base 가 살아 있으면 다시 열고 경위를 남긴다 ----

test("base 가 같은 이름으로 돌아와 있으면 PR 을 다시 열고 경위 코멘트를 남긴다", async () => {
    const api = fakeApi({
        pullRequests: [lostPullRequest()],
        timelines: { 1626: AUTO_CLOSED_TIMELINE },
        dependents: { "feat/1762-draft-resume-editor": [1839, 1837] },
        baseRefExists: true,
    });

    const result = await recover(api);

    assert.equal(result.action, "reopen");
    assert.deepEqual(result.pullRequests, [1626]);
    const writes = api.writes();
    assert.equal(writes.length, 2);
    // reopen 이 먼저다. 코멘트가 실패해도 PR 은 열려 있어야 한다.
    assert.deepEqual(writes[0], {
        apiPath: `/repos/${REPO}/pulls/1626`,
        method: "PATCH",
        body: { state: "open" },
    });
    assert.equal(writes[1].apiPath, `/repos/${REPO}/issues/1626/comments`);
    assert.ok(writes[1].body.body.startsWith(reopenMarker(REF)));
    assert.match(writes[1].body.body, /base 브랜치 `feat\/1762-draft-resume` 가 삭제되면서/);
    assert.match(writes[1].body.body, /@1hyok/);
    assert.ok(writes[1].body.body.includes(RUN_URL));
    assert.ok(!api.calls.some((call) => call.method === "POST" && call.apiPath === `/repos/${REPO}/issues`));
});

test("삭제 이벤트가 자동 폐쇄보다 먼저 도착하면 반영을 기다렸다가 다시 본다", async () => {
    let attempt = 0;
    const api = fakeApi({
        timelines: { 1626: AUTO_CLOSED_TIMELINE },
        baseRefExists: true,
    });
    const listing = api;
    const wrapped = async (apiPath, options) => {
        if ((options?.method ?? "GET") === "GET" && apiPath.startsWith(`/repos/${REPO}/pulls?state=all`)) {
            attempt += 1;
            listing.calls.push({ apiPath, method: "GET" });
            return attempt === 1 ? [lostPullRequest({ state: "open", closed_at: null })] : [lostPullRequest()];
        }
        return listing(apiPath, options);
    };
    wrapped.calls = listing.calls;

    const result = await recoverBaseDeletedPullRequests({
        api: wrapped,
        repository: REPO,
        ref: REF,
        refType: "branch",
        deletedBy: "1hyok",
        runUrl: RUN_URL,
        observedAt: OBSERVED_AT,
        sleep: noWait,
        logger: silent,
    });

    assert.equal(attempt, 2);
    assert.equal(result.action, "reopen");
});

// ---- 분기 2: base 가 없으면 추적 이슈로 기록한다 ----

test("base 가 돌아오지 않으면 되살리는 대신 끊긴 사슬을 이슈로 남긴다", async () => {
    const api = fakeApi({
        pullRequests: [lostPullRequest()],
        timelines: { 1626: AUTO_CLOSED_TIMELINE },
        dependents: { "feat/1762-draft-resume-editor": [1839, 1837] },
        baseRefExists: false,
    });

    const result = await recover(api);

    assert.equal(result.action, "track");
    assert.equal(result.issueNumber, 2099);
    const writes = api.writes();
    assert.equal(writes.length, 1);
    assert.equal(writes[0].apiPath, `/repos/${REPO}/issues`);
    assert.deepEqual(writes[0].body.labels, TRACKING_ISSUE_LABELS);
    assert.ok(writes[0].body.body.startsWith(trackingMarker(REF)));
    assert.match(writes[0].body.title, /삭제된 base 브랜치 feat\/1762-draft-resume/);
    assert.match(writes[0].body.body, /#1626/);
    assert.match(writes[0].body.body, /이 PR 의 head 를 base 로 하던 열린 PR: #1839, #1837/);
    assert.match(writes[0].body.body, /git push origin 1d16dcd67e2ab4f1c0d5f9a8b7c6d5e4f3a2b1c0:refs\/heads\/feat\/1762-draft-resume/);
    assert.match(writes[0].body.body, /gh pr reopen 1626 --repo Afternote\/Afternote-FE/);
    assert.ok(!api.calls.some((call) => call.method === "PATCH" && call.apiPath.includes("/pulls/")));
});

test("같은 브랜치의 추적 이슈가 이미 열려 있으면 새로 만들지 않고 갱신한다", async () => {
    const api = fakeApi({
        pullRequests: [lostPullRequest()],
        timelines: { 1626: AUTO_CLOSED_TIMELINE },
        baseRefExists: false,
        issues: [
            { number: 2050, body: `${trackingMarker(REF)}\n옛 기록` },
            { number: 2051, body: trackingMarker("feat/other"), pull_request: {} },
        ],
    });

    const result = await recover(api);

    assert.equal(result.action, "track");
    assert.equal(result.issueNumber, 2050);
    assert.equal(result.reused, true);
    const writes = api.writes();
    assert.equal(writes.length, 1);
    assert.equal(writes[0].method, "PATCH");
    assert.equal(writes[0].apiPath, `/repos/${REPO}/issues/2050`);
});

test("base 조회가 404 가 아닌 이유로 실패하면 조용히 추적 이슈로 흘러가지 않는다", async () => {
    const serverError = new Error("Server Error");
    serverError.status = 500;
    const api = fakeApi({
        pullRequests: [lostPullRequest()],
        timelines: { 1626: AUTO_CLOSED_TIMELINE },
        baseRefError: serverError,
    });

    await assert.rejects(() => recover(api), /Server Error/);
    assert.deepEqual(api.writes(), []);
});

test("base 는 여러 번 확인한다. 이름을 옮기는 push 가 삭제보다 늦게 도착한다", async () => {
    let attempt = 0;
    const api = async () => {
        attempt += 1;
        if (attempt < 3) {
            const notFound = new Error("Not Found");
            notFound.status = 404;
            throw notFound;
        }
        return { ref: `refs/heads/${REF}` };
    };

    assert.equal(
        await waitForBaseRef({ api, repository: REPO, ref: REF, sleep: noWait, logger: silent }),
        true,
    );
    assert.equal(attempt, 3);
});

// ---- 정상 정리에는 반응하지 않는다 ----

test("머지된 PR 의 base 브랜치를 지우는 정상 정리에는 아무것도 하지 않는다", async () => {
    const api = fakeApi({
        pullRequests: [
            lostPullRequest({ number: 1600, merged_at: "2026-09-11T20:00:00Z", closed_at: "2026-09-11T20:00:00Z" }),
        ],
        baseRefExists: true,
    });

    const result = await recover(api);

    assert.equal(result.action, "none");
    assert.equal(result.why, "no-auto-closed-pull-request");
    assert.deepEqual(api.writes(), []);
    // 머지된 PR 은 타임라인을 물어볼 필요도 없다.
    assert.ok(!api.calls.some((call) => call.apiPath.includes("/timeline")));
});

test("태그 삭제와 base 가 다른 PR 만 있는 삭제에는 아무것도 하지 않는다", async () => {
    const tagApi = fakeApi();
    assert.equal((await recover(tagApi, { refType: "tag" })).action, "none");
    assert.deepEqual(tagApi.calls, []);

    const otherBaseApi = fakeApi({
        pullRequests: [lostPullRequest({ number: 1700, base: { ref: "develop", sha: "0" } })],
    });
    assert.equal((await recover(otherBaseApi)).action, "none");
    assert.deepEqual(otherBaseApi.writes(), []);
});

// ---- 문구 ----

test("경위 코멘트는 마커로 시작하고 닫힌 원인과 확인할 것을 적는다", () => {
    const body = renderReopenComment({ ref: REF, deletedBy: "1hyok", runUrl: RUN_URL });

    assert.ok(body.startsWith(reopenMarker(REF)));
    assert.match(body, /자동으로 닫혔다/);
    assert.match(body, /머지 전에 base 와 스택 순서를 확인한다/);
    // GITHUB_TOKEN 이 일으킨 이벤트로는 새 run 이 만들어지지 않는다. 그 사실을 읽는 사람에게 남긴다.
    assert.match(body, /새 워크플로 run 이 만들어지지 않으니/);
});

test("추적 이슈는 복구 명령과 사슬을 함께 적는다", () => {
    const issue = renderTrackingIssue({
        ref: REF,
        repository: REPO,
        deletedBy: "1hyok",
        runUrl: RUN_URL,
        lost: [{ pullRequest: lostPullRequest(), dependents: [1839] }],
    });

    assert.match(issue.title, /^maintenance\(platform\): /);
    assert.ok(issue.body.startsWith(trackingMarker(REF)));
    assert.match(issue.body, /## 끊긴 지점/);
    assert.match(issue.body, /gh stack link --base develop/);
    assert.deepEqual(issue.labels, TRACKING_ISSUE_LABELS);
});

test("추적 이슈는 이슈 양식 판정을 통과한다. 열리자마자 닫히면 기록이 아니다", () => {
    // issue-metadata-guard 는 작업 유형·주 담당 모듈을 읽지 못한 이슈를 자동으로 닫는다.
    // 되살리지 못한 사슬의 유일한 기록이 그렇게 사라지면 안 된다.
    const issue = renderTrackingIssue({
        ref: REF,
        repository: REPO,
        deletedBy: "1hyok",
        runUrl: RUN_URL,
        lost: [{ pullRequest: lostPullRequest(), dependents: [1839] }],
    });
    const inspection = inspectIssue({
        number: 2100,
        body: issue.body,
        labels: issue.labels.map((name) => ({ name })),
        assignees: [],
    });

    assert.equal(inspection.status, "valid");
    assert.deepEqual([...inspection.labels].sort(), [...TRACKING_ISSUE_LABELS].sort());
    assert.deepEqual(inspection.assignees, [ASSIGNEE_BY_MODULE.platform]);
});

test("양식 선택지는 issue.yml 의 원문 그대로다", () => {
    const template = readFileSync(new URL("../ISSUE_TEMPLATE/issue.yml", import.meta.url), "utf8");

    assert.ok(template.includes(`- ${TRACKING_ISSUE_TYPE_OPTION}`), TRACKING_ISSUE_TYPE_OPTION);
    assert.ok(template.includes(`- ${TRACKING_ISSUE_MODULE_OPTION}`), TRACKING_ISSUE_MODULE_OPTION);
});

// ---- 워크플로 정책 ----

test("워크플로는 delete 이벤트를 default branch 정의로 받고, 테스트를 먼저 돌린 뒤 스크립트를 부른다", () => {
    const source = readFileSync(new URL("../workflows/base-delete-recovery.yml", import.meta.url), "utf8");

    assert.match(source, /^on:\n\s{2}delete:\n$/m);
    assert.match(source, /^permissions: \{\}$/m);
    assert.match(source, /ref: \$\{\{ github\.event\.repository\.default_branch \}\}/);
    assert.match(source, /persist-credentials: false/);
    assert.match(source, /cancel-in-progress: false/);
    assert.ok(
        source.indexOf("recover-base-deleted-pull-requests.test.mjs") <
            source.indexOf("node .github/scripts/recover-base-deleted-pull-requests.mjs"),
    );
    // job 안에서 이 테스트가 돌려면 이 파일이 읽는 디렉터리가 모두 체크아웃돼 있어야 한다.
    // 새 파일을 읽기 시작하면 그 디렉터리도 여기서 요구된다.
    const sparseCheckout = /sparse-checkout: \|\n((?:\s+\.github\/\S+\n)+)/.exec(source);
    assert.ok(sparseCheckout, "sparse-checkout 목록을 읽지 못했다");
    const testSource = readFileSync(new URL(import.meta.url), "utf8");
    const required = new Set([".github/scripts"]);
    for (const [, directory] of testSource.matchAll(/new URL\("\.\.\/([^/"]+)\//g)) {
        required.add(`.github/${directory}`);
    }
    assert.ok(required.has(".github/workflows") && required.has(".github/ISSUE_TEMPLATE"), [...required].join(", "));
    for (const directory of required) {
        assert.ok(sparseCheckout[1].includes(directory), directory);
    }
    assert.match(source, /DELETED_REF: \$\{\{ github\.event\.ref \}\}/);
    assert.match(source, /DELETED_REF_TYPE: \$\{\{ github\.event\.ref_type \}\}/);
    // 판정과 문구는 스크립트에 있다. 주석은 이력이라 남기고 실행되는 줄만 본다.
    const executable = source
        .split("\n")
        .filter((line) => !/^\s*#/.test(line))
        .join("\n");
    assert.doesNotMatch(executable, /base_ref_deleted|closed_at|gh api|gh pr /);
});

test("PR 을 다시 열고 코멘트하려면 pull-requests: write 가 필요하다 (#2068)", () => {
    // 같은 사고에서 stack-integrity-notify 는 issues: write · pull-requests: read 로 돌다가
    // PR 코멘트 POST 가 403 으로 죽었다. 새 워크플로는 처음부터 맞는 권한으로 시작한다.
    const source = readFileSync(new URL("../workflows/base-delete-recovery.yml", import.meta.url), "utf8");
    const job = source.slice(source.indexOf("    permissions:"));

    assert.match(job, /^\s{6}contents: read$/m);
    assert.match(job, /^\s{6}issues: write$/m);
    assert.match(job, /^\s{6}pull-requests: write$/m);
    assert.doesNotMatch(job, /^\s{6}(actions|contents|statuses|packages): write$/m);
});
