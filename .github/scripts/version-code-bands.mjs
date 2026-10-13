// 두 배포 채널이 쓰는 versionCode 대역과 공통 파싱 규약을 한곳에 둔다. 정책 본문은
// docs/play-release.md 의 "versionCode 정책" 절에 있다.
//
// release-distribution.yml(Firebase, main push 자동)과 release-play-internal.yml(Play 내부
// 테스트 트랙, 수동 실행)은 별개 워크플로라 서로의 run 카운터를 모른다. 같은 식을 각자 쓰면
// 같은 값이 양쪽에서 나오므로, 겹치지 않는 대역으로 갈라 둔다. 상수를 양쪽 resolver 에
// 복사하면 한쪽만 바뀌었을 때 대역이 조용히 겹치니 이 파일 하나가 정본이다.

export const ATTEMPT_STRIDE = 100n;
export const FIREBASE_BAND_MIN = 101n;
export const FIREBASE_BAND_MAX = 999_999_999n;
export const PLAY_BAND_FLOOR = 1_000_000_000n;

export function parseInteger(name, rawValue, { minimum }) {
  if (typeof rawValue !== "string" || !/^(0|[1-9][0-9]*)$/.test(rawValue)) {
    throw new Error(`${name} must be a base-10 integer.`);
  }
  const value = BigInt(rawValue);
  if (value < minimum) {
    throw new Error(`${name} must be at least ${minimum}.`);
  }
  return value;
}
