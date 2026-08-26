import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

import {
    analyzeDecisiveReviews,
    classifyRepositoryPermission,
    classifyReviewEvent,
    enforceOldestReviewOrder,
    hasSubstantiveCommitAfter,
    nextPageUrl,
    paginateRest,
    renderDismissalMessage,
    reviewDebtStatus,
    selectRequestedOutstandingReview,
    selectOlderPullRequests,
} from "./enforce-oldest-review-order.mjs";

const REPOSITORY = "afternote/app";
const silent = { log() {} };

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
        draft: false,
        requested_reviewers: [],
        user: { login: "author" },
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

test("리뷰어별 최신 결정으로 미해소 CHANGES_REQUESTED를 찾는다", () => {
    const resolved = analyzeDecisiveReviews([
        review({ state: "CHANGES_REQUESTED", submitted_at: "2026-08-01T00:00:00Z" }),
        review({ state: "APPROVED", submitted_at: "2026-08-02T00:00:00Z" }),
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

    assert.equal(selectRequestedOutstandingReview(reviewState, ["ALICE"]).reviewer, "alice");
    assert.equal(selectRequestedOutstandingReview(reviewState, ["carol"]), null);
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

test("더 오래된 실제 리뷰 빚을 찾아 현재 리뷰를 dismiss한다", async () => {
    const client = fakeClient(({ apiPath, method }) => {
        if (apiPath.endsWith("/collaborators/reviewer/permission")) {
            return response({ permission: "write" });
        }
        if (apiPath.includes("/pulls?state=open")) {
            // 응답 순서가 섞여 있어도 #1부터 검사한다. #1은 승인돼 있고 #2가 첫 빚이다.
            return response([
                pullRequest({ number: 2, title: "두 번째 오래된 미처리", created_at: "2026-08-02T00:00:00Z" }),
                pullRequest({ number: 1, title: "가장 오래됐지만 승인됨", created_at: "2026-08-01T00:00:00Z" }),
            ]);
        }
        if (apiPath.includes("/pulls/1/reviews")) {
            return response([review({ state: "APPROVED" })]);
        }
        if (apiPath.includes("/pulls/2/reviews")) {
            return response([]);
        }
        if (apiPath.endsWith("/pulls/30/reviews/900/dismissals") && method === "PUT") {
            return response({ state: "DISMISSED" });
        }
        throw new Error(`unexpected ${method} ${apiPath}`);
    });

    const result = await enforceOldestReviewOrder({
        event: reviewEvent(),
        repository: REPOSITORY,
        client,
        logger: silent,
    });

    assert.equal(result.status, "dismissed");
    assert.equal(result.oldestDebt.number, 2);
    const dismissal = client.calls.at(-1);
    assert.equal(dismissal.method, "PUT");
    assert.equal(dismissal.body.event, "DISMISS");
    assert.match(dismissal.body.message, /#2 \(두 번째 오래된 미처리\)/);
});

test("CHANGES_REQUESTED 뒤 같은 리뷰어에게 재요청하고 반영했으면 리뷰 빚으로 dismiss한다", async () => {
    const client = fakeClient(({ apiPath, method }) => {
        if (apiPath.includes("/permission")) return response({ permission: "maintain" });
        if (apiPath.includes("/pulls?state=open")) {
            return response([pullRequest({
                number: 4,
                requested_reviewers: [{ login: "alice" }],
            })]);
        }
        if (apiPath.includes("/pulls/4/reviews")) {
            return response([review({ state: "CHANGES_REQUESTED" })]);
        }
        if (apiPath.includes("/pulls/4/commits")) return response([commit()]);
        if (method === "PUT") return response({ state: "dismissed" });
        throw new Error(`unexpected ${method} ${apiPath}`);
    });

    const result = await enforceOldestReviewOrder({
        event: reviewEvent(), repository: REPOSITORY, client, logger: silent,
    });
    assert.equal(result.status, "dismissed");
    assert.equal(result.oldestDebt.debtReason, "changes-requested-fixed-rerequested");
});

test("변경요청 뒤 수정했어도 재리뷰 요청이 없으면 허용한다", async () => {
    const client = fakeClient(({ apiPath }) => {
        if (apiPath.includes("/permission")) return response({ permission: "maintain" });
        if (apiPath.includes("/pulls?state=open")) {
            return response([pullRequest({
                number: 966,
                requested_reviewers: [{ login: "koongmai" }],
            })]);
        }
        if (apiPath.includes("/pulls/966/reviews")) {
            return response([review({ state: "CHANGES_REQUESTED", user: { login: "1hyok" } })]);
        }
        throw new Error(`재요청이 없는데 불필요한 API를 호출함: ${apiPath}`);
    });

    const result = await enforceOldestReviewOrder({
        event: reviewEvent(), repository: REPOSITORY, client, logger: silent,
    });
    assert.equal(result.status, "allowed");
    assert.equal(client.calls.some((call) => call.apiPath.includes("/commits")), false);
    assert.equal(client.calls.some((call) => call.method !== "GET"), false);
});

test("변경요청 뒤 반영 커밋이 없거나 이미 승인된 PR만 있으면 허용한다", async () => {
    const client = fakeClient(({ apiPath }) => {
        if (apiPath.includes("/permission")) return response({ permission: "admin" });
        if (apiPath.includes("/pulls?state=open")) {
            return response([
                pullRequest({ number: 1, requested_reviewers: [{ login: "alice" }] }),
                pullRequest({ number: 2 }),
            ]);
        }
        if (apiPath.includes("/pulls/1/reviews")) {
            return response([review({ state: "CHANGES_REQUESTED" })]);
        }
        if (apiPath.includes("/pulls/1/commits")) {
            return response([
                commit({ commit: { committer: { date: "2026-08-01T00:00:00Z" } } }),
            ]);
        }
        if (apiPath.includes("/pulls/2/reviews")) return response([review()]);
        throw new Error(`unexpected ${apiPath}`);
    });

    const result = await enforceOldestReviewOrder({
        event: reviewEvent(), repository: REPOSITORY, client, logger: silent,
    });
    assert.equal(result.status, "allowed");
    assert.equal(client.calls.some((call) => call.method === "PUT"), false);
});

test("dry run은 순서 위반을 끝까지 계산하되 리뷰를 dismiss하지 않는다", async () => {
    const lines = [];
    const client = fakeClient(({ apiPath }) => {
        if (apiPath.includes("/permission")) return response({ permission: "write" });
        if (apiPath.includes("/pulls?state=open")) {
            return response([pullRequest({ number: 6, title: "먼저 리뷰할 PR" })]);
        }
        if (apiPath.includes("/pulls/6/reviews")) return response([]);
        throw new Error(`dry run에서 쓰기 API를 호출함: ${apiPath}`);
    });

    const result = await enforceOldestReviewOrder({
        event: reviewEvent(),
        repository: REPOSITORY,
        client,
        dryRun: true,
        logger: { log: (line) => lines.push(line) },
    });

    assert.equal(result.status, "dry-run");
    assert.equal(result.oldestDebt.number, 6);
    assert.match(lines[0], /\[dry-run\].*#6/);
    assert.equal(client.calls.some((call) => call.method !== "GET"), false);
});

test("dismiss API가 200이어도 상태가 DISMISSED가 아니면 성공으로 보고하지 않는다", async () => {
    const client = fakeClient(({ apiPath, method }) => {
        if (apiPath.includes("/permission")) return response({ permission: "write" });
        if (apiPath.includes("/pulls?state=open")) return response([pullRequest({ number: 6 })]);
        if (apiPath.includes("/pulls/6/reviews")) return response([]);
        if (method === "PUT") return response({ state: "APPROVED" });
        throw new Error(`unexpected ${method} ${apiPath}`);
    });

    await assert.rejects(
        () => enforceOldestReviewOrder({
            event: reviewEvent(), repository: REPOSITORY, client, logger: silent,
        }),
        /상태가 DISMISSED가 아닙니다/,
    );
});

test("쓰기 권한이 없는 리뷰어는 열린 PR 목록도 조회하지 않는다", async () => {
    const client = fakeClient(({ apiPath }) => {
        assert.match(apiPath, /\/permission$/);
        return response({ permission: "read" });
    });

    const result = await enforceOldestReviewOrder({
        event: reviewEvent(), repository: REPOSITORY, client, logger: silent,
    });
    assert.equal(result.status, "skipped");
    assert.equal(client.calls.length, 1);
});

test("비결정 리뷰와 fork는 GitHub API를 전혀 호출하지 않는다", async () => {
    const client = fakeClient(() => {
        throw new Error("API를 호출하면 안 됨");
    });
    const commented = reviewEvent({
        review: { id: 1, state: "commented", user: { login: "reviewer" } },
    });
    assert.equal((await enforceOldestReviewOrder({
        event: commented, repository: REPOSITORY, client, logger: silent,
    })).status, "skipped");

    const forkPayload = reviewEvent();
    forkPayload.pull_request.head.repo.full_name = "outside/fork";
    assert.equal((await enforceOldestReviewOrder({
        event: forkPayload, repository: REPOSITORY, client, logger: silent,
    })).status, "skipped");
    assert.deepEqual(client.calls, []);
});

test("열린 PR, 리뷰, 커밋 조회 실패 시 dismiss 쓰기가 발생하지 않는다", async (t) => {
    const stages = ["open", "reviews", "commits"];
    for (const failingStage of stages) {
        await t.test(failingStage, async () => {
            const client = fakeClient(({ apiPath }) => {
                if (apiPath.includes("/permission")) return response({ permission: "write" });
                if (apiPath.includes("/pulls?state=open")) {
                    if (failingStage === "open") throw new Error("open read failed");
                    return response([pullRequest({
                        number: 1,
                        requested_reviewers: [{ login: "alice" }],
                    })]);
                }
                if (apiPath.includes("/pulls/1/reviews")) {
                    if (failingStage === "reviews") throw new Error("reviews read failed");
                    return response([review({ state: "CHANGES_REQUESTED" })]);
                }
                if (apiPath.includes("/pulls/1/commits")) {
                    if (failingStage === "commits") throw new Error("commits read failed");
                    return response([commit()]);
                }
                throw new Error(`unexpected ${apiPath}`);
            });

            await assert.rejects(
                () => enforceOldestReviewOrder({
                    event: reviewEvent(), repository: REPOSITORY, client, logger: silent,
                }),
                /read failed/,
            );
            assert.equal(client.calls.some((call) => call.method === "PUT"), false);
        });
    }
});

test("dismiss 안내는 가장 오래된 PR 번호와 한 줄 제목을 명시한다", () => {
    const message = renderDismissalMessage({
        number: 77,
        title: "여러 줄\n제목",
    });
    assert.match(message, /#77 \(여러 줄 제목\)/);
    assert.match(message, /자동 취소/);
});

test("workflow는 trusted default branch에서만 write-token 스크립트를 실행한다", async () => {
    const workflow = await readFile(
        new URL("../workflows/oldest-review-order.yml", import.meta.url),
        "utf8",
    );

    assert.match(workflow, /^\s{2}pull_request_review:\s*$/m);
    assert.match(workflow, /^\s{4}types: \[submitted\]\s*$/m);
    assert.match(workflow, /^\s{2}contents: read\s*$/m);
    assert.match(workflow, /^\s{2}pull-requests: write\s*$/m);
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
});
