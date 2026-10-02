// dependabot PR 이 열릴 때 Repository Quality 게이트를 스스로 지키게 만든다 (#2112).
//
// 봇은 이슈를 만들지 못하고 본문에 CI Test Plan 도 쓰지 않아, 열리는 순간
// Repository Quality 와 기기 잡이 구조적으로 실패한다(#1397·#1699·#2106). 사람이 매번
// 손으로 하던 세 가지 — 대표 이슈 신설, 제목 끝 `(#N)`, 본문의 `Refs #N` + CI Test Plan —
// 를 여기서 한다. mode 는 사람이 정하지 않는다. `ci-test-plan.mjs` 가 변경 파일로 판정한
// 결과를 그대로 쓴다. 게이트 면제는 하지 않는다 — 봇 PR 도 워크플로 파일을 건드린다.
//
// 순수 함수(needsGate·buildPlan·buildIssue·applyGate)와 GitHub 호출(main)을 갈라 두어
// 정책은 dependabot-pr-gate.test.mjs 가 잠근다.

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
            "Repository Quality 가 모든 PR 에 제목 대표 이슈 번호와 본문 Refs 를 요구하는데 봇은 이슈를 만들지 못한다. 이 이슈로 연결해 수용한다.",
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

/** 제목 끝에 `(#N)`, 본문 맨 위에 `Refs #N` 과 CI Test Plan 을 붙인다. 봇 본문은 그대로 아래 둔다. */
export function applyGate({ title, body, issueNumber, plan }) {
    const baseTitle = String(title ?? "").replace(TITLE_ISSUE_PATTERN, "").trim();
    const planJson = JSON.stringify(
        { androidTest: { mode: plan.mode, reason: plan.reason, tests: [] } },
        null,
        2,
    );
    const original = String(body ?? "").replace(GATE_MARKER, "").trim();
    const nextBody = [
        GATE_MARKER,
        `Refs #${issueNumber}`,
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

async function main() {
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

    // 제목에 이슈 번호가 이미 있으면(사람이 붙였거나 앞선 실행이 본문 갱신 전에 끊겼거나) 재사용한다.
    let issueNumber = Number(TITLE_ISSUE_PATTERN.exec(pullRequest.title ?? "")?.[1]);
    if (!issueNumber) {
        const issue = buildIssue({ title: pullRequest.title, prNumber, changedPaths, plan });
        const created = await github(`${apiUrl}/repos/${repository}/issues`, token, {
            method: "POST",
            body: JSON.stringify(issue),
        });
        issueNumber = created.number;
        console.log(`대표 이슈 #${issueNumber} 생성`);
    }

    const next = applyGate({ title: pullRequest.title, body: pullRequest.body, issueNumber, plan });
    await github(`${apiUrl}/repos/${repository}/pulls/${prNumber}`, token, {
        method: "PATCH",
        body: JSON.stringify(next),
    });
    console.log(`#${prNumber} 제목·본문 갱신: Refs #${issueNumber}, androidTest mode=${plan.mode}`);
}

if (import.meta.url === `file://${process.argv[1]}`) {
    main().catch((error) => {
        console.error(`::error::${error.message}`);
        process.exitCode = 1;
    });
}
