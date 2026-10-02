import { appendFile } from "node:fs/promises";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";

import {
  ATTEMPT_STRIDE,
  FIREBASE_BAND_MAX,
  FIREBASE_BAND_MIN,
  parseInteger,
} from "./version-code-bands.mjs";

// Play 쪽 resolver 와 달리 원장 대조가 없다. Firebase App Distribution 은 versionCode 로
// 빌드를 구분하지 않아 대조할 최댓값 자체가 없기 때문이다. 단조 증가는 이 워크플로가
// run_number 를 단독으로 소유하는 것으로 성립한다. release-distribution.yml 의 트리거는
// push: [main] 하나뿐이다.
export function resolveFirebaseVersionCode(runNumberRaw, runAttemptRaw) {
  const runNumber = parseInteger("GITHUB_RUN_NUMBER", runNumberRaw, { minimum: 1n });
  const runAttempt = parseInteger("GITHUB_RUN_ATTEMPT", runAttemptRaw, { minimum: 1n });

  if (runAttempt >= ATTEMPT_STRIDE) {
    throw new Error(`GITHUB_RUN_ATTEMPT must be lower than ${ATTEMPT_STRIDE}.`);
  }

  const candidate = runNumber * ATTEMPT_STRIDE + runAttempt;
  if (candidate < FIREBASE_BAND_MIN || candidate > FIREBASE_BAND_MAX) {
    throw new Error(
      `Resolved versionCode ${candidate} must stay within the Firebase band ` +
        `${FIREBASE_BAND_MIN}-${FIREBASE_BAND_MAX}.`,
    );
  }
  return Number(candidate);
}

async function main() {
  const outputPath = process.env.GITHUB_OUTPUT;
  if (!outputPath) {
    throw new Error("GITHUB_OUTPUT is required.");
  }
  const versionCode = resolveFirebaseVersionCode(
    process.env.GITHUB_RUN_NUMBER,
    process.env.GITHUB_RUN_ATTEMPT,
  );
  await appendFile(outputPath, `version_code=${versionCode}\n`, "utf8");
  console.log(`Resolved Firebase App Distribution versionCode ${versionCode}.`);
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main().catch((error) => {
    console.error(`::error::${error.message}`);
    process.exitCode = 1;
  });
}
