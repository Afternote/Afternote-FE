import assert from "node:assert/strict";
import fs from "node:fs/promises";
import test from "node:test";

import {
    buildQaAuditInput,
    buildQaAuditPrompt,
    finalizeDeploymentDecision,
    inspectQaMetadata,
    isGenericQaText,
    validateQaAuditPlan,
} from "./qa-semantic-audit.mjs";
import {
    QA_METADATA_GATE_CUTOFF,
    validatePullRequestEvent,
} from "./validate-pr-qa-metadata.mjs";

function qaBody(metadata) {
    return `## QA 메타데이터
\`\`\`json
${JSON.stringify(metadata, null, 2)}
\`\`\``;
}

function issueEvidence(issueNumber) {
    return {
        kind: "issue",
        ref: `#${issueNumber}`,
        assertion: "재현 조건과 관찰 가능한 기대 결과를 정의한다",
    };
}

function runtimeMetadata(issueNumber, overrides = {}) {
    return {
        scope: "app-runtime",
        precondition: `이슈 #${issueNumber}의 재현 데이터가 준비된 로그인 상태`,
        action: `이슈 #${issueNumber}의 사용자 흐름을 명시된 순서로 수행한다`,
        expected: `이슈 #${issueNumber}에 정의된 화면 상태와 저장 결과가 표시된다`,
        risk: `사용자가 이슈 #${issueNumber}의 기능을 완료할 수 없다`,
        evidence: [issueEvidence(issueNumber)],
        ...overrides,
    };
}

function exclusionMetadata(issueNumber) {
    return {
        scope: "ci-only",
        exclusionReason: "GitHub Actions 제어 변경으로 APK 사용자 흐름이 존재하지 않는다",
        evidence: [
            {
                kind: "ci",
                ref: "Unit Test / Run deployment script tests",
                assertion: "같은 스크립트 입력과 종료 상태를 CI에서 검증한다",
                input: `이슈 #${issueNumber}에 해당하는 workflow fixture`,
                boundary: "GitHub Actions 스크립트의 파싱과 결과 검증 경계",
                observation: "node test가 기대 JSON과 종료 코드를 단언한다",
            },
        ],
    };
}

function pullRequest({ number, title, mergeCommitSha, issueNumbers, changedFiles, metadata }) {
    return {
        number,
        title,
        body: qaBody(metadata),
        url: `https://github.com/Afternote/Afternote-FE/pull/${number}`,
        mergeCommitSha,
        changedFiles,
        closingIssues: issueNumbers.map((issueNumber) => ({
            number: issueNumber,
            title: `이슈 ${issueNumber}`,
            body: `이슈 ${issueNumber}의 재현 절차와 기대 결과`,
            url: `https://github.com/Afternote/Afternote-FE/issues/${issueNumber}`,
            labels: ["bug"],
        })),
    };
}

function fixture802() {
    const issueNumbers = [550, 563, 565, 569, 602, 617, 675, 680, 699, 706, 796, 801];
    const runtimeSources = [
        [682, [675], "refactor(afternote): 에디터 폼 상태 재설계", "e635dd56575efbbe2cc036963bd53e4cbdf0f0e4", "feature/afternote/presentation/src/main/kotlin/com/afternote/feature/afternote/presentation/author/editor/state/AfternoteEditorState.kt"],
        [754, [617], "fix(afternote): 수신 목록 서버 서비스명 표시", "e3cbee2327901525c8d7baaf81679b44ddde18e8", "feature/afternote/data/src/main/kotlin/com/afternote/feature/afternote/data/dto/ReceiverAfternoteDto.kt"],
        [774, [680], "fix(afternote): 갤러리 수신자 선택 경로 통일", "cf9625f5c225d699ee4c638dcc77f672c7ed04e0", "feature/afternote/presentation/src/main/kotlin/com/afternote/feature/afternote/presentation/author/editor/receiver/AddAfternoteEditorReceiverDialog.kt"],
        [776, [602, 706], "fix(afternote): 에디터 검증 안내 소비 신호 도입", "62d5935c614ba2ac00a015e182a1769fd4f275bc", "feature/afternote/presentation/src/main/kotlin/com/afternote/feature/afternote/presentation/author/editor/AfternoteEditorViewModel.kt"],
        [720, [699], "fix(mindrecord): 일기 목록 DTO 와이어 키 수정", "ed2a3c7f282b2acd8e501348f758d07a92559ce3", "feature/mindrecord/data/src/main/kotlin/com/afternote/feature/mindrecord/data/dto/DiaryDto.kt"],
        [718, [563], "fix(mindrecord): 주간리포트 캘린더 일자 매칭", "dc79b1dd8038c960897a3030e653a253faea3ee6", "feature/mindrecord/presentation/src/main/kotlin/com/afternote/feature/mindrecord/presentation/viewmodel/WeeklyReportRecordedDays.kt"],
        [719, [565], "fix(mindrecord): 데일리질문 저장 무음 실패 해소", "95cf0e23751e7e37af136dde45225689e9596e30", "feature/mindrecord/presentation/src/main/kotlin/com/afternote/feature/mindrecord/presentation/viewmodel/DailyQuestionWriteViewModel.kt"],
        [755, [569], "fix(timeletter): 수신인 왕복 시 작성 내용 보존", "5b473918bdc889749a18b5e7b7c08096c1768ab3", "feature/timeletter/presentation/src/main/kotlin/com/afternote/feature/timeletter/presentation/viewmodel/TimeLetterWriteViewModel.kt"],
        [742, [550], "fix(timeletter): 삭제 API 및 확인 흐름 수정", "245a5bea8f4cdedfec815a40b7d0ca56824fa557", "feature/timeletter/presentation/src/main/kotlin/com/afternote/feature/timeletter/presentation/viewmodel/TimeletterViewModel.kt"],
    ];
    const metadataByPullRequest = new Map([
        [
            742,
            runtimeMetadata(550, {
                precondition: "삭제할 타임레터가 목록에 있고 DELETE 성공·실패 응답을 각각 재현할 수 있다",
                action: "삭제 확인에서 확인을 눌러 DELETE 요청을 보내고 성공 응답과 실패 응답을 각각 발생시킨다",
                expected: "성공하면 삭제 항목이 목록에서 사라지고, 실패하면 기존 항목이 보존되며 오류 안내가 표시된다",
                risk: "실패한 삭제가 성공처럼 보이거나 기존 항목이 유실될 수 있다",
                evidence: [issueEvidence(550)],
            }),
        ],
        [
            776,
            runtimeMetadata(706, {
                precondition: "수정할 애프터노트 초안이 에디터에 열린 상태",
                action: "필수 입력을 비웠다가 다시 채우고 저장한 뒤 상세 화면에 재진입한다",
                expected: "입력 검증 이후 변경 내용이 에디터와 상세 화면에 동일하게 유지된다",
                risk: "에디터 리팩터링으로 유효한 수정 내용이 저장되지 않거나 이전 값으로 되돌아갈 수 있다",
                evidence: [issueEvidence(602), issueEvidence(706)],
            }),
        ],
    ]);
    const pendingPullRequests = runtimeSources.map(
        ([number, sourceIssues, title, mergeCommitSha, changedFile]) =>
            pullRequest({
                number,
                title,
                mergeCommitSha,
                issueNumbers: sourceIssues,
                changedFiles: [changedFile],
                metadata:
                    metadataByPullRequest.get(number) ??
                    runtimeMetadata(sourceIssues[0], {
                        evidence: sourceIssues.map(issueEvidence),
                    }),
            }),
    );
    pendingPullRequests.push(
        pullRequest({
            number: 797,
            title: "fix(ci): Review Assign 자동 reviewer 요청 제거",
            mergeCommitSha: "7951e3eecc57f78246c38d6d1f1da038d3add8d3",
            issueNumbers: [796],
            changedFiles: [".github/workflows/PRassign.yml"],
            metadata: exclusionMetadata(796),
        }),
        pullRequest({
            number: 802,
            title: "fix(ci): Review Assign 자동 assignee 제거",
            mergeCommitSha: "3f6ff7d9ddbe61da6af49a9bfdbb5b2a3fa12419",
            issueNumbers: [801],
            changedFiles: [".github/workflows/PRassign.yml"],
            metadata: exclusionMetadata(801),
        }),
    );
    const context = {
        repository: "Afternote/Afternote-FE",
        targetCoveredBySuccessfulDistribution: false,
        baselineDistribution: { headSha: "pr-805-resolved-successful-main-baseline" },
        targetPullRequest: pendingPullRequests.at(-1),
        pendingPullRequests,
    };
    const decision = {
        decision: "deploy",
        risk: "normal",
        reason: "PR #805 방식으로 성공한 main 배포 기준점 이후 변경을 재계산했습니다.",
        includedIssues: issueNumbers,
        changedFiles: pendingPullRequests.flatMap((pullRequest) => pullRequest.changedFiles),
        qaPoints: [],
    };
    const groups = pendingPullRequests
        .filter((pullRequest) => ![797, 802].includes(pullRequest.number))
        .map((pullRequest) => ({
            id: `pr-${pullRequest.number}-qa`,
            priority: pullRequest.number === 742 ? "P0" : pullRequest.number === 776 ? "P1" : "P2",
            sourceIds: [`pr-${pullRequest.number}`],
            ...(pullRequest.number === 776
                ? {
                      mergeReason:
                          "#602와 #706이 같은 애프터노트 에디터 입력 검증·저장·재진입 흐름을 다룬다",
                  }
                : {}),
        }));
    const plan = {
        groups,
        exclusions: [{ sourceId: "pr-797" }, { sourceId: "pr-802" }],
        coverageGaps: [],
    };
    return { issueNumbers, context, decision, plan };
}

test("accepts runnable metadata with canonical fields", () => {
    const inspection = inspectQaMetadata(qaBody(runtimeMetadata(550)), {
        pullRequestNumber: 550,
    });

    assert.equal(inspection.valid, true);
    assert.equal(inspection.metadata.scope, "app-runtime");
    assert.match(inspection.metadata.action, /#550/);
});

test("rejects the former generic fallback even when JSON is present", () => {
    const metadata = runtimeMetadata(550, {
        action: "#550 관련 동작을 재현하고 수정 후 기대 결과가 충족되는지 확인",
    });
    const inspection = inspectQaMetadata(qaBody(metadata), { pullRequestNumber: 550 });

    assert.equal(inspection.valid, false);
    assert.match(inspection.errors.join("\n"), /generic QA/);
    assert.equal(isGenericQaText(metadata.action), true);
});

test("requires matching input, boundary, and observation evidence for exclusions", () => {
    const metadata = exclusionMetadata(796);
    delete metadata.evidence[0].observation;
    const inspection = inspectQaMetadata(qaBody(metadata), { pullRequestNumber: 796 });

    assert.equal(inspection.valid, false);
    assert.match(inspection.errors.join("\n"), /동일 입력·경계·관찰 결과/);
});

test("validates pull request events with the same parser used by the audit", () => {
    const result = validatePullRequestEvent({
        number: 809,
        pull_request: { number: 809, body: qaBody(exclusionMetadata(809)) },
    });

    assert.equal(result.valid, true);
    assert.equal(result.metadata.scope, "ci-only");
});

const beforeCutoff = new Date(Date.parse(QA_METADATA_GATE_CUTOFF) - 1_000).toISOString();
const afterCutoff = new Date(Date.parse(QA_METADATA_GATE_CUTOFF) + 1_000).toISOString();

test("grandfathers pull requests created before the gate cutoff when the section is absent", () => {
    const result = validatePullRequestEvent({
        pull_request: { number: 741, created_at: beforeCutoff, body: "## 작업 내용\n설명만 있다." },
    });

    assert.equal(result.grandfathered, true);
    assert.equal(result.valid, true);
    assert.deepEqual(result.errors, []);
});

test("still validates grandfathered pull requests once they add the section", () => {
    const metadata = exclusionMetadata(809);
    metadata.scope = "invalid-scope";
    const result = validatePullRequestEvent({
        pull_request: { number: 741, created_at: beforeCutoff, body: qaBody(metadata) },
    });

    assert.equal(result.grandfathered, false);
    assert.equal(result.valid, false);
});

test("enforces the section for pull requests created after the cutoff", () => {
    const result = validatePullRequestEvent({
        pull_request: { number: 999, created_at: afterCutoff, body: "## 작업 내용\n섹션 없음." },
    });

    assert.equal(result.grandfathered, false);
    assert.equal(result.valid, false);
    assert.match(result.errors.join("\n"), /섹션이 없습니다/);
});

test("enforces the section when created_at is missing", () => {
    const result = validatePullRequestEvent({
        pull_request: { number: 999, body: "## 작업 내용\n섹션 없음." },
    });

    assert.equal(result.grandfathered, false);
    assert.equal(result.valid, false);
});

test("PR #802 fixture produces exact issues, exclusions, merge, and actionable deletion QA", () => {
    const { issueNumbers, context, decision, plan } = fixture802();
    const input = buildQaAuditInput(context, decision);
    const validation = validateQaAuditPlan(plan, input);
    const result = finalizeDeploymentDecision(decision, input, JSON.stringify(plan), {
        model: "fixture-model",
    });

    assert.equal(input.status, "ready_for_ai");
    assert.equal(context.targetPullRequest.number, 802);
    assert.equal(
        context.targetPullRequest.mergeCommitSha,
        "3f6ff7d9ddbe61da6af49a9bfdbb5b2a3fa12419",
    );
    assert.equal(validation.valid, true, validation.errors.join("\n"));
    assert.equal(result.qaAudit.status, "ready");
    assert.deepEqual(result.includedIssues, issueNumbers);
    assert.deepEqual(
        result.qaAudit.exclusions.flatMap((item) => item.issueNumbers).sort((a, b) => a - b),
        [796, 801],
    );
    assert.ok(
        result.qaScenarios.some(
            (scenario) =>
                scenario.sourceIds.length === 1 &&
                scenario.sourceIds[0] === "pr-776" &&
                JSON.stringify(scenario.issueNumbers) === JSON.stringify([602, 706]) &&
                /#602.*#706/.test(scenario.mergeReason),
        ),
    );
    const deletionPoint = result.qaPoints.find((point) => point.includes("#550"));
    assert.match(deletionPoint, /삭제 확인/);
    assert.match(deletionPoint, /DELETE/);
    assert.match(deletionPoint, /성공.*목록/);
    assert.match(deletionPoint, /실패.*기존 항목.*안내/);
    assert.equal(result.qaPoints.filter((point) => /관련 동작을 재현/.test(point)).length, 0);
});

test("fails closed without an AI result", () => {
    const { context, decision } = fixture802();
    const input = buildQaAuditInput(context, decision);
    const result = finalizeDeploymentDecision(decision, input, "", {
        channelStatus: "token=false",
    });

    assert.equal(result.decision, "hold");
    assert.equal(result.boundaryDecision, "deploy");
    assert.equal(result.qaAudit.status, "human_review_required");
    assert.deepEqual(result.qaPoints, []);
});

test("fails closed when the model drops a canonical source", () => {
    const { context, decision, plan } = fixture802();
    const input = buildQaAuditInput(context, decision);
    plan.groups = plan.groups.filter((group) => !group.sourceIds.includes("pr-742"));
    const result = finalizeDeploymentDecision(decision, input, JSON.stringify(plan));

    assert.equal(result.qaAudit.status, "human_review_required");
    assert.match(result.qaAudit.errors.join("\n"), /pr-742.*정확히 한 번/);
});

test("fails closed when model-authored audit text contains markup", () => {
    const { context, decision, plan } = fixture802();
    const input = buildQaAuditInput(context, decision);
    plan.groups.find((group) => group.sourceIds.includes("pr-776")).mergeReason =
        "<img src=x> 같은 에디터 흐름";
    const result = finalizeDeploymentDecision(decision, input, JSON.stringify(plan));

    assert.equal(result.qaAudit.status, "human_review_required");
    assert.match(result.qaAudit.errors.join("\n"), /mergeReason/);
});

test("prompt treats issue content as untrusted and prohibits canonical rewrites", () => {
    const { context, decision } = fixture802();
    const input = buildQaAuditInput(context, decision);
    const prompt = buildQaAuditPrompt(input);

    assert.match(prompt, /untrusted data/);
    assert.match(prompt, /must never be rewritten or invented/);
    assert.match(prompt, /feature\/timeletter\/presentation.*TimeletterViewModel\.kt/);
});

test("workflow uses a personal Copilot secret and runs all strict regression tests", async () => {
    const workflow = await fs.readFile(
        new URL("../workflows/deployment-decision.yml", import.meta.url),
        "utf8",
    );
    const unitWorkflow = await fs.readFile(
        new URL("../workflows/unit-test.yml", import.meta.url),
        "utf8",
    );

    assert.match(workflow, /COPILOT_GITHUB_TOKEN:\s*\$\{\{ secrets\.COPILOT_PERSONAL_TOKEN \}\}/);
    assert.doesNotMatch(workflow, /copilot-requests:\s*write/);
    assert.match(workflow, /--deny-tool='shell,write,read,url,memory'/);
    assert.match(workflow, /--disable-builtin-mcps/);
    assert.match(workflow, /--max-ai-credits=1/);
    assert.match(workflow, /node-version: '22'/);
    assert.match(workflow, /qa-semantic-audit\.mjs finalize/);
    assert.match(unitWorkflow, /validate-pr-qa-metadata\.mjs/);
    assert.match(unitWorkflow, /node --test \.github\/scripts\/\*\.test\.mjs/);
});
