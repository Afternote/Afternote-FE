#!/usr/bin/env bash

set -uo pipefail

if [ "$#" -gt 1 ]; then
    echo "Usage: $0 [repository-root]" >&2
    exit 2
fi

if [ "$#" -eq 1 ]; then
    repository_root=$(cd "$1" && pwd -P) || exit 2
else
    repository_root=$(git rev-parse --show-toplevel) || exit 2
fi

if ! git -C "$repository_root" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    echo "Repository Quality: not a Git worktree: $repository_root" >&2
    exit 2
fi

require_command() {
    local command_name=$1

    if ! command -v "$command_name" >/dev/null 2>&1; then
        echo "Repository Quality: required command is missing: $command_name" >&2
        exit 2
    fi
}

require_command actionlint
require_command bash
require_command git
require_command shellcheck

actionlint_binary=$(command -v actionlint)
bash_binary=$(command -v bash)
shellcheck_binary=$(command -v shellcheck)

quality_failed=0
workflow_count=0
shell_script_count=0

cd "$repository_root" || exit 2

while IFS= read -r -d '' workflow_file; do
    workflow_count=$((workflow_count + 1))
    workflow_shellcheck_command=$shellcheck_binary

    case "$workflow_file" in
        .github/workflows/merge-order-guard.yml)
            # File-scoped SC2016 suppression: the GraphQL queries intentionally pass
            # literal $owner/$repo/$pr/$n variables instead of expanding them in Bash.
            workflow_shellcheck_command="$shellcheck_binary -e SC2016"
            ;;
    esac

    if ! "$actionlint_binary" \
        -color=false \
        -shellcheck="$workflow_shellcheck_command" \
        "$workflow_file"; then
        echo "Repository Quality: actionlint failed: $workflow_file" >&2
        quality_failed=1
    fi
done < <(git ls-files -z -- '.github/workflows/*.yml' '.github/workflows/*.yaml')

if [ "$workflow_count" -eq 0 ]; then
    echo "Repository Quality: no tracked workflow YAML files found" >&2
    quality_failed=1
fi

while IFS= read -r -d '' shell_file; do
    shell_script_count=$((shell_script_count + 1))

    case "$shell_file" in
        build-leaf.sh)
            # File-scoped suppressions for existing intentional behavior:
            # SC2086 splits the accumulated Gradle task list into separate arguments.
            # SC2126 keeps a numeric child-module count used by the following test.
            if ! "$shellcheck_binary" -e SC2086,SC2126 "$shell_file"; then
                echo "Repository Quality: ShellCheck failed: $shell_file" >&2
                quality_failed=1
            fi
            ;;
        *)
            if ! "$shellcheck_binary" "$shell_file"; then
                echo "Repository Quality: ShellCheck failed: $shell_file" >&2
                quality_failed=1
            fi
            ;;
    esac

    if ! "$bash_binary" -n "$shell_file"; then
        echo "Repository Quality: bash syntax failed: $shell_file" >&2
        quality_failed=1
    fi
done < <(git ls-files -z -- '*.sh')

if [ "$shell_script_count" -eq 0 ]; then
    echo "Repository Quality: no tracked shell scripts found" >&2
    quality_failed=1
fi

merge_marker_output=$(git grep -nI -E \
    '^(<<<<<<<|=======|>>>>>>>)( |$)' \
    -- \
    . \
    ':(exclude,glob)**/build/**' \
    ':(exclude,glob)**/.gradle/**' \
    ':(exclude,glob)**/.kotlin/**' \
    ':(exclude,glob)**/.cxx/**' \
    ':(exclude,glob)**/.cache/**' \
    ':(exclude,glob)**/generated/**')
merge_marker_status=$?

case "$merge_marker_status" in
    0)
        echo "$merge_marker_output" >&2
        echo "Repository Quality: merge conflict marker found" >&2
        quality_failed=1
        ;;
    1)
        ;;
    *)
        echo "Repository Quality: merge conflict marker scan failed" >&2
        quality_failed=1
        ;;
esac

if [ "$quality_failed" -ne 0 ]; then
    exit 1
fi

echo "Repository Quality: passed ($workflow_count workflows, $shell_script_count shell scripts)"
