package com.afternote.feature.receiver.data.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

/**
 * 수신 목록의 최상위 계약은 엄격하게 유지하면서 category 계약 위반의 실패 범위만 원소 하나로 좁힌다.
 *
 * [ReceivedAfternoteDto.category]를 nullable로 완화하면 서버 계약과 앱 모델이 어긋나고, 직접 생성된 DTO에도
 * 존재 불가능한 상태가 들어온다. 대신 배열 원소를 먼저 JSON으로 받아 category가 문자열이면 그대로 strict
 * DTO로 디코딩하고, 아니면 category만 검증용 문자열로 치환해 나머지 필드를 strict 검증한 뒤 원소를 제외한다.
 * 따라서 다른 필수 필드의 계약 위반은 감추지 않고 목록 전체 디코딩 실패로 남는다.
 */
object ReceivedAfternoteListDtoSerializer : KSerializer<ReceivedAfternoteListDto> {
    override val descriptor: SerialDescriptor = ReceivedAfternoteListWireDto.serializer().descriptor

    override fun deserialize(decoder: Decoder): ReceivedAfternoteListDto {
        val jsonDecoder =
            decoder as? JsonDecoder
                ?: throw SerializationException("ReceivedAfternoteListDto supports JSON only")
        val json = jsonDecoder.json
        val wire =
            json.decodeFromJsonElement(
                ReceivedAfternoteListWireDto.serializer(),
                jsonDecoder.decodeJsonElement(),
            )
        val afternotes =
            wire.afternotes.mapNotNull { element ->
                val item = element as? JsonObject
                if (item == null) {
                    json.decodeFromJsonElement(ReceivedAfternoteDto.serializer(), element)
                    return@mapNotNull null
                }
                val category = item["category"] as? JsonPrimitive
                if (category?.isString != true) {
                    // category만 유효한 문자열로 바꿔 나머지 필수 필드까지 strict 검증한 뒤 제외한다.
                    // 그래야 category 오류가 함께 들어왔다는 이유로 id/title 등의 계약 위반이 가려지지 않는다.
                    json.decodeFromJsonElement(
                        ReceivedAfternoteDto.serializer(),
                        JsonObject(item + ("category" to JsonPrimitive(CATEGORY_VALIDATION_PLACEHOLDER))),
                    )
                    return@mapNotNull null
                }
                json.decodeFromJsonElement(ReceivedAfternoteDto.serializer(), item)
            }

        return ReceivedAfternoteListDto(
            afternotes = afternotes,
            totalCount = wire.totalCount,
            decodingRejectedItemCount = wire.afternotes.size - afternotes.size,
        )
    }

    override fun serialize(
        encoder: Encoder,
        value: ReceivedAfternoteListDto,
    ) {
        val jsonEncoder =
            encoder as? JsonEncoder
                ?: throw SerializationException("ReceivedAfternoteListDto supports JSON only")
        val json = jsonEncoder.json
        val wire =
            ReceivedAfternoteListWireDto(
                afternotes =
                    value.afternotes.map { afternote ->
                        json.encodeToJsonElement(ReceivedAfternoteDto.serializer(), afternote)
                    },
                totalCount = value.totalCount,
            )
        jsonEncoder.encodeJsonElement(json.encodeToJsonElement(ReceivedAfternoteListWireDto.serializer(), wire))
    }
}

private const val CATEGORY_VALIDATION_PLACEHOLDER = "SOCIAL"

@Serializable
private data class ReceivedAfternoteListWireDto(
    @SerialName("afternotes") val afternotes: List<JsonElement>,
    @SerialName("totalCount") val totalCount: Int,
)
