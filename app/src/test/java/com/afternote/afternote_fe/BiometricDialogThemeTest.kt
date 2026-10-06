package com.afternote.afternote_fe

import android.app.Activity
import android.app.Application
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Looper
import android.util.TypedValue
import android.widget.Button
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** 실제 앱 테마로 AndroidX 지문 대화상자와 같은 AppCompat dialog를 생성한다. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 27, 28], application = Application::class)
class BiometricDialogThemeTest {
    @Test
    fun `API26부터 28 앱 테마는 AppCompat 인증 대화상자를 띄우고 닫을 수 있다`() {
        val controller = Robolectric.buildActivity(Activity::class.java)
        controller.get().setTheme(R.style.Theme_Afternotefe_Main)
        controller.setup()
        val activity = controller.get()
        var canceled = false
        val dialog = createAppCompatDialog(activity) { canceled = true }

        try {
            // 플랫폼 Material 테마만 있으면 show()에서 AppCompat 테마 요구 예외가 발생한다.
            dialog.show()
            assertTrue(dialog.isShowing)
            dialog.findViewById<Button>(android.R.id.button2).performClick()
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(canceled)
            assertFalse(dialog.isShowing)
        } finally {
            dialog.dismiss()
            controller.pause().stop().destroy()
        }
    }

    @Test
    @Config(sdk = [29, 30, 35])
    fun `API29 이상은 플랫폼 라이트 테마와 액션바 없는 동작을 유지한다`() {
        val controller = Robolectric.buildActivity(Activity::class.java)
        controller.get().setTheme(R.style.Theme_Afternotefe_Main)
        controller.setup()
        val activity = controller.get()
        val dialog =
            android.app.AlertDialog
                .Builder(activity)
                .setTitle("본인 인증")
                .setNegativeButton(android.R.string.cancel, null)
                .create()

        try {
            val attributes =
                activity.obtainStyledAttributes(
                    intArrayOf(android.R.attr.isLightTheme, android.R.attr.windowActionBar, android.R.attr.windowNoTitle),
                )
            try {
                assertTrue(attributes.getBoolean(0, false))
                assertFalse(attributes.getBoolean(1, true))
                assertTrue(attributes.getBoolean(2, false))
            } finally {
                attributes.recycle()
            }
            assertFalse(activity.theme.resolveAttribute(androidx.appcompat.R.attr.windowActionBar, TypedValue(), true))
            dialog.show()
            assertTrue(dialog.isShowing)
            dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).performClick()
            shadowOf(Looper.getMainLooper()).idle()
            assertFalse(dialog.isShowing)
        } finally {
            dialog.dismiss()
            controller.pause().stop().destroy()
        }
    }

    private fun createAppCompatDialog(
        context: Context,
        onCancel: () -> Unit,
    ): Dialog {
        // AppCompat는 biometric의 runtime 의존이다. 테스트용 compile 의존을 늘리지 않고
        // 실제 라이브러리의 공개 Builder API를 호출한다.
        val builderClass = Class.forName("androidx.appcompat.app.AlertDialog\$Builder")
        val builder = builderClass.getConstructor(Context::class.java).newInstance(context)
        builderClass.getMethod("setTitle", CharSequence::class.java).invoke(builder, "지문 인증")
        builderClass.getMethod("setMessage", CharSequence::class.java).invoke(builder, "지문 센서를 터치하세요")
        builderClass
            .getMethod("setNegativeButton", Int::class.javaPrimitiveType, DialogInterface.OnClickListener::class.java)
            .invoke(builder, android.R.string.cancel, DialogInterface.OnClickListener { _, _ -> onCancel() })
        return builderClass.getMethod("create").invoke(builder) as Dialog
    }
}
