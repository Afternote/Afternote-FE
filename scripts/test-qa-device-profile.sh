#!/usr/bin/env bash

# qa-device-profile.sh 의 동작을 adb 스텁으로 검증한다. 실기기·에뮬레이터 없이 돈다.

set -euo pipefail

script_directory=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)
profile_script="${script_directory}/qa-device-profile.sh"
fixture_root=$(mktemp -d /tmp/qa-device-profile-fixtures.XXXXXX)

cleanup() {
    rm -rf "$fixture_root"
}
trap cleanup EXIT

failures=0

# 스텁 adb 는 호출 인자를 call_log 에 적고, 화면 상태는 환경변수로 흉내낸다.
create_adb_stub() {
    local stub_path="${fixture_root}/adb"

    cat > "$stub_path" <<'STUB'
#!/usr/bin/env bash
echo "$*" >> "$STUB_CALL_LOG"

case "$*" in
    devices)
        echo "List of devices attached"
        printf '%b\n' "$STUB_DEVICES"
        ;;
    *"wm size reset"* | *"wm density reset"*)
        : > "$STUB_OVERRIDE_CLEARED"
        ;;
    *"wm size "* | *"wm density "*)
        ;;
    *"wm size")
        echo "Physical size: $STUB_PHYSICAL_SIZE"
        [ -n "$STUB_OVERRIDE_SIZE" ] && [ ! -f "$STUB_OVERRIDE_CLEARED" ] && echo "Override size: $STUB_OVERRIDE_SIZE"
        ;;
    *"wm density")
        echo "Physical density: $STUB_PHYSICAL_DENSITY"
        [ -n "$STUB_OVERRIDE_DENSITY" ] && [ ! -f "$STUB_OVERRIDE_CLEARED" ] && echo "Override density: $STUB_OVERRIDE_DENSITY"
        ;;
    *"emu avd name")
        echo "$STUB_AVD"
        echo "OK"
        ;;
    *"getprop ro.build.version.sdk")
        echo "$STUB_API"
        ;;
esac
exit 0
STUB

    chmod +x "$stub_path"
    echo "$stub_path"
}

run_profile() {
    ADB=$(create_adb_stub) \
    STUB_CALL_LOG="${fixture_root}/calls" \
    STUB_OVERRIDE_CLEARED="${fixture_root}/cleared" \
    STUB_DEVICES="${STUB_DEVICES-emulator-5554\tdevice}" \
    STUB_PHYSICAL_SIZE="${STUB_PHYSICAL_SIZE:-1080x2400}" \
    STUB_PHYSICAL_DENSITY="${STUB_PHYSICAL_DENSITY:-420}" \
    STUB_OVERRIDE_SIZE="${STUB_OVERRIDE_SIZE:-}" \
    STUB_OVERRIDE_DENSITY="${STUB_OVERRIDE_DENSITY:-}" \
    STUB_AVD="${STUB_AVD:-QA_Test_AVD}" \
    STUB_API="${STUB_API:-35}" \
        "$profile_script" "$@"
}

reset_fixtures() {
    rm -f "${fixture_root}/calls" "${fixture_root}/cleared"
    unset STUB_DEVICES STUB_PHYSICAL_SIZE STUB_PHYSICAL_DENSITY STUB_OVERRIDE_SIZE STUB_OVERRIDE_DENSITY STUB_AVD STUB_API
}

assert_contains() {
    local description=$1
    local haystack=$2
    local needle=$3

    if [[ "$haystack" == *"$needle"* ]]; then
        echo "  ok: $description"
    else
        echo "  FAIL: $description" >&2
        echo "    기대(포함): $needle" >&2
        echo "    실제: $haystack" >&2
        failures=$((failures + 1))
    fi
}

assert_not_contains() {
    local description=$1
    local haystack=$2
    local needle=$3

    if [[ "$haystack" != *"$needle"* ]]; then
        echo "  ok: $description"
    else
        echo "  FAIL: $description" >&2
        echo "    공개 출력에 포함되면 안 됨: $needle" >&2
        failures=$((failures + 1))
    fi
}

assert_status() {
    local description=$1
    local expected=$2
    local actual=$3

    if [ "$expected" = "$actual" ]; then
        echo "  ok: $description"
    else
        echo "  FAIL: $description — 종료 코드 기대 $expected, 실제 $actual" >&2
        failures=$((failures + 1))
    fi
}

echo "표준 화면(override 없음)"
reset_fixtures
output=$(run_profile status)
assert_contains "실효 dp 는 411x914" "$output" "411x914dp"
assert_contains "override 없음으로 표시" "$output" "override:      없음"

echo "override 가 걸린 화면 — 2026-08-25 감사에서 실제로 발견된 값"
reset_fixtures
STUB_OVERRIDE_SIZE="1080x2340" STUB_OVERRIDE_DENSITY="387"
output=$(run_profile status)
assert_contains "override 를 실효값으로 반영" "$output" "446x967dp"
assert_contains "override 를 그대로 노출" "$output" "1080x2340 @387dpi"

echo "compact 전환"
reset_fixtures
output=$(run_profile compact)
calls=$(cat "${fixture_root}/calls")
assert_contains "wm size 를 720x1600 으로" "$calls" "wm size 720x1600"
assert_contains "wm density 를 320 으로" "$calls" "wm density 320"

echo "standard 전환"
reset_fixtures
STUB_OVERRIDE_SIZE="720x1600" STUB_OVERRIDE_DENSITY="320"
output=$(run_profile standard)
calls=$(cat "${fixture_root}/calls")
assert_contains "size override 해제" "$calls" "wm size reset"
assert_contains "density override 해제" "$calls" "wm density reset"
assert_contains "해제 후 물리 해상도로 복귀" "$output" "411x914dp"

echo "증거용 JSON"
reset_fixtures
STUB_OVERRIDE_SIZE="1080x2340" STUB_OVERRIDE_DENSITY="387"
output=$(run_profile status --json)
assert_not_contains "로컬 serial 을 제외한다" "$output" '"serial"'
assert_not_contains "로컬 AVD 이름을 제외한다" "$output" '"avd"'
assert_contains "api_level 을 담는다" "$output" '"api_level": 35'
assert_contains "override 를 담는다" "$output" '"override": "1080x2340"'
assert_contains "실효 dp 를 담는다" "$output" '"effective_dp": "446x967"'
if command -v python3 >/dev/null 2>&1; then
    if echo "$output" | python3 -c 'import json,sys; json.load(sys.stdin)' 2>/dev/null; then
        echo "  ok: JSON 으로 파싱된다"
    else
        echo "  FAIL: JSON 파싱 실패" >&2
        failures=$((failures + 1))
    fi
fi

echo "override 가 없을 때의 JSON"
reset_fixtures
output=$(run_profile status --json)
assert_contains "override 는 null" "$output" '"override": null'
assert_contains "override_density 는 null" "$output" '"override_density": null'

echo "잘못된 사용"
reset_fixtures
set +e
run_profile bogus >/dev/null 2>&1
assert_status "알 수 없는 하위 명령은 2" 2 $?
run_profile >/dev/null 2>&1
assert_status "하위 명령 없으면 2" 2 $?
run_profile status compact >/dev/null 2>&1
assert_status "하위 명령 두 개는 2" 2 $?
run_profile status --serial >/dev/null 2>&1
assert_status "--serial 값 누락은 2" 2 $?
set -e

echo "기기 선택"
reset_fixtures
STUB_DEVICES='emulator-5554\tdevice\nemulator-5556\tdevice'
set +e
output=$(run_profile status 2>&1)
assert_status "기기가 여러 대면 2" 2 $?
set -e
assert_contains "--serial 를 안내" "$output" "--serial"

reset_fixtures
STUB_DEVICES='emulator-5554\tdevice\nemulator-5556\tdevice'
output=$(run_profile status --serial emulator-5556)
assert_contains "--serial 로 지정하면 그 기기를 쓴다" "$output" "serial:        emulator-5556"

reset_fixtures
STUB_DEVICES=''
set +e
run_profile status >/dev/null 2>&1
assert_status "기기가 없으면 2" 2 $?
set -e

if [ "$failures" -gt 0 ]; then
    echo "실패 ${failures}건" >&2
    exit 1
fi

echo "전부 통과"
