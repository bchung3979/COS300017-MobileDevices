package com.example.fakeuber

import androidx.lifecycle.ViewModel
import com.example.fakeuber.model.Car
import com.example.fakeuber.model.CarRepository
import com.example.fakeuber.model.Rental

/** 3 kiểu sắp xếp đề yêu cầu, NONE = giữ thứ tự gốc. */
enum class SortOrder { NONE, RATING, YEAR, COST }

/**
 * Toàn bộ trạng thái của màn hình chính: số dư, đơn thuê, yêu thích, từ khoá tìm kiếm,
 * cách sắp xếp và xe đang hiện.
 *
 * Vì sao dùng ViewModel? Bật/tắt dark mode (hoặc xoay màn hình) sẽ huỷ và tạo lại Activity,
 * còn ViewModel thì vẫn sống nên dữ liệu không mất. Dữ liệu chỉ nằm trong bộ nhớ:
 * tắt app là mọi thứ về lại ban đầu, đúng yêu cầu "không lưu dữ liệu" của đề.
 */
class MainViewModel : ViewModel() {

    /** Số credit còn lại. */
    var balance: Int = CarRepository.START_BALANCE
        private set // Activity đọc được nhưng không sửa được

    // "Backing property": bên trong là danh sách sửa được, bên ngoài chỉ thấy List chỉ-đọc,
    // nên chỉ book() / cancel() mới thay đổi được nó.
    private val _rentals = mutableListOf<Rental>()
    val rentals: List<Rental> get() = _rentals

    private val favouriteIds = mutableSetOf<Int>()

    /** Từ khoá tìm kiếm hiện tại. Rỗng = không lọc. */
    var query: String = ""
        private set

    var sortOrder: SortOrder = SortOrder.NONE
        private set

    /** Vị trí của xe đang hiện trong [displayedCars]. */
    var currentIndex: Int = 0
        private set

    /**
     * Danh sách đang xem = chưa thuê → khớp từ khoá → đã sắp xếp.
     * Luôn tạo danh sách MỚI từ CarRepository.cars nên dữ liệu gốc không bao giờ bị thay đổi.
     */
    fun displayedCars(): List<Car> {
        val rentedIds = _rentals.map { it.car.id }.toSet()
        val visible = CarRepository.cars.filter { it.id !in rentedIds && it.matches(query) }
        return when (sortOrder) {
            SortOrder.NONE -> visible
            SortOrder.RATING -> visible.sortedByDescending { it.rating } // cao → thấp
            SortOrder.YEAR -> visible.sortedByDescending { it.year }     // mới → cũ
            SortOrder.COST -> visible.sortedBy { it.costPerDay }         // rẻ → đắt
        }
    }

    /** Xe đang hiện trên thẻ, hoặc null nếu không còn xe nào. */
    fun currentCar(): Car? = displayedCars().getOrNull(currentIndex)

    /** Sang xe tiếp theo; hết danh sách thì quay về xe đầu. */
    fun next() {
        val size = displayedCars().size
        // Phép chia lấy dư giúp quay vòng: 5 xe, vị trí 4 + 1 = 5 → 5 % 5 = 0 (về xe đầu).
        if (size > 0) currentIndex = (currentIndex + 1) % size
    }

    /**
     * Đặt từ khoá mới. Bỏ qua nếu không đổi: sau khi bật dark mode, SearchView tự khôi phục chữ
     * và gọi lại hàm này, nếu không chặn thì app sẽ nhảy về xe đầu tiên.
     */
    fun setQuery(text: String) {
        val trimmed = text.trim()
        if (trimmed == query) return
        query = trimmed
        currentIndex = 0
    }

    /** Chọn lại đúng kiểu đang dùng thì tắt sắp xếp. */
    fun setSort(order: SortOrder) {
        sortOrder = if (order == sortOrder) SortOrder.NONE else order
        currentIndex = 0
    }

    // ---------- Yêu thích ----------

    fun isFavourite(car: Car): Boolean = car.id in favouriteIds

    /** Thêm hoặc bỏ [car] khỏi yêu thích; trả về true nếu giờ nó là yêu thích. */
    fun toggleFavourite(car: Car): Boolean =
        if (favouriteIds.remove(car.id)) false else favouriteIds.add(car.id)

    /** Các xe yêu thích (kể cả xe đang được thuê). */
    fun favouriteCars(): List<Car> = CarRepository.cars.filter { it.id in favouriteIds }

    /** true nếu [car] đang được thuê (khi đó xe bị ẩn khỏi danh sách). */
    fun isRented(car: Car): Boolean = _rentals.any { it.car.id == car.id }

    /** Chuyển thẻ tới [car] nếu xe có trong danh sách đang xem (dùng cho chip yêu thích). */
    fun show(car: Car): Boolean {
        val index = displayedCars().indexOfFirst { it.id == car.id }
        if (index >= 0) currentIndex = index
        return index >= 0
    }

    // ---------- Đơn thuê ----------

    /** Lưu đơn từ RentActivity: xe rời danh sách, trừ credit. */
    fun book(rental: Rental) {
        _rentals.add(rental)
        balance -= rental.total
        keepIndexInRange()
    }

    /** Huỷ đơn: xe quay lại danh sách, hoàn credit. */
    fun cancel(rental: Rental) {
        if (_rentals.remove(rental)) balance += rental.total
        keepIndexInRange()
    }

    /** Khi danh sách ngắn lại, đảm bảo vị trí vẫn trỏ vào một xe có thật. */
    private fun keepIndexInRange() {
        val size = displayedCars().size
        currentIndex = if (size == 0) 0 else currentIndex.coerceAtMost(size - 1)
    }
}

/** Tìm theo tên hãng, tên model hoặc cả hai, không phân biệt hoa thường. */
private fun Car.matches(query: String): Boolean =
    query.isBlank() ||
        brand.contains(query, ignoreCase = true) ||
        model.contains(query, ignoreCase = true) ||
        displayName.contains(query, ignoreCase = true)
