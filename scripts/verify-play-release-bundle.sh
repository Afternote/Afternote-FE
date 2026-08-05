#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd -- "${script_dir}/.." && pwd)"
bundle_path="${repo_root}/app/build/outputs/bundle/release/app-release.aab"
mapping_path="${repo_root}/app/build/outputs/mapping/release/mapping.txt"

case "${1:-}" in
    "")
        "${repo_root}/gradlew" :app:bundleRelease -x :app:uploadCrashlyticsMappingFileRelease --no-daemon --console=plain
        ;;
    --skip-build)
        ;;
    *)
        echo "Usage: $0 [--skip-build]" >&2
        exit 2
        ;;
esac

if [[ ! -s "${bundle_path}" ]]; then
    echo "AAB를 찾을 수 없거나 비어 있습니다: ${bundle_path}" >&2
    exit 1
fi

if [[ ! -s "${mapping_path}" ]]; then
    echo "R8 mapping 파일을 찾을 수 없거나 비어 있습니다: ${mapping_path}" >&2
    exit 1
fi

bundle_entries="$(unzip -Z1 "${bundle_path}")"
required_entries=(
    "BundleConfig.pb"
    "base/manifest/AndroidManifest.xml"
    "base/resources.pb"
    "base/dex/classes.dex"
)

for required_entry in "${required_entries[@]}"; do
    if ! grep -Fqx "${required_entry}" <<<"${bundle_entries}"; then
        echo "AAB 필수 항목이 없습니다: ${required_entry}" >&2
        exit 1
    fi
done

export LC_ALL=C
if ! verification_output="$(jarsigner -verify "${bundle_path}" 2>&1)"; then
    printf '%s\n' "${verification_output}" >&2
    exit 1
fi
if [[ "${verification_output}" != *"jar verified."* ]]; then
    printf '%s\n' "${verification_output}" >&2
    echo "AAB JAR 서명을 확인하지 못했습니다." >&2
    exit 1
fi

if command -v shasum >/dev/null 2>&1; then
    bundle_sha256="$(shasum -a 256 "${bundle_path}" | awk '{print $1}')"
elif command -v sha256sum >/dev/null 2>&1; then
    bundle_sha256="$(sha256sum "${bundle_path}" | awk '{print $1}')"
else
    echo "SHA-256 계산 도구(shasum 또는 sha256sum)가 없습니다." >&2
    exit 1
fi

signer_sha256="$(
    keytool -printcert -jarfile "${bundle_path}" |
        awk -F': ' '/SHA256:/{print $2; exit}'
)"
if [[ -z "${signer_sha256}" ]]; then
    echo "AAB 서명 인증서 SHA-256을 읽지 못했습니다." >&2
    exit 1
fi

bundle_size="$(wc -c < "${bundle_path}" | tr -d '[:space:]')"

printf 'AAB: %s\n' "${bundle_path}"
printf '크기(bytes): %s\n' "${bundle_size}"
printf 'AAB SHA-256: %s\n' "${bundle_sha256}"
printf '서명 인증서 SHA-256: %s\n' "${signer_sha256}"
printf 'R8 mapping: %s\n' "${mapping_path}"
