import assert from "node:assert/strict";
import { mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { readFile } from "node:fs/promises";
import { spawnSync } from "node:child_process";
import { tmpdir } from "node:os";
import { join } from "node:path";
import test from "node:test";

const guard = await readFile(new URL("../workflows/merge-order-guard.yml", import.meta.url), "utf8");

function issueParser() {
    const match = /issues=\$\(\n\s+PR_JSON="\$pr_json" node <<'NODE'\n([\s\S]*?)\n[ \t]*NODE\n\s+\)/.exec(guard);
    assert.ok(match, "merge-order issue parser must stay extractable for policy tests");
    return match[1];
}

function collectIssues(pr, repository = "Afternote/Afternote-FE") {
    const result = spawnSync(process.execPath, ["-e", issueParser()], {
        encoding: "utf8",
        env: {
            ...process.env,
            GITHUB_REPOSITORY: repository,
            PR_JSON: JSON.stringify(pr),
        },
    });

    assert.equal(result.status, 0, result.stderr);
    return result.stdout.trim().split("\n").filter(Boolean).map(Number);
}

function refreshParser() {
    const match = /heads=\$\(\n\s+BLOCKED="\$blocked" PRS_FILE="\$prs_file" node <<'NODE'\n([\s\S]*?)\n[ \t]*NODE\n\s+\)/.exec(
        guard,
    );
    assert.ok(match, "merge-order refresh parser must stay extractable for policy tests");
    return match[1];
}

function collectRefreshHeads(prs, blocked, repository = "Afternote/Afternote-FE") {
    const directory = mkdtempSync(join(tmpdir(), "merge-order-refresh-"));
    const prsFile = join(directory, "open-prs.json");
    writeFileSync(
        prsFile,
        JSON.stringify([
            {
                data: {
                    repository: {
                        pullRequests: { nodes: prs },
                    },
                },
            },
        ]),
    );
    try {
        const result = spawnSync(process.execPath, ["-e", refreshParser()], {
            encoding: "utf8",
            env: {
                ...process.env,
                BLOCKED: blocked.join("\n"),
                GITHUB_REPOSITORY: repository,
                PRS_FILE: prsFile,
            },
        });

        assert.equal(result.status, 0, result.stderr);
        return result.stdout.trim().split("\n").filter(Boolean);
    } finally {
        rmSync(directory, { recursive: true, force: true });
    }
}

test("closing references and same-repository close keywords are inspected as a union", () => {
    const issues = collectIssues({
        title: "fix: resolves #9 and Fixes afternote/afternote-fe#10",
        body: "Closes other/repo#11\n- fixed #12\nmentions #13 only\nClosed #12",
        closingIssuesReferences: {
            nodes: [
                { number: 8, repository: { nameWithOwner: "Afternote/Afternote-FE" } },
                { number: 10, repository: { nameWithOwner: "AFTERNOTE/afternote-fe" } },
                { number: 11, repository: { nameWithOwner: "other/repo" } },
            ],
        },
    });

    assert.deepEqual(issues, [8, 9, 10, 12]);
});

test("an empty GraphQL closing list still keeps title and body references", () => {
    const issues = collectIssues({
        title: "[fix] #99 unrelated title number",
        body: "Resolves #1176",
        closingIssuesReferences: { nodes: [] },
    });

    assert.deepEqual(issues, [1176]);
});

test("the guard fetches PR text and filters pull-request numbers before dependency lookup", () => {
    assert.match(guard, /closingIssuesReferences\(first:20\)\{nodes\{number repository\{nameWithOwner\}\}\}/);
    assert.match(guard, /if has\("pull_request"\) then "pull_request" else "issue" end/);
    assert.match(guard, /if \[ "\$issue_kind" = "pull_request" \]; then[\s\S]*?continue/);
});

test("closing a blocker refreshes PRs found only through unlinked close keywords", () => {
    const heads = collectRefreshHeads(
        [
            {
                headRefName: "linked",
                title: "fix: linked",
                body: "",
                closingIssuesReferences: {
                    nodes: [
                        { number: 1176, repository: { nameWithOwner: "Afternote/Afternote-FE" } },
                    ],
                },
            },
            {
                headRefName: "text-only",
                title: "fix: text only",
                body: "Closes #1176",
                closingIssuesReferences: { nodes: [] },
            },
            {
                headRefName: "foreign",
                title: "fix: foreign",
                body: "Closes other/repo#1176",
                closingIssuesReferences: {
                    nodes: [{ number: 1176, repository: { nameWithOwner: "other/repo" } }],
                },
            },
            {
                headRefName: "mention-only",
                title: "docs: mention #1176",
                body: "Related to #1176",
                closingIssuesReferences: { nodes: [] },
            },
        ],
        [1176],
    );

    assert.deepEqual(heads, ["linked", "text-only"]);
});

test("refresh queries every open PR page with the same repository-aware closing data", () => {
    assert.match(guard, /gh api graphql --paginate --slurp/);
    assert.match(guard, /pullRequests\(states:OPEN,first:100,after:\$endCursor\)/);
    assert.match(guard, /nodes\{headRefName title body closingIssuesReferences/);
    assert.match(guard, /pageInfo\{hasNextPage endCursor\}/);
});

test("refresh passes large open-PR payloads through a file instead of one environment value", () => {
    const heads = collectRefreshHeads(
        [
            {
                headRefName: "large-body",
                title: "fix: large body",
                body: `Closes #1176\n${"x".repeat(160 * 1024)}`,
                closingIssuesReferences: { nodes: [] },
            },
        ],
        [1176],
    );

    assert.deepEqual(heads, ["large-body"]);
    assert.match(guard, /> "\$prs_file"/);
    assert.doesNotMatch(guard, /PRS_JSON=/);
});

function stackRegistrationBlock() {
    const match = /# 조회 자체는 `if` 조건에 두어[\s\S]*?\n {10}fi\n/.exec(guard);
    assert.ok(match, "stack registration warning block must stay extractable for policy tests");
    return match[0];
}

test("stack registration is inspected through stackEntry, not the base branch name alone", () => {
    const block = stackRegistrationBlock();

    assert.match(block, /pullRequest\(number:\$pr\)\{stackEntry\{position\}\}/);
    // base 이름 판정(stacked)과 등록 판정을 겹쳐 둔다 — 트렁크 base 인 PR 은 조회하지 않는다.
    assert.match(block, /if \[ "\$stacked" -eq 1 \]; then/);
});

test("an unregistered stack PR is only warned about, never failed", () => {
    const block = stackRegistrationBlock();

    // CI 가 고칠 수 없는 신호다 — gh stack 링크는 대화형 Ctrl+B 뿐이라 error 로 올리면
    // #1059 가 걷어낸 «수명 내내 red» 가 되돌아온다.
    assert.match(block, /echo "::warning::네이티브 스택 미등록/);
    assert.doesNotMatch(block, /::error::/);
    assert.doesNotMatch(block, /fail=1/);
});

test("a failed registration lookup degrades to a warning instead of killing the blocked_by verdict", () => {
    const block = stackRegistrationBlock();

    // 조회를 `if` 조건에 두면 set -e 가 스크립트를 끊지 않는다.
    assert.match(block, /if stack_entry=\$\(gh api graphql/);
    assert.match(block, /else\n\s+echo "::warning::스택 등록 여부를 조회하지 못했다/);
});

test("API failures remain fail-closed", () => {
    assert.match(guard, /set -euo pipefail/);
    assert.doesNotMatch(guard, /issue_kind=\$\(gh api[^\n]*\|\|\s*true/);
    assert.doesNotMatch(guard, /open_blockers=\$\(gh api[^\n]*\|\|\s*true/);
});
