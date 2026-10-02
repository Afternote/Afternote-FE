package com.afternote.feature.timeletter.data.network

internal object TimeLetterMockFixtures {
    val MOCK_LIST_JSON =
        """
        {"status":200,"code":0,"message":null,"data":{
          "timeLetters":[
            {"id":1,"title":"미래의 나에게","sendAt":"2030-01-01T00:00:00","status":"SCHEDULED","blocks":[{"id":1,"blockType":"TEXT","blockOrder":1,"textContent":"지금 이 순간을 잊지 마.","url":null,"mimeType":null}],"receiverIds":[1]},
            {"id":2,"title":"10년 후의 나에게","sendAt":"2035-06-15T00:00:00","status":"SCHEDULED","blocks":[{"id":2,"blockType":"TEXT","blockOrder":1,"textContent":"지금보다 더 행복하길 바라.","url":null,"mimeType":null}],"receiverIds":[2]}
          ],
          "totalCount":2
        }}
        """.trimIndent()

    val MOCK_TEMPORARY_LIST_JSON =
        """
        {"status":200,"code":0,"message":null,"data":{
          "timeLetters":[
            {"id":3,"title":"임시저장 레터","sendAt":null,"status":"DRAFT","blocks":[],"receiverIds":[]}
          ],
          "totalCount":1
        }}
        """.trimIndent()

    val MOCK_CREATE_JSON =
        """
        {"status":200,"code":0,"message":null,"data":{"id":99,"title":"새 타임레터","sendAt":null,"status":"DRAFT","blocks":[],"receiverIds":[]}}
        """.trimIndent()

    val MOCK_UPDATE_JSON =
        """
        {"status":200,"code":0,"message":null,"data":{"id":99,"title":"수정된 타임레터","sendAt":"2030-01-01T00:00:00","status":"SCHEDULED","blocks":[{"id":1,"blockType":"TEXT","blockOrder":1,"textContent":"수정된 내용","url":null,"mimeType":null}],"receiverIds":[1]}}
        """.trimIndent()

    val MOCK_DELETE_JSON =
        """
        {"status":200,"code":0,"message":null,"data":null}
        """.trimIndent()

    fun detailJson(id: Long): String =
        """
        {"status":200,"code":0,"message":null,"data":{"id":$id,"title":"타임레터 #$id","sendAt":"2030-01-01T00:00:00","status":"SCHEDULED","blocks":[{"id":1,"blockType":"TEXT","blockOrder":1,"textContent":"내용입니다.","url":null,"mimeType":null}],"receiverIds":[1]}}
        """.trimIndent()
}
