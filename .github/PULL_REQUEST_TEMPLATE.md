## 📌𝘐𝘴𝘴𝘶𝘦𝘴
<!-- PR 제목 끝에 대표 Issue 하나를 `(#N)` 형식으로 적고 아래에도 같은 번호를 연결하세요. 관련된 기존 Issue를 재사용하세요. 관련 Issue가 없을 때만 새로 만들고, 여러 PR이 같은 Issue를 공유해도 됩니다. 이 Issue의 작업을 최종 완료하는 PR에서만 Closes/Fixes/Resolves로 바꾸세요. 대표 Issue에는 PR 작성자가 담당자로 지정돼 있어야 합니다. -->
- Refs #

## 📎𝘞𝘰𝘳𝘬 𝘋𝘦𝘴𝘤𝘳𝘪𝘱𝘵𝘪𝘰𝘯
- 
-

### Production visibility
- [ ] `src/main`·`src/debug`·`src/release` 공개 범위는 프로덕션 사용처만으로 정했고, 테스트 직접 접근을 이유로 넓히지 않았습니다.
- [ ] 새로 만들거나 수정한 선언은 같은 파일 `private` → 같은 모듈 `internal` → 다른 모듈 `public` 순으로 최소 범위를 확인했습니다.
- [ ] 테스트 seam이 필요하면 공개 프로덕션 API 대신 공개 동작·test fixture·테스트 소스셋을 사용했습니다.
- [ ] explicit API warning/strict 진단과 visibility Konsist 결과를 확인하고, 예외라면 생성 코드·프레임워크 근거를 변경 근처에 남겼습니다.

## 🧪 CI Test Plan
<!--
일반 CI는 변경 파일과 역의존 모듈만 자동으로 검사합니다.
Android 계측 테스트만 none / selected / full 중 하나를 명시하세요.
selected이면 현재 revision에 존재하는 FQCN#method와 실행 lane(api30/api34)을 tests에 적습니다.
-->
```json
{
  "androidTest": {
    "mode": "none",
    "reason": "계측 테스트가 필요 없거나 필요한 이유를 변경 경계 기준으로 작성",
    "tests": []
  }
}
```

## 📷𝘚𝘤𝘳𝘦𝘦𝘯𝘴𝘩𝘰𝘵


## 💬𝘛𝘰 𝘙𝘦𝘷𝘪𝘦𝘸𝘦𝘳𝘴
