package com.example.fakeuber.ui

import android.view.View
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import com.google.android.material.snackbar.Snackbar

/**
 * Snackbar 2 dòng của app: tiêu đề in đậm + một dòng chi tiết.
 *
 * Dùng Snackbar thay vì Toast vì Snackbar theo màu theme và có thể nằm ngay trên thanh
 * Back/Save nhờ [anchor] (Toast dạng chữ không đặt được vị trí từ Android 11).
 */
object FuSnackbar {

    fun show(parent: View, title: String, message: String, anchor: View? = null) {
        val text = buildSpannedString {
            bold { append(title) }
            append("\n")
            append(message)
        }
        val snackbar = Snackbar.make(parent, text, Snackbar.LENGTH_LONG).setTextMaxLines(4)
        if (anchor != null) snackbar.anchorView = anchor
        snackbar.show()
    }
}
