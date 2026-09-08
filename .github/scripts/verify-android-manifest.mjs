#!/usr/bin/env node

import path from 'node:path';
import process from 'node:process';
import { readFile } from 'node:fs/promises';
import { pathToFileURL } from 'node:url';

export const ALLOWED_PERMISSIONS = new Set([
  'android.permission.ACCESS_NETWORK_STATE',
  'android.permission.FOREGROUND_SERVICE',
  'android.permission.INTERNET',
  'android.permission.POST_NOTIFICATIONS',
  'android.permission.RECEIVE_BOOT_COMPLETED',
  'android.permission.RECORD_AUDIO',
  'android.permission.USE_BIOMETRIC',
  'android.permission.USE_FINGERPRINT',
  'android.permission.WAKE_LOCK',
  'com.google.android.c2dm.permission.RECEIVE',
]);

export const ALLOWED_UNPROTECTED_EXPORTED_COMPONENTS = new Set([
  'androidx.activity.ComponentActivity',
  'androidx.compose.ui.tooling.PreviewActivity',
  'com.afternote.afternote_fe.MainActivity',
  'com.afternote.afternote_fe.debug.DebugSettingsActivity',
  'com.kakao.sdk.auth.AuthCodeHandlerActivity',
]);

// enableEdgeToEdge() 를 쓰는 액티비티는 키보드가 떠도 윈도우 프레임이 줄지 않는다. 그 대신 IME
// inset 이 와야 하는데, adjustResize 선언이 없으면 그마저 오지 않아 Compose 의 imePadding() 이
// 전부 0 을 더한다 — 코드는 살아 있는데 하단 CTA 가 키보드에 덮인다 (#1849). 눈으로도 테스트로도
// 안 잡히는 회귀라 매니페스트 정책으로 못 박는다.
export const IME_RESIZE_REQUIRED_ACTIVITIES = new Set(['com.afternote.afternote_fe.MainActivity']);

function attributes(source) {
  return Object.fromEntries(
    [...source.matchAll(/android:([A-Za-z][A-Za-z0-9]*)\s*=\s*"([^"]*)"/g)].map((match) => [
      match[1],
      match[2],
    ]),
  );
}

export function inspectManifest(
  source,
  {
    allowedPermissions = ALLOWED_PERMISSIONS,
    allowedUnprotectedExportedComponents = ALLOWED_UNPROTECTED_EXPORTED_COMPONENTS,
    imeResizeRequiredActivities = IME_RESIZE_REQUIRED_ACTIVITIES,
  } = {},
) {
  const violations = [];
  const declaredActivities = new Set();
  const application = /<application\b([^>]*)>/s.exec(source);
  if (!application) {
    violations.push('application declaration is missing');
  } else {
    const applicationAttributes = attributes(application[1]);
    if (applicationAttributes.usesCleartextTraffic !== 'false') {
      violations.push('application must explicitly set android:usesCleartextTraffic="false"');
    }
  }

  for (const match of source.matchAll(/<uses-permission\b([^>]*)\/?\s*>/gs)) {
    const permission = attributes(match[1]).name;
    if (!permission) {
      violations.push('uses-permission without android:name');
      continue;
    }
    const isAndroidxDynamicReceiverPermission = permission.endsWith('.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION');
    if (!allowedPermissions.has(permission) && !isAndroidxDynamicReceiverPermission) {
      violations.push(`permission is not allowlisted: ${permission}`);
    }
  }

  for (const match of source.matchAll(/<(activity|activity-alias|service|receiver|provider)\b([^>]*)\/?\s*>/gs)) {
    const [, kind, rawAttributes] = match;
    const component = attributes(rawAttributes);
    if (kind === 'activity' && component.name) {
      declaredActivities.add(component.name);
      const softInputModes = (component.windowSoftInputMode ?? '').split('|');
      if (imeResizeRequiredActivities.has(component.name) && !softInputModes.includes('adjustResize')) {
        violations.push(
          `activity must declare android:windowSoftInputMode="adjustResize": ${component.name}`,
        );
      }
    }
    if (component.exported !== 'true') {
      continue;
    }
    const name = component.name ?? '(missing android:name)';
    if (kind === 'provider') {
      violations.push(`exported provider is forbidden: ${name}`);
      continue;
    }
    if (!component.permission && !allowedUnprotectedExportedComponents.has(name)) {
      violations.push(`unprotected exported ${kind} is not allowlisted: ${name}`);
    }
  }

  for (const activity of imeResizeRequiredActivities) {
    if (!declaredActivities.has(activity)) {
      violations.push(`activity requiring adjustResize is missing: ${activity}`);
    }
  }

  return violations;
}

async function main() {
  const manifestPath = process.argv[2];
  if (!manifestPath || process.argv.length !== 3) {
    throw new Error('Usage: verify-android-manifest.mjs <merged-AndroidManifest.xml>');
  }

  const source = await readFile(manifestPath, 'utf8');
  const violations = inspectManifest(source);
  if (violations.length > 0) {
    throw new Error(violations.map((violation) => `Manifest policy: ${violation}`).join('\n'));
  }
  console.log(`Manifest policy: passed (${manifestPath})`);
}

const invokedPath = process.argv[1] ? pathToFileURL(path.resolve(process.argv[1])).href : '';
if (import.meta.url === invokedPath) {
  main().catch((error) => {
    console.error(error.message);
    process.exitCode = 1;
  });
}
