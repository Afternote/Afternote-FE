package com.afternote.feature.receiver.domain.error

/**
 * 열람 신청에 실을 증빙 서류가 하나도 없는 입력 (#380, #1701).
 *
 * 서버에 닿기 전에 호출 자체가 성립하지 않는다는 뜻이라 [ReceiverFailure] 계열(서버·전송 실패를
 * Data 계층이 번역한 결과)에 넣지 않았다. 그 계열은 원인 예외를 필수로 요구하는데, 이 실패에는
 * 원인이 될 예외가 없다.
 *
 * 화면 문구는 호출처 리소스가 갖는다 — 이 타입은 «어떤 실패인지» 만 나른다.
 */
class DeliveryDocumentsMissingException : Exception("delivery verification requires at least one document url")
