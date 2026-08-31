import assert from "node:assert/strict";
import { fileURLToPath } from "node:url";
import path from "node:path";
import test from "node:test";

import { inspectKtlintCoverage, resolveKtlintBearingConventions } from "./ktlint-coverage.mjs";

const root = path.resolve(fileURLToPath(new URL("../..", import.meta.url)));

test("every registered module has a ktlintCheck task", async () => {
    // resolve-pr-impact.mjs 는 `.kt`·`.kts` 가 바뀐 모듈마다 `<모듈>:ktlintCheck` 를 고르고,
    // 그 태스크가 실재하는지는 확인하지 않는다 (#1419). 모든 모듈이 자기 build.gradle.kts 를
    // 갖고 그 파일이 `.kts` 라, 태스크가 없는 모듈은 자기 빌드 스크립트만 고쳐도 Ktlint job 을
    // 태스크 선택 단계에서 죽인다. 전역 폴백(globalKtlintChange)이 걸린 PR 은 이 구멍을 지나쳐
    // 잠복하므로, 새 모듈이 추가되는 시점에 여기서 잡는다.
    const modules = await inspectKtlintCoverage(root);
    const uncovered = modules.filter(({ hasKtlint }) => !hasKtlint).map(({ projectPath }) => projectPath);
    assert.deepEqual(
        uncovered,
        [],
        `ktlintCheck 가 없는 모듈: ${uncovered.join(", ")} — 컨벤션 플러그인을 타거나 ` +
            `id("org.jlleitschuh.gradle.ktlint") 를 직접 적용해야 한다`,
    );
});

test("the convention chain that carries ktlint is discovered transitively", async () => {
    // 목록을 손으로 적으면 컨벤션이 새로 생길 때 표류한다. 사슬을 타고 계산하는지 확인한다 —
    // android.lint 가 ktlint 를 직접 적용하는 시드이고, android.library 가 그걸 apply 하며,
    // android.library.compose 가 다시 android.library 를 apply 한다.
    const bearing = await resolveKtlintBearingConventions(root);
    assert.ok(bearing.has("afternote.android.lint"), "직접 적용 시드를 못 찾았다");
    assert.ok(bearing.has("afternote.android.library"), "1단계 전이를 못 따라갔다");
    assert.ok(bearing.has("afternote.android.library.compose"), "2단계 전이를 못 따라갔다");
    assert.ok(bearing.has("afternote.jvm.library"), "JVM 쪽 시드를 못 찾았다");
    assert.ok(bearing.has("afternote.jvm.domain"), "JVM 전이를 못 따라갔다");
    // ktlint 와 무관한 컨벤션까지 물들면 판정이 무의미해진다.
    assert.ok(!bearing.has("afternote.kover"), "무관한 컨벤션이 섞였다");
});
