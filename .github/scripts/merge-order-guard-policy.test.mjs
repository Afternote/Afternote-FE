import assert from "node:assert/strict";
import { mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { readFile } from "node:fs/promises";
import { spawnSync } from "node:child_process";
import { tmpdir } from "node:os";
import { join } from "node:path";
import test from "node:test";

const guard = await readFile(new URL("../workflows/merge-order-guard.yml", import.meta.url), "utf8");

function issueParser() {
    const match = /refs=\$\(\n\s+PR_JSON="\$pr_json" node <<'NODE'\n([\s\S]*?)\n[ \t]*NODE\n\s+\)/.exec(guard);
    assert.ok(match, "merge-order issue parser must stay extractable for policy tests");
    return match[1];
}

// 파서는 번호만 내지 않고 «출처» 를 함께 낸다 — 그 출처가 곧 조회를 한 번 더 할지 말지를
// 가르는 근거다 (#1465). 그래서 판정도 태그째로 본다.
function collectRefs(pr, repository = "Afternote/Afternote-FE") {
    const result = spawnSync(process.execPath, ["-e", issueParser()], {
        encoding: "utf8",
        env: {
            ...process.env,
            GITHUB_REPOSITORY: repository,
            PR_JSON: JSON.stringify(pr),
        },
    });

    assert.equal(result.status, 0, result.stderr);
    return result.stdout.trim().split("\n").filter(Boolean);
}

function collectIssues(pr, repository = "Afternote/Afternote-FE") {
    return collectRefs(pr, repository)
        .map((line) => Number(line.split(" ")[1]))
        .sort((a, b) => a - b);
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

test("linked closing issues are tagged as issues and body-only references stay unresolved", () => {
    // closingIssuesReferences 는 IssueConnection 이라 PR 이 섞일 수 없다. 여기서 온 번호는
    // 종류가 이미 확정이라 다시 묻지 않는다 — 확정되지 않은 것은 본문 파싱으로만 나온 쪽뿐이다.
    const refs = collectRefs({
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

    assert.deepEqual(refs, ["issue 8", "issue 10", "unknown 9", "unknown 12"]);
});

test("the kind probe only runs for references GraphQL could not confirm", () => {
    // 이 조회가 태그와 무관하게 돌면 닫는 이슈 하나마다 잡당 조회가 하나씩 늘고, PR 수 ×
    // 재실행 수만큼 곱해져 installation 한도를 태운다 (#1465).
    assert.match(
        guard,
        /if \[ "\$kind" = "unknown" \]; then\n\s+issue_kind=\$\(gh api "repos\/\$GITHUB_REPOSITORY\/issues\/\$n"/,
    );
});

test("the guard resolves the base branch from the PR query it already makes", () => {
    // base 이름을 REST 로 따로 물으면 한 잡이 같은 PR 을 두 번 조회한다 (#1465).
    assert.match(guard, /pullRequest\(number:\$pr\)\{baseRefName title body closingIssuesReferences/);
    assert.match(guard, /BASE_REF=\$\(jq -r '\.baseRefName' <<< "\$pr_json"\)/);
    assert.doesNotMatch(guard, /gh api "repos\/\$GITHUB_REPOSITORY\/pulls\/\$PR_NUMBER"/);
});

test("the blocked_by loop keeps its verdict in the current shell", () => {
    // 파이프로 먹이면 루프가 서브셸이 되어 fail=1 이 밖으로 나오지 않는다 — 가드가 조용히
    // 초록으로 샌다. 루프 안에서 gh 를 부르므로 목록은 stdin 이 아니라 별도 fd 로 준다.
    assert.match(guard, /while read -r kind n <&3; do/);
    assert.match(guard, /done 3<<< "\$refs"/);
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

test("merge queue groups still produce the guard context", () => {
    // guard 가 merge group 에서 빠지면 required context 가 비어 큐가 멈춘다.
    assert.match(guard, /^\s{2}merge_group:\n\s{4}types: \[checks_requested\]$/m);
    assert.match(guard, /^\s+github\.event_name == 'merge_group'$/m);
    assert.match(guard, /MERGE_GROUP_HEAD_REF: \$\{\{ github\.event\.merge_group\.head_ref \}\}/);
    assert.match(guard, /MERGE_GROUP_BASE_REF: \$\{\{ github\.event\.merge_group\.base_ref \}\}/);
});

test("the queue ref yields the pull request number and base branch", () => {
    // gh-readonly-queue/<base>/pr-<N>-<sha> 에서 번호와 base 를 뽑아 같은 검사를 돌린다.
    const script = /if \[ -z "\$\{PR_NUMBER:-\}" \] && \[ -n "\$\{MERGE_GROUP_HEAD_REF:-\}" \]; then\n([\s\S]*?)\n\s+fi\n/.exec(
        guard,
    );
    assert.ok(script, "queue ref parser must stay extractable for policy tests");

    const result = spawnSync(
        "bash",
        [
            "-c",
            `set -euo pipefail
PR_NUMBER=""
MERGE_GROUP_HEAD_REF="refs/heads/gh-readonly-queue/develop/pr-1477-0123456789abcdef0123456789abcdef01234567"
MERGE_GROUP_BASE_REF="refs/heads/develop"
${script[1]}
printf '%s %s' "$PR_NUMBER" "$BASE_REF"`,
        ],
        { encoding: "utf8" },
    );

    assert.equal(result.status, 0, result.stderr);
    assert.equal(result.stdout, "1477 develop");
});
