package com.afternote.feature.afternote.presentation.shared.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.afternote.feature.afternote.presentation.R
import com.afternote.core.ui.R as CoreUiR

/** UI service titles and their label/icon resources. */
enum class AfternoteService(
    val displayKey: String,
    @param:StringRes val stringResId: Int,
    @param:DrawableRes val iconResId: Int,
) {
    INSTAGRAM(
        displayKey = "인스타그램",
        stringResId = R.string.afternote_receiver_afternote_item_instagram,
        iconResId = R.drawable.afternote_img_insta_pattern,
    ),
    GALLERY(
        displayKey = "갤러리",
        stringResId = R.string.afternote_receiver_afternote_item_gallery,
        iconResId = CoreUiR.drawable.core_ui_afternote_gallery_pattern,
    ),
    MEMORIAL(
        displayKey = "추억 노트",
        stringResId = R.string.afternote_receiver_afternote_item_memorial_guideline,
        iconResId = CoreUiR.drawable.core_ui_afternote_memorial_guideline,
    ),
    NAVER_MAIL(
        displayKey = "네이버 메일",
        stringResId = R.string.afternote_receiver_afternote_item_naver_mail,
        iconResId = R.drawable.afternote_img_naver_mail_pattern,
    ),
    DAUM_MAIL(
        displayKey = "다음 메일",
        stringResId = R.string.afternote_receiver_afternote_item_daum_mail,
        iconResId = R.drawable.afternote_img_daum_mail_pattern,
    ),
    GOOGLE_MAIL(
        displayKey = "구글 메일",
        stringResId = R.string.afternote_receiver_afternote_item_google_mail,
        iconResId = R.drawable.afternote_img_google_mail_pattern,
    ),
    OUTLOOK(
        displayKey = "outlook",
        stringResId = R.string.afternote_receiver_afternote_item_outlook,
        iconResId = R.drawable.afternote_img_outlook_pattern,
    ),
    FACEBOOK(
        displayKey = "페이스북",
        stringResId = R.string.afternote_category_social_network,
        iconResId = R.drawable.afternote_img_facebook_pattern,
    ),
    X(
        displayKey = "X",
        stringResId = R.string.afternote_category_social_network,
        iconResId = R.drawable.afternote_img_x_pattern,
    ),
    THREAD(
        displayKey = "스레드",
        stringResId = R.string.afternote_category_social_network,
        iconResId = R.drawable.afternote_img_thread_pattern,
    ),
    TIKTOK(
        displayKey = "틱톡",
        stringResId = R.string.afternote_category_social_network,
        iconResId = R.drawable.afternote_img_tiktok_pattern,
    ),
    YOUTUBE(
        displayKey = "유튜브",
        stringResId = R.string.afternote_category_social_network,
        iconResId = R.drawable.afternote_img_youtube_pattern,
    ),
    KAKAOTALK(
        displayKey = "카카오톡",
        stringResId = R.string.afternote_category_social_network,
        iconResId = R.drawable.afternote_img_kakaotalk_pattern,
    ),
    KAKAOSTORY(
        displayKey = "카카오스토리",
        stringResId = R.string.afternote_category_social_network,
        iconResId = R.drawable.afternote_img_kakaostory_pattern,
    ),
    NAVER_BLOG(
        displayKey = "네이버 블로그",
        stringResId = R.string.afternote_category_social_network,
        iconResId = R.drawable.afternote_img_naverblog_pattern,
    ),
    NAVER_CAFE(
        displayKey = "네이버 카페",
        stringResId = R.string.afternote_category_social_network,
        iconResId = R.drawable.afternote_img_navercafe_pattern,
    ),
    NAVER_BAND(
        displayKey = "네이버 밴드",
        stringResId = R.string.afternote_category_social_network,
        iconResId = R.drawable.afternote_img_naverband_pattern,
    ),
    DISCORD(
        displayKey = "디스코드",
        stringResId = R.string.afternote_category_social_network,
        iconResId = R.drawable.afternote_img_discord_pattern,
    ),
    GOOGLE_PHOTO(
        displayKey = "구글 포토",
        stringResId = R.string.afternote_receiver_afternote_item_gallery,
        iconResId = R.drawable.afternote_img_googlephoto_pattern,
    ),
    MYBOX(
        displayKey = "네이버 MYBOX",
        stringResId = R.string.afternote_receiver_afternote_item_gallery,
        iconResId = R.drawable.afternote_img_mybox_pattern,
    ),
    ICLOUD(
        displayKey = "아이클라우드",
        stringResId = R.string.afternote_receiver_afternote_item_gallery,
        iconResId = R.drawable.afternote_img_icloud_pattern,
    ),
    ONEDRIVE(
        displayKey = "Onedrive",
        stringResId = R.string.afternote_receiver_afternote_item_gallery,
        iconResId = R.drawable.afternote_img_onedrive_pattern,
    ),
    TALKDRIVE(
        displayKey = "카카오톡 톡서랍",
        stringResId = R.string.afternote_receiver_afternote_item_gallery,
        iconResId = R.drawable.afternote_img_talkdrive_pattern,
    ),
    FILES(
        displayKey = "파일",
        stringResId = R.string.afternote_receiver_afternote_item_gallery,
        iconResId = CoreUiR.drawable.core_ui_afternote_gallery_pattern,
    ),
    ;

    companion object {
        private val byDisplayKey: Map<String, AfternoteService> =
            entries.associateBy { it.displayKey }

        fun fromDisplayKeyOrNull(displayKey: String): AfternoteService? = byDisplayKey[displayKey]
    }
}
