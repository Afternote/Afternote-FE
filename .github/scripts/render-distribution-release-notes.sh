#!/usr/bin/env bash

set -euo pipefail

output_path="${1:?release notes output path is required}"
event_name="${EVENT_NAME:-}"
issue_numbers="${ISSUE_NUMBERS:-}"
qa_points="${QA_POINTS:-}"

extract_section() {
    local heading="$1"
    local body_file="$2"

    awk -v expected_heading="$heading" '
        /^#[#]*[[:space:]]+/ {
            current_heading = $0
            sub(/^#[#]*[[:space:]]+/, "", current_heading)
            sub(/[[:space:]]+$/, "", current_heading)

            if (capturing) {
                exit
            }

            if (current_heading == expected_heading) {
                capturing = 1
                next
            }
        }

        capturing {
            print
        }
    ' "$body_file"
}

print_main_pr_format() {
    printf '%s\n' \
        'main 릴리스 PR 본문에 다음 섹션을 채워 주세요.' \
        '## 포함 이슈' \
        '- #123' \
        '## QA 포인트' \
        '- 사용자가 실행할 동작과 기대 결과'
}

case "$event_name" in
    workflow_dispatch)
        distribution_title="Afternote QA 배포"
        qa_points="$(printf '%s\n' "$qa_points" | tr ';' '\n')"
        ;;
    push)
        distribution_title="Afternote 릴리스 후보 배포"
        release_pr_body_file="${RELEASE_PR_BODY_FILE:-}"
        if [[ ! -s "$release_pr_body_file" ]]; then
            printf '::error::main push와 연결된 릴리스 PR 본문을 찾지 못했습니다.\n' >&2
            print_main_pr_format >&2
            exit 1
        fi

        issue_numbers="$(extract_section '포함 이슈' "$release_pr_body_file")"
        qa_points="$(extract_section 'QA 포인트' "$release_pr_body_file")"
        ;;
    *)
        printf '::error::지원하지 않는 배포 이벤트입니다: %s\n' "${event_name:-<empty>}" >&2
        exit 1
        ;;
esac

normalized_issues="$(
    printf '%s\n' "$issue_numbers" |
        awk '
            {
                remaining = $0
                while (match(remaining, /#[0-9]+/)) {
                    issue = substr(remaining, RSTART, RLENGTH)
                    if (!seen[issue]++) {
                        print issue
                    }
                    remaining = substr(remaining, RSTART + RLENGTH)
                }
            }
        '
)"

normalized_qa_points="$(
    printf '%s\n' "$qa_points" |
        awk '
            /^[[:space:]]*<!--/ {
                in_comment = 1
            }

            in_comment {
                if ($0 ~ /-->/) {
                    in_comment = 0
                }
                next
            }

            {
                point = $0
                sub(/^[[:space:]]*[-*][[:space:]]*/, "", point)
                sub(/^[[:space:]]*[0-9]+\.[[:space:]]*/, "", point)
                sub(/^[[:space:]]+/, "", point)
                sub(/[[:space:]]+$/, "", point)

                if (point != "" && tolower(point) != "no response" && !seen[point]++) {
                    print point
                }
            }
        '
)"

if [[ -z "$normalized_issues" ]]; then
    printf '::error::포함 이슈에는 #123 형식의 이슈 번호가 하나 이상 필요합니다.\n' >&2
    if [[ "$event_name" == "push" ]]; then
        print_main_pr_format >&2
    fi
    exit 1
fi

if [[ -z "$normalized_qa_points" ]]; then
    printf '::error::QA 포인트에는 확인할 동작과 기대 결과가 하나 이상 필요합니다.\n' >&2
    if [[ "$event_name" == "push" ]]; then
        print_main_pr_format >&2
    fi
    exit 1
fi

if printf '%s\n' "$normalized_qa_points" | grep -Eiq '#[0-9]+[[:space:]]*관련 동작을 재현하고 수정 후 기대 결과가 충족되는지 확인|PR[[:space:]]*#[0-9]+의 변경 흐름을 실행하고 기존 동작이 회귀하지 않는지 확인'; then
    printf '::error::QA 포인트에 사전조건·행동·기대 결과가 없는 generic fallback 문구를 사용할 수 없습니다.\n' >&2
    exit 1
fi

source_ref="${SOURCE_REF:-unknown}"
source_ref="${source_ref#refs/heads/}"
source_sha="${SOURCE_SHA:-unknown}"
short_sha="${source_sha:0:7}"

mkdir -p "$(dirname "$output_path")"
{
    printf '%s\n' "$distribution_title"
    printf '기준: %s @ %s\n\n' "$source_ref" "$short_sha"
    printf '포함 이슈\n'
    while IFS= read -r issue; do
        printf -- '- %s\n' "$issue"
    done <<< "$normalized_issues"
    printf '\nQA 포인트\n'
    while IFS= read -r point; do
        printf -- '- %s\n' "$point"
    done <<< "$normalized_qa_points"
} > "$output_path"
