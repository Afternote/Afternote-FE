---
name: Check existing abstractions before declining extraction
description: 공통 컴포넌트 추출 검토 시, 추상화 비용을 평가하기 전에 이미 존재하는 공통 인터페이스/베이스 타입을 먼저 확인할 것
type: feedback
originSessionId: 5d0214e9-0c4a-48ab-8935-cc75e03f0775
---
공통 컴포넌트 추출 가능성을 평가할 때, "타입이 달라서 추상화 비용이 크다"고 결론내기 전에 코드베이스에 이미 정의된 공통 인터페이스(예: `ProcessingMethodOption` 같은 marker interface)가 있는지 먼저 grep으로 확인한다.

**Why:** Social/Gallery 라디오 섹션 추출 검토 시 enum 두 개가 다르다는 이유로 "추상화 비용 있음, 보류 권장"으로 답했으나, 두 enum 모두 이미 `ProcessingMethodOption` 인터페이스(title/description)를 구현 중이었음. 사용자가 재질문해서 발견했고, 결과적으로 generic 함수 하나로 비용 0에 추출 완료. 첫 평가가 표면적이었음.

**How to apply:** "이 둘은 타입이 달라서 추상화가 어렵다" 류의 판단 직전에:
1. 두 타입의 정의 파일을 읽어 공통 슈퍼타입/인터페이스 implements 여부 확인
2. 공통 프로퍼티가 이미 인터페이스로 추출되어 있다면 generic 함수로 즉시 추출 가능
3. "추상화 비용"은 새 인터페이스를 만들어야 할 때만 진짜 비용임
