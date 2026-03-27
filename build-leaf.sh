#!/bin/bash

# 1. 인자가 없으면 에러 메시지 출력
if [ -z "$1" ]; then
    echo "❌ 사용법: ./build-leaf.sh :모듈명 (예: ./build-leaf.sh :core)"
    exit 1
fi

TARGET_PATH=$1
echo "🔍 $TARGET_PATH 하위의 최하위 모듈을 찾는 중..."

# 2. 전체 모듈 중 입력한 경로로 시작하는 것들만 추출
ALL_MODULES=$(./gradlew -q projects | grep -o "$TARGET_PATH:[a-zA-Z0-9_.:-]*")

if [ -z "$ALL_MODULES" ]; then
    echo "❌ $TARGET_PATH 하위에서 모듈을 찾을 수 없습니다."
    exit 1
fi

# 3. 자식 모듈이 없는 '최하위(Leaf)' 모듈만 필터링
LEAF_TASKS=""
for mod in $ALL_MODULES; do
    # 현재 모듈명 뒤에 ':'가 붙은 경로가 없다면 최하위 모듈임
    if ! echo "$ALL_MODULES" | grep -q "^$mod:"; then
        LEAF_TASKS="$LEAF_TASKS $mod:build"
    fi
done

echo "📦 빌드 대상: $LEAF_TASKS"

# 4. 한꺼번에 빌드 실행
./gradlew $LEAF_TASKS