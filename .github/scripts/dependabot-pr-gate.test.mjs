import assert from "node:assert/strict";
import { execFile } from "node:child_process";
import { mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { createServer } from "node:http";
import { tmpdir } from "node:os";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";
import { promisify } from "node:util";

import { inspectCiTestPlan } from "./ci-test-plan.mjs";
import {
    DEPENDABOT_LOGIN,
    GATE_MARKER,
    ISSUE_ASSIGNEES,
    ISSUE_LABELS,
    applyGate,
    awaitGate,
    awaitsGate,
    buildIssue,
    buildPlan,
    needsGate,
} from "./dependabot-pr-gate.mjs";

const GATE_SCRIPT = fileURLToPath(new URL("./dependabot-pr-gate.mjs", import.meta.url));
const escapeRegExp = (value) => value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");

const BOT_TITLE = "chore(deps): bump the github-actions-updates group with 3 updates";
const BOT_BODY = "Bumps the github-actions-updates group with 3 updates: actions/setup-java ...";

test("처리되지 않은 봇 PR 만 게이트 대상이다", () => {
    assert.equal(needsGate({ title: BOT_TITLE, body: BOT_BODY }), true);
    assert.equal(needsGate({ title: `${BOT_TITLE} (#2111)`, body: BOT_BODY }), true, "제목만 고친 것은 본문이 아직 비었다");
    assert.equal(needsGate({ title: BOT_TITLE, body: `${GATE_MARKER}\n${BOT_BODY}` }), true, "본문만 고친 것은 제목이 아직 비었다");
    assert.equal(needsGate({ title: `${BOT_TITLE} (#2111)`, body: `${GATE_MARKER}\n${BOT_BODY}` }), false);
    assert.equal(needsGate({ title: null, body: null }), true);
});

test("mode 는 ci-test-plan 의 판정을 그대로 따른다: 기기 레인 정의를 건드리면 full, 아니면 none", () => {
    const full = buildPlan([".github/workflows/android-managed-device.yml", ".github/workflows/lint.yml"]);
    assert.equal(full.mode, "full");
    assert.deepEqual(full.requiredPaths, [".github/workflows/android-managed-device.yml"]);
    assert.match(full.reason, /android-managed-device\.yml/);

    const none = buildPlan([".github/workflows/codeql.yml", ".github/workflows/lint.yml"]);
    assert.equal(none.mode, "none");
    assert.deepEqual(none.requiredPaths, []);

    const gradle = buildPlan(["gradle/libs.versions.toml"]);
    assert.equal(gradle.mode, "full", "gradle/ 접두사도 full 이다");
});

test("봇 범프가 androidTest 경계 경로를 건드리면 자동으로 정하지 않고 실패한다", () => {
    assert.throws(
        () => buildPlan(["app/src/main/AndroidManifest.xml"]),
        /사람이 plan 을 정해야 합니다/,
    );
});

test("대표 이슈는 issue.yml 양식이고 platform maintenance 로 1hyok 에게 간다", () => {
    const plan = buildPlan([".github/workflows/codeql.yml"]);
    const issue = buildIssue({ title: BOT_TITLE, prNumber: 2106, changedPaths: [".github/workflows/codeql.yml"], plan });

    assert.equal(issue.title, "chore(ci): dependabot 범프 수용: bump the github-actions-updates group with 3 updates");
    assert.deepEqual(issue.labels, ISSUE_LABELS);
    assert.deepEqual(issue.assignees, ISSUE_ASSIGNEES);
    assert.deepEqual(ISSUE_LABELS, ["maintenance", "area:platform"]);
    assert.deepEqual(ISSUE_ASSIGNEES, ["1hyok"]);
    assert.match(issue.body, /^### 작업 유형\n\nmaintenance — /);
    assert.match(issue.body, /### 주 담당 모듈\n\nplatform — /);
    assert.match(issue.body, /PR #2106/);
    assert.match(issue.body, /- `\.github\/workflows\/codeql\.yml`/);
    assert.match(issue.body, /mode=none/);
    assert.match(issue.body, /### 참고/);
});

test("제목은 (#N) 으로 정확히 한 번 끝나고 본문은 Refs 와 유효한 CI Test Plan 으로 시작한다", () => {
    const plan = buildPlan([".github/workflows/android-managed-device.yml"]);
    const next = applyGate({ title: BOT_TITLE, body: BOT_BODY, issueNumber: 2111, plan });

    assert.equal(next.title, `${BOT_TITLE} (#2111)`);
    assert.equal((next.title.match(/\(#\d+\)/g) ?? []).length, 1);
    assert.ok(next.body.startsWith(`${GATE_MARKER}\nRefs #2111\n`));
    assert.ok(next.body.endsWith(BOT_BODY), "봇 본문은 그대로 아래에 남는다");
    assert.equal(needsGate(next), false, "한 번 적용한 결과는 다시 대상이 아니다");

    const inspected = inspectCiTestPlan(next.body, { pullRequestNumber: 2106 });
    assert.deepEqual(inspected.errors ?? [], [], JSON.stringify(inspected));
    assert.equal(inspected.plan?.androidTest?.mode ?? inspected.androidTest?.mode, "full");
});

test("이미 (#N) 이 붙은 제목에 다시 적용해도 번호가 겹치지 않는다", () => {
    const plan = buildPlan([".github/workflows/codeql.yml"]);
    const once = applyGate({ title: BOT_TITLE, body: BOT_BODY, issueNumber: 2111, plan });
    const twice = applyGate({ title: once.title, body: once.body, issueNumber: 2111, plan });
    assert.equal(twice.title, once.title);
    assert.equal((twice.body.match(/Refs #2111/g) ?? []).length, 2, "본문 재적용은 앞선 머리말을 봇 본문으로 취급한다");
    assert.equal((twice.body.match(new RegExp(GATE_MARKER.replace(/[-[\]{}()*+?.,\\^$|#\s]/g, "\\$&"), "g")) ?? []).length, 1);
});

test("워크플로는 봇 PR 에만 돌고 PR 코드를 체크아웃하지 않는다", async () => {
    const source = await readFile(new URL("../workflows/dependabot-pr-gate.yml", import.meta.url), "utf8");

    assert.match(source, /pull_request_target:/);
    assert.match(source, new RegExp(`if: github\\.event\\.pull_request\\.user\\.login == '${DEPENDABOT_LOGIN.replace(/[[\]]/g, "\\$&")}'`));
    assert.match(source, /ref: \$\{\{ github\.event\.repository\.default_branch \}\}/);
    assert.doesNotMatch(source, /pull_request\.head/, "PR 쪽 ref 를 참조하지 않는다");
    assert.match(source, /persist-credentials: false/);
    assert.match(source, /sparse-checkout: \.github\/scripts/);
    assert.match(source, /timeout-minutes: 5/);
    assert.match(source, /^permissions: \{\}/m);
    assert.match(source, /issues: write/);
    assert.match(source, /pull-requests: write/);
    assert.match(source, /run: node \.github\/scripts\/dependabot-pr-gate\.mjs$/m, "인자 없이 불러야 채우기다");
});

const FILLED = {
    user: { login: DEPENDABOT_LOGIN },
    state: "open",
    title: `${BOT_TITLE} (#2139)`,
    body: `${GATE_MARKER}\nRefs #2139\n\n${BOT_BODY}`,
    head: { sha: "a".repeat(40) },
    changed_files: 3,
};
const UNFILLED = { ...FILLED, title: BOT_TITLE, body: BOT_BODY };

function sequence(...pullRequests) {
    let calls = 0;
    return {
        load: async () => pullRequests[Math.min(calls++, pullRequests.length - 1)],
        calls: () => calls,
    };
}

test("await 는 게이트가 채운 뒤의 live PR 을 돌려준다 (#2212)", async () => {
    const live = sequence(UNFILLED, UNFILLED, FILLED);
    const result = await awaitGate({ loadPullRequest: live.load, timeoutMs: 5_000, intervalMs: 5 });

    assert.equal(result.timedOut, false);
    assert.equal(result.polls, 3);
    assert.equal(result.pullRequest, FILLED);
    assert.equal(needsGate(result.pullRequest), false);
});

test("await 는 기다릴 이유가 없으면 첫 조회로 끝난다", async () => {
    const handFilled = { ...UNFILLED, title: `${BOT_TITLE} (#2139)`, body: "Refs #2139\n\n## CI Test Plan\n..." };
    assert.equal(needsGate(handFilled), true, "마커가 없어 게이트는 다시 채우려 한다");
    assert.equal(awaitsGate(handFilled), false, "검증은 사람이 채운 PR 을 기다리지 않는다");
    for (const pullRequest of [
        FILLED,
        handFilled,
        { ...UNFILLED, user: { login: "1hyok" } },
        { ...UNFILLED, state: "closed" },
    ]) {
        const live = sequence(pullRequest);
        const result = await awaitGate({ loadPullRequest: live.load, timeoutMs: 5_000, intervalMs: 5 });
        assert.equal(live.calls(), 1, JSON.stringify(pullRequest.user));
        assert.equal(result.timedOut, false);
        assert.equal(result.pullRequest, pullRequest);
    }
});

test("await 는 페이로드가 이미 판정할 수 있으면 API 를 부르지 않는다", async () => {
    const live = sequence(UNFILLED);
    const ready = await awaitGate({ loadPullRequest: live.load, initial: FILLED, timeoutMs: 5_000, intervalMs: 5 });
    assert.equal(live.calls(), 0, "게이트가 채운 뒤의 edited·synchronize 는 종전처럼 페이로드로 끝난다");
    assert.equal(ready.pullRequest, FILLED);
    assert.equal(ready.polls, 0);

    const stale = sequence(UNFILLED, FILLED);
    const waited = await awaitGate({ loadPullRequest: stale.load, initial: UNFILLED, timeoutMs: 5_000, intervalMs: 5 });
    assert.equal(stale.calls(), 2, "opened 페이로드는 옛 값이라 live 를 기다린다");
    assert.equal(waited.pullRequest, FILLED);
});

test("await 는 조회 오류를 제한 시간 안에서 다시 시도하고, 한 번도 못 읽으면 오류를 던진다", async () => {
    let calls = 0;
    const flaky = async () => {
        calls += 1;
        if (calls === 1) throw new Error("GET pulls -> 502");
        return calls === 2 ? UNFILLED : FILLED;
    };
    const recovered = await awaitGate({ loadPullRequest: flaky, timeoutMs: 5_000, intervalMs: 5 });
    assert.equal(recovered.timedOut, false);
    assert.equal(recovered.pullRequest, FILLED);
    assert.equal(calls, 3);

    const down = async () => {
        throw new Error("GET pulls -> 403 rate limit");
    };
    await assert.rejects(awaitGate({ loadPullRequest: down, timeoutMs: 30, intervalMs: 5 }), /403 rate limit/);

    let readOnce = 0;
    const thenDown = async () => {
        readOnce += 1;
        if (readOnce === 1) return UNFILLED;
        throw new Error("GET pulls -> 502");
    };
    const partial = await awaitGate({ loadPullRequest: thenDown, timeoutMs: 30, intervalMs: 5 });
    assert.equal(partial.timedOut, true);
    assert.equal(partial.pullRequest, UNFILLED, "읽은 적이 있으면 마지막 live 상태를 넘긴다");
    assert.match(partial.lastError.message, /502/);
});

test("await 는 제한 시간이 지나면 기다림을 끝내고 마지막 live 상태를 넘긴다", async () => {
    const live = sequence(UNFILLED);
    const started = Date.now();
    const result = await awaitGate({ loadPullRequest: live.load, timeoutMs: 60, intervalMs: 10 });

    assert.equal(result.timedOut, true);
    assert.equal(result.pullRequest, UNFILLED);
    assert.ok(live.calls() >= 2, "제한 시간 안에서는 다시 조회한다");
    assert.ok(Date.now() - started < 1_000, "무기한 대기하지 않는다");

    const once = sequence(UNFILLED);
    const immediate = await awaitGate({ loadPullRequest: once.load, timeoutMs: 0, intervalMs: 10 });
    assert.equal(immediate.timedOut, true);
    assert.equal(once.calls(), 1);
});

async function runGateCli(args, { env = {}, pullRequest } = {}) {
    const requests = [];
    const server = createServer((request, response) => {
        requests.push(`${request.method} ${request.url}`);
        response.setHeader("Content-Type", "application/json");
        response.end(JSON.stringify(pullRequest ?? {}));
    });
    await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
    const { port } = server.address();
    try {
        const result = await promisify(execFile)(process.execPath, [GATE_SCRIPT, ...args], {
            env: {
                PATH: process.env.PATH,
                GITHUB_API_URL: `http://127.0.0.1:${port}`,
                GITHUB_REPOSITORY: "Afternote/Afternote-FE",
                ...env,
            },
        }).then(
            ({ stdout, stderr }) => ({ code: 0, stdout, stderr }),
            (error) => ({ code: error.code, stdout: error.stdout, stderr: error.stderr }),
        );
        return { ...result, requests };
    } finally {
        server.close();
    }
}

test("await 명령은 live PR JSON 을 파일에 쓰고, 제한 시간을 넘겨도 판정을 검증에 넘긴다", async () => {
    const directory = await mkdtemp(path.join(tmpdir(), "dependabot-pr-gate-"));
    try {
        const filledPath = path.join(directory, "filled.json");
        const filled = await runGateCli(["await", filledPath], {
            env: { GH_TOKEN: "t", PR_NUMBER: "2138", AWAIT_TIMEOUT_SECONDS: "90" },
            pullRequest: FILLED,
        });
        assert.equal(filled.code, 0, filled.stderr);
        assert.deepEqual(filled.requests, ["GET /repos/Afternote/Afternote-FE/pulls/2138"]);
        assert.deepEqual(JSON.parse(await readFile(filledPath, "utf8")), FILLED);

        const unfilledPath = path.join(directory, "unfilled.json");
        const unfilled = await runGateCli(["await", unfilledPath], {
            env: { GH_TOKEN: "t", PR_NUMBER: "2138", AWAIT_TIMEOUT_SECONDS: "0" },
            pullRequest: UNFILLED,
        });
        assert.equal(unfilled.code, 0, "시간 초과는 실패가 아니다. 검증이 평소 오류 문구로 판정한다");
        assert.match(unfilled.stdout, /::warning::dependabot-pr-gate 가 0초 안에 #2138/);
        assert.deepEqual(JSON.parse(await readFile(unfilledPath, "utf8")), UNFILLED);

        const eventPath = path.join(directory, "event.json");
        await writeFile(eventPath, JSON.stringify({ action: "synchronize", pull_request: { ...FILLED, number: 2138 } }));
        const payloadPath = path.join(directory, "payload.json");
        const fromPayload = await runGateCli(["await", payloadPath], {
            env: { GH_TOKEN: "t", PR_NUMBER: "2138", AWAIT_TIMEOUT_SECONDS: "90", GITHUB_EVENT_PATH: eventPath },
            pullRequest: UNFILLED,
        });
        assert.equal(fromPayload.code, 0, fromPayload.stderr);
        assert.deepEqual(fromPayload.requests, [], "채워진 페이로드면 API 를 부르지 않는다");
        assert.deepEqual(JSON.parse(await readFile(payloadPath, "utf8")), { ...FILLED, number: 2138 });

        const otherPath = path.join(directory, "other.json");
        const otherPullRequest = await runGateCli(["await", otherPath], {
            env: { GH_TOKEN: "t", PR_NUMBER: "2140", AWAIT_TIMEOUT_SECONDS: "0", GITHUB_EVENT_PATH: eventPath },
            pullRequest: FILLED,
        });
        assert.equal(otherPullRequest.code, 0, otherPullRequest.stderr);
        assert.deepEqual(otherPullRequest.requests, ["GET /repos/Afternote/Afternote-FE/pulls/2140"], "다른 PR 의 페이로드는 쓰지 않는다");

        const missing = await runGateCli(["await", path.join(directory, "x.json")], {
            env: { GH_TOKEN: "t", PR_NUMBER: "2138" },
        });
        assert.equal(missing.code, 1);
        assert.match(missing.stderr, /AWAIT_TIMEOUT_SECONDS/);
        assert.deepEqual(missing.requests, [], "입력이 틀리면 API 를 부르지 않는다");
    } finally {
        await rm(directory, { recursive: true, force: true });
    }
});

test("인자 없이 부르면 채우기다: live PR 을 읽고 봇 PR 이 아니면 아무것도 쓰지 않는다", async () => {
    const result = await runGateCli([], {
        env: { GITHUB_TOKEN: "t", PR_NUMBER: "2138" },
        pullRequest: { user: { login: "1hyok" }, state: "open", title: "x", body: "" },
    });
    assert.equal(result.code, 0, result.stderr);
    assert.deepEqual(result.requests, ["GET /repos/Afternote/Afternote-FE/pulls/2138"]);
    assert.match(result.stdout, /작성자가 1hyok 이라 건너뜀/);
});

test("모르는 명령은 채우기로 떨어지지 않는다", async () => {
    const result = await runGateCli(["wait"], { env: { GITHUB_TOKEN: "t", PR_NUMBER: "2138" } });
    assert.equal(result.code, 1);
    assert.match(result.stderr, /알 수 없는 명령: wait/);
    assert.deepEqual(result.requests, [], "이슈 생성·PR 편집 요청이 나가지 않는다");
});

test("게이트는 검증을 다시 부르지 않고, 두 검증이 dependabot PR 에서 게이트를 기다린다 (#2212)", async () => {
    const readWorkflow = (name) => readFile(new URL(`../workflows/${name}`, import.meta.url), "utf8");
    const gate = await readWorkflow("dependabot-pr-gate.yml");
    const repositoryQuality = await readWorkflow("repository-quality.yml");
    const managedDevice = await readWorkflow("android-managed-device.yml");
    const login = escapeRegExp(DEPENDABOT_LOGIN);

    // 같은 sha 의 dispatch 는 PR 롤업의 옛 실패를 못 덮고(#1825), 재실행은 옛 페이로드를 쓴다.
    assert.doesNotMatch(gate, /^\s+actions:/m, "게이트에 actions 권한을 주지 않는다");

    // Repository Quality: pull_request 이면서 봇이 연 PR 만 live PR 을 기다렸다가 그 값으로 판정한다.
    assert.match(repositoryQuality, /PR_AUTHOR: \$\{\{ github\.event\.pull_request\.user\.login \}\}/);
    assert.match(
        repositoryQuality,
        new RegExp(
            `elif \\[ "\\$GITHUB_EVENT_NAME" = "pull_request" \\] && \\[ "\\$PR_AUTHOR" = "${login}" \\]; then\\n` +
                `(?:\\s+#.*\\n)*` +
                `\\s+AWAIT_TIMEOUT_SECONDS=(\\d+) node \\.github/scripts/dependabot-pr-gate\\.mjs await "\\$pull_request_file"\\n` +
                `\\s+CHANGED_FILES=\\$\\(jq -r '\\.changed_files' "\\$pull_request_file"\\)`,
        ),
    );
    const awaitSeconds = Number(repositoryQuality.match(/AWAIT_TIMEOUT_SECONDS=(\d+) node/)[1]);
    const quotaSeconds = Number(repositoryQuality.match(/ensure-api-quota\.mjs ensure --max-wait (\d+)/)[1]);
    const jobMinutes = Number(repositoryQuality.match(/name: Repository Quality\n\s+runs-on: .+\n\s+timeout-minutes: (\d+)/)[1]);
    assert.ok(
        awaitSeconds + quotaSeconds + 30 <= jobMinutes * 60,
        `게이트 대기 ${awaitSeconds}s + quota 대기 ${quotaSeconds}s 가 job timeout ${jobMinutes}분을 넘기면 판정 전에 취소된다`,
    );

    // Managed Device: 판정 재료는 신뢰 정책 사본으로 기다린 live PR 이다. PR 사본 스크립트를 부르지 않는다.
    assert.match(
        managedDevice,
        /install -m 0644 \\\n\s+\.github\/scripts\/dependabot-pr-gate\.mjs \\\n\s+"\$policy_dir\/dependabot-pr-gate\.mjs"\n\s+dependabot_gate=trusted/,
    );
    // 신뢰 사본에 대기 명령이 있는지를 이 문자열로 가른다. 스크립트가 선언 모양을 바꾸면 기기 레인
    // 대기가 조용히 꺼지므로, grep 문자열이 스크립트에 실제로 있는지 대조한다.
    const probe = managedDevice.match(/grep -q '([^']+)' \.github\/scripts\/dependabot-pr-gate\.mjs/)?.[1];
    assert.ok(probe, "스테이징 판별 grep 이 없다");
    assert.ok((await readFile(GATE_SCRIPT, "utf8")).includes(probe), `dependabot-pr-gate.mjs 에 '${probe}' 가 없다`);
    assert.equal((managedDevice.match(/dependabot_gate=unavailable/g) ?? []).length, 2, "도입 PR·bootstrap 은 페이로드를 읽는다");
    assert.match(managedDevice, /EVENT_PR_AUTHOR: \$\{\{ github\.event\.pull_request\.user\.login \}\}/);
    assert.match(managedDevice, /DEPENDABOT_GATE: \$\{\{ steps\.policy\.outputs\.dependabot_gate \}\}/);
    assert.match(
        managedDevice,
        new RegExp(`if \\[\\[ "\\$EVENT_PR_AUTHOR" == "${login}" && "\\$DEPENDABOT_GATE" == "trusted" \\]\\]; then`),
    );
    assert.match(
        managedDevice,
        /AWAIT_TIMEOUT_SECONDS=\d+ \\\n\s+node "\$RUNNER_TEMP\/android-test-policy\/dependabot-pr-gate\.mjs" await "\$pull_request_file"/,
    );
    assert.match(managedDevice, /if \[\[ "\$live_head_sha" != "\$EVENT_PR_SHA" \]\]; then/);
    assert.doesNotMatch(managedDevice, /node \.github\/scripts\/dependabot-pr-gate\.mjs/);
});
