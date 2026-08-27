import assert from "node:assert/strict";
import test from "node:test";

import { verifySelectedAndroidTests } from "./verify-android-test-plan-result.mjs";

const result = (body = "") => ({
    file: "TEST.xml",
    xml: `<testsuite><testcase classname="com.example.RuntimeTest" name="works">${body}</testcase></testsuite>`,
});

test("선택한 FQCN#method의 성공 XML을 확인한다", () => {
    assert.equal(verifySelectedAndroidTests(["com.example.RuntimeTest#works"], [result()]), 1);
});

test("full lane의 FQCN class selector는 해당 class의 모든 결과가 성공해야 한다", () => {
    assert.equal(verifySelectedAndroidTests(["com.example.RuntimeTest"], [result()]), 1);
    assert.throws(
        () => verifySelectedAndroidTests(["com.example.RuntimeTest"], [result("<failure/>")]),
        /성공하지 않았습니다/,
    );
    assert.throws(
        () => verifySelectedAndroidTests(["com.example.MissingTest"], [result()]),
        /찾지 못했습니다/,
    );
});

test("누락되거나 실패한 직접 테스트를 거부한다", () => {
    assert.throws(
        () => verifySelectedAndroidTests(["com.example.RuntimeTest#missing"], [result()]),
        /찾지 못했습니다/,
    );
    assert.throws(
        () => verifySelectedAndroidTests(["com.example.RuntimeTest#works"], [result("<failure/>")]),
        /성공하지 않았습니다/,
    );
});
