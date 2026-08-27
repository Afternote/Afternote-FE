import assert from "node:assert/strict";
import test from "node:test";

import { resolveAndroidTestRef } from "./resolve-android-test-ref.mjs";

function bodyWith(metadata) {
    return `## QA Metadata
\`\`\`json
${JSON.stringify(metadata)}
\`\`\``;
}

test("유효한 required 결정에서 exact testRef를 꺼낸다", () => {
    const testRef = "app/src/androidTest/java/com/afternote/FlowAndroidTest.kt#flow_succeeds";
    const body = bodyWith({
        scope: "app-runtime",
        precondition: "로그인한 사용자가 작성 화면에 있다",
        action: "등록 버튼을 눌러 실제 화면 흐름을 실행한다",
        expected: "완료 화면과 저장 결과가 함께 보인다",
        risk: "사용자가 작성 결과를 저장하지 못한다",
        androidTest: {
            required: true,
            reason: "실제 Compose와 navigation 경계를 통과해야 한다",
            testRef,
        },
        evidence: [
            {
                kind: "test",
                ref: testRef,
                assertion: "사용자 입력이 실제 화면 전환과 저장을 만든다",
                input: "등록 버튼 클릭",
                boundary: "AndroidJUnit4 Activity와 Compose navigation",
                observation: "완료 화면과 repository 저장 호출",
            },
        ],
    });

    assert.equal(resolveAndroidTestRef(body, 1), testRef);
});

test("invalid 또는 false 결정은 실행 ref를 만들지 않는다", () => {
    assert.equal(resolveAndroidTestRef("## 설명\n없음", 2), "");
});
