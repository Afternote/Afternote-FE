import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const canaryWorkflow = await readFile(
  new URL("../workflows/firebase-wif-canary.yml", import.meta.url),
  "utf8",
);
const productionWorkflow = await readFile(
  new URL("../workflows/release-distribution.yml", import.meta.url),
  "utf8",
);

test("WIF canary retains the production JSON rollback path", () => {
  assert.match(productionWorkflow, /FIREBASE_SERVICE_ACCOUNT_JSON/);
  assert.doesNotMatch(canaryWorkflow, /\$\{\{\s*secrets\.FIREBASE_SERVICE_ACCOUNT_JSON\s*\}\}/);
});

test("WIF canary is manual, protected, and branch-restricted", () => {
  assert.match(canaryWorkflow, /workflow_dispatch:/);
  assert.match(canaryWorkflow, /environment: release-distribution/);
  assert.match(canaryWorkflow, /refs\/heads\/develop\|refs\/heads\/main/);
  assert.match(canaryWorkflow, /CONFIRM_UPLOAD/);
});

test("WIF credentials are issued only after the release APK build", () => {
  const buildIndex = canaryWorkflow.indexOf("./gradlew assembleRelease");
  const authIndex = canaryWorkflow.indexOf(
    "google-github-actions/auth@7c6bc770dae815cd3e89ee6cdf493a5fab2cc093",
  );
  const uploadIndex = canaryWorkflow.indexOf("appDistributionUploadRelease");

  assert.notEqual(buildIndex, -1);
  assert.notEqual(authIndex, -1);
  assert.notEqual(uploadIndex, -1);
  assert.ok(buildIndex < authIndex);
  assert.ok(authIndex < uploadIndex);
  assert.match(canaryWorkflow, /id-token: write/);
});

test("WIF canary never publishes the signed APK as an Actions artifact", () => {
  assert.doesNotMatch(canaryWorkflow, /actions\/upload-artifact/);
  assert.doesNotMatch(canaryWorkflow, /actions\/download-artifact/);
});
