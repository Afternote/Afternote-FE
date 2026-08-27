// PR 본문의 `QA Metadata` 섹션을 파싱·검증한다. validate-pr-qa-metadata.mjs 가 Unit Test 에서
// 이 함수들로 게이트를 건다 (#809).
//
// 원래 qa-semantic-audit.mjs 의 앞부분이었다. 그 스크립트는 deployment-decision.yml 의 판정 위에
// Copilot 의미 감사를 얹었는데, develop 이 판정 워크플로 자체를 걷어내(d657a0492) 토대가 사라졌다.
// 배포 시점은 릴리스 PR 을 여는 사람이 정하고 QA 포인트도 사람이 쓰므로, 남는 것은 PR 단위로
// 구조화 QA 원천을 강제하는 이 게이트뿐이다.

export const QA_METADATA_SCOPES = [
    "app-runtime",
    "release-only",
    "ci-only",
    "covered-by-ci",
];

const RUNNABLE_SCOPES = new Set(["app-runtime", "release-only"]);
const EXCLUDED_SCOPES = new Set(["ci-only", "covered-by-ci"]);
const EVIDENCE_KINDS = new Set([
    "issue",
    "test",
    "ci",
    "screenshot",
    "measurement",
    "diff",
]);
const MAX_METADATA_TEXT_LENGTH = 1_000;
const MAX_EVIDENCE_ITEMS = 20;
const GENERIC_QA_PATTERNS = [
    /#\d+\s*관련 동작을 재현하고 수정 후 기대 결과가 충족되는지 확인/i,
    /PR\s*#\d+의 변경 흐름을 실행하고 기존 동작이 회귀하지 않는지 확인/i,
    /^(?:관련|기존|정상)\s*(?:동작|기능|흐름)(?:을|이|가)?\s*(?:확인|테스트)(?:한다|하기|해보기)?[.!]?$/i,
];
const PLACEHOLDER_VALUES = new Set([
    "",
    "-",
    "#",
    "...",
    "n/a",
    "na",
    "none",
    "no response",
    "todo",
    "tbd",
    "없음",
    "해당 없음",
]);

function normalizedText(value) {
    return typeof value === "string" ? value.trim() : "";
}

export function isGenericQaText(value) {
    const text = normalizedText(value);
    return (
        PLACEHOLDER_VALUES.has(text.toLowerCase()) ||
        GENERIC_QA_PATTERNS.some((pattern) => pattern.test(text))
    );
}

function qaMetadataSection(body) {
    const lines = String(body ?? "").split(/\r?\n/);
    let capturing = false;
    const section = [];

    for (const line of lines) {
        const heading = /^(#{1,6})\s+(.+?)\s*$/.exec(line);
        if (heading) {
            if (capturing) {
                break;
            }
            capturing = /qa\s*(?:메타데이터|metadata)/i.test(heading[2]);
            continue;
        }
        if (capturing) {
            section.push(line);
        }
    }

    return section.join("\n").replace(/<!--[\s\S]*?-->/g, "");
}

export function hasQaMetadataSection(body) {
    return Boolean(qaMetadataSection(body).trim());
}

export function extractQaMetadata(body) {
    const section = qaMetadataSection(body);
    if (!section.trim()) {
        throw new Error("`QA 메타데이터` 섹션이 없습니다.");
    }

    const blocks = [...section.matchAll(/```(?:json)?\s*\r?\n([\s\S]*?)```/gi)];
    if (blocks.length !== 1) {
        throw new Error("`QA 메타데이터`에는 JSON 코드 블록이 정확히 하나 있어야 합니다.");
    }

    let metadata;
    try {
        metadata = JSON.parse(blocks[0][1]);
    } catch {
        throw new Error("`QA 메타데이터` JSON을 해석할 수 없습니다.");
    }
    if (!metadata || typeof metadata !== "object" || Array.isArray(metadata)) {
        throw new Error("`QA 메타데이터` 최상위 값은 JSON 객체여야 합니다.");
    }
    return metadata;
}

function validateTextField(metadata, key, errors) {
    const value = normalizedText(metadata[key]);
    if (!value) {
        errors.push(`\`${key}\` 문자열이 필요합니다.`);
        return "";
    }
    if (isGenericQaText(value)) {
        errors.push(`\`${key}\`에 placeholder 또는 generic QA 문구를 사용할 수 없습니다.`);
    }
    if (value.length > MAX_METADATA_TEXT_LENGTH) {
        errors.push(`\`${key}\`는 ${MAX_METADATA_TEXT_LENGTH}자 이하여야 합니다.`);
    }
    return value;
}

function validateEvidence(metadata, errors) {
    if (!Array.isArray(metadata.evidence) || metadata.evidence.length === 0) {
        errors.push("`evidence`에는 근거 객체가 하나 이상 필요합니다.");
        return [];
    }
    if (metadata.evidence.length > MAX_EVIDENCE_ITEMS) {
        errors.push(`\`evidence\`는 ${MAX_EVIDENCE_ITEMS}개 이하여야 합니다.`);
    }

    return metadata.evidence.flatMap((item, index) => {
        if (!item || typeof item !== "object" || Array.isArray(item)) {
            errors.push(`\`evidence[${index}]\`는 객체여야 합니다.`);
            return [];
        }
        const kind = normalizedText(item.kind).toLowerCase();
        const ref = normalizedText(item.ref);
        const assertion = normalizedText(item.assertion);
        if (!EVIDENCE_KINDS.has(kind)) {
            errors.push(
                `\`evidence[${index}].kind\`는 ${[...EVIDENCE_KINDS].join(", ")} 중 하나여야 합니다.`,
            );
        }
        if (!ref || isGenericQaText(ref)) {
            errors.push(`\`evidence[${index}].ref\`에 구체적인 이슈·테스트·job 참조가 필요합니다.`);
        }
        if (!assertion || isGenericQaText(assertion)) {
            errors.push(`\`evidence[${index}].assertion\`에 해당 근거가 증명하는 내용을 적어야 합니다.`);
        }
        if (ref.length > MAX_METADATA_TEXT_LENGTH || assertion.length > MAX_METADATA_TEXT_LENGTH) {
            errors.push(`\`evidence[${index}]\`의 문자열은 ${MAX_METADATA_TEXT_LENGTH}자 이하여야 합니다.`);
        }

        const normalized = { kind, ref, assertion };
        for (const key of ["input", "boundary", "observation"]) {
            const value = normalizedText(item[key]);
            if (value) {
                normalized[key] = value;
                if (isGenericQaText(value)) {
                    errors.push(`\`evidence[${index}].${key}\`에 generic 문구를 사용할 수 없습니다.`);
                }
                if (value.length > MAX_METADATA_TEXT_LENGTH) {
                    errors.push(
                        `\`evidence[${index}].${key}\`는 ${MAX_METADATA_TEXT_LENGTH}자 이하여야 합니다.`,
                    );
                }
            }
        }
        return [normalized];
    });
}

export function inspectQaMetadata(body, options = {}) {
    const pullRequestNumber = options.pullRequestNumber ?? "?";
    const errors = [];
    let raw;
    try {
        raw = extractQaMetadata(body);
    } catch (error) {
        return {
            valid: false,
            metadata: null,
            errors: [`PR #${pullRequestNumber}: ${error.message}`],
        };
    }

    const scope = normalizedText(raw.scope).toLowerCase();
    if (!QA_METADATA_SCOPES.includes(scope)) {
        errors.push(`\`scope\`는 ${QA_METADATA_SCOPES.join(", ")} 중 하나여야 합니다.`);
    }
    const evidence = validateEvidence(raw, errors);
    const metadata = { scope, evidence };

    if (RUNNABLE_SCOPES.has(scope)) {
        for (const key of ["precondition", "action", "expected", "risk"]) {
            metadata[key] = validateTextField(raw, key, errors);
        }
        if (normalizedText(raw.exclusionReason)) {
            errors.push(`\`${scope}\`에는 \`exclusionReason\`을 함께 둘 수 없습니다.`);
        }
    } else if (EXCLUDED_SCOPES.has(scope)) {
        metadata.exclusionReason = validateTextField(raw, "exclusionReason", errors);
        for (const key of ["precondition", "action", "expected", "risk"]) {
            if (normalizedText(raw[key])) {
                errors.push(`\`${scope}\`에서는 \`${key}\` 대신 \`exclusionReason\`을 사용합니다.`);
            }
        }
        const matchingBoundaryEvidence = evidence.some(
            (item) =>
                (item.kind === "ci" || item.kind === "test") &&
                normalizedText(item.input) &&
                normalizedText(item.boundary) &&
                normalizedText(item.observation),
        );
        if (!matchingBoundaryEvidence) {
            errors.push(
                `\`${scope}\` 제외에는 동일 입력·경계·관찰 결과를 적은 ci/test evidence가 필요합니다.`,
            );
        }
    }

    return {
        valid: errors.length === 0,
        metadata: errors.length === 0 ? metadata : null,
        errors: errors.map((error) => `PR #${pullRequestNumber}: ${error}`),
    };
}

