import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import fs from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const scriptPath = fileURLToPath(
    new URL("./render-distribution-release-notes.sh", import.meta.url),
);

async function runRenderer(t, qaPoints) {
    const directory = await fs.mkdtemp(path.join(os.tmpdir(), "afternote-release-notes-"));
    t.after(() => fs.rm(directory, { recursive: true, force: true }));
    const outputPath = path.join(directory, "notes.txt");
    const result = spawnSync("bash", [scriptPath, outputPath], {
        encoding: "utf8",
        env: {
            ...process.env,
            EVENT_NAME: "workflow_dispatch",
            ISSUE_NUMBERS: "#550",
            QA_POINTS: qaPoints,
            SOURCE_REF: "develop",
            SOURCE_SHA: "1234567890abcdef",
        },
    });
    return { result, outputPath };
}

test("release notes reject an empty QA section", async (t) => {
    const { result } = await runRenderer(t, "");

    assert.equal(result.status, 1);
    assert.match(result.stderr, /QA 포인트에는 확인할 동작과 기대 결과가 하나 이상 필요합니다/);
});

test("release notes reject the former generic fallback", async (t) => {
    const { result } = await runRenderer(
        t,
        "#550 관련 동작을 재현하고 수정 후 기대 결과가 충족되는지 확인",
    );

    assert.equal(result.status, 1);
    assert.match(result.stderr, /generic fallback/);
});

test("release notes reject the release-scope placeholder", async (t) => {
    const { result } = await runRenderer(
        t,
        "테스터가 실행할 동작과 기대 결과를 직접 채워 주세요.",
    );

    assert.equal(result.status, 1);
    assert.match(result.stderr, /generic fallback/);
});

test("release notes retain an actionable QA scenario", async (t) => {
    const point = "삭제 확인을 누르면 성공 시 목록에서 제거되고 실패 시 기존 항목과 오류 안내가 유지된다";
    const { result, outputPath } = await runRenderer(t, point);

    assert.equal(result.status, 0, result.stderr);
    assert.match(await fs.readFile(outputPath, "utf8"), new RegExp(point));
});
