#!/usr/bin/env bash
# 본 repo 의 docs/claude/hooks/*.sh 를 사용자 .claude/hooks/ 로 symlink 한다.
# 기존 파일이 있으면 skip — 본인 hook 덮어쓰기 0.
# 강제 X, opt-in. 자세한 가이드는 docs/claude/README.md 참고.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC_DIR="$REPO_ROOT/docs/claude/hooks"
DST_DIR="$REPO_ROOT/.claude/hooks"

if [ ! -d "$SRC_DIR" ]; then
  echo "✗ docs/claude/hooks/ 가 없습니다. 본 repo 위에서 실행했는지 확인하세요." >&2
  exit 1
fi

mkdir -p "$DST_DIR"

linked=0
skipped=0

for src in "$SRC_DIR"/*.sh; do
  name=$(basename "$src")
  dst="$DST_DIR/$name"

  if [ -e "$dst" ] || [ -L "$dst" ]; then
    echo "  skip: .claude/hooks/$name (이미 존재 — 덮어쓰지 않습니다)"
    skipped=$((skipped + 1))
  else
    ln -s "$src" "$dst"
    echo "  ✓ .claude/hooks/$name → docs/claude/hooks/$name"
    linked=$((linked + 1))
  fi
done

echo ""
echo "완료: linked=$linked, skipped=$skipped"
echo ""
echo "남은 작업 — Claude 가 hook 을 실행하도록 .claude/settings.local.json 에 등록:"
echo "  docs/claude/README.md 의 'install 스크립트' 섹션 JSON 참조."
echo ""
echo "기존 hook 을 본 repo 버전으로 교체하려면 먼저 .claude/hooks/<파일> 삭제 후 재실행."
