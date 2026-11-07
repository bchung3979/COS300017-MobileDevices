package com.example.fakeuber

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.core.view.isVisible
import com.example.fakeuber.databinding.ActivityMainBinding
import com.example.fakeuber.databinding.ItemRentalBinding
import com.example.fakeuber.model.Car
import com.example.fakeuber.model.Rental
import com.example.fakeuber.ui.DarkModeToggle
import com.example.fakeuber.ui.FavouriteAdapter
import com.example.fakeuber.ui.FuSnackbar
import java.util.Locale

/**
 * Màn hình chính: hiện từng xe một (thông tin nằm dưới ảnh), số dư credit ở header,
 * tìm kiếm, sắp xếp, danh sách yêu thích và các xe đang thuê.
 *
 * Cả class dùng chung một luồng: thao tác của người dùng → gọi hàm trong MainViewModel → render().
 * render() vẽ lại toàn bộ màn hình từ dữ liệu, nên giao diện luôn khớp với dữ liệu,
 * kể cả sau khi Activity bị tạo lại do bật/tắt dark mode.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // "by viewModels()" trả lại ĐÚNG ViewModel cũ khi Activity bị tạo lại (dark mode, xoay màn hình),
    // nhờ vậy số dư, danh sách thuê và yêu thích không bị mất.
    private val viewModel: MainViewModel by viewModels()

    private val favouriteAdapter = FavouriteAdapter(
        isRented = { car -> viewModel.isRented(car) },
        onClick = { car -> openFavourite(car) },
    )

    /**
     * Mở RentActivity và nhận kết quả trả về (Activity Result API).
     * - RESULT_OK kèm Rental (Parcelable) = người dùng bấm Save → ghi nhận đơn thuê.
     * - Trường hợp còn lại = Back / huỷ → không thay đổi gì, chỉ báo cho người dùng.
     */
    private val rentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            val rental = data?.let { IntentCompat.getParcelableExtra(it, RentActivity.EXTRA_RENTAL, Rental::class.java) }
            if (result.resultCode == RESULT_OK && rental != null) {
                onBooked(rental)
            } else {
                val car = data?.let { IntentCompat.getParcelableExtra(it, RentActivity.EXTRA_CAR, Car::class.java) }
                FuSnackbar.show(
                    binding.root,
                    getString(R.string.snack_not_saved_title),
                    getString(R.string.snack_not_saved_msg, car?.displayName ?: getString(R.string.this_car)),
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarPadding()

        setUpStaticText()
        DarkModeToggle.bind(binding.darkToggle)
        binding.rvFavourites.adapter = favouriteAdapter

        setUpCarCard()
        setUpSearch()
        binding.btnSort.setOnClickListener { showSortMenu() }

        render()
    }

    // ---------------- Thiết lập ----------------

    /** Edge-to-edge: chừa khoảng trống để nội dung không bị thanh trạng thái / thanh điều hướng che. */
    private fun applySystemBarPadding() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    /** Phần chữ cố định: logo và tiêu đề, mỗi cái có một phần tô màu cam. */
    private fun setUpStaticText() {
        binding.tvLogo.text = withAccent(getString(R.string.logo_main), getString(R.string.logo_dot))
        binding.tvHeadline.text =
            withAccent(getString(R.string.headline_start) + " ", getString(R.string.headline_accent))
        binding.statYear.tvStatLabel.text = getString(R.string.stat_year)
        binding.statKm.tvStatLabel.text = getString(R.string.stat_km)
        binding.statCost.tvStatLabel.text = getString(R.string.stat_cost)
    }

    /** Ghép chữ thường + phần chữ màu cam (chữ cỡ lớn nên chỉ cần tương phản 3:1). */
    private fun withAccent(plain: String, accent: String): CharSequence = buildSpannedString {
        append(plain)
        color(ContextCompat.getColor(this@MainActivity, R.color.fu_accent)) { append(accent) }
    }

    /** Các nút trên thẻ xe: Next, Rent, trái tim và nhấn giữ ảnh. */
    private fun setUpCarCard() = with(binding) {
        btnNext.setOnClickListener {
            viewModel.next()
            render()
        }
        btnRent.setOnClickListener {
            val car = viewModel.currentCar() ?: return@setOnClickListener
            // Intent tường minh mang theo xe (Parcelable) + số dư hiện tại.
            // Dùng launch() thay vì startActivity() vì cần nhận kết quả trả về.
            rentLauncher.launch(RentActivity.newIntent(this@MainActivity, car, viewModel.balance))
        }
        btnFavourite.setOnClickListener { toggleFavourite() }
        // Nhấn giữ ảnh = cách thứ hai để thêm yêu thích (đề cho phép icon tim, nút hoặc nhấn giữ).
        imgCar.setOnLongClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            toggleFavourite()
            true // đã xử lý xong, Android sẽ không gửi thêm sự kiện click thường
        }
        // Accessibility: TalkBack đọc "nhấn đúp và giữ để thêm/bỏ yêu thích" khi chọn vào ảnh.
        ViewCompat.setAccessibilityDelegate(imgCar, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.addAction(
                    AccessibilityActionCompat(
                        AccessibilityActionCompat.ACTION_LONG_CLICK.id,
                        getString(R.string.cd_long_press_favourite),
                    )
                )
            }
        })
        btnClearSearch.setOnClickListener { searchView.setQuery("", false) }
    }

    /** SearchView lọc ngay trong lúc gõ. */
    private fun setUpSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            // Được gọi mỗi khi gõ hoặc xoá một ký tự.
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setQuery(newText.orEmpty())
                render()
                return true
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                binding.searchView.clearFocus() // ẩn bàn phím
                return true
            }
        })
    }

    /** PopupMenu ngay dưới nút Sort, cách sắp xếp đang dùng có dấu tick. */
    private fun showSortMenu() {
        val popup = PopupMenu(this, binding.btnSort)
        popup.menuInflater.inflate(R.menu.menu_sort, popup.menu)
        val checkedId = when (viewModel.sortOrder) {
            SortOrder.RATING -> R.id.sort_rating
            SortOrder.YEAR -> R.id.sort_year
            SortOrder.COST -> R.id.sort_cost
            SortOrder.NONE -> null
        }
        checkedId?.let { popup.menu.findItem(it).isChecked = true }

        popup.setOnMenuItemClickListener { item ->
            val order = when (item.itemId) {
                R.id.sort_rating -> SortOrder.RATING
                R.id.sort_year -> SortOrder.YEAR
                R.id.sort_cost -> SortOrder.COST
                else -> return@setOnMenuItemClickListener false
            }
            viewModel.setSort(order)
            render()
            true
        }
        popup.show()
    }

    // ---------------- Hành động ----------------

    private fun toggleFavourite() {
        val car = viewModel.currentCar() ?: return
        viewModel.toggleFavourite(car)
        render()
    }

    /** Chạm vào chip yêu thích: hiện xe đó trên thẻ (xoá ô tìm kiếm nếu nó đang che mất xe). */
    private fun openFavourite(car: Car) {
        if (viewModel.isRented(car)) {
            FuSnackbar.show(
                binding.root,
                getString(R.string.snack_fav_rented_title, car.displayName),
                getString(R.string.snack_fav_rented_msg),
            )
            return
        }
        if (!viewModel.show(car)) {
            binding.searchView.setQuery("", false) // kích hoạt onQueryTextChange → bỏ lọc
            viewModel.show(car)
        }
        render()
        binding.main.smoothScrollTo(0, binding.cardCar.top - resources.getDimensionPixelSize(R.dimen.fu_section_gap) * 2)
    }

    /** Người dùng bấm Save ở màn Rent: trừ credit, ẩn xe khỏi danh sách, báo đặt thành công. */
    private fun onBooked(rental: Rental) {
        viewModel.book(rental)
        render()
        val unit = resources.getQuantityString(R.plurals.days_unit, rental.days)
        FuSnackbar.show(
            binding.root,
            getString(R.string.snack_booked_title, rental.car.displayName),
            getString(R.string.snack_booked_msg, rental.days, unit, rental.total),
        )
    }

    /** Huỷ đơn: hoàn credit và đưa xe trở lại danh sách. */
    private fun onCancel(rental: Rental) {
        viewModel.cancel(rental)
        render()
        FuSnackbar.show(
            binding.root,
            getString(R.string.snack_cancelled_title, rental.total),
            getString(R.string.snack_cancelled_msg, rental.car.displayName),
        )
    }

    // ---------------- Vẽ giao diện ----------------

    /** Vẽ lại mọi thứ phụ thuộc vào dữ liệu. */
    private fun render() {
        binding.tvCredits.text = getString(R.string.credits_pill, viewModel.balance)
        renderFavourites()
        renderCars()
        renderRentals()
    }

    private fun renderFavourites() {
        val favourites = viewModel.favouriteCars()
        binding.tvFavCount.text = getString(R.string.favourites_count, favourites.size)
        binding.tvFavEmpty.isVisible = favourites.isEmpty()
        binding.rvFavourites.isVisible = favourites.isNotEmpty()
        favouriteAdapter.submit(favourites)
    }

    private fun renderCars() = with(binding) {
        val cars = viewModel.displayedCars()
        val car = viewModel.currentCar()
        tvPosition.text = getString(R.string.position, if (car == null) 0 else viewModel.currentIndex + 1, cars.size)
        btnNext.isEnabled = cars.size > 1 // còn 0 hoặc 1 xe thì không có gì để chuyển

        // Hiện thẻ xe HOẶC thẻ "không có xe", không bao giờ cả hai.
        cardCar.isVisible = car != null
        cardNoCars.isVisible = car == null
        if (car != null) {
            showCar(car)
        } else if (viewModel.query.isNotEmpty()) {
            tvNoCarsTitle.text = getString(R.string.no_match_title, viewModel.query)
            tvNoCarsSub.text = getString(R.string.no_match_sub)
            btnClearSearch.isVisible = true
        } else {
            tvNoCarsTitle.text = getString(R.string.all_rented_title)
            tvNoCarsSub.text = getString(R.string.all_rented_sub)
            btnClearSearch.isVisible = false
        }
    }

    /** Đổ thông tin một xe vào thẻ. */
    private fun showCar(car: Car) = with(binding) {
        imgCar.setImageResource(car.imageRes)
        imgCar.contentDescription = getString(R.string.cd_car_photo, car.displayName)
        tvType.text = car.type
        tvBrand.text = car.brand
        tvModel.text = car.model
        tvCost.text = car.costPerDay.toString()
        ratingBar.rating = car.rating
        tvRating.text = String.format(Locale.US, "%.1f", car.rating)
        statYear.tvStatValue.text = car.year.toString()
        statKm.tvStatValue.text = getString(R.string.km_value, String.format(Locale.US, "%,d", car.km))
        statCost.tvStatValue.text = getString(R.string.cr_value, car.costPerDay)

        val fav = viewModel.isFavourite(car)
        btnFavourite.setImageResource(if (fav) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline)
        btnFavourite.contentDescription = getString(
            if (fav) R.string.cd_remove_favourite else R.string.cd_add_favourite, car.displayName
        )
    }

    /** Mỗi đơn thuê là một thẻ item_rental. Dựng lại từ đầu mỗi lần (tối đa chỉ 5 đơn). */
    private fun renderRentals() {
        val rentals = viewModel.rentals
        binding.tvRentalsEmpty.isVisible = rentals.isEmpty()
        binding.layoutRentals.isVisible = rentals.isNotEmpty()
        binding.layoutRentals.removeAllViews()
        val inflater = LayoutInflater.from(this)
        for (rental in rentals) {
            val row = ItemRentalBinding.inflate(inflater, binding.layoutRentals, true)
            val unit = resources.getQuantityString(R.plurals.days_unit, rental.days)
            row.imgRental.setImageResource(rental.car.imageRes)
            row.tvRentalName.text = rental.car.displayName
            row.tvRentalDetail.text = getString(R.string.rental_detail, rental.days, unit, rental.total)
            row.btnCancelBooking.contentDescription = getString(R.string.cd_cancel_booking, rental.car.displayName)
            // Lambda "nhớ" đúng biến rental của vòng lặp này (closure), nên mỗi nút huỷ đúng đơn của nó.
            row.btnCancelBooking.setOnClickListener { onCancel(rental) }
        }
    }
}
