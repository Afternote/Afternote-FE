import assert from "node:assert/strict";
import test from "node:test";

import { inspectQaMetadata, isGenericQaText } from "./qa-metadata.mjs";
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

test("validates pull request events with the same parser used by the gate", () => {
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
