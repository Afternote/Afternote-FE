package com.afternote.feature.afternote.presentation.shared.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.afternote.feature.afternote.presentation.R

/**
 * Maps API [typeKey] (when present), UI display keys / service titles, and label + icon resources.
 *
 * Replaces the former parallel [DISPLAY_ENTRIES] table in [com.afternote.feature.afternote.presentation.shared.util.AfternoteDisplayRes].
 */
enum class AfternoteService(
    val typeKey: String?,
    val displayKey: String,
    @StringRes val stringResId: Int,
    @DrawableRes val iconResId: Int,
) {
    SOCIAL_NETWORK_CATEGORY(
        typeKey = "SOCIAL_NETWORK",
        displayKey = "SOCIAL_NETWORK",
        stringResId = R.string.afternote_category_social_network,
        iconResId = R.drawable.feature_afternote_ic_social_pattern,
    ),
    GALLERY_AND_FILES_CATEGORY(
        typeKey = "GALLERY_AND_FILES",
        displayKey = "GALLERY_AND_FILES",
        stringResId = R.string.receiver_afternote_item_gallery,
        iconResId = R.drawable.feature_afternote_ic_gallery_pattern,
    ),
    MEMORIAL_CATEGORY(
        typeKey = "MEMORIAL",
        displayKey = "MEMORIAL",
        stringResId = R.string.receiver_afternote_item_memorial_guideline,
        iconResId = R.drawable.feature_afternote_ic_memorial_guideline,
    ),
    INSTAGRAM(
        typeKey = "INSTAGRAM",
        displayKey = "인스타그램",
        stringResId = R.string.receiver_afternote_item_instagram,
        iconResId = R.drawable.feature_afternote_img_insta_pattern,
    ),
    GALLERY(
        typeKey = "GALLERY",
        displayKey = "갤러리",
        stringResId = R.string.receiver_afternote_item_gallery,
        iconResId = R.drawable.feature_afternote_ic_gallery_pattern,
    ),
    MEMORIAL_GUIDELINE(
        typeKey = "GUIDE",
        displayKey = "추모 가이드라인",
        stringResId = R.string.receiver_afternote_item_memorial_guideline,
        iconResId = R.drawable.feature_afternote_ic_memorial_guideline,
    ),
    NAVER_MAIL(
        typeKey = "NAVER_MAIL",
        displayKey = "네이버 메일",
        stringResId = R.string.receiver_afternote_item_naver_mail,
        iconResId = R.drawable.feature_afternote_img_naver_mail_pattern,
    ),
    FACEBOOK(
        typeKey = "FACEBOOK",
        displayKey = "페이스북",
        stringResId = R.string.afternote_category_social_network,
        iconResId = R.drawable.feature_afternote_img_facebook_pattern,
    ),
    X(
        typeKey = "X",
        displayKey = "X",
        stringResId = R.string.afternote_category_social_network,
        iconResId = R.drawable.feature_afternote_img_x_pattern,
    ),
    THREAD(
        typeKey = "THREAD",
        displayKey = "스레드",
        stringResId = R.string.afternote_category_social_network,
        iconResId = R.drawable.feature_afternote_img_thread_pattern,
    ),
    TIKTOK(
        typeKey = "TIKTOK",
        displayKey = "틱톡",
        stringResId = R.string.afternote_category_social_network,
        iconResId = R.drawable.feature_afternote_img_tiktok_pattern,
    ),
    YOUTUBE(
        typeKey = "YOUTUBE",
        displayKey = "유튜브",
        stringResId = R.string.afternote_category_social_network,
        iconResId = R.drawable.feature_afternote_img_youtube_pattern,
    ),
    KAKAOTALK(
        typeKey = "KAKAOTALK",
        displayKey = "카카오톡",
        stringResId = R.string.afternote_category_social_network,
        iconResId = R.drawable.feature_afternote_img_kakaotalk_pattern,
    ),
    KAKAOSTORY(
        typeKey = "KAKAOSTORY",
        displayKey = "카카오스토리",
        stringResId = R.string.afternote_category_social_network,
        iconResId = R.drawable.feature_afternote_img_kakaostory_pattern,
    ),
    NAVER_BLOG(
        typeKey = "NAVER_BLOG",
        displayKey = "네이버 블로그",
        stringResId = R.string.afternote_category_social_network,
        iconResId = R.drawable.feature_afternote_img_naverblog_pattern,
    ),
    NAVER_CAFE(
        typeKey = "NAVER_CAFE",
        displayKey = "네이버 카페",
        stringResId = R.string.afternote_category_social_network,
        iconResId = R.drawable.feature_afternote_img_navercafe_pattern,
    ),
    NAVER_BAND(
        typeKey = "NAVER_BAND",
        displayKey = "네이버 밴드",
        stringResId = R.string.afternote_category_social_network,
        iconResId = R.drawable.feature_afternote_img_naverband_pattern,
    ),
    DISCORD(
        typeKey = "DISCORD",
        displayKey = "디스코드",
        stringResId = R.string.afternote_category_social_network,
        iconResId = R.drawable.feature_afternote_img_discord_pattern,
    ),
    GOOGLE_PHOTO(
        typeKey = "GOOGLE_PHOTO",
        displayKey = "구글 포토",
        stringResId = R.string.receiver_afternote_item_gallery,
        iconResId = R.drawable.feature_afternote_img_googlephoto_pattern,
    ),
    MYBOX(
        typeKey = "MYBOX",
        displayKey = "네이버 MYBOX",
        stringResId = R.string.receiver_afternote_item_gallery,
        iconResId = R.drawable.feature_afternote_img_mybox_pattern,
    ),
    ICLOUD(
        typeKey = "ICLOUD",
        displayKey = "아이클라우드",
        stringResId = R.string.receiver_afternote_item_gallery,
        iconResId = R.drawable.feature_afternote_img_icloud_pattern,
    ),
    ONEDRIVE(
        typeKey = "ONEDRIVE",
        displayKey = "Onedrive",
        stringResId = R.string.receiver_afternote_item_gallery,
        iconResId = R.drawable.feature_afternote_img_onedrive_pattern,
    ),
    TALKDRIVE(
        typeKey = "TALKDRIVE",
        displayKey = "카카오톡 톡서랍",
        stringResId = R.string.receiver_afternote_item_gallery,
        iconResId = R.drawable.feature_afternote_img_talkdrive_pattern,
    ),
    FILES(
        typeKey = null,
        displayKey = "파일",
        stringResId = R.string.receiver_afternote_item_gallery,
        iconResId = R.drawable.feature_afternote_ic_gallery_pattern,
    ),
    ;

    companion object {
        private val byTypeKey: Map<String, AfternoteService> =
            entries.mapNotNull { e -> e.typeKey?.let { it to e } }.toMap()

        private val byDisplayKey: Map<String, AfternoteService> =
            entries.associateBy { it.displayKey }

        fun fromTypeKeyOrNull(typeKey: String): AfternoteService? = byTypeKey[typeKey]

        fun fromDisplayKeyOrNull(displayKey: String): AfternoteService? = byDisplayKey[displayKey]
    }
}
