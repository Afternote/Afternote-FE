// dependabot PR 이 열릴 때 Repository Quality 게이트를 스스로 지키게 만든다 (#2112).
//
// 봇은 이슈를 만들지 못하고 본문에 CI Test Plan 도 쓰지 않아, 열리는 순간
// Repository Quality 와 기기 잡이 구조적으로 실패한다(#1397·#1699·#2106). 사람이 매번
// 손으로 하던 세 가지 — 대표 이슈 신설, 제목 끝 `(#N)`, 본문의 `Closes #N` + CI Test Plan —
// 를 여기서 한다. 손으로 할 때는 `Refs` 로 걸고 머지 뒤 이슈를 손으로 닫았는데(#1397·#2111), 게이트가
// PR 마다 이슈를 새로 만들므로 `Closes` 로 걸어 머지가 이슈를 닫게 한다(#2274). mode 는 사람이 정하지 않는다. `ci-test-plan.mjs` 가 변경 파일로 판정한
// 결과를 그대로 쓴다. 게이트 면제는 하지 않는다 — 봇 PR 도 워크플로 파일을 건드린다.
//
// 게이트는 GITHUB_TOKEN 으로 제목·본문을 고치므로 그 `edited` 는 다른 워크플로를 깨우지 못한다.
// 같은 순간 열린 PR Validation·Managed Device 는 opened 시점 페이로드를 읽어 red 로 남았다(#2212).
// 게이트가 검증을 다시 부르는 길은 막혀 있다. 같은 sha 에 dispatch 로 붙인 초록은 PR 롤업이 옛
// `pull_request` 실패를 덮지 못하고(#1825), 재실행은 옛 페이로드를 그대로 쓰는 데다 기기 레인의
// 1회차 인프라 복구를 잃으며, App 토큰은 이 브리지에 금지된 시크릿이 필요하다. 그래서 방향을
// 뒤집었다. 검증이 `await` 명령으로 게이트가 채울 때까지 live PR 을 기다렸다가 그 값으로 판정한다.
//
// 순수 함수(needsGate·awaitsGate·buildPlan·buildIssue·applyGate)·대기(awaitGate)와 GitHub 호출
// (fillGate·awaitMain)을 갈라 두어 정책은 dependabot-pr-gate.test.mjs 가 잠근다.

import { readFile, writeFile } from "node:fs/promises";

import { inspectAndroidTestImpact } from "./ci-test-plan.mjs";

export const GATE_MARKER = "<!-- dependabot-pr-gate:v1 -->";
export const DEPENDABOT_LOGIN = "dependabot[bot]";
export const ISSUE_LABELS = ["maintenance", "area:platform"];
export const ISSUE_ASSIGNEES = ["1hyok"];

const TITLE_ISSUE_PATTERN = /\(#(\d+)\)\s*$/;

/** 제목이 `(#N)` 으로 끝나고 본문에 이 게이트의 마커가 있으면 이미 처리된 PR 이다. */
export function needsGate({ title, body }) {
    const titled = TITLE_ISSUE_PATTERN.test(String(title ?? ""));
    const marked = String(body ?? "").includes(GATE_MARKER);
    return !(titled && marked);
}

/**
 * 변경 파일로 CI Test Plan 을 정한다. `ci-test-plan.mjs` 의 판정이 정본이다 —
 * FULL_REQUIRED_PATHS 에 걸리면 full, 아니면 none. selected 는 쓰지 않는다:
 * 봇 범프가 androidTest 소스를 바꾸는 일은 없고, 바꿨다면 사람이 봐야 한다.
 */
export function buildPlan(changedPaths) {
    const impact = inspectAndroidTestImpact(changedPaths);
    if (impact.selected.length > 0 && impact.full.length === 0) {
        throw new Error(
            `dependabot 범프가 androidTest 경계 경로를 건드렸습니다. 사람이 plan 을 정해야 합니다: ${impact.selected.join(", ")}`,
        );
    }
    if (impact.full.length > 0) {
        return {
            mode: "full",
            reason:
                `dependabot 범프가 FULL_REQUIRED_PATHS 에 드는 파일을 바꿨다(${impact.full.join(", ")}). ` +
                "워크플로 자체가 바뀌므로 기기 레인 전량으로 검증한다.",
            requiredPaths: impact.full,
        };
    }
    return {
        mode: "none",
        reason: "dependabot 범프가 바꾼 파일이 전부 androidTest 경계 밖이다. 앱 코드·기기 레인 정의를 건드리지 않는다.",
        requiredPaths: [],
    };
}

/** `await` 가 live PR 을 다시 조회하는 간격. 게이트는 job 시작 뒤 수 초 안에 끝난다(#2138 실측 7초). */
export const AWAIT_POLL_INTERVAL_MS = 5_000;

/**
 * 검증이 아직 기다려야 하는 PR 인가. dependabot 이 연 열린 PR 인데 제목에 대표 이슈 번호가 없을 때만이다.
 * 마커가 아니라 제목으로 가른다. 게이트가 경계 경로에서 멈춰 사람이 손으로 채운 PR 에는 마커가 없는데,
 * 그 PR 까지 edited 마다 제한 시간을 꽉 채워 기다리게 하지 않는다. 게이트는 제목과 본문을 한 번의
 * PATCH 로 바꾸므로 제목에 번호가 있으면 본문도 채워져 있다.
 */
export function awaitsGate(pullRequest) {
    return (
        pullRequest?.user?.login === DEPENDABOT_LOGIN &&
        pullRequest?.state === "open" &&
        !TITLE_ISSUE_PATTERN.test(String(pullRequest?.title ?? ""))
    );
}

/**
 * 게이트가 제목·본문을 채울 때까지 live PR 을 다시 읽는다(#2212). `initial`(이벤트 페이로드의 PR)이 이미
 * 판정할 수 있는 상태면 API 를 부르지 않고 그대로 돌려준다. 게이트가 채운 뒤의 edited·synchronize 는
 * 종전처럼 페이로드만으로 끝난다. 조회가 실패하면 제한 시간 안에서 다시 시도한다. 한 번도 읽지 못한 채
 * 시간이 다하면 마지막 오류를 던지고, 읽은 적이 있으면 마지막 live 상태를 돌려준다. 판정은 호출한
 * 검증의 몫이라, 게이트가 실패했으면 검증이 평소 오류 문구로 실패한다.
 */
export async function awaitGate({ loadPullRequest, initial, timeoutMs, intervalMs = AWAIT_POLL_INTERVAL_MS }) {
    if (initial && !awaitsGate(initial)) {
        return { pullRequest: initial, polls: 0, timedOut: false };
    }
    const deadline = Date.now() + timeoutMs;
    let latest;
    let lastError;
    for (let polls = 1; ; polls += 1) {
        try {
            const pullRequest = await loadPullRequest();
            if (!awaitsGate(pullRequest)) {
                return { pullRequest, polls, timedOut: false };
            }
            latest = pullRequest;
            lastError = undefined;
        } catch (error) {
            lastError = error;
        }
        const remaining = deadline - Date.now();
        if (remaining <= 0) {
            if (latest === undefined) {
                throw lastError;
            }
            return { pullRequest: latest, polls, timedOut: true, lastError };
        }
        await new Promise((resolve) => setTimeout(resolve, Math.min(intervalMs, remaining)));
    }
}

/** 대표 이슈. `.github/ISSUE_TEMPLATE/issue.yml` 양식이라 reconcile-issue-metadata 가 어사인·라벨을 맞춘다. */
export function buildIssue({ title, prNumber, changedPaths, plan }) {
    const summary = String(title ?? "").replace(/^chore\(deps\):\s*/i, "").trim() || `PR #${prNumber}`;
    const files = [...new Set(changedPaths)].sort();
    const fileLines = files.map((filePath) => `- \`${filePath}\``).join("\n");
    const modeLine =
        plan.mode === "full"
            ? `androidTest mode=full 로 검증한다. FULL_REQUIRED_PATHS 에 드는 파일: ${plan.requiredPaths.map((p) => `\`${p}\``).join(", ")}.`
            : "androidTest mode=none 이다. 변경 파일이 전부 androidTest 경계 밖이다.";
    return {
        title: `chore(ci): dependabot 범프 수용: ${summary}`,
        labels: ISSUE_LABELS,
        assignees: ISSUE_ASSIGNEES,
        body: [
            "### 작업 유형",
            "",
            "maintenance — CI·빌드·의존성·저장소 운영 유지보수",
            "",
            "### 주 담당 모듈",
            "",
            "platform — CI·빌드·릴리스·저장소 운영",
            "",
            "### 개요",
            "",
            `dependabot 이 올린 PR #${prNumber}(${summary})의 대표 이슈다. dependabot-pr-gate 워크플로가 만들었다(#2112).`,
            "",
            "Repository Quality 가 모든 PR 에 제목 대표 이슈 번호와 본문 연결을 요구하는데 봇은 이슈를 만들지 못한다. 이 이슈로 연결해 수용하고, PR 본문의 Closes 로 머지 때 함께 닫힌다.",
            "",
            `변경 파일 ${files.length}개:`,
            fileLines,
            "",
            modeLine,
            "",
            "머지 전에 확인할 것:",
            "- 태그 SHA 가 실제 릴리스와 같은지 (`gh api repos/<owner>/<action>/git/ref/tags/<tag>`).",
            "- PR 을 develop 과 합친 트리에 옛 SHA 가 실제 `uses:` 로 남지 않는지. 그룹 범프는 PR 이 열린 뒤 develop 에 들어온 참조를 놓친다.",
            "",
            "### 참고",
            "",
            "선례 #1397 · #2111. 봇 PR 을 게이트에서 면제하지 않는 결정은 #2112.",
        ].join("\n"),
    };
}

/**
 * 제목 끝에 `(#N)`, 본문 맨 위에 `Closes #N` 과 CI Test Plan 을 붙인다. 봇 본문은 그대로 아래 둔다.
 * 게이트가 만든 이슈는 이 PR 하나의 대표 이슈라 머지가 닫게 한다(#2274). 제목에 사람이 이미 붙인
 * 번호를 재사용할 때(`reusedIssue`)는 여러 봇 PR 이 공유하는 이슈일 수 있어 `Refs` 로 둔다(#1748).
 */
export function applyGate({ title, body, issueNumber, plan, reusedIssue = false }) {
    const baseTitle = String(title ?? "").replace(TITLE_ISSUE_PATTERN, "").trim();
    const planJson = JSON.stringify(
        { androidTest: { mode: plan.mode, reason: plan.reason, tests: [] } },
        null,
        2,
    );
    const original = String(body ?? "").replace(GATE_MARKER, "").trim();
    const nextBody = [
        GATE_MARKER,
        `${reusedIssue ? "Refs" : "Closes"} #${issueNumber}`,
        "",
        "## CI Test Plan",
        "",
        "```json",
        planJson,
        "```",
        "",
        "---",
        "",
        original,
    ].join("\n");
    return { title: `${baseTitle} (#${issueNumber})`, body: nextBody };
}

async function github(url, token, init = {}) {
    const response = await fetch(url, {
        ...init,
        headers: {
            Accept: "application/vnd.github+json",
            Authorization: `Bearer ${token}`,
            "X-GitHub-Api-Version": "2022-11-28",
            ...(init.body ? { "Content-Type": "application/json" } : {}),
            ...(init.headers ?? {}),
        },
    });
    if (!response.ok) {
        throw new Error(`${init.method ?? "GET"} ${url} -> ${response.status} ${await response.text()}`);
    }
    return response.json();
}

async function listChangedPaths(apiUrl, repository, prNumber, token) {
    const paths = [];
    for (let page = 1; page <= 30; page += 1) {
        const files = await github(
            `${apiUrl}/repos/${repository}/pulls/${prNumber}/files?per_page=100&page=${page}`,
            token,
        );
        for (const file of files) paths.push(file.filename);
        if (files.length < 100) break;
    }
    return paths;
}

async function fillGate() {
    const token = process.env.GITHUB_TOKEN;
    const repository = process.env.GITHUB_REPOSITORY;
    const prNumber = Number(process.env.PR_NUMBER);
    const apiUrl = process.env.GITHUB_API_URL ?? "https://api.github.com";
    if (!token || !repository || !Number.isInteger(prNumber) || prNumber <= 0) {
        throw new Error("GITHUB_TOKEN, GITHUB_REPOSITORY, PR_NUMBER are required");
    }

    // 이벤트 페이로드가 아니라 live 상태를 정본으로 삼는다. 대기 중 사람이 먼저 고쳤을 수 있다.
    const pullRequest = await github(`${apiUrl}/repos/${repository}/pulls/${prNumber}`, token);
    if (pullRequest.user?.login !== DEPENDABOT_LOGIN) {
        console.log(`#${prNumber} 작성자가 ${pullRequest.user?.login} 이라 건너뜀`);
        return;
    }
    if (pullRequest.state !== "open") {
        console.log(`#${prNumber} 이 ${pullRequest.state} 라 건너뜀`);
        return;
    }
    if (!needsGate(pullRequest)) {
        console.log(`#${prNumber} 은 이미 대표 이슈와 CI Test Plan 이 있음`);
        return;
    }

    const changedPaths = await listChangedPaths(apiUrl, repository, prNumber, token);
    const plan = buildPlan(changedPaths);

    // 제목에 이슈 번호가 이미 있으면 사람이 붙인 것이다(게이트는 제목·본문을 한 번의 PATCH 로 바꾼다). 그 번호를
    // 재사용하고, 여러 봇 PR 이 공유하는 이슈일 수 있어 Refs 로 건다(#1748).
    let issueNumber = Number(TITLE_ISSUE_PATTERN.exec(pullRequest.title ?? "")?.[1]);
    const reusedIssue = Boolean(issueNumber);
    if (!reusedIssue) {
        const issue = buildIssue({ title: pullRequest.title, prNumber, changedPaths, plan });
        const created = await github(`${apiUrl}/repos/${repository}/issues`, token, {
            method: "POST",
            body: JSON.stringify(issue),
        });
        issueNumber = created.number;
        console.log(`대표 이슈 #${issueNumber} 생성`);
    }

    const next = applyGate({ title: pullRequest.title, body: pullRequest.body, issueNumber, plan, reusedIssue });
    await github(`${apiUrl}/repos/${repository}/pulls/${prNumber}`, token, {
        method: "PATCH",
        body: JSON.stringify(next),
    });
    console.log(`#${prNumber} 제목·본문 갱신: ${reusedIssue ? "Refs" : "Closes"} #${issueNumber}, androidTest mode=${plan.mode}`);
}

/**
 * `await <출력 경로>`: dependabot PR 의 `pull_request` 검증이 부른다. 게이트를 기다린 뒤 PR JSON 을 출력
 * 경로에 쓴다. 이벤트 페이로드(GITHUB_EVENT_PATH)의 PR 이 이미 판정할 수 있는 상태면 그것을 그대로 쓴다.
 * 제한 시간을 넘겨도 실패하지 않고 경고만 남긴다. 판정은 뒤따르는 검증이 한다.
 */
async function awaitMain(outputPath) {
    const token = process.env.GITHUB_TOKEN || process.env.GH_TOKEN;
    const repository = process.env.GITHUB_REPOSITORY;
    const prNumber = Number(process.env.PR_NUMBER);
    const timeoutSeconds = Number(process.env.AWAIT_TIMEOUT_SECONDS);
    const apiUrl = process.env.GITHUB_API_URL ?? "https://api.github.com";
    if (!outputPath || !token || !repository || !Number.isInteger(prNumber) || prNumber <= 0) {
        throw new Error("await <출력 경로> 와 GITHUB_TOKEN(또는 GH_TOKEN), GITHUB_REPOSITORY, PR_NUMBER 가 필요합니다");
    }
    if (!Number.isInteger(timeoutSeconds) || timeoutSeconds < 0) {
        throw new Error(`AWAIT_TIMEOUT_SECONDS 는 0 이상의 정수여야 합니다: ${process.env.AWAIT_TIMEOUT_SECONDS}`);
    }

    const eventPullRequest = process.env.GITHUB_EVENT_PATH
        ? JSON.parse(await readFile(process.env.GITHUB_EVENT_PATH, "utf8")).pull_request
        : undefined;
    const { pullRequest, polls, timedOut, lastError } = await awaitGate({
        loadPullRequest: () => github(`${apiUrl}/repos/${repository}/pulls/${prNumber}`, token),
        initial: eventPullRequest?.number === prNumber ? eventPullRequest : undefined,
        timeoutMs: timeoutSeconds * 1000,
    });
    await writeFile(outputPath, JSON.stringify(pullRequest));
    if (timedOut) {
        console.log(
            `::warning::dependabot-pr-gate 가 ${timeoutSeconds}초 안에 #${prNumber} 의 제목·본문을 채우지 않았습니다. ` +
                "그 워크플로 실행을 확인하세요. 지금 live 상태로 판정합니다." +
                (lastError ? ` 마지막 조회 오류: ${lastError.message}` : ""),
        );
    } else if (polls > 1) {
        console.log(`#${prNumber} 게이트가 채운 제목·본문을 ${polls}번째 조회에서 확인`);
    }
}

if (import.meta.url === `file://${process.argv[1]}`) {
    const [command, ...rest] = process.argv.slice(2);
    const run =
        command === undefined
            ? fillGate
            : command === "await"
              ? () => awaitMain(rest[0])
              : () => Promise.reject(new Error(`알 수 없는 명령: ${command}. 인자 없이(채우기) 또는 await 만 받습니다`));
    run().catch((error) => {
        console.error(`::error::${error.message}`);
        process.exitCode = 1;
    });
}
