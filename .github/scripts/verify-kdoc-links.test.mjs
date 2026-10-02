import assert from "node:assert/strict";
import { mkdtemp, mkdir, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import test from "node:test";

import { scanKdocLinks } from "./verify-kdoc-links.mjs";

async function fixture(files) {
    const root = await mkdtemp(join(tmpdir(), "kdoc-links-"));
    for (const [rel, body] of Object.entries(files)) {
        await mkdir(join(root, rel, ".."), { recursive: true });
        await writeFile(join(root, rel), body);
    }
    return root;
}

const A = `package com.afternote.feature.x.presentation.detail

/** 상세 상태. */
sealed interface DetailUiState {
    data class Success(val isDeleting: Boolean) : DetailUiState
    data object Error : DetailUiState
}

class DetailViewModel {
    fun onDeleteResultConsumed() = Unit
}

@Composable
fun AccountDetailScreen() = Unit

fun <T> List<T>?.orEmptyList(): List<T> = this ?: emptyList()
`;

test("타입·중첩·멤버·최상위 함수·패키지 링크는 전부 해석된다", async () => {
    const root = await fixture({
        "feature/x/presentation/src/main/kotlin/A.kt": A,
        "feature/x/presentation/src/main/kotlin/B.kt": `package com.afternote.feature.x.presentation.other

/**
 * [com.afternote.feature.x.presentation.detail.DetailUiState] ·
 * [com.afternote.feature.x.presentation.detail.DetailUiState.Success.isDeleting] ·
 * [com.afternote.feature.x.presentation.detail.DetailViewModel.onDeleteResultConsumed] ·
 * [com.afternote.feature.x.presentation.detail.AccountDetailScreen] ·
 * [com.afternote.feature.x.presentation.detail.orEmptyList] ·
 * [com.afternote.feature.x.presentation.detail] · [com.afternote.feature.x]
 */
class B
`,
    });
    try {
        const { total, broken } = scanKdocLinks(root);
        assert.equal(total, 7);
        assert.deepEqual(broken, []);
    } finally { await rm(root, { recursive: true, force: true }); }
});

test("옛 패키지를 가리키는 링크는 끊김으로 잡히고 현재 위치가 후보로 붙는다", async () => {
    const root = await fixture({
        "feature/x/presentation/src/main/kotlin/A.kt": A,
        "feature/x/presentation/src/main/kotlin/C.kt": `package com.afternote.feature.x.presentation

/** [com.afternote.feature.x.presentation.author.detail.DetailViewModel] 과 같다. */
class C
`,
    });
    try {
        const { broken } = scanKdocLinks(root);
        assert.equal(broken.length, 1);
        assert.equal(broken[0].link, "com.afternote.feature.x.presentation.author.detail.DetailViewModel");
        assert.deepEqual(broken[0].candidates, ["com.afternote.feature.x.presentation.detail.DetailViewModel"]);
    } finally { await rm(root, { recursive: true, force: true }); }
});

test("사라진 선언은 후보 없이, 없는 멤버는 타입 위치를 후보로 달고 끊김이다", async () => {
    const root = await fixture({
        "feature/x/presentation/src/main/kotlin/A.kt": A,
        "feature/x/presentation/src/main/kotlin/D.kt": `package com.afternote.feature.x.presentation.detail

/** [com.afternote.feature.x.presentation.detail.Vanished] · [com.afternote.feature.x.presentation.detail.DetailViewModel.nope] */
class D
`,
    });
    try {
        const { broken } = scanKdocLinks(root);
        // 타입은 있는데 멤버가 없으면 «어디를 보고 고칠지» 를 알려 주는 게 맞다 — 후보는 타입 위치 하나.
        const byName = Object.fromEntries(broken.map((b) => [b.link.split(".").at(-1), b.candidates]));
        assert.deepEqual(byName, {
            Vanished: [],
            nope: ["com.afternote.feature.x.presentation.detail.DetailViewModel"],
        });
    } finally { await rm(root, { recursive: true, force: true }); }
});

test("빌드 산출물과 점 디렉터리(중첩 워크트리)는 걷지 않는다", async () => {
    const root = await fixture({
        "feature/x/presentation/src/main/kotlin/A.kt": A,
        ".claude/worktrees/other/feature/x/src/main/kotlin/Z.kt": `package com.afternote.z
/** [com.afternote.nowhere.Gone] */
class Z
`,
        "feature/x/presentation/build/generated/Gen.kt": `package com.afternote.gen
/** [com.afternote.nowhere.Gone] */
class Gen
`,
    });
    try {
        assert.deepEqual(scanKdocLinks(root).broken, []);
    } finally { await rm(root, { recursive: true, force: true }); }
});

test("Repository Quality 가 저장소 전체에 스캐너를 돌린다", async () => {
    const workflow = await readFile(new URL("../workflows/repository-quality.yml", import.meta.url), "utf8");
    assert.match(workflow, /- name: Verify KDoc FQN links\n\s+run: node \.github\/scripts\/verify-kdoc-links\.mjs$/m);
});
