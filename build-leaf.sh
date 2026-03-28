#!/bin/bash

# 1. 인자가 없으면 에러 메시지 출력
if [ -z "$1" ]; then
    echo "❌ 사용법: ./build-leaf.sh :모듈명 (예: ./build-leaf.sh :core)"
    exit 1
fi

TARGET_PATH=$1
echo "🔍 '$TARGET_PATH' 및 하위의 최하위 모듈을 찾는 중..."

# 2. 전체 모듈 목록 추출 (보이지 않는 줄바꿈 문자 \r 제거 및 모듈명만 추출)
ALL_MODULES=$(./gradlew -q projects | grep -oE "'(:[a-zA-Z0-9_:.-]+)'" | tr -d "'" | tr -d '\r' | sort -u)

# 3. 입력한 경로와 정확히 일치하거나, 해당 경로의 하위 모듈인 것들만 필터링
# 정규식 ^TARGET_PATH($|:) -> 정확히 일치($)하거나 하위 모듈(:)인 경우
MATCHED_MODULES=$(echo "$ALL_MODULES" | grep -E "^${TARGET_PATH}($|:)")

if [ -z "$MATCHED_MODULES" ]; then
    echo "❌ '$TARGET_PATH' 에 해당하는 모듈을 찾을 수 없습니다. (오타가 없는지 확인해 주세요!)"
    exit 1
fi

# 4. 자식 모듈이 없는 '최하위(Leaf)' 모듈만 찾아내기
LEAF_TASKS=""
for mod in $MATCHED_MODULES; do
    # 현재 모듈($mod)명 뒤에 ':'가 붙은 하위 모듈이 MATCHED_MODULES 목록에 존재하는지 검사
    HAS_CHILD=$(echo "$MATCHED_MODULES" | grep -E "^${mod}:" | wc -l)

    # 하위 모듈이 0개라면 그것이 최하위 모듈(Leaf)임
    if [ "$HAS_CHILD" -eq 0 ]; then
        LEAF_TASKS="$LEAF_TASKS $mod:build"
    fi
done

echo "📦 최종 빌드 대상:$LEAF_TASKS"

# 5. 한꺼번에 빌드 실행
./gradlew $LEAF_TASKS