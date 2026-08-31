import assert from "node:assert/strict";
import test from "node:test";

import {
    DEFAULT_LABEL,
    applyPlan,
    countAuthorFixes,
    countAuthorResponses,
    ensureLabelExists,
    judgeAwaitingAuthor,
    latestDecision,
    planAwaitingAuthorLabels,
    renderSummary,
} from "./label-awaiting-author-prs.mjs";

const REPO = "Afternote/Afternote-FE";

/** 테스트 출력이 CI 로그에서 실제 조작처럼 보이지 않도록 삼킨다. */
const silent = { log() {} };

function fakeApi({ responses = {}, failOn = null } = {}) {
    const calls = [];
    const api = async (apiPath, options = {}) => {
        calls.push({ apiPath, method: options.method ?? "GET", body: options.body });
        if (failOn && failOn(apiPath, options)) {
            throw new Error("boom");
        }
        for (const [pattern, value] of Object.entries(responses)) {
            if (apiPath.startsWith(pattern)) {
                return typeof value === "function" ? value(apiPath, options) : value;
            }
        }
        return options.allowNotFound ? null : {};
    };
    api.calls = calls;
    return api;
}

function commit({ login = "author", email = "author@example.com", date = "2026-08-30T00:00:00Z", changed = 3, parents = 1 } = {}) {
    return {
        commit: {
            committedDate: date,
            changedFilesIfAvailable: changed,
            parents: { totalCount: parents },
            author: { email, user: login === null ? null : { login } },
        },
    };
}

function pullRequest(overrides = {}) {
    return {
        number: 1,
        title: "fix(core): 무언가",
        isDraft: false,
        createdAt: "2026-08-01T00:00:00Z",
        author: { login: "author" },
        headRepository: { nameWithOwner: REPO },
        labels: { nodes: [] },
        reviews: { nodes: [] },
        commits: { nodes: [] },
        comments: { nodes: [] },
        ...overrides,
    };
}

function changesRequested(at = "2026-08-29T00:00:00Z", reviewer = "reviewer") {
    return { state: "CHANGES_REQUESTED", submittedAt: at, author: { login: reviewer } };
}

test("최신 결정 리뷰는 리뷰어별로 묶지 않고 PR 전체에서 하나만 고른다", () => {
    // 팀에서 한 사람이 더 늦은 판정을 내리면 그것이 PR 의 현재 상태다. 더 오래된 다른
    // 리뷰어의 변경요청을 다시 살리면 승인된 PR 에까지 라벨이 붙는다.
    const decision = latestDecision([
        { state: "CHANGES_REQUESTED", submittedAt: "2026-08-01T00:00:00Z", author: { login: "a" } },
        { state: "APPROVED", submittedAt: "2026-08-05T00:00:00Z", author: { login: "b" } },
    ]);
    assert.equal(decision.state, "APPROVED");

    // COMMENTED 는 판정이 아니다.
    const onlyComments = latestDecision([
        { state: "COMMENTED", submittedAt: "2026-08-09T00:00:00Z", author: { login: "a" } },
    ]);
    assert.equal(onlyComments, null);
});

test("변경요청 뒤 작성자가 아무것도 하지 않으면 대상이다", () => {
    const verdict = judgeAwaitingAuthor(
        pullRequest({ reviews: { nodes: [changesRequested()] } }),
        { repository: REPO },
    );
    assert.equal(verdict.awaiting, true);
    assert.equal(verdict.reviewer, "reviewer");
    assert.equal(verdict.decidedAt, "2026-08-29T00:00:00Z");
});

test("최신 판정이 승인이면 대상이 아니다", () => {
    // 승인 뒤 작성자가 손보는 것은 작성자 몫이지만, 그것은 «머지 가능한 상태» 이지
    // «지적을 방치한 상태» 가 아니다.
    const verdict = judgeAwaitingAuthor(
        pullRequest({
            reviews: {
                nodes: [changesRequested("2026-08-01T00:00:00Z"), { state: "APPROVED", submittedAt: "2026-08-02T00:00:00Z", author: { login: "reviewer" } }],
            },
        }),
        { repository: REPO },
    );
    assert.equal(verdict.awaiting, false);
});

test("리뷰어가 미는 빈 CI 재트리거 커밋은 작성자 조치가 아니다", () => {
    // 8/28 에 리뷰어가 건 +0/-0 커밋 하나로 PR 3건(#1379·#1365·#882)이 전부 가짜 빚이
    // 됐다 (#1459). 여기서 같은 실수를 하면 방치된 PR 의 라벨이 조용히 떨어진다.
    const withReviewerEmptyCommit = pullRequest({
        reviews: { nodes: [changesRequested()] },
        commits: { nodes: [commit({ login: "reviewer", email: "reviewer@example.com", changed: 0 })] },
    });
    assert.equal(judgeAwaitingAuthor(withReviewerEmptyCommit, { repository: REPO }).awaiting, true);

    // 작성자가 올린 커밋이라도 바뀐 파일이 0건이면 반영이 아니다.
    const ownEmptyCommit = pullRequest({
        reviews: { nodes: [changesRequested()] },
        commits: { nodes: [commit({ changed: 0 })] },
    });
    assert.equal(judgeAwaitingAuthor(ownEmptyCommit, { repository: REPO }).awaiting, true);
});

test("작성자의 실질 커밋이 하나라도 있으면 대상에서 빠진다", () => {
    const verdict = judgeAwaitingAuthor(
        pullRequest({
            reviews: { nodes: [changesRequested()] },
            commits: { nodes: [commit({ date: "2026-08-29T12:00:00Z" })] },
        }),
        { repository: REPO },
    );
    assert.equal(verdict.awaiting, false);
    assert.match(verdict.reason, /1커밋/);
});

test("판정 이전 커밋은 반영으로 세지 않는다", () => {
    const verdict = judgeAwaitingAuthor(
        pullRequest({
            reviews: { nodes: [changesRequested("2026-08-29T00:00:00Z")] },
            commits: { nodes: [commit({ date: "2026-08-28T00:00:00Z" })] },
        }),
        { repository: REPO },
    );
    assert.equal(verdict.awaiting, true);
});

test("병합으로 반영하고 코멘트를 남긴 PR 은 대상이 아니다", () => {
    // 비병합 커밋만 세면 base 를 끌어와 충돌을 풀며 반영한 PR 이 «무조치» 가 된다.
    // #1316 은 리뷰가 지목한 파일이 실제로 바뀌고 응답도 2건 달린 채 빚에서 빠져
    // 있었다 (#1450). 커밋과 함께 작성자의 응답을 본다.
    const verdict = judgeAwaitingAuthor(
        pullRequest({
            reviews: { nodes: [changesRequested()] },
            commits: { nodes: [commit({ date: "2026-08-29T10:00:00Z", parents: 2 })] },
            comments: { nodes: [{ createdAt: "2026-08-29T11:00:00Z", author: { login: "author" } }] },
        }),
        { repository: REPO },
    );
    assert.equal(verdict.awaiting, false);
    assert.match(verdict.reason, /응답 1건/);
});

test("작성자가 제출한 리뷰도 응답으로 센다", () => {
    // 라인 코멘트를 남기면 그것을 담은 리뷰가 함께 제출된다. 별도 조회 없이 잡힌다.
    const responses = countAuthorResponses({
        comments: [],
        reviews: [
            changesRequested(),
            { state: "COMMENTED", submittedAt: "2026-08-29T05:00:00Z", author: { login: "author" } },
        ],
        author: "author",
        since: "2026-08-29T00:00:00Z",
    });
    assert.equal(responses, 1);
});

test("변경 파일 수를 알 수 없는 커밋은 반영으로 센다", () => {
    // 조회 공백을 «빈 커밋» 으로 접으면 라벨이 없는 근거로 둔갑한다. 놓치는 쪽이
    // 잘못 붙이는 쪽보다 안전하다.
    const fixes = countAuthorFixes({
        commits: [commit({ date: "2026-08-29T10:00:00Z", changed: null })],
        author: "author",
        since: "2026-08-29T00:00:00Z",
    });
    assert.equal(fixes, 1);
});

test("계정이 연결되지 않은 커밋은 같은 PR 의 작성자 이메일로 가린다", () => {
    const fixes = countAuthorFixes({
        commits: [
            commit({ date: "2026-08-29T10:00:00Z" }),
            commit({ login: null, email: "AUTHOR@example.com", date: "2026-08-29T11:00:00Z" }),
        ],
        author: "author",
        since: "2026-08-29T00:00:00Z",
    });
    assert.equal(fixes, 2);

    // 남의 이메일까지 주우면 리뷰어 커밋이 작성자 것이 된다.
    const strangers = countAuthorFixes({
        commits: [commit({ login: null, email: "someone@example.com", date: "2026-08-29T11:00:00Z" })],
        author: "author",
        since: "2026-08-29T00:00:00Z",
    });
    assert.equal(strangers, 0);
});

test("draft·봇·fork 는 판정 대상이 아니다", () => {
    const draft = pullRequest({ isDraft: true, reviews: { nodes: [changesRequested()] } });
    assert.equal(judgeAwaitingAuthor(draft, { repository: REPO }).awaiting, false);

    const bot = pullRequest({ author: { login: "dependabot[bot]" }, reviews: { nodes: [changesRequested()] } });
    assert.equal(judgeAwaitingAuthor(bot, { repository: REPO }).awaiting, false);

    const fork = pullRequest({
        headRepository: { nameWithOwner: "someone/Afternote-FE" },
        reviews: { nodes: [changesRequested()] },
    });
    assert.equal(judgeAwaitingAuthor(fork, { repository: REPO }).awaiting, false);
});

test("계획은 붙일 것과 뗄 것을 한 번에 낸다", () => {
    const plan = planAwaitingAuthorLabels({
        repository: REPO,
        pullRequests: [
            // 무조치 · 라벨 없음 → 붙인다
            pullRequest({ number: 10, reviews: { nodes: [changesRequested()] } }),
            // 조치함 · 라벨 있음 → 뗀다
            pullRequest({
                number: 11,
                labels: { nodes: [{ name: DEFAULT_LABEL }] },
                reviews: { nodes: [changesRequested()] },
                commits: { nodes: [commit({ date: "2026-08-29T10:00:00Z" })] },
            }),
            // 무조치 · 라벨 있음 → 그대로
            pullRequest({
                number: 12,
                labels: { nodes: [{ name: DEFAULT_LABEL }] },
                reviews: { nodes: [changesRequested()] },
            }),
            // 승인 · 라벨 없음 → 그대로
            pullRequest({ number: 13 }),
        ],
    });

    assert.deepEqual(plan.toLabel.map((entry) => entry.number), [10]);
    assert.deepEqual(plan.toUnlabel.map((entry) => entry.number), [11]);
    assert.deepEqual(plan.unchanged.map((entry) => entry.number), [12, 13]);
});

test("적용은 실패한 PR 만 보고하고 나머지를 계속 처리한다", async () => {
    const api = fakeApi({ failOn: (apiPath) => apiPath.includes("/issues/10/") });
    const plan = {
        toLabel: [
            { number: 10, reason: "변경요청 뒤 작성자 조치 없음" },
            { number: 11, reason: "변경요청 뒤 작성자 조치 없음" },
        ],
        toUnlabel: [{ number: 12, reason: "작성자 조치 있음(1커밋 · 응답 0건)" }],
    };

    const failures = await applyPlan(api, REPO, plan, { logger: silent });

    assert.equal(failures.length, 1);
    assert.match(failures[0], /#10/);
    assert.ok(api.calls.some((call) => call.apiPath === `/repos/${REPO}/issues/11/labels` && call.method === "POST"));
    assert.ok(
        api.calls.some(
            (call) => call.apiPath === `/repos/${REPO}/issues/12/labels/${DEFAULT_LABEL}` && call.method === "DELETE",
        ),
    );
});

test("dry-run 은 아무것도 쓰지 않는다", async () => {
    const api = fakeApi();
    const plan = { toLabel: [{ number: 10, reason: "r" }], toUnlabel: [{ number: 11, reason: "r" }] };

    await applyPlan(api, REPO, plan, { dryRun: true, logger: silent });

    assert.equal(api.calls.filter((call) => call.method !== "GET").length, 0);
});

test("이미 있는 라벨은 다시 만들지 않는다", async () => {
    const existing = fakeApi({ responses: { [`/repos/${REPO}/labels/${DEFAULT_LABEL}`]: { name: DEFAULT_LABEL } } });
    await ensureLabelExists(existing, REPO);
    assert.equal(existing.calls.filter((call) => call.method === "POST").length, 0);

    const missing = fakeApi({ responses: { [`/repos/${REPO}/labels/${DEFAULT_LABEL}`]: () => null } });
    await ensureLabelExists(missing, REPO);
    assert.ok(missing.calls.some((call) => call.apiPath === `/repos/${REPO}/labels` && call.method === "POST"));
});

test("요약에는 붙인 PR 의 근거가 남는다", () => {
    const summary = renderSummary({
        plan: {
            toLabel: [{ number: 440, author: "koongmai", decidedAt: "2026-08-29T00:00:00Z", reviewer: "Sadturtleman" }],
            toUnlabel: [{ number: 11, reason: "작성자 조치 있음(1커밋 · 응답 0건)" }],
        },
        dryRun: false,
    });

    assert.match(summary, /#440/);
    assert.match(summary, /@koongmai/);
    assert.match(summary, /2026-08-29/);
    assert.match(summary, /#11 — 작성자 조치 있음/);
});
