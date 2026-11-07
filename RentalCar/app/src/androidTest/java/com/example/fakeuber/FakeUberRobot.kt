package com.example.fakeuber

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.provider.MediaStore
import android.os.SystemClock
import android.view.View
import androidx.core.widget.NestedScrollView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.android.material.slider.Slider
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anyOf
import org.hamcrest.Matchers.containsString

/**
 * "Robot" gom các lệnh Espresso hay lặp lại, để mỗi test đọc như các bước người dùng thao tác
 * và chi tiết Espresso chỉ nằm ở một chỗ (không lặp code).
 *
 * Thứ tự xe mặc định: 1 VF 5 Plus (60) · 2 Vios 1.5G (80) · 3 Mazda3 1.5 (90)
 *                     4 Xpander AT (100) · 5 Fortuner 2.4AT (110)
 */
class FakeUberRobot {

    // ---------- Giả lập Photo Picker ----------

    /**
     * Intent của Photo Picker được trả lời ngay bằng một ảnh có sẵn trong app, nên test không
     * mở thư viện ảnh thật. Chỉ chặn đúng các action của Photo Picker: lần đầu chặn mọi Intent
     * ra ngoài app thì chặn luôn màn hình phụ của ActivityScenario, làm mỗi test phải chờ 45 giây.
     */
    fun stubPhotoPicker() {
        val fakePhoto = Uri.parse("android.resource://com.example.fakeuber/drawable/car_mazda3")
        val photoPicker = anyOf(
            hasAction(MediaStore.ACTION_PICK_IMAGES),                         // Android 13+
            hasAction("androidx.activity.result.contract.action.PICK_IMAGES"), // bản qua Play services
            hasAction(Intent.ACTION_OPEN_DOCUMENT),                           // Android cũ
        )
        intending(photoPicker)
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(fakePhoto)))
    }

    // ---------- Màn Main ----------

    fun next(times: Int = 1) = repeat(times) { tap(R.id.btnNext) }

    fun rent() = tap(R.id.btnRent)

    fun favourite() = tap(R.id.btnFavourite)

    fun search(text: String) {
        onView(withId(androidx.appcompat.R.id.search_src_text))
            .perform(clearText(), typeText(text), closeSoftKeyboard())
    }

    fun clearSearch() = tap(R.id.btnClearSearch)

    /** Mở menu Sort và chọn mục có tiêu đề này. */
    fun sortBy(menuTitle: String) {
        tap(R.id.btnSort)
        onView(withText(menuTitle)).inRoot(isPlatformPopup()).perform(click())
    }

    /** Snackbar "booked" có thể nằm đè lên nút, nên chờ nó biến mất rồi mới bấm. */
    fun cancelFirstBooking() {
        waitForNoSnackbar()
        tap(R.id.btnCancelBooking)
    }

    // ---------- Màn Rent ----------

    fun setDays(days: Int) {
        onView(withId(R.id.sliderDays)).perform(nestedScrollTo(), setSliderValue(days.toFloat()))
    }

    fun addBothPhotos() {
        tap(R.id.tileId)
        tap(R.id.tileLicence)
    }

    fun save() = onView(withId(R.id.btnSave)).perform(click())

    fun back() = onView(withId(R.id.btnBack)).perform(click())

    /** Luồng thuê thành công: Rent → chọn ngày → thêm ảnh → Save. */
    fun bookCurrentCar(days: Int = 1) {
        rent()
        setDays(days)
        addBothPhotos()
        save()
    }

    // ---------- Kiểm tra ----------

    fun checkText(id: Int, text: String) = onView(withId(id)).check(matches(withText(text)))

    fun checkTextContains(id: Int, part: String) =
        onView(withId(id)).perform(nestedScrollTo()).check(matches(withText(containsString(part))))

    fun checkModel(model: String) = checkText(R.id.tvModel, model)

    fun checkPosition(position: String) = checkText(R.id.tvPosition, position)

    fun checkCredits(credits: Int) = checkText(R.id.tvCredits, "$credits credits")

    /**
     * Snackbar xếp hàng: cái mới chỉ hiện sau khi cái cũ trượt đi. Vì vậy không kiểm tra một lần
     * mà kiểm tra lặp lại tới tối đa [timeoutMs] cho tới khi thấy đúng thông báo.
     */
    fun checkSnackbar(part: String, timeoutMs: Long = 5_000) = eventually(timeoutMs) {
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(containsString(part))))
    }

    /** Snackbar LENGTH_LONG hiện khoảng 2,75 giây; chờ nó trượt đi. */
    fun waitForNoSnackbar(timeoutMs: Long = 6_000) = eventually(timeoutMs) {
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(doesNotExist())
    }

    fun checkDisplayed(id: Int) = onView(withId(id)).perform(nestedScrollTo()).check(matches(isDisplayed()))

    /** Chữ gợi ý trong một ô ảnh (2 ô dùng chung id con nên phải chỉ rõ ô nào). */
    fun checkTileHint(tileId: Int, hint: String) =
        onView(allOf(withId(R.id.tvDocHint), isDescendantOfA(withId(tileId))))
            .check(matches(withText(hint)))

    // ---------- Hàm hỗ trợ ----------

    /** Chạy lại [check] mỗi 100ms cho tới khi qua hoặc hết [timeoutMs]. */
    private fun eventually(timeoutMs: Long, check: () -> Unit) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (true) {
            try {
                check()
                return
            } catch (e: Throwable) { // NoMatchingViewException hoặc AssertionError
                if (SystemClock.uptimeMillis() > deadline) throw e
                Thread.sleep(100)
            }
        }
    }

    private fun tap(id: Int) = onView(withId(id)).perform(nestedScrollTo(), click())

    /**
     * scrollTo() của Espresso không hỗ trợ NestedScrollView, nên tự viết: cuộn NestedScrollView gần nhất
     * để view nằm ở phần trên màn hình (tránh thanh Back/Save). View không nằm trong NestedScrollView thì bỏ qua.
     */
    private fun nestedScrollTo(): ViewAction = object : ViewAction {
        override fun getConstraints(): Matcher<View> = withEffectiveVisibility(Visibility.VISIBLE)
        override fun getDescription() = "scroll inside NestedScrollView"
        override fun perform(uiController: UiController, view: View) {
            var parent = view.parent
            while (parent != null && parent !is NestedScrollView) parent = parent.parent
            val scroll = parent as? NestedScrollView ?: return
            val rect = Rect()
            view.getDrawingRect(rect)
            scroll.offsetDescendantRectToMyCoords(view, rect)
            scroll.scrollTo(0, (rect.top - scroll.height / 3).coerceAtLeast(0))
            uiController.loopMainThreadUntilIdle()
        }
    }

    /** Espresso không có sẵn thao tác cho Material Slider. */
    private fun setSliderValue(value: Float): ViewAction = object : ViewAction {
        override fun getConstraints(): Matcher<View> = isAssignableFrom(Slider::class.java)
        override fun getDescription() = "set slider to $value"
        override fun perform(uiController: UiController, view: View) {
            (view as Slider).value = value
            uiController.loopMainThreadUntilIdle()
        }
    }
}
