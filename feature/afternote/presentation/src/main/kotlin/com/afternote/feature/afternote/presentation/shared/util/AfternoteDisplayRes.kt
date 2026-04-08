package com.afternote.feature.afternote.presentation.shared.util

import com.afternote.feature.afternote.domain.AfternoteServiceType
import com.afternote.feature.afternote.presentation.R

/** Label and icon drawable resource IDs for an afternote type key. */
data class AfternoteRes(
    val stringResId: Int,
    val drawableResId: Int,
)

/**
 * @param typeKey Writer: e.g. SOCIAL_NETWORK, GALLERY_AND_FILES, MEMORIAL. Receiver: e.g. INSTAGRAM, GALLERY, GUIDE, NAVER_MAIL.
 */
fun getAfternoteDisplayRes(typeKey: String): AfternoteRes {
    val (stringResId, displayKey) =
        TYPE_KEY_TO_DISPLAY_INFO[typeKey]
            ?: (R.string.afternote_category_social_network to typeKey)
    val drawableResId = DISPLAY_KEY_TO_DRAWABLE[displayKey] ?: R.drawable.feature_afternote_img_logo
    return AfternoteRes(stringResId = stringResId, drawableResId = drawableResId)
}

private data class DisplayEntry(
    val typeKey: String?,
    val displayKey: String,
    val stringResId: Int,
    val drawableResId: Int,
)

private val DISPLAY_ENTRIES: List<DisplayEntry> =
    listOf(
        DisplayEntry(
            "SOCIAL_NETWORK",
            "SOCIAL_NETWORK",
            R.string.afternote_category_social_network,
            R.drawable.feature_afternote_ic_social_pattern,
        ),
        DisplayEntry(
            "GALLERY_AND_FILES",
            "GALLERY_AND_FILES",
            R.string.receiver_afternote_item_gallery,
            R.drawable.feature_afternote_ic_gallery_pattern,
        ),
        DisplayEntry(
            "MEMORIAL",
            "MEMORIAL",
            R.string.receiver_afternote_item_memorial_guideline,
            R.drawable.feature_afternote_ic_memorial_guideline,
        ),
        DisplayEntry(
            "INSTAGRAM",
            "인스타그램",
            R.string.receiver_afternote_item_instagram,
            R.drawable.feature_afternote_img_insta_pattern,
        ),
        DisplayEntry(
            "GALLERY",
            "갤러리",
            R.string.receiver_afternote_item_gallery,
            R.drawable.feature_afternote_ic_gallery_pattern,
        ),
        DisplayEntry(
            "GUIDE",
            "추모 가이드라인",
            R.string.receiver_afternote_item_memorial_guideline,
            R.drawable.feature_afternote_ic_memorial_guideline,
        ),
        DisplayEntry(
            "NAVER_MAIL",
            "네이버 메일",
            R.string.receiver_afternote_item_naver_mail,
            R.drawable.feature_afternote_img_naver_mail_pattern,
        ),
        DisplayEntry(
            "FACEBOOK",
            "페이스북",
            R.string.afternote_category_social_network,
            R.drawable.feature_afternote_img_facebook_pattern,
        ),
        DisplayEntry(
            "X",
            "X",
            R.string.afternote_category_social_network,
            R.drawable.feature_afternote_img_x_pattern,
        ),
        DisplayEntry(
            "THREAD",
            "스레드",
            R.string.afternote_category_social_network,
            R.drawable.feature_afternote_img_thread_pattern,
        ),
        DisplayEntry(
            "TIKTOK",
            "틱톡",
            R.string.afternote_category_social_network,
            R.drawable.feature_afternote_img_tiktok_pattern,
        ),
        DisplayEntry(
            "YOUTUBE",
            "유튜브",
            R.string.afternote_category_social_network,
            R.drawable.feature_afternote_img_youtube_pattern,
        ),
        DisplayEntry(
            "KAKAOTALK",
            "카카오톡",
            R.string.afternote_category_social_network,
            R.drawable.feature_afternote_img_kakaotalk_pattern,
        ),
        DisplayEntry(
            "KAKAOSTORY",
            "카카오스토리",
            R.string.afternote_category_social_network,
            R.drawable.feature_afternote_img_kakaostory_pattern,
        ),
        DisplayEntry(
            "NAVER_BLOG",
            "네이버 블로그",
            R.string.afternote_category_social_network,
            R.drawable.feature_afternote_img_naverblog_pattern,
        ),
        DisplayEntry(
            "NAVER_CAFE",
            "네이버 카페",
            R.string.afternote_category_social_network,
            R.drawable.feature_afternote_img_navercafe_pattern,
        ),
        DisplayEntry(
            "NAVER_BAND",
            "네이버 밴드",
            R.string.afternote_category_social_network,
            R.drawable.feature_afternote_img_naverband_pattern,
        ),
        DisplayEntry(
            "DISCORD",
            "디스코드",
            R.string.afternote_category_social_network,
            R.drawable.feature_afternote_img_discord_pattern,
        ),
        DisplayEntry(
            "GOOGLE_PHOTO",
            "구글 포토",
            R.string.receiver_afternote_item_gallery,
            R.drawable.feature_afternote_img_googlephoto_pattern,
        ),
        DisplayEntry(
            "MYBOX",
            "네이버 MYBOX",
            R.string.receiver_afternote_item_gallery,
            R.drawable.feature_afternote_img_mybox_pattern,
        ),
        DisplayEntry(
            "ICLOUD",
            "아이클라우드",
            R.string.receiver_afternote_item_gallery,
            R.drawable.feature_afternote_img_icloud_pattern,
        ),
        DisplayEntry(
            "ONEDRIVE",
            "Onedrive",
            R.string.receiver_afternote_item_gallery,
            R.drawable.feature_afternote_img_onedrive_pattern,
        ),
        DisplayEntry(
            "TALKDRIVE",
            "카카오톡 톡서랍",
            R.string.receiver_afternote_item_gallery,
            R.drawable.feature_afternote_img_talkdrive_pattern,
        ),
        // Writer-only: displayKey used by getIconResForServiceName; no receiver typeKey
        DisplayEntry(
            typeKey = null,
            displayKey = "파일",
            stringResId = R.string.receiver_afternote_item_gallery,
            drawableResId = R.drawable.feature_afternote_ic_gallery_pattern,
        ),
    )

private val DISPLAY_KEY_TO_DRAWABLE: Map<String, Int> =
    DISPLAY_ENTRIES.associate { it.displayKey to it.drawableResId }

private val TYPE_KEY_TO_DISPLAY_INFO: Map<String, Pair<Int, String>> =
    DISPLAY_ENTRIES
        .mapNotNull { entry ->
            entry.typeKey?.let { key -> key to Pair(entry.stringResId, entry.displayKey) }
        }.toMap()

/**
 * Icon drawable res for an [AfternoteServiceType]. Same mapping as [getAfternoteDisplayRes]; use when you have [AfternoteServiceType].
 */
fun getIconResForServiceType(serviceType: AfternoteServiceType) = getAfternoteDisplayRes(serviceType.name).drawableResId

/**
 * Uses the same [DISPLAY_KEY_TO_DRAWABLE] as receiver typeKey lookups.
 */
fun getIconResForServiceName(serviceName: String): Int =
    DISPLAY_KEY_TO_DRAWABLE[serviceName]
        ?: getIconResForServiceType(AfternoteServiceCatalog.serviceTypeFor(serviceName))

/**
 * API typeKey(예: "INSTAGRAM") → 화면 표시명(예: "인스타그램") 변환.
 * 매핑이 없으면 typeKey를 그대로 반환합니다.
 */
fun getServiceNameForTypeKey(typeKey: String): String = TYPE_KEY_TO_DISPLAY_INFO[typeKey]?.second ?: typeKey
