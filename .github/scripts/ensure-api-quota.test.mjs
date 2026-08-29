import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

import { evaluateQuota, formatSummary, parseRateLimit } from "./ensure-api-quota.mjs";

const NOW = 1_700_000_000;

test("core 한도를 읽는다", () => {
    const quota = parseRateLimit({
        resources: { core: { limit: 15000, remaining: 4200, reset: NOW + 600 } },
    });
    assert.deepEqual(quota, { remaining: 4200, limit: 15000, resetAt: NOW + 600 });
});

test("core 가 없으면 rate 폴백을 읽는다", () => {
    const quota = parseRateLimit({ rate: { limit: 5000, remaining: 10, reset: NOW } });
    assert.equal(quota.remaining, 10);
});

test("한도를 못 읽으면 조용히 통과시키지 않는다", () => {
    // 조회 실패를 «여유 있음» 으로 접으면 이 게이트가 통째로 무력해진다.
    assert.throws(() => parseRateLimit({}), /core 한도를 읽지 못했습니다/);
    assert.throws(() => parseRateLimit({ resources: { core: {} } }), /core 한도를 읽지 못했습니다/);
});

test("여유가 있으면 그대로 진행한다", () => {
    const decision = evaluateQuota(
        { remaining: 900, limit: 15000, resetAt: NOW + 600 },
        { minRemaining: 200, maxWaitSeconds: 240, nowSeconds: NOW },
    );
    assert.deepEqual(decision, { action: "proceed", waitSeconds: 0 });
});

test("부족하지만 리셋이 가까우면 기다린다", () => {
    // 즉시 실패시키면 사람이 재실행을 걸고, 그 재실행이 남은 quota 를 더 태운다.
    const decision = evaluateQuota(
        { remaining: 10, limit: 15000, resetAt: NOW + 100 },
        { minRemaining: 200, maxWaitSeconds: 240, nowSeconds: NOW },
    );
    assert.equal(decision.action, "wait");
    // 리셋 경계에서 다시 0 을 보지 않도록 여유를 둔다.
    assert.ok(decision.waitSeconds > 100);
});

test("리셋이 잡 timeout 보다 멀면 기다리지 않는다", () => {
    const decision = evaluateQuota(
        { remaining: 10, limit: 15000, resetAt: NOW + 1800 },
        { minRemaining: 200, maxWaitSeconds: 240, nowSeconds: NOW },
    );
    assert.equal(decision.action, "exhausted");
    assert.equal(decision.waitSeconds, 1800);
});

test("이미 리셋 시각이 지났으면 대기는 0 이다", () => {
    const decision = evaluateQuota(
        { remaining: 0, limit: 15000, resetAt: NOW - 30 },
        { minRemaining: 200, maxWaitSeconds: 240, nowSeconds: NOW },
    );
    assert.equal(decision.action, "wait");
    assert.equal(decision.waitSeconds, 5);
});

test("요약에 남은 호출과 리셋까지 남은 시간이 드러난다", () => {
    // 관측 가능성이 이 스크립트의 절반이다. 숫자가 요약에 없으면 「왜 빨간가」를 다시 파야 한다.
    const summary = formatSummary({ remaining: 4200, limit: 15000, resetAt: NOW + 125 }, { nowSeconds: NOW });
    assert.match(summary, /남은 호출: \*\*4200\*\* \/ 15000/);
    assert.match(summary, /사용 10800/);
    assert.match(summary, /2분 5초/);
});

test("게이트 워크플로가 quota 확인과 실패 분류를 모두 건다", async () => {
    // 스크립트만 있고 워크플로가 부르지 않으면 아무것도 달라지지 않는다.
    const gates = ["merge-order-guard.yml", "review-debt-guard.yml"];
    for (const name of gates) {
        const workflow = await readFile(new URL(`../workflows/${name}`, import.meta.url), "utf8");
        assert.match(workflow, /ensure-api-quota\.mjs ensure/, `${name} 이 사전 quota 확인을 걸지 않았다`);
        assert.match(workflow, /ensure-api-quota\.mjs classify/, `${name} 이 실패 원인 분류를 걸지 않았다`);
        // 분류는 실패했을 때만 의미가 있다.
        assert.match(workflow, /if: failure\(\)\n(?:.*\n)*?\s+node \.github\/scripts\/ensure-api-quota\.mjs classify/);
        // default branch 사본에 스크립트가 아직 없는 부트스트랩 한 번만 건너뛴다. 이 가드가
        // 없으면 스크립트를 심는 PR 이 자기 게이트에서 모듈을 못 찾아 죽는다.
        assert.match(workflow, /if \[ ! -f \.github\/scripts\/ensure-api-quota\.mjs \]; then/);
    }
});
