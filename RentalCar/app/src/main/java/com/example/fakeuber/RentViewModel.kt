package com.example.fakeuber

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.fakeuber.model.Car
import com.example.fakeuber.model.CarRepository
import com.example.fakeuber.model.Rental

/** Các lỗi về credit, được kiểm tra liên tục khi kéo Slider. */
enum class CreditProblem { OVER_LIMIT, NOT_ENOUGH_CREDITS }

/**
 * Trạng thái màn Rent: số ngày, 2 ảnh giấy tờ và phần kiểm tra hợp lệ.
 * Vẫn còn sau khi bật/tắt dark mode nên Slider và ảnh không bị mất.
 */
class RentViewModel : ViewModel() {

    lateinit var car: Car
        private set

    /** Số dư nhận từ MainActivity (ở đây chỉ đọc, việc trừ tiền do Main làm khi Save). */
    var balance: Int = 0
        private set

    var days: Int = MIN_DAYS
        private set

    var idPhoto: Uri? = null
        private set

    var licencePhoto: Uri? = null
        private set

    /** Thành true sau khi bấm Save mà thiếu ảnh, để ô ảnh chuyển sang viền đỏ. */
    var showMissingPhotos: Boolean = false
        private set

    private var started = false

    /** Gọi trong onCreate. Chỉ lần gọi đầu có tác dụng (onCreate chạy lại khi Activity bị tạo lại). */
    fun start(car: Car, balance: Int) {
        if (started) return
        this.car = car
        this.balance = balance
        started = true
    }

    /** Tổng tiền đơn thuê (credit), tính lại mỗi lần đọc. */
    val total: Int get() = car.costPerDay * days

    val balanceAfter: Int get() = balance - total

    fun setDays(value: Int) {
        days = value.coerceIn(MIN_DAYS, MAX_DAYS) // luôn nằm trong 1..7
    }

    fun setIdPhoto(uri: Uri) {
        idPhoto = uri
    }

    fun setLicencePhoto(uri: Uri) {
        licencePhoto = uri
    }

    /** Kiểm tra hạn mức 400/đơn trước, rồi mới tới số dư. null = credit hợp lệ. */
    fun creditProblem(): CreditProblem? = when {
        total > CarRepository.RENTAL_LIMIT -> CreditProblem.OVER_LIMIT
        total > balance -> CreditProblem.NOT_ENOUGH_CREDITS
        else -> null
    }

    /**
     * Nút Save. Trả về [Rental] nếu mọi thứ hợp lệ.
     * Trả về null nếu có lỗi: màn hình vẫn mở (đề yêu cầu không cho về màn Main
     * cho tới khi sửa xong lỗi hoặc bấm huỷ).
     */
    fun trySave(): Rental? {
        // Copy sang biến val cục bộ để Kotlin tự hiểu là khác null sau khi kiểm tra (smart cast).
        val id = idPhoto
        val licence = licencePhoto
        showMissingPhotos = id == null || licence == null
        if (id == null || licence == null || creditProblem() != null) return null
        return Rental(car, days, total, id.toString(), licence.toString())
    }

    companion object {
        const val MIN_DAYS = 1
        const val MAX_DAYS = 7
    }
}
