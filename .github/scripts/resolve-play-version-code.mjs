import { appendFile } from "node:fs/promises";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";

import { ATTEMPT_STRIDE, PLAY_BAND_FLOOR, parseInteger } from "./version-code-bands.mjs";

const MAX_PLAY_VERSION_CODE = 2_100_000_000n;

export function resolvePlayVersionCode(runNumberRaw, runAttemptRaw, latestVersionCodeRaw) {
  const runNumber = parseInteger("GITHUB_RUN_NUMBER", runNumberRaw, { minimum: 1n });
  const runAttempt = parseInteger("GITHUB_RUN_ATTEMPT", runAttemptRaw, { minimum: 1n });
  const latestVersionCode = parseInteger("PLAY_LATEST_VERSION_CODE", latestVersionCodeRaw, {
    minimum: 0n,
  });

  if (runAttempt >= ATTEMPT_STRIDE) {
    throw new Error(`GITHUB_RUN_ATTEMPT must be lower than ${ATTEMPT_STRIDE}.`);
  }

  // 대역 하한을 더해 Firebase 수열(101 ~ 999,999,999)과 겹치지 않게 한다. Play 원장이 비어
  // 있는 지금은 이 하한을 넣어도 단조 증가가 깨지지 않는다.
  const candidate = PLAY_BAND_FLOOR + runNumber * ATTEMPT_STRIDE + runAttempt;
  if (candidate > MAX_PLAY_VERSION_CODE) {
    throw new Error(`Resolved versionCode exceeds Google Play maximum ${MAX_PLAY_VERSION_CODE}.`);
  }
  if (candidate <= latestVersionCode) {
    throw new Error(
      `Resolved versionCode ${candidate} must be greater than current Play versionCode ${latestVersionCode}.`,
    );
  }
  return Number(candidate);
}

async function main() {
  const outputPath = process.env.GITHUB_OUTPUT;
  if (!outputPath) {
    throw new Error("GITHUB_OUTPUT is required.");
  }
  const versionCode = resolvePlayVersionCode(
    process.env.GITHUB_RUN_NUMBER,
    process.env.GITHUB_RUN_ATTEMPT,
    process.env.PLAY_LATEST_VERSION_CODE,
  );
  await appendFile(outputPath, `version_code=${versionCode}\n`, "utf8");
  console.log(`Resolved internal-track versionCode ${versionCode}.`);
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main().catch((error) => {
    console.error(`::error::${error.message}`);
    process.exitCode = 1;
  });
}
