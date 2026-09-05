import { createHash } from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { pathToFileURL } from "node:url";

export const PUBLIC_QA_EVIDENCE_SCHEMA = 1;
const NUMERIC_SENSITIVE_PREFIX = "numeric-sensitive:";
const AUTH_NUMERIC_SENSITIVE_PREFIX = "auth-numeric-sensitive:";

const FULL_SHA_PATTERN = /^[0-9a-f]{40}$/;
const SHA_PATTERN = /^[0-9a-f]{7,40}$/;
const EVIDENCE_FILE_PATTERN = /^[0-9a-f]{40}\.json$/;
const EMAIL_PATTERN = /\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b/gi;
const URL_PATTERN = /https?:\/\/[^\s<>"')\]]+/gi;
const IPV4_PATTERN = /\b(?:\d{1,3}\.){3}\d{1,3}(?::\d+)?\b/g;
const UUID_PATTERN = /\b[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\b/gi;
const EMULATOR_SERIAL_PATTERN = /\bemulator-\d+\b/gi;
const QA_ACCOUNT_PATTERN = /\bQA(?=[A-Z0-9_-]*\d)[A-Z0-9_-]+\b/gi;
const QA_ROLE_ALIAS_PATTERN = /\bQA\s+(?:Receiver|Sender|Tester|User)(?:[ _-][A-Za-z0-9_-]+)?\b/gi;
const QA_COMPACT_ROLE_ALIAS_PATTERN = /QA(?:수신자|발신자|테스터|사용자)/g;
const ENGLISH_QA_DISPLAY_ALIAS_PATTERN = /(?<![A-Za-z0-9_-])[A-Za-z][A-Za-z0-9_-]{2,}QA(?=\s*(?:님|홈))/g;
const QA_CREDENTIAL_PATTERN = /\bqa\d+[_-][a-z0-9_-]+\b/gi;
const PRIVATE_AVD_NAME_PATTERN = /\b(?:Pixel|Nexus|Medium_Phone|Small_Phone|Resizable|sdk_gphone)[A-Za-z0-9._-]*(?:QA|AVD|Emulator|Device)[A-Za-z0-9._-]*\b/gi;
const DOMAIN_PATTERN = /\b(?:[a-z0-9-]+\.)+(?:com|dev|io|kr|net|org)(?::\d+)?(?:\/[A-Za-z0-9._~:/?#[\]@!$&'()*+,;=%-]*)?/gi;
const LOCAL_HOST_PATTERN = /\b(?:localhost|host\.docker\.internal)(?::\d+)?\b/gi;
const LOCAL_PATH_PATTERN = /(?<![A-Za-z0-9_.-])\/(?:Users|private|tmp|var\/folders|data\/(?:data|user)|sdcard|storage\/emulated|opt|usr\/local)\/[^\s`"'<>),;]*/g;
const JSON_PRIVATE_NUMERIC_ID_PATTERN = /(["'](?:(?:afternote|receiver|content|account|verification|user|record|delivery)[_-]?id|sub|subject)["']\s*:\s*)["']?\d+["']?/gi;
const PRIVATE_NUMERIC_ID_PATTERN = /(\b(?:(?:afternote|receiver|content|account|verification|user|record|delivery)[_-]?id|sub|subject)\s*[:=#]\s*)\d+\b/gi;
const KOREAN_NUMERIC_ID_PATTERN = /((?:계정|수신자|콘텐츠|인증|사용자)\s*(?:ID|id|식별자)\s*[:=#]?\s*)\d+\b/gi;
const AUTH_ASSIGNMENT_PATTERN = /(\b(?:auth(?:entication)?[_-]?code|verification[_-]?code|otp|pin|passcode|master[_-]?key|access[_ -]?token|refresh[_ -]?token|token|password|credential)\b["']?\s*(?:[:=#]|\()\s*["']?)[A-Za-z0-9][A-Za-z0-9._-]{1,}(?:…)?/gi;
const DIRECT_KOREAN_AUTH_CODE_PATTERN = /((?:인증\s*(?:번호|코드))\s*(?:(?:[:=#]|은|는|이|가)\s*)?)\d{2,8}\b/gi;
const NARRATIVE_KOREAN_AUTH_CODE_PATTERN = /(인증\s*\(\s*(?:(?:실제|수신)\s*)*코드\s*)\d{2,8}\b/gi;
const PIN_VALUE_PATTERN = /((?:앱\s*잠금\s*)?\bPIN\b(?:\s*(?:[:=#]|은|는|이|가)\s*|\s*\(\s*|\s+))\d{2,8}\b/gi;
const MASTER_KEY_VALUE_PATTERN = /(마스터\s*키(?:\s*(?:[:=#]|은|는|이|가)\s*|\s*\(\s*|\s+))[A-Za-z0-9][A-Za-z0-9._-]{3,}(?:…)?/gi;
const NARRATIVE_AUTH_VALUE_PATTERN = /(\bauth(?:entication)?[_-]?code\b(?:\s+[가-힣]+){1,4}\s*\(\s*)[A-Za-z0-9][A-Za-z0-9._-]{3,}(?:…)?/gi;
const AUTH_VALUE_PATTERNS = [
    AUTH_ASSIGNMENT_PATTERN,
    DIRECT_KOREAN_AUTH_CODE_PATTERN,
    NARRATIVE_KOREAN_AUTH_CODE_PATTERN,
    PIN_VALUE_PATTERN,
    MASTER_KEY_VALUE_PATTERN,
    NARRATIVE_AUTH_VALUE_PATTERN,
];
const QUOTED_PERSON_PATTERN = /((?:수신자|발신자|사용자)\s*)["“](?!\[PERSON_REDACTED\])(?=[^"”\n]{1,40}["”])(?=[^"”\n]*[\p{L}\p{N}])[^"”\n]{1,40}["”]/gu;
const JSON_NAMED_PERSON_PATTERN = /(["']?(?:name|이름|성명)["']?\s*:\s*)(["'“])(?!\[PERSON_REDACTED\])[^"'”\n]{1,40}(["'”])/gi;
const NAMED_PERSON_PATTERN = /((?:(?:수신자|발신자|사용자|계정)\s*)?(?:이름|성명|name)\s*[:=#]\s*)[A-Za-z가-힣][A-Za-z가-힣._-]{1,30}/gi;
const PERSON_HONORIFIC_PATTERN = /(?<![A-Za-z0-9가-힣._-])(?<!\[)(?!(?:고객|사용자|회원)님)[A-Za-z가-힣][A-Za-z0-9가-힣._-]{1,30}님(?![A-Za-z0-9가-힣._-])/g;
const BARE_JSON_REDACTION_PATTERN = /((?:["'][A-Za-z0-9_-]+["']|[A-Za-z_][A-Za-z0-9_-]*)\s*:\s*)(\[[A-Z_]+_REDACTED\])(?=\s*[,}])/g;

const KNOWN_FULL_HEAD_SHA_BY_PREFIX = new Map([
    ["d459deb5", "d459deb5c867fa4d7ab4123bb2cb65b9b44925ce"],
]);

const INTERPRETATION_NOTES_BY_HEAD = new Map([
    [
        "8325a703ab3275330b30713b0f4d542c975a2024",
        [
            "not_covered의 수신자 재인증 사유는 앞선 패스 시점 기록이다. 같은 기록의 후속 패스에서는 승인·재진입까지 진행했지만 #607로 승인 이후 FE 화면의 종단 간 검증이 남았다.",
        ],
    ],
]);

const SENSITIVE_KEY_PATTERN = /^(?:account|afternote_id|authorization|avd|cookie|credential|email|fixture|password|passcode|qa_account|receiverid|secret|serial|server|session|token)$/i;
const SENSITIVE_COLLECTION_KEYS = new Set(["qa_account", "screenshots"]);
const NORMALIZED_TOP_LEVEL_KEYS = new Set([
    "schema",
    "repository",
    "head_sha",
    "head_ref",
    "branch",
    "base_sha",
    "base_ref",
    "recorded_at",
    "performed_at",
    "result",
    "scenario",
    "purpose",
    "not_covered",
    "limits",
    "device",
]);

function isPlainObject(value) {
    return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}

function jsonText(value) {
    return `${JSON.stringify(value, null, 2)}\n`;
}

function sha256(value) {
    return createHash("sha256").update(value).digest("hex");
}

function escapeRegExp(value) {
    return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function isPublicGitHubReference(value) {
    return /^https:\/\/github\.com\/Afternote\/Afternote-(?:FE|BE)\/(?:issues|pull)\/\d+(?:[#?][^\s]*)?$/i.test(
        value,
    );
}

function collectSensitiveLiterals(raw) {
    const values = new Set();

    function visit(value, keys = [], inheritedSensitive = false) {
        if ((typeof value === "number" || typeof value === "bigint") && inheritedSensitive) {
            const numericValue = String(value);
            if (numericValue.length >= 2) values.add(`${NUMERIC_SENSITIVE_PREFIX}${numericValue}`);
            return;
        }
        if (typeof value === "string") {
            if (inheritedSensitive && value.trim().length >= 3) {
                values.add(value.trim());
            }
            for (const match of value.matchAll(URL_PATTERN)) {
                if (!isPublicGitHubReference(match[0])) values.add(match[0]);
            }
            EMAIL_PATTERN.lastIndex = 0;
            for (const match of value.matchAll(EMAIL_PATTERN)) {
                values.add(match[0]);
                const localPart = match[0].split("@", 1)[0];
                if (localPart.length >= 3) values.add(localPart);
            }
            for (const pattern of [IPV4_PATTERN, UUID_PATTERN, EMULATOR_SERIAL_PATTERN]) {
                pattern.lastIndex = 0;
                for (const match of value.matchAll(pattern)) values.add(match[0]);
            }
            for (const pattern of AUTH_VALUE_PATTERNS) {
                pattern.lastIndex = 0;
                for (const match of value.matchAll(pattern)) {
                    const assignedValue = match[0].slice(match[1].length);
                    if (/^\d{2,8}$/.test(assignedValue)) {
                        values.add(`${AUTH_NUMERIC_SENSITIVE_PREFIX}${assignedValue}`);
                    } else if (assignedValue.length >= 4) {
                        values.add(assignedValue);
                    }
                }
            }
            return;
        }
        if (Array.isArray(value)) {
            value.forEach((item) => visit(item, keys, inheritedSensitive));
            return;
        }
        if (!isPlainObject(value)) return;

        for (const [key, child] of Object.entries(value)) {
            const sensitive =
                inheritedSensitive ||
                SENSITIVE_KEY_PATTERN.test(key) ||
                SENSITIVE_COLLECTION_KEYS.has(key.toLowerCase());
            visit(child, [...keys, key], sensitive);
        }
    }

    visit(raw);
    return [...values].sort((left, right) => right.length - left.length);
}

function redactText(rawText, sensitiveLiterals, redactions) {
    let value = rawText;
    const publicUrls = [];

    value = value.replace(URL_PATTERN, (url) => {
        if (isPublicGitHubReference(url)) {
            const token = `PUBLIC_GITHUB_REFERENCE_${publicUrls.length}`;
            publicUrls.push(url);
            return token;
        }
        redactions.add("text.network_endpoint");
        return "[NETWORK_ENDPOINT_REDACTED]";
    });

    for (const literal of sensitiveLiterals) {
        if (literal.startsWith(NUMERIC_SENSITIVE_PREFIX)) continue;
        if (literal.startsWith(AUTH_NUMERIC_SENSITIVE_PREFIX)) {
            const numericValue = literal.slice(AUTH_NUMERIC_SENSITIVE_PREFIX.length);
            const pattern = new RegExp(`\\b${escapeRegExp(numericValue)}\\b`, "g");
            if (!pattern.test(value)) continue;
            pattern.lastIndex = 0;
            value = value.replace(pattern, "[AUTH_VALUE_REDACTED]");
            redactions.add("text.auth_value");
            continue;
        }
        const pattern = /^\d+$/.test(literal)
            ? new RegExp(`\\b${escapeRegExp(literal)}\\b`, "g")
            : new RegExp(escapeRegExp(literal), "gi");
        if (!pattern.test(value)) continue;
        pattern.lastIndex = 0;
        value = value.replace(pattern, "[SENSITIVE_VALUE_REDACTED]");
        redactions.add("text.sensitive_value");
    }

    for (const [pattern, replacement, category] of [
        [JSON_PRIVATE_NUMERIC_ID_PATTERN, "$1\"[PRIVATE_ID_REDACTED]\"", "text.private_identifier"],
        [PRIVATE_NUMERIC_ID_PATTERN, "$1[PRIVATE_ID_REDACTED]", "text.private_identifier"],
        [KOREAN_NUMERIC_ID_PATTERN, "$1[PRIVATE_ID_REDACTED]", "text.private_identifier"],
        ...AUTH_VALUE_PATTERNS.map((pattern) => [
            pattern,
            "$1[AUTH_VALUE_REDACTED]",
            "text.auth_value",
        ]),
        [JSON_NAMED_PERSON_PATTERN, "$1$2[PERSON_REDACTED]$3", "text.person_name"],
        [QUOTED_PERSON_PATTERN, "$1\"[PERSON_REDACTED]\"", "text.person_name"],
        [NAMED_PERSON_PATTERN, "$1[PERSON_REDACTED]", "text.person_name"],
        [PERSON_HONORIFIC_PATTERN, "[PERSON_REDACTED]님", "text.person_name"],
    ]) {
        pattern.lastIndex = 0;
        if (pattern.test(value)) {
            pattern.lastIndex = 0;
            value = value.replace(pattern, replacement);
            redactions.add(category);
        }
    }

    const replacements = [
        [EMAIL_PATTERN, "[QA_ACCOUNT_REDACTED]", "text.qa_account"],
        [IPV4_PATTERN, "[NETWORK_ADDRESS_REDACTED]", "text.network_address"],
        [UUID_PATTERN, "[PRIVATE_IDENTIFIER_REDACTED]", "text.private_identifier"],
        [EMULATOR_SERIAL_PATTERN, "[EMULATOR_REDACTED]", "text.device_identifier"],
        [PRIVATE_AVD_NAME_PATTERN, "[DEVICE_NAME_REDACTED]", "text.device_identifier"],
        [QA_CREDENTIAL_PATTERN, "[QA_CREDENTIAL_REDACTED]", "text.qa_credential"],
        [QA_ROLE_ALIAS_PATTERN, "[QA_ACCOUNT_REDACTED]", "text.qa_account"],
        [QA_COMPACT_ROLE_ALIAS_PATTERN, "[QA_ACCOUNT_REDACTED]", "text.qa_account"],
        [ENGLISH_QA_DISPLAY_ALIAS_PATTERN, "[QA_ACCOUNT_REDACTED]", "text.qa_account"],
        [QA_ACCOUNT_PATTERN, "[QA_ACCOUNT_REDACTED]", "text.qa_account"],
        [LOCAL_HOST_PATTERN, "[LOCAL_HOST_REDACTED]", "text.local_host"],
        [LOCAL_PATH_PATTERN, "[LOCAL_PATH_REDACTED]", "text.local_path"],
        [DOMAIN_PATTERN, "[NETWORK_ENDPOINT_REDACTED]", "text.network_endpoint"],
    ];

    for (const [pattern, replacement, category] of replacements) {
        pattern.lastIndex = 0;
        if (pattern.test(value)) {
            pattern.lastIndex = 0;
            value = value.replace(pattern, replacement);
            redactions.add(category);
        }
    }

    publicUrls.forEach((url, index) => {
        value = value.replace(`PUBLIC_GITHUB_REFERENCE_${index}`, url);
    });
    value = value.replace(BARE_JSON_REDACTION_PATTERN, "$1\"$2\"");
    return value;
}

function sanitizeNode(value, pathParts, sensitiveLiterals, redactions) {
    if (typeof value === "string") {
        return redactText(value, sensitiveLiterals, redactions);
    }
    if (Array.isArray(value)) {
        return value.map((item, index) =>
            sanitizeNode(item, [...pathParts, String(index)], sensitiveLiterals, redactions),
        );
    }
    if (!isPlainObject(value)) return value;

    const output = {};
    for (const [key, child] of Object.entries(value)) {
        const childPath = [...pathParts, key];
        if (SENSITIVE_KEY_PATTERN.test(key) || SENSITIVE_COLLECTION_KEYS.has(key.toLowerCase())) {
            redactions.add(childPath.join("."));
            continue;
        }
        output[key] = sanitizeNode(child, childPath, sensitiveLiterals, redactions);
    }
    return output;
}

function resolveFullHeadSha(rawHeadSha) {
    const value = String(rawHeadSha ?? "").trim().toLowerCase();
    if (FULL_SHA_PATTERN.test(value)) return value;
    const known = KNOWN_FULL_HEAD_SHA_BY_PREFIX.get(value);
    if (known) return known;
    throw new Error(`전체 HEAD SHA로 해석할 수 없는 기록이 있습니다: ${value || "<empty>"}`);
}

function normalizeResult(raw) {
    if (typeof raw.result === "string" && raw.result.trim()) return raw.result.trim();
    if (Array.isArray(raw.checks) && raw.checks.length > 0) {
        const checkResults = raw.checks
            .map((check) => (typeof check?.result === "string" ? check.result.trim().toUpperCase() : ""))
            .filter(Boolean);
        if (checkResults.length === raw.checks.length && checkResults.every((result) => result === "PASS")) {
            return "PASS";
        }
        if (checkResults.length > 0) return `CHECK_RESULTS: ${[...new Set(checkResults)].join(", ")}`;
    }
    return "NOT_RECORDED";
}

function normalizeScenario(raw) {
    if (typeof raw.scenario === "string" && raw.scenario.trim()) return raw.scenario.trim();
    if (typeof raw.purpose === "string" && raw.purpose.trim()) return raw.purpose.trim();
    if (Number.isInteger(raw.pr) && Number.isInteger(raw.issue)) {
        return `PR #${raw.pr} / issue #${raw.issue} 수정 검증`;
    }
    return "원본 기록에 시나리오 요약이 없음";
}

function normalizeNotCovered(raw) {
    const value = raw.not_covered ?? raw.limits;
    if (value === undefined || value === null) return [];
    return Array.isArray(value) ? value : [value];
}

function sourceFieldMapping(raw) {
    return {
        result: typeof raw.result === "string" ? "result" : Array.isArray(raw.checks) ? "checks[].result" : null,
        scenario:
            typeof raw.scenario === "string"
                ? "scenario"
                : typeof raw.purpose === "string"
                  ? "purpose"
                  : Number.isInteger(raw.pr) && Number.isInteger(raw.issue)
                    ? "derived:pr+issue"
                    : null,
        not_covered: raw.not_covered !== undefined ? "not_covered" : raw.limits !== undefined ? "limits" : null,
        recorded_at:
            typeof raw.recorded_at === "string"
                ? "recorded_at"
                : typeof raw.performed_at === "string"
                  ? "performed_at"
                  : null,
    };
}

export function buildPublicQaEvidence(raw, originalFile) {
    if (!isPlainObject(raw)) throw new Error(`${originalFile}: 최상위 값은 JSON 객체여야 합니다.`);
    const sensitiveLiterals = collectSensitiveLiterals(raw);
    const redactions = new Set();
    const headSha = resolveFullHeadSha(raw.head_sha);
    const sourceSchema = Number.isInteger(raw.schema) ? raw.schema : "legacy";

    const detailsSource = Object.fromEntries(
        Object.entries(raw).filter(([key]) => !NORMALIZED_TOP_LEVEL_KEYS.has(key)),
    );
    if (
        typeof raw.scenario === "string" &&
        typeof raw.purpose === "string" &&
        raw.purpose.trim() &&
        raw.purpose.trim() !== raw.scenario.trim()
    ) {
        detailsSource.purpose = raw.purpose;
    }
    const details = sanitizeNode(detailsSource, ["details"], sensitiveLiterals, redactions);
    if (typeof raw.device === "string" && raw.device.trim()) {
        redactions.add("device.identifier");
    }
    const device = sanitizeNode(
        isPlainObject(raw.device)
            ? raw.device
            : { is_emulator: typeof raw.device === "string" && /emulator/i.test(raw.device) },
        ["device"],
        sensitiveLiterals,
        redactions,
    );
    const result = redactText(normalizeResult(raw), sensitiveLiterals, redactions);
    const scenario = redactText(normalizeScenario(raw), sensitiveLiterals, redactions);
    const notCovered = sanitizeNode(
        normalizeNotCovered(raw),
        ["not_covered"],
        sensitiveLiterals,
        redactions,
    );

    if (Array.isArray(raw.screenshots) && raw.screenshots.length > 0) {
        details.redacted_artifacts = {
            screenshot_references: raw.screenshots.length,
        };
    }

    const record = {
        schema: PUBLIC_QA_EVIDENCE_SCHEMA,
        source: {
            original_file: path.basename(originalFile),
            schema: sourceSchema,
            field_mapping: sourceFieldMapping(raw),
            redactions: [...redactions].sort(),
            ...(INTERPRETATION_NOTES_BY_HEAD.has(headSha)
                ? { interpretation_notes: INTERPRETATION_NOTES_BY_HEAD.get(headSha) }
                : {}),
        },
        repository: typeof raw.repository === "string" ? raw.repository : "Afternote/Afternote-FE",
        head_sha: headSha,
        head_ref:
            typeof raw.head_ref === "string"
                ? raw.head_ref
                : typeof raw.branch === "string"
                  ? raw.branch
                  : null,
        base_sha: typeof raw.base_sha === "string" ? raw.base_sha.toLowerCase() : null,
        base_ref: typeof raw.base_ref === "string" ? raw.base_ref : null,
        recorded_at:
            typeof raw.recorded_at === "string"
                ? raw.recorded_at
                : typeof raw.performed_at === "string"
                  ? raw.performed_at
                  : null,
        result,
        scenario,
        not_covered: notCovered,
        device,
        details,
    };

    validatePublicQaEvidence(record, `${headSha}.json`);
    const publicText = jsonText(record);
    for (const literal of sensitiveLiterals) {
        if (literal.startsWith(NUMERIC_SENSITIVE_PREFIX)) continue;
        if (literal.startsWith(AUTH_NUMERIC_SENSITIVE_PREFIX)) {
            const numericValue = literal.slice(AUTH_NUMERIC_SENSITIVE_PREFIX.length);
            if (new RegExp(`\\b${escapeRegExp(numericValue)}\\b`).test(publicText)) {
                throw new Error(`${originalFile}: 인증 값이 공개 기록에 남았습니다.`);
            }
            continue;
        }
        const pattern = /^\d+$/.test(literal)
            ? new RegExp(`\\b${escapeRegExp(literal)}\\b`, "g")
            : new RegExp(escapeRegExp(literal), "i");
        if (literal.length >= 3 && pattern.test(publicText)) {
            throw new Error(`${originalFile}: 제거 대상 값이 공개 기록에 남았습니다.`);
        }
    }
    return record;
}

function walkKeys(value, visitor, pathParts = []) {
    if (Array.isArray(value)) {
        value.forEach((item, index) => walkKeys(item, visitor, [...pathParts, String(index)]));
        return;
    }
    if (!isPlainObject(value)) return;
    for (const [key, child] of Object.entries(value)) {
        visitor(key, child, [...pathParts, key]);
        walkKeys(child, visitor, [...pathParts, key]);
    }
}

function walkStrings(value, visitor, pathParts = []) {
    if (typeof value === "string") {
        visitor(value, pathParts);
        return;
    }
    if (Array.isArray(value)) {
        value.forEach((item, index) => walkStrings(item, visitor, [...pathParts, String(index)]));
        return;
    }
    if (!isPlainObject(value)) return;
    for (const [key, child] of Object.entries(value)) {
        walkStrings(child, visitor, [...pathParts, key]);
    }
}

export function validatePublicQaEvidence(record, fileName = "<record>") {
    const errors = [];
    if (!isPlainObject(record)) return [`${fileName}: 최상위 값은 JSON 객체여야 합니다.`];
    if (record.schema !== PUBLIC_QA_EVIDENCE_SCHEMA) errors.push(`${fileName}: 공개 schema가 다릅니다.`);
    if (!FULL_SHA_PATTERN.test(String(record.head_sha ?? ""))) errors.push(`${fileName}: HEAD SHA가 40자가 아닙니다.`);
    if (fileName !== "<record>" && EVIDENCE_FILE_PATTERN.test(fileName)) {
        if (fileName !== `${record.head_sha}.json`) errors.push(`${fileName}: 파일명과 HEAD SHA가 다릅니다.`);
    }
    if (record.base_sha !== null && !SHA_PATTERN.test(String(record.base_sha))) {
        errors.push(`${fileName}: base SHA 형식이 올바르지 않습니다.`);
    }
    if (typeof record.result !== "string" || !record.result.trim()) errors.push(`${fileName}: result가 없습니다.`);
    if (typeof record.scenario !== "string" || !record.scenario.trim()) errors.push(`${fileName}: scenario가 없습니다.`);
    if (!Array.isArray(record.not_covered)) errors.push(`${fileName}: not_covered는 배열이어야 합니다.`);
    if (!isPlainObject(record.source) || typeof record.source.original_file !== "string") {
        errors.push(`${fileName}: source 원본 매핑이 없습니다.`);
    }

    walkKeys(record, (key, _value, keyPath) => {
        if (SENSITIVE_KEY_PATTERN.test(key) || SENSITIVE_COLLECTION_KEYS.has(key.toLowerCase())) {
            errors.push(`${fileName}: 공개 금지 키가 남았습니다: ${keyPath.join(".")}`);
        }
    });

    const text = JSON.stringify(record);
    for (const [pattern, label] of [
        [EMAIL_PATTERN, "email"],
        [IPV4_PATTERN, "network address"],
        [UUID_PATTERN, "private identifier"],
        [EMULATOR_SERIAL_PATTERN, "emulator serial"],
        [PRIVATE_AVD_NAME_PATTERN, "private AVD name"],
        [QA_CREDENTIAL_PATTERN, "QA credential"],
        [QA_ROLE_ALIAS_PATTERN, "QA role alias"],
        [QA_COMPACT_ROLE_ALIAS_PATTERN, "QA compact role alias"],
        [ENGLISH_QA_DISPLAY_ALIAS_PATTERN, "QA display alias"],
        [QA_ACCOUNT_PATTERN, "QA account"],
        [LOCAL_HOST_PATTERN, "local host"],
        [LOCAL_PATH_PATTERN, "local path"],
    ]) {
        pattern.lastIndex = 0;
        if (pattern.test(text)) errors.push(`${fileName}: ${label} 패턴이 남았습니다.`);
    }
    walkStrings(record, (value, valuePath) => {
        BARE_JSON_REDACTION_PATTERN.lastIndex = 0;
        if (BARE_JSON_REDACTION_PATTERN.test(value)) {
            errors.push(`${fileName}: ${valuePath.join(".")}에 따옴표 없는 JSON 비식별화 표지가 남았습니다.`);
        }
        for (const [pattern, label] of [
            [JSON_PRIVATE_NUMERIC_ID_PATTERN, "private numeric identifier"],
            [PRIVATE_NUMERIC_ID_PATTERN, "private numeric identifier"],
            [KOREAN_NUMERIC_ID_PATTERN, "private numeric identifier"],
            ...AUTH_VALUE_PATTERNS.map((pattern) => [pattern, "authentication value"]),
            [JSON_NAMED_PERSON_PATTERN, "person name"],
            [QUOTED_PERSON_PATTERN, "person name"],
            [NAMED_PERSON_PATTERN, "person name"],
            [PERSON_HONORIFIC_PATTERN, "person name"],
        ]) {
            pattern.lastIndex = 0;
            if (pattern.test(value)) {
                errors.push(`${fileName}: ${valuePath.join(".")}에 ${label} 패턴이 남았습니다.`);
            }
        }
    });
    URL_PATTERN.lastIndex = 0;
    for (const match of text.matchAll(URL_PATTERN)) {
        if (!isPublicGitHubReference(match[0])) errors.push(`${fileName}: 비공개 URL이 남았습니다.`);
    }

    if (errors.length > 0) throw new Error(errors.join("\n"));
    return [];
}

function evidenceFiles(directory) {
    if (!fs.existsSync(directory)) return [];
    return fs
        .readdirSync(directory)
        .filter((name) => EVIDENCE_FILE_PATTERN.test(name))
        .sort();
}

function readJson(filePath) {
    return JSON.parse(fs.readFileSync(filePath, "utf8"));
}

function buildManifest(directory, migratedOn, initialMigrationCount) {
    const records = evidenceFiles(directory).map((file) => {
        const text = fs.readFileSync(path.join(directory, file), "utf8");
        const record = JSON.parse(text);
        validatePublicQaEvidence(record, file);
        return {
            file,
            source_file: record.source.original_file,
            head_sha: record.head_sha,
            sha256: sha256(text),
        };
    });
    return {
        schema: 1,
        initial_migration_count: initialMigrationCount,
        record_count: records.length,
        last_updated_on: migratedOn,
        records,
    };
}

export function migrateQaEvidence(sourceDirectory, destinationDirectory, options = {}) {
    const migratedOn = options.migratedOn ?? new Date().toISOString().slice(0, 10);
    const sourceFiles = fs
        .readdirSync(sourceDirectory)
        .filter((name) => name.endsWith(".json"))
        .sort();
    if (sourceFiles.length === 0) throw new Error("이관할 QA JSON이 없습니다.");

    fs.mkdirSync(destinationDirectory, { recursive: true });
    const generated = [];
    for (const sourceFile of sourceFiles) {
        const raw = readJson(path.join(sourceDirectory, sourceFile));
        const record = buildPublicQaEvidence(raw, sourceFile);
        const destinationFile = `${record.head_sha}.json`;
        const destinationPath = path.join(destinationDirectory, destinationFile);
        const nextText = jsonText(record);
        if (
            fs.existsSync(destinationPath) &&
            fs.readFileSync(destinationPath, "utf8") !== nextText &&
            options.replaceExisting !== true
        ) {
            throw new Error(
                `${destinationFile}: 기존 공개 기록과 다릅니다. 시나리오를 합친 원본을 확인한 뒤 --replace-existing을 명시하세요.`,
            );
        }
        fs.writeFileSync(destinationPath, nextText);
        generated.push(destinationFile);
    }

    const existingManifestPath = path.join(destinationDirectory, "manifest.json");
    const existingManifest = fs.existsSync(existingManifestPath) ? readJson(existingManifestPath) : null;
    const initialMigrationCount = existingManifest?.initial_migration_count ?? generated.length;
    const manifest = buildManifest(destinationDirectory, migratedOn, initialMigrationCount);
    fs.writeFileSync(existingManifestPath, jsonText(manifest));
    return verifyQaEvidenceMigration(sourceDirectory, destinationDirectory);
}

export function verifyQaEvidenceMigration(sourceDirectory, destinationDirectory) {
    const sourceFiles = fs
        .readdirSync(sourceDirectory)
        .filter((name) => name.endsWith(".json"))
        .sort();
    const seenHeads = new Set();
    const mappings = { result: 0, scenario: 0, not_covered: 0 };

    for (const sourceFile of sourceFiles) {
        const raw = readJson(path.join(sourceDirectory, sourceFile));
        const expected = buildPublicQaEvidence(raw, sourceFile);
        const publicFile = `${expected.head_sha}.json`;
        const actual = readJson(path.join(destinationDirectory, publicFile));
        validatePublicQaEvidence(actual, publicFile);
        if (jsonText(actual) !== jsonText(expected)) {
            throw new Error(`${sourceFile}: 공개 기록이 정규화 결과와 다릅니다.`);
        }
        if (seenHeads.has(actual.head_sha)) throw new Error(`${sourceFile}: 중복 HEAD SHA입니다.`);
        seenHeads.add(actual.head_sha);
        mappings.result += 1;
        mappings.scenario += 1;
        mappings.not_covered += 1;
    }

    const publicSummary = validatePublicQaEvidenceDirectory(destinationDirectory);
    return {
        source_count: sourceFiles.length,
        migrated_count: seenHeads.size,
        fields_verified: mappings,
        public_record_count: publicSummary.record_count,
    };
}

export function validatePublicQaEvidenceDirectory(directory) {
    const manifestPath = path.join(directory, "manifest.json");
    if (!fs.existsSync(manifestPath)) throw new Error("QA evidence manifest.json이 없습니다.");
    const manifest = readJson(manifestPath);
    const files = evidenceFiles(directory);
    if (manifest.record_count !== files.length || manifest.records?.length !== files.length) {
        throw new Error("QA evidence manifest 건수가 실제 파일 수와 다릅니다.");
    }

    const manifestByFile = new Map(manifest.records.map((entry) => [entry.file, entry]));
    for (const file of files) {
        const text = fs.readFileSync(path.join(directory, file), "utf8");
        const record = JSON.parse(text);
        validatePublicQaEvidence(record, file);
        const entry = manifestByFile.get(file);
        if (!entry) throw new Error(`${file}: manifest 항목이 없습니다.`);
        if (entry.head_sha !== record.head_sha || entry.source_file !== record.source.original_file) {
            throw new Error(`${file}: manifest 원본 매핑이 다릅니다.`);
        }
        if (entry.sha256 !== sha256(text)) throw new Error(`${file}: manifest 해시가 다릅니다.`);
    }
    return { record_count: files.length };
}

function parseCliArguments(argv) {
    const [command, firstPath, secondPath, ...rest] = argv;
    const options = {};
    for (let index = 0; index < rest.length; index += 1) {
        if (rest[index] === "--replace-existing") {
            options.replaceExisting = true;
            continue;
        }
        if (rest[index] === "--migrated-on" && rest[index + 1]) {
            options.migratedOn = rest[index + 1];
            index += 1;
            continue;
        }
        throw new Error("Usage: qa-evidence.mjs migrate <source-dir> <destination-dir> [--migrated-on YYYY-MM-DD] [--replace-existing] | verify <source-dir> <destination-dir> | validate <destination-dir>");
    }
    return { command, firstPath, secondPath, options };
}

function runCli() {
    const { command, firstPath, secondPath, options } = parseCliArguments(process.argv.slice(2));
    let summary;
    if (command === "migrate" && firstPath && secondPath) {
        summary = migrateQaEvidence(firstPath, secondPath, options);
    } else if (command === "verify" && firstPath && secondPath) {
        summary = verifyQaEvidenceMigration(firstPath, secondPath);
    } else if (command === "validate" && firstPath && !secondPath) {
        summary = validatePublicQaEvidenceDirectory(firstPath);
    } else {
        throw new Error("Usage: qa-evidence.mjs migrate <source-dir> <destination-dir> [--migrated-on YYYY-MM-DD] [--replace-existing] | verify <source-dir> <destination-dir> | validate <destination-dir>");
    }
    process.stdout.write(`${JSON.stringify(summary)}\n`);
}

const invokedFile = process.argv[1] ? pathToFileURL(path.resolve(process.argv[1])).href : null;
if (invokedFile === import.meta.url) {
    try {
        runCli();
    } catch (error) {
        process.stderr.write(`${error.message}\n`);
        process.exitCode = 1;
    }
}
