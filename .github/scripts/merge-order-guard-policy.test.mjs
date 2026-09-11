import assert from "node:assert/strict";
import { existsSync, mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { readFile } from "node:fs/promises";
import { spawnSync } from "node:child_process";
import { tmpdir } from "node:os";
import { join } from "node:path";
import test from "node:test";

const guard = await readFile(new URL("../workflows/merge-order-guard.yml", import.meta.url), "utf8");
const stackNotify = await readFile(new URL("../workflows/stack-integrity-notify.yml", import.meta.url), "utf8");
const codeowners = await readFile(new URL("../CODEOWNERS", import.meta.url), "utf8");

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
                number: 101,
                title: "fix: linked",
                body: "",
                closingIssuesReferences: {
                    nodes: [
                        { number: 1176, repository: { nameWithOwner: "Afternote/Afternote-FE" } },
                    ],
                },
            },
            {
                number: 102,
                title: "fix: text only",
                body: "Closes #1176",
                closingIssuesReferences: { nodes: [] },
            },
            {
                number: 103,
                title: "fix: foreign",
                body: "Closes other/repo#1176",
                closingIssuesReferences: {
                    nodes: [{ number: 1176, repository: { nameWithOwner: "other/repo" } }],
                },
            },
            {
                number: 104,
                title: "docs: mention #1176",
                body: "Related to #1176",
                closingIssuesReferences: { nodes: [] },
            },
        ],
        [1176],
    );

    assert.deepEqual(heads, ["101", "102"]);
});

test("refresh queries every open PR page with the same repository-aware closing data", () => {
    assert.match(guard, /gh api graphql --paginate --slurp/);
    assert.match(guard, /pullRequests\(states:OPEN,first:100,after:\$endCursor\)/);
    assert.match(guard, /nodes\{number headRefOid title body closingIssuesReferences/);
    assert.match(guard, /pageInfo\{hasNextPage endCursor\}/);
});

test("refresh passes large open-PR payloads through a file instead of one environment value", () => {
    const heads = collectRefreshHeads(
        [
            {
                number: 105,
                title: "fix: large body",
                body: `Closes #1176\n${"x".repeat(160 * 1024)}`,
                closingIssuesReferences: { nodes: [] },
            },
        ],
        [1176],
    );

    assert.deepEqual(heads, ["105"]);
    assert.match(guard, /> "\$prs_file"/);
    assert.doesNotMatch(guard, /PRS_JSON=/);
});

test("merge-group guard queries complete native-stack membership and fails closed", () => {
    assert.match(
        guard,
        /pullRequest\(number:\$pr\)\{number state mergedAt stackEntry\{position\} stack\{number size entries\(first:100\)/,
    );
    assert.match(
        guard,
        /if \[ "\$EVENT_NAME" = "merge_group" \][\s\S]*node \.github\/scripts\/merge-order-stack-integrity\.mjs blockers <<< "\$stack_pr_json"/,
    );
    assert.match(guard, /echo "::error::네이티브 스택 #\$stack_number/);
    assert.match(guard, /done 4<<< "\$stack_blockers"/);
});

test("ordinary PR guard stays non-stale while merge queue performs the live verdict", () => {
    assert.match(guard, /^\s{2}pull_request_target:\n\s{4}types: \[opened, reopened, synchronize, edited\]$/m);
    assert.doesNotMatch(guard, /statuses: write/);
    const start = guard.indexOf('if [ "$EVENT_NAME" = "merge_group" ]');
    const end = guard.indexOf("          # GitHub 가 close keyword", start);
    assert.ok(start >= 0 && end > start, "merge-group-only stack verdict must stay extractable");
    const stackVerdict = guard.slice(start, end);
    assert.match(stackVerdict, /merge-order-stack-integrity\.mjs blockers/);
    assert.doesNotMatch(guard.slice(0, start), /stack_blockers=/);
    assert.match(guard, /네이티브 스택 미등록/);
    assert.match(guard, /if stack_entry=\$\(gh api graphql/);
    assert.match(guard, /else\n\s+echo "::warning::스택 등록 여부를 조회하지 못했다/);
});

test("close notifier executes only trusted default-branch policy", () => {
    assert.match(stackNotify, /^\s{2}pull_request_target:\n\s{4}types: \[closed, reopened\]$/m);
    assert.doesNotMatch(stackNotify, /github\.event\.pull_request\.merged == false/);
    assert.match(stackNotify, /ref: \$\{\{ github\.event\.repository\.default_branch \}\}/);
    assert.match(stackNotify, /persist-credentials: false/);
    assert.match(stackNotify, /^permissions: \{\}$/m);
    assert.match(stackNotify, /group: stack-integrity-notify-\$\{\{ github\.repository_id \}\}-\$\{\{ github\.event\.pull_request\.number \}\}/);
    assert.match(stackNotify, /cancel-in-progress: false/);
    assert.doesNotMatch(stackNotify, /github\.event\.pull_request\.head\.(sha|ref)/);
    assert.doesNotMatch(stackNotify, /statuses: write|contents: write|actions: write/);
    assert.doesNotMatch(stackNotify, /actions\/checkout[\s\S]*github\.event\.pull_request\.head/);
});

test("declares the repository code owner for GitHub automation and policy changes", () => {
    assert.match(codeowners, /^\/\.github\/ @1hyok$/m);
});

test("live CLOSED warns upper PRs while OPEN or MERGED resolves the same bot comment", () => {
    assert.match(stackNotify, /issues: write/);
    assert.match(stackNotify, /live_state=\$\(jq -r '\.state'/);
    assert.match(stackNotify, /CLOSED\)[\s\S]*notice_state="active"/);
    assert.match(stackNotify, /OPEN\)[\s\S]*notice_state="resolved"/);
    assert.match(stackNotify, /MERGED\)[\s\S]*notice_state="resolved"/);
    assert.doesNotMatch(stackNotify, /PR_ACTION/);
    assert.match(stackNotify, /merge-order-stack-integrity\.mjs open-above <<< "\$closed_pr_json"/);
    assert.match(stackNotify, /stack-integrity:closed-unmerged-\$CLOSED_PR/);
    assert.match(stackNotify, /issues\/\$target\/comments/);
    assert.match(stackNotify, /--method PATCH/);
    assert.match(stackNotify, /스택 연결 조치 안내 해소/);
    assert.match(stackNotify, /gh stack unstack \$stack_number/);
    assert.match(stackNotify, /gh stack link --base develop/);
    assert.match(stackNotify, /unstack\/relink 자체에는 Actions 이벤트가 없어/);
    assert.doesNotMatch(stackNotify, /^\s+gh stack (unstack|link)/m);
});

test("API failures remain fail-closed", () => {
    assert.match(guard, /set -euo pipefail/);
    assert.doesNotMatch(guard, /issue_kind=\$\(gh api[^\n]*\|\|\s*true/);
    assert.doesNotMatch(guard, /open_blockers=\$\(gh api[^\n]*\|\|\s*true/);
    assert.doesNotMatch(guard, /stack_pr_json=\$\(gh api[^\n]*\|\|\s*true/);
    assert.match(stackNotify, /set -euo pipefail/);
    assert.doesNotMatch(stackNotify, /closed_pr_json=\$\(gh api[^\n]*\|\|\s*true/);
});

test("merge queue groups still produce the guard context", () => {
    // guard 가 merge group 에서 빠지면 required context 가 비어 큐가 멈춘다.
    assert.match(guard, /^\s{2}merge_group:\n\s{4}types: \[checks_requested\]$/m);
    assert.match(guard, /github\.event_name == 'merge_group'/);
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

function shellStep(name) {
    const start = guard.indexOf(`      - name: ${name}\n`);
    assert.ok(start >= 0, name);
    const run = guard.indexOf("        run: |\n", start) + "        run: |\n".length;
    const lines = guard.slice(run).split("\n");
    const body = [];
    for (const line of lines) {
        if (line && !line.startsWith("          ")) break;
        body.push(line.slice(10));
    }
    return body.join("\n");
}

function runGuard({ base = "feature/parent", blockers = [], apiFailure = false, event = "pull_request" } = {}) {
    const script = shellStep("머지 큐 스택 무결성·closing 이슈 blocked_by 검사");
    const fake = `gh() {
        if [[ "$*" == *stackEntry* ]]; then printf '%s' '{"stackEntry":null}';
        elif [[ "$*" == *graphql* ]]; then printf '%s' "$FIXTURE_PR";
        elif [[ "$*" == *dependencies/blocked_by* ]]; then
            [ "$FIXTURE_API_FAILURE" != "true" ] || return 42
            printf '%s' "$FIXTURE_OPEN_BLOCKERS"
        else echo "Unexpected gh call: $*" >&2; return 99; fi
    }
`;
    return spawnSync("bash", ["-c", fake + script], {
        encoding: "utf8",
        env: { ...process.env, EVENT_NAME: event, PR_NUMBER: "1930", BASE_REF: base,
            GITHUB_REPOSITORY: "Afternote/Afternote-FE", FIXTURE_API_FAILURE: String(apiFailure),
            FIXTURE_OPEN_BLOCKERS: blockers.filter((b) => b.state === "open").map((b) => b.number).join("\n"),
            FIXTURE_PR: JSON.stringify({ baseRefName: base, title: "fix: guard", body: "",
                closingIssuesReferences: { nodes: [{ number: 1920, repository: { nameWithOwner: "Afternote/Afternote-FE" } }] } }) },
    });
}

for (const base of ["develop", "main", "feature/parent"]) {
    test(`open blockers fail the executable guard for ${base}`, () => {
        const result = runGuard({ base, blockers: [{ number: 708, state: "open" }] });
        assert.equal(result.status, 1, result.stderr);
        assert.match(result.stdout, /::error::#1920.*#708/);
    });
    test(`closed or absent blockers pass the executable guard for ${base}`, () => {
        for (const blockers of [[], [{ number: 708, state: "closed" }]]) {
            const result = runGuard({ base, blockers });
            assert.equal(result.status, 0, result.stderr);
            assert.doesNotMatch(result.stdout, /::error::/);
        }
    });
}

test("dependency API failure cannot become a green stacked guard", () => {
    const result = runGuard({ apiFailure: true });
    assert.equal(result.status, 42, result.stderr);
});

test("merge queue still fails on an open issue blocker", () => {
    const result = runGuard({ event: "merge_group", base: "develop", blockers: [{ number: 708, state: "open" }] });
    assert.equal(result.status, 1, result.stderr);
});

test("blocker changes rerun the trusted pull_request_target run and dispatch only when none exists (#1983)", () => {
    // #1955 는 rerun 이 PR 사본의 옛 워크플로 코드를 돌린다는 이유로 dispatch 재게시로 바꿨다.
    // #1977 뒤 PR HEAD 의 run 은 base YAML·default-branch 스크립트로 도는 run 이라 재실행이 곧
    // 최신 판정이고, check-run 재게시가 임의 suite 에 얹혀 무관한 워크플로 이름 아래 guard 가
    // 또 뜨는 일을 피한다. rerun 대상은 pull_request_target run 으로만 좁힌다.
    assert.match(guard, /gh run list --workflow merge-order-guard.yml --event pull_request_target \\\n\s+--commit "\$sha" --status completed --limit 1/);
    assert.match(guard, /\[ -n "\$run_id" \] && gh run rerun "\$run_id"/);
    assert.doesNotMatch(guard, /gh run list --workflow merge-order-guard.yml --branch/);
    assert.match(guard, /gh workflow run merge-order-guard.yml --ref "\$DEFAULT_BRANCH" -f pull_request_number="\$pr"/);
    assert.match(guard, /github.ref_name == github.event.repository.default_branch/);
    assert.match(guard, /needs.guard.outputs.target_sha/);
    // HEAD SHA 는 이미 받은 GraphQL 페이지에서 읽는다 — PR 마다 REST 왕복을 더하지 않는다 (#1465).
    assert.match(guard, /nodes\{number headRefOid title body closingIssuesReferences/);
    assert.doesNotMatch(guard.slice(guard.indexOf("dispatch_failed=0")), /gh pr view/);
});

test("dispatch publication refuses changed or closed HEAD and publishes failure on the captured HEAD", () => {
    const script = shellStep("Publish trusted guard verdict on the captured PR HEAD");
    const directory = mkdtempSync(join(tmpdir(), "merge-order-publish-"));
    try {
        for (const [state, head, result, expected] of [
            ["OPEN", "original", "failure", "failure"],
            ["OPEN", "original", "success", "success"],
            ["OPEN", "original", "cancelled", "failure"],
            ["OPEN", "changed", "success", null],
            ["CLOSED", "original", "success", null],
        ]) {
            const run = spawnSync("bash", ["-c", `gh() {
                if [ "$1" = "pr" ]; then printf '%s' "$FIXTURE_CURRENT";
                elif [ "$1" = "api" ]; then cat "$RUNNER_TEMP/guard-check.json" >&2;
                else return 99; fi
            }
` + script], { encoding: "utf8", env: { ...process.env,
                RUNNER_TEMP: directory, PR_NUMBER: "1930", TARGET_SHA: "original", GUARD_RESULT: result,
                GITHUB_SERVER_URL: "https://github.com", GITHUB_REPOSITORY: "Afternote/Afternote-FE", GITHUB_RUN_ID: "123",
                FIXTURE_CURRENT: JSON.stringify({ state, headRefOid: head }) } });
            assert.equal(run.status, 0, run.stderr);
            if (expected === null) assert.equal(run.stderr, "");
            else {
                const check = JSON.parse(run.stderr);
                assert.equal(check.name, "guard");
                assert.equal(check.head_sha, "original");
                assert.equal(check.conclusion, expected);
            }
        }
    } finally { rmSync(directory, { recursive: true, force: true }); }
});


function runRefreshLoop(ghMock, prs) {
    const start = guard.indexOf("          dispatch_failed=0");
    assert.ok(start >= 0);
    const script = guard.slice(start).split("\n").map((line) => line.slice(10)).join("\n");
    const directory = mkdtempSync(join(tmpdir(), "merge-order-refresh-loop-"));
    const prsFile = join(directory, "open-prs.json");
    writeFileSync(prsFile, JSON.stringify([{ data: { repository: { pullRequests: { nodes: prs } } } }]));
    try {
        return spawnSync("bash", ["-c", `set -euo pipefail
heads="${prs.map((pr) => pr.number).join(" ")}"
DEFAULT_BRANCH=develop
prs_file="${prsFile}"
${ghMock}
` + script], { encoding: "utf8" });
    } finally {
        rmSync(directory, { recursive: true, force: true });
    }
}

test("one failed refresh dispatch does not prevent the remaining PRs from refreshing", () => {
    const result = runRefreshLoop(`gh() {
  [ "$1 $2" != "run list" ] || return 0
  echo "$*"; [[ "$*" != *pull_request_number=101* ]]; }`,
    [{ number: 101, headRefOid: "aaa" }, { number: 102, headRefOid: "bbb" }]);
    assert.equal(result.status, 1, result.stderr);
    assert.match(result.stdout, /pull_request_number=101/);
    assert.match(result.stdout, /pull_request_number=102/);
});

test("refresh reruns the existing pull_request_target run in place instead of publishing a new check-run", () => {
    const result = runRefreshLoop(`gh() {
  case "$1 $2" in
    "run list") [[ "$*" == *"--event pull_request_target"* && "$*" == *"--commit aaa"* ]] && echo 555; return 0 ;;
    "run rerun") echo "rerun $3"; return 0 ;;
    *) echo "$*"; return 0 ;;
  esac }`,
    [{ number: 101, headRefOid: "aaa" }]);
    assert.equal(result.status, 0, result.stderr);
    assert.match(result.stdout, /^rerun 555$/m);
    assert.doesNotMatch(result.stdout, /workflow run/);
});

test("refresh falls back to dispatch when no pull_request_target run exists or the rerun is refused", () => {
    const result = runRefreshLoop(`gh() {
  case "$1 $2" in
    "run list") [[ "$*" == *"--commit aaa"* ]] && echo 555; return 0 ;;
    "run rerun") echo "rerun refused" >&2; return 1 ;;
    *) echo "$*"; return 0 ;;
  esac }`,
    [{ number: 101, headRefOid: "aaa" }, { number: 102, headRefOid: "bbb" }, { number: 103 }]);
    assert.equal(result.status, 0, result.stderr);
    assert.match(result.stdout, /pull_request_number=101/);
    assert.match(result.stdout, /pull_request_number=102/);
    assert.match(result.stdout, /pull_request_number=103/);
});

test("PR HEAD guard runs default-branch policy once, so no second verdict is republished (#1977)", () => {
    // pull_request 로 돌면 PR 사본 YAML 이 판정해 낡은 브랜치는 옛 정책이고 PR 이 가드를 고칠 수
    // 있다. 종전 처방(inline 종료 → 최신 정책 dispatch → check-run 재게시)은 GITHUB_TOKEN 의
    // check-run 이 임의 suite 에 얹혀 무관한 워크플로 이름 아래 guard 가 두 줄로 떴다.
    assert.doesNotMatch(guard, /^\s{2}pull_request:$/m);
    assert.doesNotMatch(guard, /github\.event_name == 'pull_request'/);
    assert.match(guard, /github\.event_name == 'pull_request_target'/);
    assert.doesNotMatch(guard, /github\.event\.pull_request\.head\.(sha|ref)/);
    assert.doesNotMatch(guard, /actions\/checkout[\s\S]*?ref: \$\{\{ github\.event\.pull_request/);
    assert.match(guard, /ref: \$\{\{ github\.event\.repository\.default_branch \}\}/);
    assert.ok(
        !existsSync(new URL("../workflows/refresh-merge-order-policy.yml", import.meta.url)),
        "inline 종료 뒤 최신 정책을 다시 dispatch 하는 refresh 워크플로는 pull_request_target 이 대체했다",
    );
    // dispatch 재게시는 이벤트가 나지 않는 SHA(토큰 커밋·선행 이슈 닫힘) 전용으로만 남는다.
    assert.match(guard, /github\.event_name == 'workflow_dispatch' &&\n\s+github\.ref_name == github\.event\.repository\.default_branch/);
});