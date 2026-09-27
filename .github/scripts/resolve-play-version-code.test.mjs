import assert from "node:assert/strict";
import test from "node:test";

import { resolvePlayVersionCode } from "./resolve-play-version-code.mjs";
import { PLAY_BAND_FLOOR } from "./version-code-bands.mjs";

test("derives distinct increasing codes for runs and reruns", () => {
  const first = resolvePlayVersionCode("1", "1", "0");
  const rerun = resolvePlayVersionCode("1", "2", String(first));
  const nextRun = resolvePlayVersionCode("2", "1", String(rerun));

  assert.equal(first, 1_000_000_101);
  assert.ok(first < rerun);
  assert.ok(rerun < nextRun);
});

test("keeps even the smallest run inside the Play band", () => {
  assert.ok(BigInt(resolvePlayVersionCode("1", "1", "0")) >= PLAY_BAND_FLOOR);
});

test("fails when the candidate is not greater than Play", () => {
  assert.throws(
    () => resolvePlayVersionCode("10", "1", "1000001001"),
    /must be greater than current Play versionCode/,
  );
});

test("rejects invalid run metadata and excessive attempts", () => {
  assert.throws(() => resolvePlayVersionCode("0", "1", "0"), /must be at least 1/);
  assert.throws(() => resolvePlayVersionCode("1", "0", "0"), /must be at least 1/);
  assert.throws(() => resolvePlayVersionCode("1", "100", "0"), /must be lower than 100/);
  assert.throws(() => resolvePlayVersionCode("1.5", "1", "0"), /base-10 integer/);
});

test("fails before exceeding the Google Play maximum", () => {
  assert.equal(resolvePlayVersionCode("10999999", "99", "0"), 2_099_999_999);
  assert.throws(
    () => resolvePlayVersionCode("11000000", "1", "0"),
    /exceeds Google Play maximum/,
  );
});
