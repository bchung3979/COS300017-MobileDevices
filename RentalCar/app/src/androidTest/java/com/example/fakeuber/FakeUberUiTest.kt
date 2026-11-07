package com.example.fakeuber

import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test giao diện cho toàn bộ luồng thuê xe. Mỗi test mở một MainActivity mới (500 credit,
 * chưa có đơn, chưa có yêu thích). Không test RatingBar (đề có lưu ý Espresso khó test nó),
 * chỉ kiểm tra chữ và nút bấm.
 */
@RunWith(AndroidJUnit4::class)
class FakeUberUiTest {

    // Rule chạy theo "order": phần ghi/chặn Intent phải sẵn sàng TRƯỚC khi mở MainActivity.
    @get:Rule(order = 0)
    val intents = IntentsRule()

    @get:Rule(order = 1)
    val activity = ActivityScenarioRule(MainActivity::class.java)

    private val robot = FakeUberRobot()

    @Before
    fun setUp() {
        robot.stubPhotoPicker()
    }

    // ---------- Duyệt xe ----------

    @Test
    fun next_movesThroughCarsAndWrapsAround() {
        with(robot) {
            checkPosition("1 / 5")
            checkModel("VF 5 Plus")
            next()
            checkPosition("2 / 5")
            checkModel("Vios 1.5G")
            next(4)
            checkPosition("1 / 5")
            checkModel("VF 5 Plus")
        }
    }

    // ---------- Thuê xe ----------

    @Test
    fun save_booksCar_spendsCredits_andListsIt() {
        with(robot) {
            bookCurrentCar(days = 3) // VF 5: 60 x 3 = 180
            checkSnackbar("booked")
            checkCredits(320)
            checkPosition("1 / 4")
            checkDisplayed(R.id.btnCancelBooking)
        }
    }

    @Test
    fun back_cancels_andNothingChanges() {
        with(robot) {
            rent()
            setDays(5)
            back()
            checkSnackbar("Rental not saved")
            checkCredits(500)
            checkPosition("1 / 5")
        }
    }

    @Test
    fun overLimit_blocksSave() {
        with(robot) {
            next(4) // Fortuner, 110 / day
            rent()
            setDays(4) // 440 > 400
            checkTextContains(R.id.tvError, "can't go over 400")
            addBothPhotos()
            save()
            checkSnackbar("Can't save yet")
            checkDisplayed(R.id.btnSave) // vẫn còn ở màn Rent
        }
    }

    @Test
    fun notEnoughCredits_blocksSave() {
        with(robot) {
            next(4)
            bookCurrentCar(days = 3) // Fortuner 330 → còn 170
            checkCredits(170)
            next() // sau khi đặt, thẻ đang ở Xpander; Next quay vòng về VF 5
            rent()
            setDays(3) // VF 5: 60 x 3 = 180 > 170
            checkTextContains(R.id.tvError, "Not enough credits")
        }
    }

    @Test
    fun missingPhotos_markTilesRequired() {
        with(robot) {
            rent()
            save()
            checkSnackbar("Can't save yet")
            checkTileHint(R.id.tileId, "Required")
            checkTileHint(R.id.tileLicence, "Required")
        }
    }

    @Test
    fun cancelBooking_refundsCredits_andCarComesBack() {
        with(robot) {
            bookCurrentCar(days = 2) // 120
            checkCredits(380)
            cancelFirstBooking()
            checkSnackbar("120 credits refunded")
            checkCredits(500)
            checkPosition("1 / 5")
        }
    }

    // ---------- Tìm kiếm & sắp xếp ----------

    @Test
    fun search_filtersByName_andShowsEmptyState() {
        with(robot) {
            search("toyota")
            checkPosition("1 / 2")
            search("xyz")
            checkPosition("0 / 0")
            checkTextContains(R.id.tvNoCarsTitle, "No cars match")
            clearSearch()
            checkPosition("1 / 5")
        }
    }

    @Test
    fun sort_reordersByRatingYearAndCost() {
        with(robot) {
            sortBy("Rating · high to low")
            checkModel("Fortuner 2.4AT") // 4.9
            sortBy("Year · newest first")
            checkModel("VF 5 Plus") // 2025
            next()
            checkModel("Vios 1.5G") // 2024
            sortBy("Cost · lowest first")
            next()
            checkModel("Vios 1.5G") // 60 rồi tới 80
        }
    }

    // ---------- Yêu thích ----------

    @Test
    fun heart_addsCarToFavourites() {
        with(robot) {
            checkText(R.id.tvFavCount, "0 saved")
            favourite()
            checkText(R.id.tvFavCount, "1 saved")
            checkDisplayed(R.id.rvFavourites)
        }
    }
}
