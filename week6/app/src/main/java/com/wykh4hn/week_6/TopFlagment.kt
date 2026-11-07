package com.wykh4hn.week_6

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment

class TopFlagment : Fragment(R.layout.top_flagment_layout) {

    private lateinit var firstName: EditText
    private lateinit var lastName: EditText
    private lateinit var button: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firstName = view.findViewById(R.id.firstName)
        lastName = view.findViewById(R.id.lastName)
        button = view.findViewById(R.id.button)

        button.setOnClickListener {
            val fName = firstName.text.toString()
            val lName = lastName.text.toString()
            (activity as? MainActivity)?.showText(fName, lName)
        }
    }
}
