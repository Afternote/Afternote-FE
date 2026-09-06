package com.afternote.afternote_fe.test

import android.app.ActivityOptions
import android.os.Build
import android.os.Bundle

/**
 * 실제 알림 탭은 시스템(SystemUI)이 content [android.app.PendingIntent]를 보내므로 background
 * activity start 가 허용된다. 알림 진입 androidTest 는 앱 프로세스가 스스로 보내기 때문에 Android 14의
 * BAL hardening 에 막힌다 — 앱에 보이는 창이 없을 때만(`Background activity launch blocked`, `BAL_BLOCK`).
 * 보내는 쪽 권한을 시스템과 같게 명시해 프로덕션 진입과 같은 조건으로 맞춘다.
 *
 * 알림 진입 테스트가 두 벌(웜 진입·콜드 스타트)이라 같은 이유의 설명이 갈라지지 않도록 여기 한 곳에 둔다.
 */
fun backgroundActivityStartOptions(): Bundle? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        ActivityOptions
            .makeBasic()
            .setPendingIntentBackgroundActivityStartMode(
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED,
            ).toBundle()
    } else {
        null
    }
