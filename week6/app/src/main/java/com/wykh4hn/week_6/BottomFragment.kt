package com.wykh4hn.week_6

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment

class BottomFragment : Fragment(R.layout.bottom_flagment_layout) {

    private lateinit var textView: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        textView = view.findViewById(R.id.textView)
    }

    fun showText(firstName: String, lastName: String) {
        textView.text = getString(R.string.full_name_format, firstName, lastName)
    }
}
