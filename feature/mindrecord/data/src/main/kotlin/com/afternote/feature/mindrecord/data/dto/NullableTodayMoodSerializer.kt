package com.afternote.feature.mindrecord.data.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * 값 자체가 조건부인 **nullable 기분 필드** 전용 직렬화기.
 *
 * `coerceInputValues` 는 생성자 기본값이 있어야 동작한다. #789 로 기본값을 걷어내면서
 * 이 필드들도 함께 엄격해졌는데, 그 결과 "키 누락" 뿐 아니라 "클라가 모르는 **값**" 까지
 * 파싱 실패가 됐다. 두 상황은 성격이 다르다.
 *
 * - 키 누락 → 계약이 바뀐 것. 실패해야 드러난다.
 * - 모르는 값 → 서버가 기분 종류를 늘렸거나 표기가 바뀐 것. 그 주/그 달 목록 전체를
 *   날릴 일이 아니라 이모지 한 칸만 비우면 된다 (#591 에서 감정 값이 한글로 관측된 전례).
 *
 * 그래서 키의 존재는 계약으로 두고(`= null` 기본값 없음), 값만 관대하게 접는다.
 */
object NullableTodayMoodSerializer : KSerializer<TodayMoodDto?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.afternote.mindrecord.NullableTodayMood", PrimitiveKind.STRING).nullable

    override fun deserialize(decoder: Decoder): TodayMoodDto? {
        if (!decoder.decodeNotNullMark()) {
            return decoder.decodeNull()
        }
        val raw = decoder.decodeString()
        return TodayMoodDto.entries.firstOrNull { it.name == raw }
    }

    override fun serialize(
        encoder: Encoder,
        value: TodayMoodDto?,
    ) {
        if (value == null) encoder.encodeNull() else encoder.encodeString(value.name)
    }
}
