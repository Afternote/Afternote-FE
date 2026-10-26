import assert from "node:assert/strict";
import test from "node:test";

import { resolveFirebaseVersionCode } from "./resolve-firebase-version-code.mjs";
import { resolvePlayVersionCode } from "./resolve-play-version-code.mjs";
import { FIREBASE_BAND_MAX, PLAY_BAND_FLOOR } from "./version-code-bands.mjs";

test("derives distinct increasing codes for runs and reruns", () => {
  const first = resolveFirebaseVersionCode("1", "1");
  const rerun = resolveFirebaseVersionCode("1", "2");
  const nextRun = resolveFirebaseVersionCode("2", "1");

  assert.equal(first, 101);
  assert.ok(first < rerun);
  assert.ok(rerun < nextRun);
});

test("rejects invalid run metadata and excessive attempts", () => {
  assert.throws(() => resolveFirebaseVersionCode("0", "1"), /must be at least 1/);
  assert.throws(() => resolveFirebaseVersionCode("1", "0"), /must be at least 1/);
  assert.throws(() => resolveFirebaseVersionCode("1", "100"), /must be lower than 100/);
  assert.throws(() => resolveFirebaseVersionCode("1.5", "1"), /base-10 integer/);
});

test("stops at the Firebase band ceiling instead of entering the Play band", () => {
  assert.equal(resolveFirebaseVersionCode("9999999", "99"), Number(FIREBASE_BAND_MAX));
  assert.throws(
    () => resolveFirebaseVersionCode("10000000", "1"),
    /must stay within the Firebase band/,
  );
});

// 완료 조건: 두 채널이 같은 값을 낼 수 없다. 산출 가능한 Firebase 최댓값과 Play 최솟값을
// 직접 비교해야 가드가 의미를 갖는다. 한쪽 대역 상수만 움직여도 여기서 걸린다.
test("the two channels cannot produce the same versionCode", () => {
  const largestFirebase = BigInt(resolveFirebaseVersionCode("9999999", "99"));
  const smallestPlay = BigInt(resolvePlayVersionCode("1", "1", "0"));

  assert.ok(largestFirebase < PLAY_BAND_FLOOR);
  assert.ok(smallestPlay >= PLAY_BAND_FLOOR);
  assert.ok(largestFirebase < smallestPlay);
});
