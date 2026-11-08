package com.example.fakeuber.ui

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.example.fakeuber.R
import com.example.fakeuber.databinding.ViewDarkToggleBinding

/**
 * Gắn xử lý cho công tắc sáng/tối ở header của cả 2 màn hình (dùng chung để hành vi giống nhau).
 *
 * AppCompatDelegate.setDefaultNightMode() đổi chế độ cho cả app và tạo lại các Activity đang mở;
 * sau đó Android tự lấy màu trong values-night/colors.xml.
 */
object DarkModeToggle {

    fun bind(toggle: ViewDarkToggleBinding) {
        val res = toggle.root.resources
        val isNight = (res.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

        // Gán trạng thái hiện tại TRƯỚC rồi mới gắn listener,
        // nếu không thì việc gán isChecked sẽ kích hoạt listener và bị lặp.
        toggle.switchDark.isChecked = isNight
        toggle.imgMode.setImageResource(if (isNight) R.drawable.ic_moon else R.drawable.ic_sun)

        toggle.switchDark.setOnCheckedChangeListener { _, checked ->
            // Chỉ đổi chế độ khi công tắc thật sự khác chế độ đang hiển thị.
            // Chặn trường hợp Android khôi phục trạng thái cũ của Switch rồi bắn listener
            // (lỗi lặp sáng/tối khi đổi chế độ ở màn Rent, xem DEVLOG #24).
            if (checked == isNight) return@setOnCheckedChangeListener
            AppCompatDelegate.setDefaultNightMode(
                if (checked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }
}
