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
    // 위임 대상의 descriptor 를 그대로 내보내면 이 타입의 직렬화 이름이 파일 안에 숨긴
    // ReceivedAfternoteListWireDto 로 잡힌다. 구조는 그대로 위임하되 이름만 공개 DTO 로 돌려 놓는다.
    override val descriptor: SerialDescriptor =
        SerialDescriptor(
            "com.afternote.feature.receiver.data.dto.ReceivedAfternoteListDto",
            ReceivedAfternoteListWireDto.serializer().descriptor,
        )

    override fun deserialize(decoder: Decoder): ReceivedAfternoteListDto {
        // 아래에서 쓰는 decodeJsonElement 는 JsonDecoder 에만 있다. 다른 포맷(ProtoBuf 등)에서는
        // 이 전략 자체가 성립하지 않으므로 여기서 명시적으로 끊는다.
        val jsonDecoder =
            decoder as? JsonDecoder
                ?: throw SerializationException("ReceivedAfternoteListDto supports JSON only")
        // 새 Json 인스턴스를 만들면 NetworkModule 의 ignoreUnknownKeys·coerceInputValues 가 날아간다.
        // 원소 디코딩도 같은 규칙을 따르도록 디코더가 쓰고 있는 인스턴스를 그대로 재사용한다.
        val json = jsonDecoder.json
        // 응답을 통째로 JsonElement 로 받아 봉투 모양으로 다시 읽는다. 봉투(afternotes 배열·totalCount)는
        // strict 로 검증되고, 배열 원소는 JsonElement 인 채로 통과해 검증이 아래 mapNotNull 로 미뤄진다.
        // 그래서 totalCount 누락처럼 봉투가 깨진 응답은 원소 격리 없이 목록 전체 실패로 끝난다.
        val wire =
            json.decodeFromJsonElement(
                ReceivedAfternoteListWireDto.serializer(),
                jsonDecoder.decodeJsonElement(),
            )
        val afternotes =
            wire.afternotes.mapNotNull { rawAfternote ->
                if (rawAfternote !is JsonObject) {
                    throw SerializationException(
                        "afternotes element must be a JSON object, but was ${rawAfternote::class.simpleName}",
                    )
                }
                val rawCategory = rawAfternote["category"] as? JsonPrimitive
                if (rawCategory?.isString != true) {
                    // category만 유효한 문자열로 바꿔 나머지 필수 필드까지 strict 검증한 뒤 제외한다.
                    // 그래야 category 오류가 함께 들어왔다는 이유로 id/title 등의 계약 위반이 가려지지 않는다.
                    json.decodeFromJsonElement(
                        ReceivedAfternoteDto.serializer(),
                        JsonObject(rawAfternote + ("category" to JsonPrimitive(CATEGORY_VALIDATION_PLACEHOLDER))),
                    )
                    return@mapNotNull null
                }
                // category 가 문자열임이 확인된 원소만 여기 도달한다. 이 블록의 디코딩 세 번 중
                // 결과를 실제로 쓰는 곳은 여기뿐이고, 나머지 둘은 예외를 던지게 하려는 검증용이다.
                json.decodeFromJsonElement(ReceivedAfternoteDto.serializer(), rawAfternote)
            }

        return ReceivedAfternoteListDto(
            afternotes = afternotes,
            // 서버가 내려준 값을 제외 후 크기로 덮지 않는다. 페이징 총량의 정본은 서버다.
            totalCount = wire.totalCount,
            decodingRejectedItemCount = wire.afternotes.size - afternotes.size,
        )
    }

    /**
     * 인코딩도 wire DTO 를 거쳐 나간다. [ReceivedAfternoteListDto.decodingRejectedItemCount] 는 wire 에
     * 없는 필드라, 이렇게 갈아 담는 것만으로 내부 집계값이 JSON 으로 새 나가지 않는다.
     */
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

// category 자리를 채우기만 하면 되는 값이라 계약상 유효한 문자열이면 무엇이든 된다. 이 값으로 만들어진
// DTO 는 검증 직후 버려지므로 실제 데이터로 쓰이지 않는다.
private const val CATEGORY_VALIDATION_PLACEHOLDER = "SOCIAL"

/**
 * 공개 DTO 와 실제 JSON 사이에 끼운 중간 표현. 원소 타입만 [JsonElement] 로 열어 두어, 봉투 계약은
 * strict 로 유지하면서 원소 검증만 [ReceivedAfternoteListDtoSerializer] 가 직접 하도록 넘긴다.
 * 구현 세부이므로 private 이다 — 밖에서 쓰이기 시작하면 원소 검증이 통째로 빠진 경로가 생긴다.
 */
@Serializable
private data class ReceivedAfternoteListWireDto(
    @SerialName("afternotes") val afternotes: List<JsonElement>,
    @SerialName("totalCount") val totalCount: Int,
)
