#!/usr/bin/env bash

# 릴리스 산출물 기동 스모크.
#
# R8 이 만든 런타임 결함은 빌드·서명·크기 검증을 전부 통과한다 — 이름을 «문자열로» 찾는 코드
# (navigation 의 enum 인자, 직렬화 serialName, 리플렉션)는 컴파일도 되고 패키징도 되지만 기동에서
# 죽는다. #1753 이 그랬다: nav enum keep 이 없어 배포본이 스플래시도 못 넘겼는데 릴리스 PR 의
# 체크는 전부 초록이었다. 최소 조건은 하나다 — «배포될 그 APK 가 실제로 켜지는가».
#
# 대상은 preflight 가 이미 만든 universal APK 다. 여기서 새로 빌드하지 않으므로 검증 대상과
# 배포 대상이 갈라지지 않는다.

set -euo pipefail

: "${RELEASE_SMOKE_APK_PATH:?RELEASE_SMOKE_APK_PATH is required}"
: "${ANDROID_HOME:?ANDROID_HOME is required}"

package_name="com.afternote.afternote_fe"
# 기기 관련 값은 로컬에서 이 스크립트를 그대로 돌려 볼 수 있도록 덮어쓸 수 있게 둔다.
avd_name="${RELEASE_SMOKE_AVD:-afternote-release-smoke}"
system_image="${RELEASE_SMOKE_SYSTEM_IMAGE:-system-images;android-34;default;x86_64}"
emulator_port="${RELEASE_SMOKE_PORT:-5554}"
serial="emulator-${emulator_port}"
boot_timeout_seconds=420
# 기동 «직후» 가 아니라 잠시 살아 있는지를 본다 — NavHost 조립 크래시는 첫 프레임 언저리에 난다.
settle_seconds=20

adb="${ANDROID_HOME}/platform-tools/adb"
sdkmanager="${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager"
avdmanager="${ANDROID_HOME}/cmdline-tools/latest/bin/avdmanager"
logcat_dump="${RUNNER_TEMP:-/tmp}/release-startup-smoke-logcat.txt"
emulator_log="${RUNNER_TEMP:-/tmp}/release-startup-smoke-emulator.txt"

[[ -s "${RELEASE_SMOKE_APK_PATH}" ]] || {
    echo "::error::스모크 대상 APK 가 없습니다: ${RELEASE_SMOKE_APK_PATH}"
    exit 1
}

emulator_pid=""
cleanup() {
    "${adb}" -s "${serial}" emu kill >/dev/null 2>&1 || true
    if [[ -n "${emulator_pid}" ]]; then
        kill "${emulator_pid}" >/dev/null 2>&1 || true
    fi
}
trap cleanup EXIT

fail() {
    local message="$1"
    echo "::error::${message}"
    if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
        {
            echo "### Release startup smoke: FAIL"
            echo "- ${message}"
            if [[ -s "${logcat_dump}" ]]; then
                echo ""
                echo '```'
                grep -E "FATAL EXCEPTION|AndroidRuntime|${package_name}" "${logcat_dump}" | tail -n 60
                echo '```'
            fi
        } >> "${GITHUB_STEP_SUMMARY}"
    fi
    exit 1
}

# 준비는 멱등하게 — 재실행이나 이미 있는 기기를 파괴하지 않는다.
if ! "${avdmanager}" list avd -c 2>/dev/null | grep -qx "${avd_name}"; then
    "${sdkmanager}" --install "platform-tools" "emulator" "${system_image}" >/dev/null
    echo no | "${avdmanager}" create avd \
        --name "${avd_name}" \
        --package "${system_image}" \
        --device "pixel_6" >/dev/null
fi

"${ANDROID_HOME}/emulator/emulator" -avd "${avd_name}" -port "${emulator_port}" \
    -no-window -no-audio -no-boot-anim -no-snapshot \
    -gpu swiftshader_indirect \
    > "${emulator_log}" 2>&1 &
emulator_pid=$!

boot_deadline=$(( $(date +%s) + boot_timeout_seconds ))
until [[ "$("${adb}" -s "${serial}" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; do
    if grep -qE '^(FATAL|ERROR)' "${emulator_log}"; then
        tail -n 30 "${emulator_log}"
        fail "에뮬레이터가 기동하지 못했습니다."
    fi
    if (( $(date +%s) > boot_deadline )); then
        tail -n 30 "${emulator_log}"
        fail "에뮬레이터 부팅이 ${boot_timeout_seconds}초 안에 끝나지 않았습니다."
    fi
    sleep 5
done

"${adb}" -s "${serial}" install -r "${RELEASE_SMOKE_APK_PATH}"

launcher_activity="$(
    "${adb}" -s "${serial}" shell cmd package resolve-activity --brief \
        -a android.intent.action.MAIN -c android.intent.category.LAUNCHER "${package_name}" |
        tr -d '\r' | tail -n 1
)"
[[ "${launcher_activity}" == "${package_name}/"* ]] ||
    fail "런처 액티비티를 찾지 못했습니다: ${launcher_activity}"

"${adb}" -s "${serial}" logcat -c
"${adb}" -s "${serial}" shell am start -W -n "${launcher_activity}"
sleep "${settle_seconds}"
"${adb}" -s "${serial}" logcat -d > "${logcat_dump}"

# 두 축을 모두 본다 — 크래시 뒤 시스템이 프로세스를 되살리면 pid 만으로는 통과해 버리고(#1753
# 실측에서 실제로 그랬다), 반대로 조용히 죽는 경우엔 logcat 에 FATAL 이 남지 않는다.
if grep -q "FATAL EXCEPTION" "${logcat_dump}"; then
    fail "기동 중 처리되지 않은 예외가 발생했습니다."
fi
if [[ -z "$("${adb}" -s "${serial}" shell pidof "${package_name}" | tr -d '\r')" ]]; then
    fail "기동 후 앱 프로세스가 살아 있지 않습니다."
fi

if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
    {
        echo "### Release startup smoke: PASS"
        echo "- artifact: \`$(basename -- "${RELEASE_SMOKE_APK_PATH}")\`"
        echo "- device: \`${avd_name}\` (\`${system_image}\`)"
        echo "- launched: \`${launcher_activity}\`"
    } >> "${GITHUB_STEP_SUMMARY}"
fi
