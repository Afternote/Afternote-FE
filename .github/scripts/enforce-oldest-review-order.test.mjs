import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

import {
    analyzeDecisiveReviews,
    classifyRepositoryPermission,
    classifyReviewEvent,
    evaluatePullRequestOrder,
    hasSubstantiveCommitAfter,
    nextPageUrl,
    paginateRest,
    parsePolicyDismissalMessage,
    postOrderStatus,
    recalculatePullRequests,
    renderDismissalMessage,
    replayHistoricalApproval,
    replayMarker,
    reviewDebtStatus,
    selectLatestActiveDecisiveReviewsByReviewer,
    selectLatestActiveHumanDecisiveReview,
    selectRequestedOutstandingReview,
    selectOlderPullRequests,
    STATUS_CONTEXT,
} from "./enforce-oldest-review-order.mjs";

const REPOSITORY = "afternote/app";
const silent = { log() {} };
const REPLAY_REVIEW_ID = 5032388275;
const REPLAY_PULL_NUMBER = 821;
const REPLAY_DISMISSAL_EVENT_ID = 30053730355;
const REPLAY_PREDECESSOR_PULL_NUMBER = 741;

function reviewEvent(overrides = {}) {
    return {
        action: "submitted",
        repository: { full_name: REPOSITORY },
        pull_request: {
            number: 30,
            title: "현재 PR",
            created_at: "2026-08-20T00:00:00Z",
            draft: false,
            head: { repo: { full_name: REPOSITORY } },
        },
        review: {
            id: 900,
            state: "approved",
            user: { login: "reviewer", type: "User" },
        },
        ...overrides,
    };
}

function pullRequest(overrides = {}) {
    return {
        number: 1,
        title: "오래된 PR",
        created_at: "2026-08-01T00:00:00Z",
        state: "open",
        draft: false,
        requested_reviewers: [],
        user: { login: "author" },
        head: {
            sha: "1111111111111111111111111111111111111111",
            repo: { full_name: REPOSITORY },
        },
        ...overrides,
    };
}

function review(overrides = {}) {
    return {
        id: 1,
        state: "APPROVED",
        submitted_at: "2026-08-02T00:00:00Z",
        user: { login: "alice" },
        ...overrides,
    };
}

function commit(overrides = {}) {
    return {
        sha: "abc",
        parents: [{ sha: "parent" }],
        commit: { committer: { date: "2026-08-03T00:00:00Z" } },
        ...overrides,
    };
}

function response(data, link = null) {
    return {
        data,
        headers: { get: (name) => name.toLowerCase() === "link" ? link : null },
    };
}

function fakeClient(handler) {
    const calls = [];
    return {
        calls,
        async request(apiPath, options = {}) {
            const call = {
                apiPath,
                method: options.method ?? "GET",
                body: options.body,
            };
            calls.push(call);
            return handler(call);
        },
    };
}

test("submitted APPROVED/CHANGES_REQUESTED 리뷰만 대상이다", () => {
    const approved = classifyReviewEvent(reviewEvent(), REPOSITORY);
    assert.equal(approved.status, "eligible");
    assert.equal(approved.context.state, "APPROVED");

    const changesRequested = classifyReviewEvent(
        reviewEvent({ review: { id: 901, state: "changes_requested", user: { login: "reviewer" } } }),
        REPOSITORY,
    );
    assert.equal(changesRequested.status, "eligible");
    assert.equal(changesRequested.context.state, "CHANGES_REQUESTED");

    const commented = classifyReviewEvent(
        reviewEvent({ review: { id: 902, state: "commented", user: { login: "reviewer" } } }),
        REPOSITORY,
    );
    assert.equal(commented.status, "skipped");
    assert.match(commented.reason, /결정 리뷰가 아님/);
});

test("봇, fork, draft 리뷰는 API 조회 전에 건너뛴다", () => {
    const bot = classifyReviewEvent(
        reviewEvent({ review: { id: 1, state: "approved", user: { login: "renovate[bot]" } } }),
        REPOSITORY,
    );
    assert.match(bot.reason, /봇 리뷰어/);

    const forkPayload = reviewEvent();
    forkPayload.pull_request.head.repo.full_name = "someone/fork";
    assert.match(classifyReviewEvent(forkPayload, REPOSITORY).reason, /fork PR/);

    const draftPayload = reviewEvent();
    draftPayload.pull_request.draft = true;
    assert.match(classifyReviewEvent(draftPayload, REPOSITORY).reason, /draft PR/);
});

test("payload 저장소가 실행 저장소와 다르면 조용히 통과시키지 않는다", () => {
    assert.throws(
        () => classifyReviewEvent(reviewEvent(), "different/repository"),
        /이벤트 저장소.*실행 저장소.*다릅니다/,
    );
});

test("write, maintain, admin 만 팀 리뷰어로 판정한다", () => {
    for (const permission of ["write", "maintain", "admin"]) {
        assert.equal(classifyRepositoryPermission(permission), "team");
    }
    for (const permission of ["none", "read", "triage"]) {
        assert.equal(classifyRepositoryPermission(permission), "non-team");
    }
    assert.throws(() => classifyRepositoryPermission("mystery-role"), /알 수 없는/);
});

test("현재 PR보다 오래된 ready PR을 생성일과 번호 순으로 고른다", () => {
    const selected = selectOlderPullRequests(
        [
            pullRequest({ number: 11, created_at: "2026-08-20T00:00:00Z" }),
            pullRequest({ number: 7, created_at: "2026-08-10T00:00:00Z" }),
            pullRequest({ number: 3, created_at: "2026-08-01T00:00:00Z", draft: true }),
            pullRequest({ number: 4, created_at: "2026-08-01T00:00:00Z", user: { login: "Reviewer" } }),
            pullRequest({ number: 9, created_at: "2026-08-20T00:00:00Z" }),
            pullRequest({ number: 5, created_at: "2026-08-01T00:00:00Z" }),
            pullRequest({ number: 40, created_at: "2026-08-21T00:00:00Z" }),
        ],
        { number: 10, createdAt: "2026-08-20T00:00:00Z" },
        "reviewer",
    );

    assert.deepEqual(selected.map((item) => item.number), [5, 7, 9]);
});

test("결정 리뷰가 하나도 없으면 즉시 리뷰 빚이다", () => {
    const result = analyzeDecisiveReviews([
        review({ state: "COMMENTED" }),
        review({ state: "DISMISSED", user: { login: "bob" } }),
    ]);

    assert.deepEqual(result, { kind: "no-decisive-review", debt: true, blockedAt: null });
});

test("PR 전체의 최신 결정으로 현재 상태를 판단한다", () => {
    const resolved = analyzeDecisiveReviews([
        review({
            state: "CHANGES_REQUESTED",
            submitted_at: "2026-08-01T00:00:00Z",
            user: { login: "alice" },
        }),
        review({
            state: "APPROVED",
            submitted_at: "2026-08-02T00:00:00Z",
            user: { login: "bob" },
        }),
    ]);
    assert.equal(resolved.kind, "resolved");

    const blocked = analyzeDecisiveReviews([
        review({ state: "CHANGES_REQUESTED", submitted_at: "2026-08-01T00:00:00Z" }),
        review({ state: "APPROVED", submitted_at: "2026-08-02T00:00:00Z" }),
        review({
            state: "CHANGES_REQUESTED",
            submitted_at: "2026-08-03T00:00:00Z",
            user: { login: "bob" },
        }),
    ]);
    assert.equal(blocked.kind, "changes-requested");
    assert.equal(blocked.blockedAt, "2026-08-03T00:00:00Z");
    assert.equal(blocked.outstandingReviews.at(-1).reviewer, "bob");
});

test("변경요청을 낸 리뷰어에게 현재 재요청이 걸린 경우만 고른다", () => {
    const reviewState = analyzeDecisiveReviews([
        review({ state: "CHANGES_REQUESTED", user: { login: "Alice" } }),
        review({
            state: "CHANGES_REQUESTED",
            submitted_at: "2026-08-03T00:00:00Z",
            user: { login: "bob" },
        }),
    ]);

    assert.equal(selectRequestedOutstandingReview(reviewState, ["alice"]), null);
    assert.equal(selectRequestedOutstandingReview(reviewState, ["BOB"]).reviewer, "bob");
    assert.equal(selectRequestedOutstandingReview(reviewState, ["carol"]), null);
});

test("더 오래된 리뷰어의 요청은 이후 다른 변경요청을 리뷰 빚으로 되돌리지 않는다", () => {
    const reviews = [
        review({
            state: "CHANGES_REQUESTED",
            submitted_at: "2026-08-23T02:12:43Z",
            user: { login: "Sadturtleman" },
        }),
        review({
            state: "CHANGES_REQUESTED",
            submitted_at: "2026-08-23T11:45:05Z",
            user: { login: "1hyok" },
        }),
    ];
    const commits = [commit({
        commit: { committer: { date: "2026-08-23T03:15:59Z" } },
    })];

    const result = reviewDebtStatus(reviews, commits, ["Sadturtleman"]);

    assert.equal(result.debt, false);
    assert.equal(result.reason, "changes-requested-not-rerequested");
    assert.equal(result.blockedAt, "2026-08-23T11:45:05Z");
});

test("변경요청 뒤 실질 커밋만 반영으로 세고 merge commit은 제외한다", () => {
    const blockedAt = "2026-08-02T00:00:00Z";
    assert.equal(
        hasSubstantiveCommitAfter([
            commit({
                sha: "merge",
                parents: [{ sha: "a" }, { sha: "b" }],
                commit: { committer: { date: "2026-08-04T00:00:00Z" } },
            }),
            commit({
                sha: "before",
                commit: { committer: { date: "2026-08-01T00:00:00Z" } },
            }),
        ], blockedAt),
        false,
    );
    assert.equal(hasSubstantiveCommitAfter([commit()], blockedAt), true);
});

test("깨진 커밋 항목이 있으면 일부 근거만으로 리뷰 빚을 판정하지 않는다", () => {
    assert.throws(
        () => hasSubstantiveCommitAfter([commit(), { sha: "broken", parents: null }], "2026-08-02T00:00:00Z"),
        /parents 응답이 올바르지 않습니다/,
    );
});

test("reviewDebtStatus는 명시적 재요청과 변경요청 반영을 모두 요구한다", () => {
    const reviews = [review({ state: "CHANGES_REQUESTED" })];
    assert.equal(
        reviewDebtStatus(reviews, [
            commit({ commit: { committer: { date: "2026-08-01T00:00:00Z" } } }),
        ], ["alice"]).debt,
        false,
    );
    assert.equal(reviewDebtStatus(reviews, [commit()], ["alice"]).debt, true);
    assert.equal(reviewDebtStatus(reviews, [commit()], []).reason, "changes-requested-not-rerequested");
    assert.equal(reviewDebtStatus(reviews, [commit()], ["bob"]).debt, false);
    assert.throws(() => reviewDebtStatus(reviews, undefined, ["alice"]), /커밋 목록이 없습니다/);
});

test("REST Link의 next 페이지를 끝까지 읽는다", async () => {
    assert.equal(
        nextPageUrl('<https://api.github.com/first>; rel="prev", <https://api.github.com/second>; rel="next"'),
        "https://api.github.com/second",
    );

    const client = fakeClient(({ apiPath }) => {
        if (apiPath === "/first") {
            return response([{ id: 1 }], '<https://api.github.com/second>; rel="next"');
        }
        if (apiPath === "https://api.github.com/second") {
            return response([{ id: 2 }]);
        }
        throw new Error(`unexpected ${apiPath}`);
    });

    assert.deepEqual(await paginateRest(client, "/first"), [{ id: 1 }, { id: 2 }]);
    assert.equal(client.calls.length, 2);
});

test("REST pagination 순환과 비배열 응답은 실패한다", async () => {
    const cyclic = fakeClient(() => response([], '</first>; rel="next"'));
    await assert.rejects(() => paginateRest(cyclic, "/first"), /페이지네이션이 순환합니다/);

    const malformed = fakeClient(() => response({ id: 1 }));
    await assert.rejects(() => paginateRest(malformed, "/first"), /응답이 배열이 아닙니다/);
});

test("dismiss 안내는 가장 오래된 PR 번호와 한 줄 제목을 명시한다", () => {
    const message = renderDismissalMessage({
        number: 77,
        title: "여러 줄\n제목",
    });
    assert.match(message, /#77 \(여러 줄 제목\)/);
    assert.match(message, /자동 취소/);
    assert.deepEqual(parsePolicyDismissalMessage(message), {
        number: 77,
        title: "여러 줄 제목",
    });
});

test("현재 활성 human 결정 리뷰만 최신순으로 고른다", () => {
    const latest = selectLatestActiveHumanDecisiveReview([
        review({ id: 10, state: "DISMISSED", submitted_at: "2026-08-10T00:00:00Z" }),
        review({ id: 11, state: "APPROVED", submitted_at: "2026-08-11T00:00:00Z" }),
        review({
            id: 12,
            state: "APPROVED",
            submitted_at: "2026-08-12T00:00:00Z",
            user: { login: "github-actions[bot]", type: "Bot" },
        }),
        review({
            id: 13,
            state: "CHANGES_REQUESTED",
            submitted_at: "2026-08-13T00:00:00Z",
            user: { login: "bob", type: "User" },
        }),
    ]);
    assert.equal(latest.id, 13);
    assert.equal(latest.reviewer, "bob");
});

test("검증된 replay bot 승인은 원 DISMISSED human 리뷰어에게 귀속한다", () => {
    const original = review({
        id: REPLAY_REVIEW_ID,
        state: "DISMISSED",
        submitted_at: "2026-08-01T00:00:00Z",
        user: { login: "koongmai", type: "User" },
    });
    const proxy = review({
        id: REPLAY_REVIEW_ID + 1,
        state: "APPROVED",
        submitted_at: "2026-08-10T00:00:00Z",
        user: { login: "github-actions[bot]", type: "Bot" },
        body: `${replayMarker(REPLAY_REVIEW_ID)}\n\n원 리뷰 이관`,
    });
    const selected = selectLatestActiveDecisiveReviewsByReviewer([original, proxy]);
    assert.equal(selected.length, 1);
    assert.equal(selected[0].reviewer, "koongmai");
    assert.equal(selected[0].proxyReviewId, REPLAY_REVIEW_ID);
});

test("allowlist 밖 marker를 bot 승인의 human proxy로 신뢰하지 않는다", () => {
    const unknownReviewId = 900;
    assert.throws(
        () => selectLatestActiveDecisiveReviewsByReviewer([
            review({
                id: unknownReviewId,
                state: "DISMISSED",
                user: { login: "alice", type: "User" },
            }),
            review({
                id: unknownReviewId + 1,
                state: "APPROVED",
                user: { login: "github-actions[bot]", type: "Bot" },
                body: replayMarker(unknownReviewId),
            }),
        ]),
        /allowlist/,
    );
});

test("이관 승인 원 작성자의 write 권한이 사라지면 failure로 막는다", async () => {
    const original = review({
        id: REPLAY_REVIEW_ID,
        state: "DISMISSED",
        user: { login: "koongmai", type: "User" },
    });
    const proxy = review({
        id: REPLAY_REVIEW_ID + 1,
        state: "APPROVED",
        submitted_at: "2026-08-20T01:00:00Z",
        user: { login: "github-actions[bot]", type: "Bot" },
        body: replayMarker(REPLAY_REVIEW_ID),
    });
    const current = pullRequest({ number: REPLAY_PULL_NUMBER });
    const client = fakeClient(({ apiPath }) => {
        if (apiPath.includes(`/pulls/${REPLAY_PULL_NUMBER}/reviews`)) {
            return response([original, proxy]);
        }
        if (apiPath.includes("/collaborators/koongmai/permission")) {
            return response({ permission: "read" });
        }
        throw new Error(`unexpected ${apiPath}`);
    });

    const result = await evaluatePullRequestOrder({
        client,
        repository: REPOSITORY,
        pullRequest: current,
        openPullRequests: [current],
        reviewCache: new Map(),
    });
    assert.equal(result.state, "failure");
    assert.equal(result.invalidProxyReviewId, REPLAY_REVIEW_ID);
});

test("A의 순서 위반 뒤 B가 정상 승인해도 A의 위반을 failure로 유지한다", async () => {
    const older = pullRequest({ number: 1, user: { login: "bob" } });
    const current = pullRequest({
        number: 30,
        created_at: "2026-08-20T00:00:00Z",
        head: { sha: "3030303030303030303030303030303030303030", repo: { full_name: REPOSITORY } },
    });
    const currentReviews = [
        review({
            id: 10,
            state: "APPROVED",
            submitted_at: "2026-08-20T01:00:00Z",
            user: { login: "alice", type: "User" },
        }),
        review({
            id: 11,
            state: "APPROVED",
            submitted_at: "2026-08-20T02:00:00Z",
            user: { login: "bob", type: "User" },
        }),
    ];
    const client = fakeClient(({ apiPath }) => {
        if (apiPath.includes("/pulls/30/reviews")) return response(currentReviews);
        if (apiPath.includes("/collaborators/alice/permission")) return response({ permission: "write" });
        if (apiPath.includes("/collaborators/bob/permission")) return response({ permission: "write" });
        if (apiPath.includes("/pulls/1/reviews")) return response([]);
        throw new Error(`unexpected ${apiPath}`);
    });
    const result = await evaluatePullRequestOrder({
        client,
        repository: REPOSITORY,
        pullRequest: current,
        openPullRequests: [older, current],
        reviewCache: new Map(),
    });
    assert.equal(result.state, "failure");
    assert.equal(result.oldestDebt.number, 1);
    assert.match(result.reason, /@alice/);
});

test("commit status는 고정 context와 요청 상태를 검증한다", async () => {
    const client = fakeClient(({ apiPath, method, body }) => {
        assert.match(apiPath, /\/statuses\/1{40}$/);
        assert.equal(method, "POST");
        return response({ state: body.state, context: body.context });
    });
    await postOrderStatus({
        client,
        repository: REPOSITORY,
        sha: "1111111111111111111111111111111111111111",
        state: "pending",
        description: "재계산 중",
    });
    assert.equal(client.calls[0].body.context, STATUS_CONTEXT);
});

test("열린 PR 전체를 pending 뒤 success/failure로 재계산하고 리뷰는 쓰지 않는다", async () => {
    const pulls = [
        pullRequest({ number: 1 }),
        pullRequest({
            number: 2,
            created_at: "2026-08-02T00:00:00Z",
            head: { sha: "2222222222222222222222222222222222222222", repo: { full_name: REPOSITORY } },
        }),
    ];
    const client = fakeClient(({ apiPath, method, body }) => {
        if (apiPath.includes("/pulls?state=open")) return response(pulls);
        if (apiPath.includes("/reviews")) return response([]);
        if (apiPath === `/repos/${REPOSITORY}/pulls/1`) return response(pulls[0]);
        if (apiPath === `/repos/${REPOSITORY}/pulls/2`) return response(pulls[1]);
        if (apiPath.includes("/statuses/") && method === "POST") {
            return response({ state: body.state, context: body.context });
        }
        throw new Error(`unexpected ${method} ${apiPath}`);
    });
    const result = await recalculatePullRequests({
        client, repository: REPOSITORY, logger: silent,
    });
    assert.equal(result.results.length, 2);
    const writes = client.calls.filter((call) => call.method !== "GET");
    assert.deepEqual(writes.map((call) => call.body.state), ["pending", "pending", "success", "success"]);
    assert.equal(writes.some((call) => call.method === "PUT"), false);
    assert.equal(writes.some((call) => call.apiPath.includes("/reviews")), false);
});

test("판정 read 실패 시 pending만 남고 final status는 쓰지 않는다", async () => {
    const client = fakeClient(({ apiPath, method, body }) => {
        if (apiPath.includes("/pulls?state=open")) return response([pullRequest()]);
        if (apiPath.includes("/statuses/") && method === "POST") {
            return response({ state: body.state, context: body.context });
        }
        if (apiPath.includes("/reviews")) throw new Error("reviews read failed");
        throw new Error(`unexpected ${method} ${apiPath}`);
    });
    await assert.rejects(
        () => recalculatePullRequests({ client, repository: REPOSITORY, logger: silent }),
        /reviews read failed/,
    );
    const statusWrites = client.calls.filter((call) => call.apiPath.includes("/statuses/"));
    assert.deepEqual(statusWrites.map((call) => call.body.state), ["pending"]);
});

test("strict review_id provenance를 만족한 과거 APPROVED만 bot 승인으로 이관한다", async () => {
    const original = review({
        id: REPLAY_REVIEW_ID,
        state: "DISMISSED",
        submitted_at: "2026-08-02T00:00:00Z",
        user: { login: "koongmai", type: "User" },
    });
    const current = pullRequest({
        number: REPLAY_PULL_NUMBER,
        created_at: "2026-08-20T00:00:00Z",
    });
    const client = fakeClient(({ apiPath, method, body }) => {
        if (apiPath.includes("/pulls?state=open")) return response([current]);
        if (apiPath.includes(`/pulls/${REPLAY_PULL_NUMBER}/reviews`) && method === "GET") {
            return response([original]);
        }
        if (apiPath.includes(`/issues/${REPLAY_PULL_NUMBER}/timeline`)) {
            return response([{
                id: REPLAY_DISMISSAL_EVENT_ID,
                event: "review_dismissed",
                actor: { login: "github-actions[bot]" },
                dismissed_review: {
                    review_id: String(REPLAY_REVIEW_ID),
                    state: "approved",
                    dismissal_message: renderDismissalMessage({
                        number: REPLAY_PREDECESSOR_PULL_NUMBER,
                        title: "먼저 볼 PR",
                    }),
                },
            }]);
        }
        if (apiPath.includes("/collaborators/koongmai/permission")) {
            return response({ permission: "write" });
        }
        if (apiPath.endsWith(`/pulls/${REPLAY_PULL_NUMBER}`) && method === "GET") {
            return response(current);
        }
        if (apiPath.endsWith(`/pulls/${REPLAY_PULL_NUMBER}/reviews`) && method === "POST") {
            return response({
                state: "APPROVED",
                user: { login: "github-actions[bot]" },
                body: body.body,
            });
        }
        throw new Error(`unexpected ${method} ${apiPath}`);
    });
    const result = await replayHistoricalApproval({
        client, repository: REPOSITORY, reviewId: String(REPLAY_REVIEW_ID), logger: silent,
    });
    assert.equal(result.status, "replayed");
    const created = client.calls.find((call) => call.method === "POST");
    assert.equal(created.body.event, "APPROVE");
    assert.match(created.body.body, new RegExp(replayMarker(REPLAY_REVIEW_ID)));
    assert.match(created.body.body, /@koongmai/);
    assert.match(created.body.body, new RegExp(`pullrequestreview-${REPLAY_REVIEW_ID}`));
});

test("CHANGES_REQUESTED dismissal은 historical replay 전에 fail-closed한다", async () => {
    const original = review({
        id: REPLAY_REVIEW_ID,
        state: "DISMISSED",
        user: { login: "koongmai", type: "User" },
    });
    const current = pullRequest({ number: REPLAY_PULL_NUMBER });
    const client = fakeClient(({ apiPath, method }) => {
        if (apiPath.includes("/pulls?state=open")) return response([current]);
        if (apiPath.includes(`/pulls/${REPLAY_PULL_NUMBER}/reviews`) && method === "GET") {
            return response([original]);
        }
        if (apiPath.includes(`/issues/${REPLAY_PULL_NUMBER}/timeline`)) {
            return response([{
                id: REPLAY_DISMISSAL_EVENT_ID,
                event: "review_dismissed",
                actor: { login: "github-actions[bot]" },
                dismissed_review: {
                    review_id: REPLAY_REVIEW_ID,
                    state: "changes_requested",
                    dismissal_message: renderDismissalMessage({
                        number: REPLAY_PREDECESSOR_PULL_NUMBER,
                        title: "먼저 볼 PR",
                    }),
                },
            }]);
        }
        throw new Error(`unexpected ${method} ${apiPath}`);
    });
    await assert.rejects(
        () => replayHistoricalApproval({ client, repository: REPOSITORY, reviewId: REPLAY_REVIEW_ID }),
        /CHANGES_REQUESTED/,
    );
    assert.equal(client.calls.some((call) => call.method === "POST"), false);
});

test("동일 marker가 있어도 dismissal provenance를 먼저 다시 검증한다", async () => {
    const original = review({
        id: REPLAY_REVIEW_ID,
        state: "DISMISSED",
        user: { login: "koongmai", type: "User" },
    });
    const existing = review({
        id: REPLAY_REVIEW_ID + 1,
        state: "APPROVED",
        user: { login: "github-actions[bot]", type: "Bot" },
        body: replayMarker(REPLAY_REVIEW_ID),
    });
    const current = pullRequest({ number: REPLAY_PULL_NUMBER });
    const client = fakeClient(({ apiPath, method }) => {
        if (apiPath.includes("/pulls?state=open")) return response([current]);
        if (apiPath.includes(`/pulls/${REPLAY_PULL_NUMBER}/reviews`) && method === "GET") {
            return response([original, existing]);
        }
        if (apiPath.includes(`/issues/${REPLAY_PULL_NUMBER}/timeline`)) {
            return response([{
                id: REPLAY_DISMISSAL_EVENT_ID,
                event: "review_dismissed",
                actor: { login: "not-the-policy-bot" },
                dismissed_review: {
                    review_id: REPLAY_REVIEW_ID,
                    state: "approved",
                    dismissal_message: renderDismissalMessage({
                        number: REPLAY_PREDECESSOR_PULL_NUMBER,
                        title: "먼저 볼 PR",
                    }),
                },
            }]);
        }
        throw new Error(`unexpected ${method} ${apiPath}`);
    });

    await assert.rejects(
        () => replayHistoricalApproval({
            client,
            repository: REPOSITORY,
            reviewId: REPLAY_REVIEW_ID,
        }),
        /dismissal actor/,
    );
    assert.equal(client.calls.some((call) => call.method === "POST"), false);
});

test("동일 시각의 더 큰 DISMISSED 결정 리뷰가 있으면 오래된 승인을 이관하지 않는다", async () => {
    const submittedAt = "2026-08-02T00:00:00Z";
    const original = review({
        id: REPLAY_REVIEW_ID,
        state: "DISMISSED",
        submitted_at: submittedAt,
        user: { login: "koongmai", type: "User" },
    });
    const newer = review({
        id: REPLAY_REVIEW_ID + 1,
        state: "DISMISSED",
        submitted_at: submittedAt,
        user: { login: "koongmai", type: "User" },
    });
    const current = pullRequest({ number: REPLAY_PULL_NUMBER });
    const client = fakeClient(({ apiPath, method }) => {
        if (apiPath.includes("/pulls?state=open")) return response([current]);
        if (apiPath.includes(`/pulls/${REPLAY_PULL_NUMBER}/reviews`) && method === "GET") {
            return response([original, newer]);
        }
        if (apiPath.includes(`/issues/${REPLAY_PULL_NUMBER}/timeline`)) {
            return response([{
                id: REPLAY_DISMISSAL_EVENT_ID,
                event: "review_dismissed",
                actor: { login: "github-actions[bot]" },
                dismissed_review: {
                    review_id: REPLAY_REVIEW_ID,
                    state: "approved",
                    dismissal_message: renderDismissalMessage({
                        number: REPLAY_PREDECESSOR_PULL_NUMBER,
                        title: "먼저 볼 PR",
                    }),
                },
            }]);
        }
        if (apiPath.includes("/collaborators/koongmai/permission")) {
            return response({ permission: "write" });
        }
        throw new Error(`unexpected ${method} ${apiPath}`);
    });

    await assert.rejects(
        () => replayHistoricalApproval({
            client,
            repository: REPOSITORY,
            reviewId: REPLAY_REVIEW_ID,
        }),
        /더 새로운 결정 리뷰/,
    );
    assert.equal(client.calls.some((call) => call.method === "POST"), false);
});

test("무권한 이벤트 감지와 trusted status/replay workflow를 분리한다", async () => {
    const workflow = await readFile(
        new URL("../workflows/oldest-review-order.yml", import.meta.url),
        "utf8",
    );
    const eventWorkflow = await readFile(
        new URL("../workflows/oldest-review-order-event.yml", import.meta.url),
        "utf8",
    );

    assert.match(eventWorkflow, /^\s{2}pull_request_review:\s*$/m);
    assert.match(eventWorkflow, /types: \[submitted, dismissed\]/);
    assert.match(eventWorkflow, /^\s{2}pull_request:\s*$/m);
    assert.match(eventWorkflow, /^permissions: \{\}\s*$/m);
    assert.doesNotMatch(eventWorkflow, /actions\/checkout|GITHUB_TOKEN|github\.token/);
    assert.match(workflow, /^\s{2}workflow_run:\s*$/m);
    assert.match(workflow, /workflows: \[Oldest Review Order Event\]/);
    assert.doesNotMatch(workflow, /^\s{2}pull_request(?:_review)?:\s*$/m);
    assert.doesNotMatch(workflow, /^\s{2}pull_request_target:\s*$/m);
    assert.match(workflow, /^\s{2}workflow_dispatch:\s*$/m);
    assert.match(workflow, /^\s{6}contents: read\s*$/m);
    assert.match(workflow, /^\s{6}pull-requests: read\s*$/m);
    assert.match(workflow, /^\s{6}statuses: write\s*$/m);
    assert.match(workflow, /^\s{6}pull-requests: write\s*$/m);
    assert.match(workflow, /^\s{4}timeout-minutes: \d+\s*$/m);
    assert.match(workflow, /^\s{10}ref: \$\{\{ github\.event\.repository\.default_branch \}\}\s*$/m);
    assert.match(workflow, /^\s{10}persist-credentials: false\s*$/m);
    assert.match(workflow, /GITHUB_TOKEN: \$\{\{ github\.token \}\}/);
    assert.match(workflow, /GITHUB_REPOSITORY: \$\{\{ github\.repository \}\}/);
    assert.match(workflow, /GITHUB_EVENT_PATH: \$\{\{ github\.event_path \}\}/);
    assert.doesNotMatch(workflow, /node --test/);
});

test("PR 생성 가드도 변경요청자의 명시적 재요청을 확인한다", async () => {
    const workflow = await readFile(
        new URL("../workflows/review-debt-guard.yml", import.meta.url),
        "utf8",
    );

    assert.match(workflow, /requested_reviewers\[\]\.login/);
    assert.match(workflow, /blocked_by/);
    assert.match(workflow, /재리뷰 요청 없음/);
    assert.match(workflow, /sort_by\(\.t\) \| last/);
    assert.doesNotMatch(workflow, /group_by\(\.u/);
});
