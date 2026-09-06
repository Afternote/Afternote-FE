#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd -- "${script_dir}/.." && pwd)"
# shellcheck source=scripts/jarsigner-verification-policy.sh
# repository-quality checks each script independently without -x.
# shellcheck disable=SC1091
source "${script_dir}/jarsigner-verification-policy.sh"
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
# jarsigner는 unsigned entry가 섞여 있어도 "jar verified."와 exit 0을 내므로 -strict로 경고를 exit code에 승격시킨다.
# -strict exit code는 경고 비트의 합이다. 자가서명 upload key는 인증서 체인 그룹(4)을 항상 세우므로 0·4만 허용하고,
# unsigned entry(16)·서명 검증 실패(1) 등 나머지 비트는 실패로 처리한다.
strict_status=0
verification_output="$(jarsigner -verify -strict "${bundle_path}" 2>&1)" || strict_status=$?
verify_jarsigner_result "${strict_status}" "${verification_output}"

sha256_file() {
    if command -v shasum >/dev/null 2>&1; then
        shasum -a 256 "$1" | awk '{print $1}'
    elif command -v sha256sum >/dev/null 2>&1; then
        sha256sum "$1" | awk '{print $1}'
    else
        echo "SHA-256 계산 도구(shasum 또는 sha256sum)가 없습니다." >&2
        return 1
    fi
}

# 미주입 로컬 빌드도 Gradle 과 같은 기본값으로 대조한다. 값을 중복 선언하면 Gradle 의
# 기본값이 바뀌었을 때 올바른 AAB 를 거절하므로 원본 상수를 읽되, 읽기 실패는 허용하지 않는다.
if [[ "${AFTERNOTE_VERSION_CODE+x}" == "x" ]]; then
    expected_version_code="$(printf '%s' "${AFTERNOTE_VERSION_CODE}" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"
else
    expected_version_code="$(sed -n 's/^const val DEFAULT_AFTERNOTE_VERSION_CODE = \([0-9_]*\)$/\1/p' \
        "${repo_root}/build-logic/src/main/kotlin/VersionCode.kt" | tr -d '_')"
fi
if [[ ! "${expected_version_code}" =~ ^[1-9][0-9]*$ ]] ||
    [[ ${#expected_version_code} -gt 10 ]] || [[ "${expected_version_code}" -gt 2100000000 ]]; then
    echo "AFTERNOTE_VERSION_CODE 또는 Gradle 기본값은 1 이상 2100000000 이하의 10진 정수여야 합니다." >&2
    exit 1
fi

# preflight 가 검증한 jar 를 재사용할 수 있다. 로컬/Play CI 에 jar 가 없으면 같은 고정
# 버전을 일회용 경로에 받고, 제공된 jar 도 실행 직전에 정본 SHA-256 과 대조한다.
readonly bundletool_version="1.18.3"
readonly bundletool_sha256="a099cfa1543f55593bc2ed16a70a7c67fe54b1747bb7301f37fdfd6d91028e29"
bundletool_jar="${BUNDLETOOL_JAR:-}"
if [[ -z "${bundletool_jar}" ]]; then
    bundletool_dir="$(mktemp -d "${TMPDIR:-/tmp}/afternote-bundletool.XXXXXX")"
    trap 'rm -rf "${bundletool_dir}"' EXIT
    bundletool_jar="${bundletool_dir}/bundletool-all-${bundletool_version}.jar"
    curl --fail --location --silent --show-error \
        --output "${bundletool_jar}" \
        "https://github.com/google/bundletool/releases/download/${bundletool_version}/bundletool-all-${bundletool_version}.jar"
fi
if [[ ! -s "${bundletool_jar}" ]] || [[ "$(sha256_file "${bundletool_jar}")" != "${bundletool_sha256}" ]]; then
    echo "bundletool SHA-256 불일치 또는 파일 누락: ${bundletool_jar}" >&2
    exit 1
fi
if ! manifest_version_code="$(java -jar "${bundletool_jar}" dump manifest \
    --bundle="${bundle_path}" --module=base --xpath=/manifest/@android:versionCode)"; then
    echo "bundletool 로 AAB manifest versionCode를 읽지 못했습니다." >&2
    exit 1
fi
if [[ ! "${manifest_version_code}" =~ ^[1-9][0-9]*$ ]] || [[ "${manifest_version_code}" != "${expected_version_code}" ]]; then
    echo "AAB manifest versionCode(${manifest_version_code})가 기대값(${expected_version_code})과 다릅니다." >&2
    exit 1
fi

bundle_sha256="$(sha256_file "${bundle_path}")"
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
printf 'versionCode: %s\n' "${manifest_version_code}"
printf '크기(bytes): %s\n' "${bundle_size}"
printf 'AAB SHA-256: %s\n' "${bundle_sha256}"
printf '서명 인증서 SHA-256: %s\n' "${signer_sha256}"
printf 'R8 mapping: %s\n' "${mapping_path}"
