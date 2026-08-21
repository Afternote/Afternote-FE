#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd -- "${script_dir}/../.." && pwd)"
bundle_path="${repo_root}/app/build/outputs/bundle/release/app-release.aab"
mapping_path="${repo_root}/app/build/outputs/mapping/release/mapping.txt"
rules_path="${repo_root}/app/proguard-rules.pro"

[[ -s "${bundle_path}" && -s "${mapping_path}" ]] || {
    echo "Run the successful release preflight before negative fixtures." >&2
    exit 1
}

fixture_root="$(mktemp -d "${TMPDIR:-/tmp}/afternote-release-negative.XXXXXX")"
rules_backup="${fixture_root}/proguard-rules.pro"
positive_bundle="${fixture_root}/positive.aab"
positive_mapping="${fixture_root}/positive-mapping.txt"
cp "${rules_path}" "${rules_backup}"
cp "${bundle_path}" "${positive_bundle}"
cp "${mapping_path}" "${positive_mapping}"

rules_restored=false
restore_rules() {
    if [[ "${rules_restored}" == "false" ]]; then
        cp "${rules_backup}" "${rules_path}"
        rules_restored=true
    fi
}
cleanup() {
    restore_rules
    rm -rf "${fixture_root}"
}
trap cleanup EXIT

printf '\n-afternote-invalid-keep-rule\n' >> "${rules_path}"
set +e
invalid_rule_output="$("${repo_root}/scripts/verify-play-release-bundle.sh" 2>&1)"
invalid_rule_status=$?
set -e
restore_rules
if [[ ${invalid_rule_status} -eq 0 ]]; then
    echo "Invalid R8 rule fixture unexpectedly passed." >&2
    exit 1
fi
if ! grep -Eiq 'afternote-invalid-keep-rule|unknown option' <<< "${invalid_rule_output}"; then
    printf 'Invalid R8 rule failed for an unexpected reason:\n%s\n' "${invalid_rule_output}" >&2
    exit 1
fi
echo "PASS: invalid R8 keep-rule fixture was rejected."

prepare_verifier_fixture() {
    local name="$1"
    local root="${fixture_root}/${name}"
    mkdir -p \
        "${root}/scripts" \
        "${root}/app/build/outputs/bundle/release" \
        "${root}/app/build/outputs/mapping/release"
    cp "${repo_root}/scripts/verify-play-release-bundle.sh" "${root}/scripts/"
    cp "${positive_bundle}" "${root}/app/build/outputs/bundle/release/app-release.aab"
    cp "${positive_mapping}" "${root}/app/build/outputs/mapping/release/mapping.txt"
    printf '%s' "${root}"
}

missing_entry_root="$(prepare_verifier_fixture missing-entry)"
missing_entry_bundle="${missing_entry_root}/app/build/outputs/bundle/release/app-release.aab"
zip -q -d "${missing_entry_bundle}" base/resources.pb
set +e
missing_entry_output="$("${missing_entry_root}/scripts/verify-play-release-bundle.sh" --skip-build 2>&1)"
missing_entry_status=$?
set -e
if [[ ${missing_entry_status} -eq 0 ]] ||
    ! grep -Fq 'AAB 필수 항목이 없습니다: base/resources.pb' <<< "${missing_entry_output}"; then
    printf 'Missing-entry fixture did not fail closed:\n%s\n' "${missing_entry_output}" >&2
    exit 1
fi
echo "PASS: missing AAB entry fixture was rejected."

signature_root="$(prepare_verifier_fixture invalid-signature)"
signature_bundle="${signature_root}/app/build/outputs/bundle/release/app-release.aab"
mutation_root="${fixture_root}/signature-mutation"
mkdir -p "${mutation_root}/base"
unzip -p "${positive_bundle}" base/resources.pb > "${mutation_root}/base/resources.pb"
printf 'tampered' >> "${mutation_root}/base/resources.pb"
(
    cd "${mutation_root}"
    zip -q -u "${signature_bundle}" base/resources.pb
)
set +e
signature_output="$("${signature_root}/scripts/verify-play-release-bundle.sh" --skip-build 2>&1)"
signature_status=$?
set -e
if [[ ${signature_status} -eq 0 ]] ||
    ! grep -Eiq 'digest error|invalid signature|SecurityException' <<< "${signature_output}"; then
    printf 'Invalid-signature fixture did not fail closed:\n%s\n' "${signature_output}" >&2
    exit 1
fi
echo "PASS: invalid AAB signature fixture was rejected."
