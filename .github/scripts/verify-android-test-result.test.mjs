import assert from "node:assert/strict";
import test from "node:test";

import { verifyAndroidTestXml } from "./verify-android-test-result.mjs";

const testRef =
    "app/src/androidTest/java/com/afternote/CustomerCenterAndroidTest.kt#bothEntryPoints_openCustomerCenter";

test("선언한 class와 method의 성공 testcase를 직접 실행 증거로 인정한다", () => {
    const result = verifyAndroidTestXml(testRef, [
        {
            file: "TEST-pixel2.xml",
            xml: `<?xml version="1.0"?><testsuite><testcase classname="com.afternote.CustomerCenterAndroidTest" name="bothEntryPoints_openCustomerCenter" time="1.2"/></testsuite>`,
        },
    ]);

    assert.equal(result.testName, "bothEntryPoints_openCustomerCenter");
});

test("동명이지만 다른 class인 testcase는 직접 증거가 아니다", () => {
    assert.throws(
        () =>
            verifyAndroidTestXml(testRef, [
                {
                    file: "TEST-other.xml",
                    xml: `<testsuite><testcase classname="com.afternote.OtherAndroidTest" name="bothEntryPoints_openCustomerCenter"/></testsuite>`,
                },
            ]),
        /실행 결과를 찾지 못했습니다/,
    );
});

test("failure, error, skipped testcase를 성공 증거로 인정하지 않는다", () => {
    for (const result of ["failure", "error", "skipped"]) {
        assert.throws(
            () =>
                verifyAndroidTestXml(testRef, [
                    {
                        file: `TEST-${result}.xml`,
                        xml: `<testsuite><testcase classname="com.afternote.CustomerCenterAndroidTest" name="bothEntryPoints_openCustomerCenter"><${result}/></testcase></testsuite>`,
                    },
                ]),
            /성공하지 않았습니다/,
        );
    }
});
