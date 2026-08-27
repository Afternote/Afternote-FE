## 📌𝘐𝘴𝘴𝘶𝘦𝘴
<!-- 관련된 기존 Issue를 재사용하세요. 관련 Issue가 없을 때만 새로 만들고, 여러 PR이 같은 Issue를 공유해도 됩니다. 이 Issue의 작업을 최종 완료하는 PR에서만 Closes/Fixes/Resolves로 바꾸세요. -->
- Refs #

## 📎𝘞𝘰𝘳𝘬 𝘋𝘦𝘴𝘤𝘳𝘪𝘱𝘵𝘪𝘰𝘯
- 
-

## 🧪 QA Metadata
<!--
사용자·릴리스 동작을 검증해야 하면 아래 app-runtime 예시를 채웁니다.
앱 QA가 필요 없으면 scope를 ci-only 또는 covered-by-ci로 바꾸고
precondition/action/expected/risk 대신 exclusionReason을 쓰세요. 이 경우 evidence에는
동일 input·boundary·observation을 단언하는 ci/test 근거가 하나 이상 필요합니다.
androidTest.required가 true이면 실제 Activity/Compose/Android 경계를 검증할 testRef를 적습니다.
false이면 reason과 함께 동일 input·boundary·observation의 ci/test evidence로 제외를 증명합니다.
-->
```json
{
  "scope": "app-runtime",
  "precondition": "",
  "action": "",
  "expected": "",
  "risk": "",
  "androidTest": {
    "required": true,
    "reason": "",
    "testRef": "app/src/androidTest/...Test.kt#testName"
  },
  "evidence": [
    {
      "kind": "test",
      "ref": "app/src/androidTest/...Test.kt#testName",
      "assertion": "",
      "input": "",
      "boundary": "",
      "observation": ""
    }
  ]
}
```

## 📷𝘚𝘤𝘳𝘦𝘦𝘯𝘴𝘩𝘰𝘵


## 💬𝘛𝘰 𝘙𝘦𝘷𝘪𝘦𝘸𝘦𝘳𝘴
