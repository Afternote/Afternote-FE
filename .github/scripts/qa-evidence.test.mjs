import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import test from "node:test";

import {
    buildPublicQaEvidence,
    migrateQaEvidence,
    validatePublicQaEvidenceDirectory,
    verifyQaEvidenceMigration,
} from "./qa-evidence.mjs";

function rawFixture() {
    return {
        schema: 1,
        repository: "Afternote/Afternote-FE",
        head_ref: "fix/fixture",
        head_sha: "05b5a49170952a4c6f9dea6e1aa4145ba097ad54",
        base_ref: "develop",
        base_sha: "5f0b7bae84726a15a08023fecbf2fb939c9876a2",
        recorded_at: "2026-08-27T10:00:00+09:00",
        result: "PASS",
        scenario:
            "Pixel_7_Example_QA에서 앱 잠금 PIN과 PIN 인증 방식을 사용했다. 이슈 #39에서 QA123 user@example.com 계정의 afternoteId=39, sub=4321, PIN=1234를 입력한 뒤 같은 값 1234와 인증(실제 수신 코드 654321), 같은 코드 654321, 마스터 키(abc123-…) 및 authCode(def456-…), authCode 필드로 얻는다(ghi789-…), 같은 값 def456-…·ghi789-…, 수신자 \"홍길동\"과 빈 수신자 \"—\", ExampleQA 홈, 응답 {\"receiverId\":14,\"name\":\"QA수신자\",\"relation\":\"FAMILY\"}, credentials:{id:qa123_secret, password:secret42}을 http://10.0.2.2:8080에서 확인",
        purpose: "로그인 실패 경로와 복구 동작을 함께 확인",
        not_covered: ["실기기"],
        qa_account: {
            email: "user@example.com",
            afternote_id: 39,
            receiver: { receiverId: "2db6a7cf-70b5-4b70-99f6-4b6046f424af", name: "테스트수신자" },
        },
        app: {
            package: "com.afternote.afternote_fe",
            session: "private-session",
            note: "기기 내부 /data/user/0/com.afternote.afternote_fe/files/session.json 확인",
        },
        device: {
            serial: "emulator-5554",
            avd: "Private_QA_Device",
            is_emulator: true,
            api_level: 35,
            screen: { physical: "1080x2400", density: 420 },
        },
        reviews: ["https://github.com/Afternote/Afternote-FE/pull/1098#discussion_r1"],
        screenshots: ["https://github.com/user-attachments/assets/2db6a7cf-70b5-4b70-99f6-4b6046f424af"],
    };
}

test("sanitizes account, endpoint, device, session, and screenshot identifiers", () => {
    const record = buildPublicQaEvidence(rawFixture(), "fixture.json");
    const text = JSON.stringify(record);

    assert.equal(record.head_sha, rawFixture().head_sha);
    assert.equal(record.result, "PASS");
    assert.deepEqual(record.not_covered, ["실기기"]);
    assert.match(record.scenario, /REDACTED/);
    assert.match(record.scenario, /이슈 #39/);
    assert.doesNotMatch(record.scenario, /afternoteId=39/);
    assert.doesNotMatch(record.scenario, /PIN=1234/);
    assert.doesNotMatch(record.scenario, /1234/);
    assert.doesNotMatch(record.scenario, /654321/);
    assert.doesNotMatch(record.scenario, /abc123/);
    assert.doesNotMatch(record.scenario, /def456/);
    assert.doesNotMatch(record.scenario, /ghi789/);
    assert.doesNotMatch(record.scenario, /Pixel_7_Example_QA/);
    assert.doesNotMatch(record.scenario, /ExampleQA/);
    assert.doesNotMatch(record.scenario, /receiverId\":14/);
    assert.match(record.scenario, /앱 잠금 PIN과 PIN 인증 방식/);
    assert.match(record.scenario, /\"name\":\"\[PERSON_REDACTED\]\",\"relation\"/);
    const embeddedObject = record.scenario.match(/응답 (\{[^}]+\}), credentials/)?.[1];
    assert.ok(embeddedObject);
    assert.doesNotThrow(() => JSON.parse(embeddedObject));
    assert.equal(JSON.parse(embeddedObject).receiverId, "[PRIVATE_ID_REDACTED]");
    assert.doesNotMatch(record.scenario, /:\s*\[[A-Z_]+_REDACTED\]/);
    assert.match(record.scenario, /id:\"\[QA_CREDENTIAL_REDACTED\]\"/);
    assert.match(record.scenario, /password:\"\[[A-Z_]+_REDACTED\]\"/);
    assert.doesNotMatch(record.scenario, /authCode=abcd/);
    assert.doesNotMatch(record.scenario, /4321/);
    assert.doesNotMatch(record.scenario, /홍길동/);
    assert.match(record.scenario, /빈 수신자 \"—\"/);
    assert.equal(record.details.purpose, "로그인 실패 경로와 복구 동작을 함께 확인");
    assert.match(text, /github\.com\/Afternote\/Afternote-FE\/pull\/1098/);
    for (const secret of [
        "user@example.com",
        "QA123",
        "10.0.2.2",
        "emulator-5554",
        "Private_QA_Device",
        "private-session",
        "테스트수신자",
        "2db6a7cf-70b5-4b70-99f6-4b6046f424af",
        "user-attachments",
        "/data/user/0/",
    ]) {
        assert.equal(text.includes(secret), false, secret);
    }
    assert.equal(record.details.redacted_artifacts.screenshot_references, 1);
});

test("migrates and verifies a one-to-one public record", () => {
    const temporaryRoot = fs.mkdtempSync(path.join(os.tmpdir(), "qa-evidence-test-"));
    const source = path.join(temporaryRoot, "source");
    const destination = path.join(temporaryRoot, "destination");
    fs.mkdirSync(source);
    fs.writeFileSync(path.join(source, "fixture.json"), `${JSON.stringify(rawFixture())}\n`);

    const migrated = migrateQaEvidence(source, destination, { migratedOn: "2026-08-27" });
    assert.equal(migrated.source_count, 1);
    assert.equal(migrated.migrated_count, 1);
    assert.deepEqual(migrated.fields_verified, { result: 1, scenario: 1, not_covered: 1 });
    assert.equal(verifyQaEvidenceMigration(source, destination).migrated_count, 1);
    assert.equal(validatePublicQaEvidenceDirectory(destination).record_count, 1);

    const changed = rawFixture();
    changed.result = "FAIL";
    fs.writeFileSync(path.join(source, "fixture.json"), `${JSON.stringify(changed)}\n`);
    assert.throws(
        () => migrateQaEvidence(source, destination, { migratedOn: "2026-08-27" }),
        /--replace-existing/,
    );
    assert.equal(
        migrateQaEvidence(source, destination, {
            migratedOn: "2026-08-27",
            replaceExisting: true,
        }).migrated_count,
        1,
    );
});

test("validates the committed public QA evidence corpus", () => {
    const directory = path.join(process.cwd(), "docs", "qa", "evidence");
    if (!fs.existsSync(path.join(directory, "manifest.json"))) return;

    const summary = validatePublicQaEvidenceDirectory(directory);
    const manifest = JSON.parse(fs.readFileSync(path.join(directory, "manifest.json"), "utf8"));
    assert.equal(manifest.initial_migration_count, 34);
    assert.equal(summary.record_count, manifest.record_count);
    assert.ok(summary.record_count >= 34);

    const multiPass = JSON.parse(
        fs.readFileSync(
            path.join(directory, "8325a703ab3275330b30713b0f4d542c975a2024.json"),
            "utf8",
        ),
    );
    assert.match(multiPass.source.interpretation_notes[0], /후속 패스/);
    assert.match(multiPass.source.interpretation_notes[0], /#607/);
});
