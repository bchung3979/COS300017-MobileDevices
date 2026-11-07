package com.example.fakeuber

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.viewModels
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import com.example.fakeuber.databinding.ActivityRentBinding
import com.example.fakeuber.databinding.ItemDocTileBinding
import com.example.fakeuber.databinding.ItemSpecBinding
import com.example.fakeuber.model.Car
import com.example.fakeuber.model.CarRepository
import com.example.fakeuber.ui.DarkModeToggle
import com.example.fakeuber.ui.FuSnackbar
import java.util.Locale

/**
 * Màn thuê xe. Nhận [Car] (Parcelable) từ một Intent tường minh, cho chọn 1-7 ngày,
 * thêm 2 ảnh giấy tờ và kiểm tra credit. Sau đó:
 *  - Save → trả về [com.example.fakeuber.model.Rental] (Parcelable) kèm RESULT_OK
 *  - Back (nút Back, mũi tên, nút back hệ thống) → RESULT_CANCELED, không lưu gì.
 */
class RentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRentBinding
    private val viewModel: RentViewModel by viewModels() // giữ số ngày + ảnh khi bật/tắt dark mode

    // Photo Picker: trình chọn ảnh của hệ thống, KHÔNG cần xin quyền truy cập bộ nhớ,
    // người dùng chỉ cấp quyền cho đúng tấm ảnh họ chọn.
    // Launcher phải được đăng ký TRƯỚC khi Activity khởi động, nên khai báo ở đây chứ không tạo trong onClick.
    // uri = null nghĩa là người dùng thoát ra mà không chọn ảnh.
    private val pickIdPhoto = registerForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.setIdPhoto(uri)
            render()
        }
    }
    private val pickLicencePhoto = registerForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.setLicencePhoto(uri)
            render()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarPadding()

        // Lấy xe (Parcelable) ra khỏi Intent. IntentCompat lo phần khác biệt giữa các phiên bản Android.
        val car = IntentCompat.getParcelableExtra(intent, EXTRA_CAR, Car::class.java)
        if (car == null) { // không có xe (không nên xảy ra): đóng màn hình thay vì để app crash
            finish()
            return
        }
        viewModel.start(car, intent.getIntExtra(EXTRA_BALANCE, 0))

        DarkModeToggle.bind(binding.darkToggle)
        showCar(car)
        setUpCreditLabels()
        buildDayLabels()
        setUpListeners()
        render()
    }

    private fun applySystemBarPadding() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rentRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    private fun setUpListeners() {
        // Chỉ xử lý khi giá trị khác với ViewModel: render() cũng gán giá trị cho Slider,
        // nếu không kiểm tra sẽ bị lặp vô hạn. (Không dùng "fromUser" để test Espresso kéo được Slider.)
        binding.sliderDays.addOnChangeListener { _, value, _ ->
            if (value.toInt() != viewModel.days) {
                viewModel.setDays(value.toInt())
                render()
            }
        }
        val imagesOnly = PickVisualMediaRequest(PickVisualMedia.ImageOnly) // chỉ ảnh, không video
        binding.tileId.root.setOnClickListener { pickIdPhoto.launch(imagesOnly) }
        binding.tileLicence.root.setOnClickListener { pickLicencePhoto.launch(imagesOnly) }

        binding.btnSave.setOnClickListener { save() }
        binding.btnBack.setOnClickListener { cancelRental() }
        binding.btnBackTop.setOnClickListener { cancelRental() }
        // Nút back / vuốt back của hệ thống cũng là huỷ.
        onBackPressedDispatcher.addCallback(this) { cancelRental() }
    }

    /** Hợp lệ → gửi Rental về MainActivity. Không hợp lệ → ở lại màn hình này và báo lỗi. */
    private fun save() {
        val rental = viewModel.trySave()
        if (rental == null) {
            render() // hiện ô ảnh viền đỏ "Required" nếu còn thiếu ảnh
            FuSnackbar.show(
                binding.rentRoot,
                getString(R.string.snack_cant_save_title),
                getString(R.string.snack_cant_save_msg),
                anchor = binding.bottomBar,
            )
            return
        }
        // RESULT_OK + Rental (Parcelable) được gửi về rentLauncher của MainActivity.
        setResult(RESULT_OK, Intent().putExtra(EXTRA_RENTAL, rental))
        finish()
    }

    /** Back = huỷ: không lưu gì. Gửi kèm xe để màn Main hiện đúng tên xe trong thông báo. */
    private fun cancelRental() {
        setResult(RESULT_CANCELED, Intent().putExtra(EXTRA_CAR, viewModel.car))
        finish()
    }

    // ---------------- Vẽ giao diện ----------------

    /** Phần không đổi trong suốt lúc màn hình đang mở. */
    private fun showCar(car: Car) = with(binding) {
        imgCar.setImageResource(car.imageRes)
        imgCar.contentDescription = getString(R.string.cd_car_photo, car.displayName)
        tvMeta.text = getString(R.string.rent_meta, car.brand, car.year, car.type)
        tvModel.text = car.model
        tvCost.text = car.costPerDay.toString()
        ratingBar.rating = car.rating
        tvRating.text = String.format(Locale.US, "%.1f", car.rating)
        tvKm.text = getString(R.string.dot_km, String.format(Locale.US, "%,d", car.km))
        showSpec(specSeats, R.drawable.ic_seats, R.string.spec_seats, car.seats.toString())
        showSpec(specFuel, R.drawable.ic_fuel, R.string.spec_fuel, car.fuel)
        showSpec(specType, R.drawable.ic_car, R.string.spec_type, car.type)
    }

    private fun showSpec(spec: ItemSpecBinding, @DrawableRes icon: Int, @StringRes label: Int, value: String) {
        spec.imgSpec.setImageResource(icon)
        spec.tvSpecLabel.text = getString(label)
        spec.tvSpecValue.text = value
    }

    private fun setUpCreditLabels() = with(binding) {
        rowBalance.tvRowLabel.text = getString(R.string.your_balance)
        rowAfter.tvRowLabel.text = getString(R.string.balance_after)
        // "Balance after rental" là dòng quan trọng nhất: chữ đậm, số to hơn.
        rowAfter.tvRowLabel.setTextColor(color(R.color.fu_text))
        rowAfter.tvRowLabel.setTypeface(null, Typeface.BOLD)
        rowAfter.tvRowValue.textSize = 20f
    }

    /** Thêm các số 1..7 dưới Slider. */
    private fun buildDayLabels() {
        for (day in RentViewModel.MIN_DAYS..RentViewModel.MAX_DAYS) {
            val label = TextView(this).apply {
                text = day.toString()
                gravity = Gravity.CENTER
                textSize = 13f
            }
            binding.layoutDayLabels.addView(label, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
    }

    /** Vẽ lại mọi thứ phụ thuộc vào số ngày, ảnh và kết quả kiểm tra. */
    private fun render() = with(binding) {
        val days = viewModel.days
        val total = viewModel.total
        val unit = resources.getQuantityString(R.plurals.days_unit, days)
        val problem = viewModel.creditProblem()

        tvCredits.text = getString(R.string.credits_pill, viewModel.balance)

        // Thẻ chọn ngày
        if (sliderDays.value.toInt() != days) sliderDays.value = days.toFloat()
        tvDays.text = days.toString()
        tvDaysUnit.text = unit
        layoutDayLabels.children.forEachIndexed { index, view ->
            val selected = index + 1 == days
            (view as TextView).setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
            view.setTextColor(color(if (selected) R.color.fu_text else R.color.fu_text_secondary))
        }

        // Ô ảnh giấy tờ
        showTile(tileId, R.string.doc_id, R.string.cd_doc_id, viewModel.idPhoto)
        showTile(tileLicence, R.string.doc_licence, R.string.cd_doc_licence, viewModel.licencePhoto)

        // Thẻ Credits
        rowCost.tvRowLabel.text = getString(R.string.cost_times_days, viewModel.car.costPerDay, days, unit)
        rowCost.tvRowValue.text = getString(R.string.cr_value, total)
        rowBalance.tvRowValue.text = getString(R.string.cr_value, viewModel.balance)
        rowAfter.tvRowValue.text = getString(R.string.cr_value, viewModel.balanceAfter)
        rowAfter.tvRowValue.setTextColor(color(if (viewModel.balanceAfter < 0) R.color.fu_error else R.color.fu_text))

        tvLimit.text = getString(R.string.limit_value, total, CarRepository.RENTAL_LIMIT)
        // Thanh thể hiện "đơn này dùng bao nhiêu trong hạn mức 400". Thanh không vượt quá 100%,
        // nên khi vượt hạn mức thì chuyển sang màu đỏ.
        progressLimit.setProgressCompat(total.coerceAtMost(CarRepository.RENTAL_LIMIT), true)
        progressLimit.setIndicatorColor(
            color(if (problem == CreditProblem.OVER_LIMIT) R.color.fu_error else R.color.fu_accent)
        )

        tvError.isVisible = problem != null
        tvError.text = when (problem) {
            CreditProblem.OVER_LIMIT -> getString(R.string.error_over_limit, total, CarRepository.RENTAL_LIMIT)
            CreditProblem.NOT_ENOUGH_CREDITS -> getString(R.string.error_not_enough, viewModel.balance, total)
            null -> ""
        }

        btnSave.text = getString(R.string.save_total, total)
    }

    /** Một ô ảnh giấy tờ có 3 trạng thái: trống, "Required" (đỏ, sau khi Save thất bại), đã có ảnh (✓). */
    private fun showTile(tile: ItemDocTileBinding, @StringRes title: Int, @StringRes uploadLabel: Int, photo: Uri?) {
        val name = getString(title)
        val missing = photo == null && viewModel.showMissingPhotos
        tile.tvDocTitle.text = name

        if (photo != null) {
            tile.imgDoc.showThumbnail(photo)
            tile.imgDocCheck.isVisible = true
            tile.tvDocHint.text = getString(R.string.doc_tap_change)
            tile.root.contentDescription = getString(R.string.cd_doc_added, name)
        } else {
            tile.imgDoc.tag = null
            tile.imgDoc.scaleType = ImageView.ScaleType.CENTER_INSIDE
            tile.imgDoc.setImageResource(R.drawable.ic_add_photo)
            tile.imgDocCheck.isVisible = false
            tile.tvDocHint.text = getString(if (missing) R.string.doc_required else R.string.doc_tap_upload)
            tile.root.contentDescription = getString(uploadLabel)
        }
        tile.tvDocHint.setTextColor(color(if (missing) R.color.fu_error else R.color.fu_text_secondary))
        tile.root.setBackgroundResource(if (missing) R.drawable.bg_doc_tile_error else R.drawable.bg_doc_tile)
    }

    /**
     * Hiện ảnh xem trước. Ảnh chụp điện thoại có thể rộng 4000px, nên chỉ giải mã bản thu nhỏ
     * (~200px) để tiết kiệm RAM. "tag" ghi nhớ Uri đang hiện để kéo Slider không phải tải lại ảnh.
     */
    private fun ImageView.showThumbnail(uri: Uri) {
        if (tag == uri) return
        tag = uri
        scaleType = ImageView.ScaleType.CENTER_CROP
        val bitmap = runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val options = BitmapFactory.Options().apply {
                inSampleSize = (minOf(bounds.outWidth, bounds.outHeight) / 200).coerceAtLeast(1)
            }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        }.getOrNull()
        if (bitmap != null) setImageBitmap(bitmap) else setImageURI(uri)
    }

    private fun color(@ColorRes res: Int): Int = ContextCompat.getColor(this, res)

    companion object {
        const val EXTRA_CAR = "com.example.fakeuber.EXTRA_CAR"
        const val EXTRA_BALANCE = "com.example.fakeuber.EXTRA_BALANCE"
        const val EXTRA_RENTAL = "com.example.fakeuber.EXTRA_RENTAL"

        /**
         * Tạo Intent TƯỜNG MINH mở màn hình này (tường minh = ghi rõ class đích, Android
         * không phải đi tìm app nào xử lý). Extras: xe (Parcelable) + số dư hiện tại.
         */
        fun newIntent(context: Context, car: Car, balance: Int): Intent =
            Intent(context, RentActivity::class.java)
                .putExtra(EXTRA_CAR, car)
                .putExtra(EXTRA_BALANCE, balance)
    }
}
