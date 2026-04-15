package com.afternote.feature.afternote.data.network

import com.afternote.core.model.AlbumCover
import com.afternote.feature.afternote.data.dto.MusicSearchResponse
import com.afternote.feature.afternote.data.dto.MusicTrack
import com.afternote.feature.afternote.data.network.AfternoteMockFixtures.MOCK_LIST_JSON
import kotlinx.serialization.json.Json

/**
 * DEBUG 목업 전용 고정 데이터.
 *
 * [AfternoteDebugMockNetworkInterceptor]가 반환하는 JSON 본문과,
 * (이전 프레젠테이션 더미에 있던) UI 시드와 동일 출처를 유지합니다.
 */
internal object AfternoteMockFixtures {
    private val json =
        Json {
            encodeDefaults = false
            ignoreUnknownKeys = true
        }

    /**
     * 작성자 홈 등에서 쓰던 애프터노트 목 시드 (서비스명 → 날짜).
     * 목록 API [MOCK_LIST_JSON]과 항목 수가 다를 수 있음.
     */
    val authorHomeListSeeds: List<Pair<String, String>> =
        listOf(
            "인스타그램" to "2025.02.01",
            "갤러리" to "2025.01.28",
            "추모 가이드라인" to "2025.01.28",
            "페이스북" to "2025.01.20",
            "파일" to "2025.01.15",
            "카카오톡" to "2025.01.10",
        )

    /** 플레이리스트 앨범 행 목업 시드 (이 모듈 DEBUG가 유일한 정의). */
    val playlistAlbumCoverSeeds: List<AlbumCover> =
        listOf(
            AlbumCover(id = "1"),
            AlbumCover(id = "2"),
            AlbumCover(id = "3"),
            AlbumCover(id = "4"),
        )

    val musicSearchResponseJson: String
        get() =
            json.encodeToString(
                MusicSearchResponse(
                    tracks =
                        listOf(
                            MusicTrack(artist = "김범수", title = "보고싶다", albumImageUrl = null),
                            MusicTrack(artist = "윤도현", title = "사랑했나봐", albumImageUrl = null),
                            MusicTrack(artist = "김광석", title = "나의 옛날이야기", albumImageUrl = null),
                            MusicTrack(artist = "이문세", title = "그대와 영원히", albumImageUrl = null),
                            MusicTrack(artist = "넬", title = "흩어진 꿈", albumImageUrl = null),
                            MusicTrack(artist = "폴킴", title = "안녕", albumImageUrl = null),
                            MusicTrack(artist = "에일리", title = "첫눈처럼 너에게 가겠다", albumImageUrl = null),
                            MusicTrack(artist = "폴킴", title = "너를 만나", albumImageUrl = null),
                            MusicTrack(artist = "박효신", title = "겨울비", albumImageUrl = null),
                        ),
                ),
            )

    fun detailJsonForId(id: Long): String =
        when (id) {
            1L -> MOCK_DETAIL_SOCIAL_INSTAGRAM_JSON
            2L -> MOCK_DETAIL_GALLERY_JSON
            3L -> MOCK_DETAIL_SOCIAL_NAVER_MAIL_JSON
            else -> mockDetailGenericJson(id)
        }

    private fun mockDetailGenericJson(id: Long): String =
        """
        {"status":200,"code":0,"message":null,"data":{"afternoteId":$id,"category":"SOCIAL","title":"[목업] 애프터노트 상세","createdAt":"2024-01-01T00:00:00","updatedAt":"2024-01-02T00:00:00","credentials":null,"receivers":[],"processMethod":null,"actions":[],"leaveMessage":null,"playlist":null}}
        """.trimIndent()

    /** 소셜 네트워크 상세 시나리오 — 인스타그램 (구 PREVIEW_CONTENT). */
    val MOCK_DETAIL_SOCIAL_INSTAGRAM_JSON =
        """
        {"status":200,"code":0,"message":null,"data":{
          "afternoteId":1,
          "category":"SOCIAL",
          "title":"인스타그램",
          "createdAt":"2025-11-26T10:00:00",
          "updatedAt":"2025-11-26T12:00:00",
          "credentials":{"id":"qwerty123","password":"qwerty123"},
          "receivers":[{"receiverId":1,"name":"황규운","relation":"친구","phone":""}],
          "processMethod":"MEMORIAL",
          "actions":["게시물 내리기","추모 게시물 올리기","추모 계정으로 전환하기"],
          "leaveMessage":"이 계정에는 우리 가족 여행 사진이 많아.\n계정 삭제하지 말고 꼭 추모 계정으로 남겨줘!",
          "playlist":null
        }}
        """.trimIndent()

    /** 갤러리 상세 — 목록 id 2와 대응 (구 GALLERY_PREVIEW_CONTENT와 유사). */
    val MOCK_DETAIL_GALLERY_JSON =
        """
        {"status":200,"code":0,"message":null,"data":{
          "afternoteId":2,
          "category":"GALLERY",
          "title":"갤러리",
          "createdAt":"2025-11-26T10:00:00",
          "updatedAt":"2025-11-26T12:00:00",
          "credentials":null,
          "receivers":[
            {"receiverId":1,"name":"김지은","relation":"친구","phone":""},
            {"receiverId":2,"name":"김혜성","relation":"친구","phone":""}
          ],
          "processMethod":"TRANSFER_TO_ADDITIONAL_AFTERNOTE_EDIT_RECEIVER",
          "actions":["'엽사' 폴더 박선호에게 전송","'흑역사' 폴더 삭제"],
          "leaveMessage":"",
          "playlist":null
        }}
        """.trimIndent()

    /** 소셜 네트워크 상세 시나리오 — 네이버 메일 (구 NAVER_MAIL_PREVIEW_CONTENT). */
    val MOCK_DETAIL_SOCIAL_NAVER_MAIL_JSON =
        """
        {"status":200,"code":0,"message":null,"data":{
          "afternoteId":3,
          "category":"SOCIAL",
          "title":"네이버 메일",
          "createdAt":"2025-11-26T10:00:00",
          "updatedAt":"2025-11-26T12:00:00",
          "credentials":{"id":"qwerty123","password":"qwerty123"},
          "receivers":[{"receiverId":1,"name":"황규운","relation":"친구","phone":""}],
          "processMethod":"TRANSFER",
          "actions":["자동 응답 설정 (부재 알림)","메일함 데이터 백업","중요 메일 전달"],
          "leaveMessage":"",
          "playlist":null
        }}
        """.trimIndent()

    val MOCK_LIST_JSON =
        """
        {"status":200,"code":0,"message":null,"data":{"content":[
          {"afternoteId":1,"title":"인스타그램","category":"SOCIAL","createdAt":"2024-06-01"},
          {"afternoteId":2,"title":"갤러리","category":"GALLERY","createdAt":"2024-06-02"},
          {"afternoteId":3,"title":"네이버 메일","category":"SOCIAL","createdAt":"2024-06-03"}
        ],"pageNumber":0,"size":10,"hasNext":false}}
        """.trimIndent()

    val MOCK_CREATE_JSON =
        """
        {"status":200,"code":0,"message":null,"data":{"afternoteId":99}}
        """.trimIndent()

    val MOCK_PATCH_JSON =
        """
        {"status":200,"code":0,"message":null,"data":{"afternoteId":99}}
        """.trimIndent()

    val MOCK_DELETE_JSON =
        """
        {"status":200,"code":0,"message":null,"data":{"afternoteId":1}}
        """.trimIndent()

    val MOCK_SOCIAL_LOGIN_JSON =
        """
        {"status":200,"code":0,"message":null,"data":{"accessToken":"mock_access_token","refreshToken":"mock_refresh_token","userId":1,"isNewUser":false}}
        """.trimIndent()

    val MOCK_LOGIN_JSON =
        """
        {"status":200,"code":0,"message":null,"data":{"accessToken":"mock_access_token","refreshToken":"mock_refresh_token","userId":1}}
        """.trimIndent()

    val MOCK_LOGOUT_JSON =
        """
        {"status":200,"code":0,"message":null,"data":null}
        """.trimIndent()

    val MOCK_HOME_SUMMARY_JSON =
        """
        {"status":200,"code":0,"message":null,"data":{"userName":"박서연","isRecipientDesignated":false,"diaryCategoryCount":12,"deepThoughtCategoryCount":6}}
        """.trimIndent()
}
