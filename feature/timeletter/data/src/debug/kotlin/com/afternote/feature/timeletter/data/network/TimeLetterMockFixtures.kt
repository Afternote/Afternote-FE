package com.afternote.feature.timeletter.data.network

internal object TimeLetterMockFixtures {
    val MOCK_LIST_JSON =
        """
        {"status":200,"code":0,"message":null,"data":{
          "timeLetters":[
            {"id":1,"title":"미래의 나에게","content":"지금 이 순간을 잊지 마.","sendAt":"2030-01-01","status":"SCHEDULED","mediaList":[],"receiverIds":[1],"createdAt":"2026-01-01T00:00:00","updatedAt":"2026-01-01T00:00:00"},
            {"id":2,"title":"10년 후의 나에게","content":"지금보다 더 행복하길 바라.","sendAt":"2035-06-15","status":"SCHEDULED","mediaList":[],"receiverIds":[2],"createdAt":"2026-02-01T00:00:00","updatedAt":"2026-02-01T00:00:00"}
          ],
          "totalCount":2
        }}
        """.trimIndent()

    val MOCK_TEMPORARY_LIST_JSON =
        """
        {"status":200,"code":0,"message":null,"data":{
          "timeLetters":[
            {"id":3,"title":"임시저장 레터","content":null,"sendAt":null,"status":"DRAFT","mediaList":[],"receiverIds":[],"createdAt":"2026-03-01T00:00:00","updatedAt":"2026-03-01T00:00:00"}
          ],
          "totalCount":1
        }}
        """.trimIndent()

    val MOCK_CREATE_JSON =
        """
        {"status":200,"code":0,"message":null,"data":{"id":99,"title":"새 타임레터","content":null,"sendAt":null,"status":"DRAFT","mediaList":[],"receiverIds":[],"createdAt":"2026-05-15T00:00:00","updatedAt":"2026-05-15T00:00:00"}}
        """.trimIndent()

    val MOCK_UPDATE_JSON =
        """
        {"status":200,"code":0,"message":null,"data":{"id":99,"title":"수정된 타임레터","content":"수정된 내용","sendAt":"2030-01-01","status":"SCHEDULED","mediaList":[],"receiverIds":[1],"createdAt":"2026-05-15T00:00:00","updatedAt":"2026-05-15T00:00:00"}}
        """.trimIndent()

    val MOCK_DELETE_JSON =
        """
        {"status":200,"code":0,"message":null,"data":null}
        """.trimIndent()

    fun detailJson(id: Long): String =
        """
        {"status":200,"code":0,"message":null,"data":{"id":$id,"title":"타임레터 #$id","content":"내용입니다.","sendAt":"2030-01-01","status":"SCHEDULED","mediaList":[],"receiverIds":[1],"createdAt":"2026-01-01T00:00:00","updatedAt":"2026-01-01T00:00:00"}}
        """.trimIndent()
}
