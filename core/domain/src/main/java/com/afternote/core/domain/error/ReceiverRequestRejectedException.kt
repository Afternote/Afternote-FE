package com.afternote.core.domain.error

/**
 * 사용자가 입력을 수정해 해결할 수 있는 수신자 등록·수정 요청 오류.
 *
 * 수신자 관리의 입력 거절은 인증·계정 실패인 [CoreAuthFailure]와 구분한다. 현재 소비처가
 * 가르는 사유는 하나이므로 별도 sealed 계층을 추가하지 않고 이 타입으로 표현한다.
 *
 * 표시 문구는 소비처가 타입을 보고 로컬 리소스로 고른다(BE#92). [message]는 서버 원문이나
 * [cause]의 문자열을 복사하지 않는 고정 진단 문구다. 원본 인프라 예외는 [cause]로 보존하며,
 * 서버 코드·원문을 별도 도메인 프로퍼티로 옮기지 않는다.
 *
 * @param cause 이 실패를 만든 인프라 예외.
 */
public class ReceiverRequestRejectedException(
    cause: Throwable,
) : Exception("receiver request rejected", cause)
