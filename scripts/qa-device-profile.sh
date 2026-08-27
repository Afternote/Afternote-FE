#!/usr/bin/env bash

# 에뮬레이터 QA 기기의 화면 프로파일을 조회·전환한다.
#
# 이 레포의 QA 증거 대장(docs/qa/evidence/<full-head-sha>.json)은 오랫동안 AVD 이름만 적어
# 왔는데, 이름만으로는 어떤 화면에서 검증했는지 단정할 수 없다. `wm size`/`wm density`
# override 는 재부팅으로도 풀리지 않고 세션 사이에 조용히 남기 때문이다. 실제로 2026-08-25
# 감사에서 당시 QA AVD에 1080x2340 @387dpi override 가 남아 있어, 표준 411x914dp 가
# 아니라 446x967dp 에서 QA 가 돌고 있었다.
#
# 그래서 실측 전에는 status 로 실효 dp 를 확인한다. --json 출력은 공개 증거에 넣을 수 있도록
# 로컬 serial과 AVD 이름을 제외한다.

set -uo pipefail

readonly COMPACT_WIDTH_PX=720
readonly COMPACT_HEIGHT_PX=1600
readonly COMPACT_DENSITY=320

usage() {
    cat >&2 <<'USAGE'
Usage: qa-device-profile.sh <status|standard|compact> [--serial <serial>] [--json]

  status     현재 화면 프로파일을 출력한다 (전환하지 않는다)
  standard   화면 override 를 제거해 기기 물리 해상도로 되돌린다
  compact    720x1600 @320dpi(=360x800dp) 로 전환한다 — 국내 보급형 기준

  --serial   대상 기기 지정 (생략하면 연결된 기기가 하나일 때만 자동 선택)
  --json     공개 QA 증거용 device 블록을 출력한다 (serial·AVD 이름 제외)
USAGE
    exit 2
}

# 물리 픽셀과 density 로 실효 dp 를 구한다. dp = px / (density / 160)
#
# 반올림이 아니라 잘라내기다 — 프레임워크가 Configuration.screenWidthDp 를 (int) 캐스팅으로
# 만들기 때문에, 반올림하면 1080x2340 @387dpi 같은 값에서 실제(446)와 1dp 어긋난다.
effective_dp() {
    local width_px=$1
    local height_px=$2
    local density=$3

    if [ "$density" -le 0 ] 2>/dev/null; then
        echo "unknown"
        return 1
    fi

    echo "$((width_px * 160 / density))x$((height_px * 160 / density))"
}

# 인자가 정확히 이 순서일 필요는 없다 — 하위 명령 하나 + 플래그.
parse_arguments() {
    command_name=""
    serial=""
    json_output="false"

    while [ "$#" -gt 0 ]; do
        case "$1" in
            status | standard | compact)
                [ -n "$command_name" ] && usage
                command_name=$1
                ;;
            --serial)
                [ "$#" -lt 2 ] && usage
                serial=$2
                shift
                ;;
            --json)
                json_output="true"
                ;;
            -h | --help)
                usage
                ;;
            *)
                usage
                ;;
        esac
        shift
    done

    [ -z "$command_name" ] && usage
    return 0
}

resolve_adb() {
    if [ -n "${ADB:-}" ]; then
        echo "$ADB"
        return 0
    fi

    if command -v adb >/dev/null 2>&1; then
        command -v adb
        return 0
    fi

    local candidate="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
    if [ -x "$candidate" ]; then
        echo "$candidate"
        return 0
    fi

    echo "qa-device-profile: adb 를 찾지 못했다 (ADB 환경변수로 지정 가능)" >&2
    return 2
}

resolve_serial() {
    local adb=$1
    local requested=$2

    if [ -n "$requested" ]; then
        echo "$requested"
        return 0
    fi

    local devices
    devices=$("$adb" devices | awk 'NR > 1 && $2 == "device" { print $1 }')

    local count
    count=$(echo "$devices" | grep -c '[^[:space:]]')

    if [ "$count" -eq 0 ]; then
        echo "qa-device-profile: 연결된 기기가 없다" >&2
        return 2
    fi

    if [ "$count" -gt 1 ]; then
        echo "qa-device-profile: 기기가 여러 대다 — --serial 로 지정할 것:" >&2
        echo "$devices" >&2
        return 2
    fi

    echo "$devices"
}

# `Physical size: 1080x2400` / `Override size: 720x1600` 에서 값만 뽑는다.
extract_size() {
    local label=$1
    sed -n "s/^${label} size: \([0-9]*x[0-9]*\).*/\1/p" | tail -1
}

extract_density() {
    local label=$1
    sed -n "s/^${label} density: \([0-9]*\).*/\1/p" | tail -1
}

main() {
    parse_arguments "$@" || exit $?

    local adb
    adb=$(resolve_adb) || exit $?

    local target
    target=$(resolve_serial "$adb" "$serial") || exit $?

    case "$command_name" in
        standard)
            "$adb" -s "$target" shell wm size reset >/dev/null || exit 2
            "$adb" -s "$target" shell wm density reset >/dev/null || exit 2
            ;;
        compact)
            "$adb" -s "$target" shell wm size "${COMPACT_WIDTH_PX}x${COMPACT_HEIGHT_PX}" >/dev/null || exit 2
            "$adb" -s "$target" shell wm density "$COMPACT_DENSITY" >/dev/null || exit 2
            ;;
    esac

    local size_output density_output
    size_output=$("$adb" -s "$target" shell wm size) || exit 2
    density_output=$("$adb" -s "$target" shell wm density) || exit 2

    local physical_size override_size physical_density override_density
    physical_size=$(echo "$size_output" | extract_size "Physical")
    override_size=$(echo "$size_output" | extract_size "Override")
    physical_density=$(echo "$density_output" | extract_density "Physical")
    override_density=$(echo "$density_output" | extract_density "Override")

    local active_size=${override_size:-$physical_size}
    local active_density=${override_density:-$physical_density}

    local dp
    dp=$(effective_dp "${active_size%x*}" "${active_size#*x}" "$active_density")

    local avd api_level
    avd=$("$adb" -s "$target" emu avd name 2>/dev/null | head -1 | tr -d '\r')
    api_level=$("$adb" -s "$target" shell getprop ro.build.version.sdk | tr -d '\r')

    if [ "$json_output" = "true" ]; then
        printf '{\n'
        printf '  "is_emulator": %s,\n' "$([ -n "$avd" ] && echo true || echo false)"
        printf '  "api_level": %s,\n' "${api_level:-null}"
        printf '  "screen": {\n'
        printf '    "physical": "%s",\n' "$physical_size"
        printf '    "density": %s,\n' "$physical_density"
        printf '    "override": %s,\n' "$([ -n "$override_size" ] && printf '"%s"' "$override_size" || echo null)"
        printf '    "override_density": %s,\n' "${override_density:-null}"
        printf '    "effective_dp": "%s"\n' "$dp"
        printf '  }\n'
        printf '}\n'
        return 0
    fi

    echo "serial:        $target"
    echo "avd:           ${avd:-(실기기)}"
    echo "api:           ${api_level:-?}"
    echo "physical:      $physical_size @${physical_density}dpi"
    if [ -n "$override_size" ] || [ -n "$override_density" ]; then
        echo "override:      ${override_size:-(없음)} @${override_density:-$physical_density}dpi"
    else
        echo "override:      없음"
    fi
    echo "effective:     ${dp}dp"
}

main "$@"
