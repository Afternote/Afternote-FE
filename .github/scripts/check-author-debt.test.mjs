import assert from "node:assert/strict";
import { mkdtemp, readFile, rm } from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import test from "node:test";

import { renderAuthorDebtTsv, runAuthorDebtCheck } from "./check-author-debt.mjs";

const REPO = "Afternote/Afternote-FE";
const silent = { log() {} };

function changesRequested() {
    return {
        state: "CHANGES_REQUESTED",
        submittedAt: "2026-08-29T00:00:00Z",
        authorCanPushToRepository: true,
        author: { login: "reviewer" },
    };
}

function pullRequest(overrides = {}) {
    return {
        number: 10,
        title: "fix(core): review debt",
        isDraft: false,
        createdAt: "2026-08-01T00:00:00Z",
        author: { login: "author" },
        headRepository: { nameWithOwner: REPO },
        labels: { nodes: [] },
        reviews: { pageInfo: { hasPreviousPage: false }, nodes: [changesRequested()] },
        commits: { pageInfo: { hasPreviousPage: false }, nodes: [] },
        comments: { pageInfo: { hasPreviousPage: false }, nodes: [] },
        userContentEdits: { pageInfo: { hasPreviousPage: false }, nodes: [] },
        ...overrides,
    };
}

function graphqlApi(pullRequests) {
    return async (apiPath, options = {}) => {
        assert.equal(apiPath, "/graphql");
        assert.equal(options.method, "POST");
        assert.equal(
            options.body.variables.searchQuery,
            "repo:Afternote/Afternote-FE is:pr is:open author:author",
        );
        return {
            data: {
                search: {
                    pageInfo: { hasNextPage: false, endCursor: null },
                    nodes: pullRequests,
                },
            },
        };
    };
}

function environment(outputFile, overrides = {}) {
    return {
        GITHUB_TOKEN: "test-token",
        GITHUB_REPOSITORY: REPO,
        AUTHOR: "author",
        CURRENT_PULL_REQUEST_NUMBER: "99",
        AUTHOR_DEBT_FILE: outputFile,
        ...overrides,
    };
}

test("CLI 는 live GraphQL 결과를 판정해 안전한 4열 TSV 를 쓴다", async (t) => {
    const root = await mkdtemp(path.join(os.tmpdir(), "author-debt-"));
    t.after(() => rm(root, { recursive: true, force: true }));
    const outputFile = path.join(root, "debt.tsv");

    const debts = await runAuthorDebtCheck({
        env: environment(outputFile),
        api: graphqlApi([
            pullRequest({ title: "fix(core): tab\tand\nnewline" }),
            pullRequest({ number: 99, title: "현재 PR" }),
        ]),
        logger: silent,
    });

    assert.equal(debts.length, 1);
    assert.equal(
        await readFile(outputFile, "utf8"),
        "10\t2026-08-01\treviewer\tfix(core): tab and newline\n",
    );
});

test("CLI 는 빚이 없어도 빈 결과 파일을 새로 쓴다", async (t) => {
    const root = await mkdtemp(path.join(os.tmpdir(), "author-debt-empty-"));
    t.after(() => rm(root, { recursive: true, force: true }));
    const outputFile = path.join(root, "debt.tsv");

    await runAuthorDebtCheck({
        env: environment(outputFile),
        api: graphqlApi([]),
        logger: silent,
    });

    assert.equal(await readFile(outputFile, "utf8"), "");
});

test("CLI 는 API 실패와 불완전한 GraphQL 응답을 빚 없음으로 통과시키지 않는다", async (t) => {
    const root = await mkdtemp(path.join(os.tmpdir(), "author-debt-fail-"));
    t.after(() => rm(root, { recursive: true, force: true }));
    const outputFile = path.join(root, "debt.tsv");

    await assert.rejects(
        runAuthorDebtCheck({
            env: environment(outputFile),
            api: async () => {
                throw new Error("API unavailable");
            },
            logger: silent,
        }),
        /API unavailable/,
    );

    await assert.rejects(
        runAuthorDebtCheck({
            env: environment(outputFile),
            api: async () => ({ data: { search: null } }),
            logger: silent,
        }),
        /작성자의 열린 PR 검색 결과가 없습니다/,
    );
});

test("TSV 렌더러는 행과 열을 깨는 제어 문자를 제거한다", () => {
    assert.equal(
        renderAuthorDebtTsv([
            { number: 7, createdDate: "2026-08-31", reviewer: "r\t1", title: "a\rb\nc" },
        ]),
        "7\t2026-08-31\tr 1\ta b c\n",
    );
});
