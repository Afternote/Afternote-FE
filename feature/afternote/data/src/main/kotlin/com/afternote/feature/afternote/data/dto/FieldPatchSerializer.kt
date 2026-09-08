package com.afternote.feature.afternote.data.dto

import com.afternote.feature.afternote.domain.model.author.FieldPatch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * [FieldPatch] 슬롯을 wire 로 옮기는 직렬화기 (#1617).
 *
 * **「키를 뺀다」는 이 직렬화기가 하는 일이 아니다.** kotlinx 는 `encodeDefaults = false` 에서
 * *선언된 기본값과 같은* 필드만 생략하므로, 생략은 DTO 가 슬롯의 기본값을 [FieldPatch.Unchanged] 로
 * 두는 것으로 성립한다. 이 직렬화기는 나머지 둘만 맡는다 — [FieldPatch.Set] 의 값을 그대로 쓰고,
 * 그 값이 `null` 이면 JSON `null` 을 남겨 서버가 삭제로 읽게 한다.
 *
 * 그래서 [FieldPatch.Unchanged] 가 여기까지 오면 계약이 깨진 것이다: DTO 슬롯의 기본값이
 * [FieldPatch.Unchanged] 가 아니거나 `encodeDefaults` 가 켜졌다는 뜻이고, 둘 다 「안 건드림」을
 * 「삭제」로 바꿔 버린다. 조용히 null 을 쓰지 않고 즉시 실패시킨다.
 */
class FieldPatchSerializer<T>(
    private val valueSerializer: KSerializer<T>,
) : KSerializer<FieldPatch<T>> {
    override val descriptor: SerialDescriptor = valueSerializer.descriptor

    override fun serialize(
        encoder: Encoder,
        value: FieldPatch<T>,
    ) {
        when (value) {
            is FieldPatch.Unchanged -> {
                error(
                    "FieldPatch.Unchanged 는 직렬화 대상이 아니다 — 키가 생략됐어야 한다. " +
                        "DTO 슬롯의 기본값과 Json 의 encodeDefaults 설정을 확인할 것.",
                )
            }

            is FieldPatch.Set -> {
                encoder.encodeSerializableValue(valueSerializer, value.value)
            }
        }
    }

    /** 요청 전용 타입이라 역직렬화는 쓰이지 않지만, 계약상 값이 있으면 [FieldPatch.Set] 이다. */
    override fun deserialize(decoder: Decoder): FieldPatch<T> = FieldPatch.Set(decoder.decodeSerializableValue(valueSerializer))
}
