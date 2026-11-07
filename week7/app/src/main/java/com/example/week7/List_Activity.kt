package com.example.week7

import android.os.Bundle
import android.widget.ListView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.ArrayAdapter
import android.widget.Toast

class List_Activity : AppCompatActivity() {
    lateinit var IsStudent: ListView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.list_layout)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.IsStudents)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        var strList: Array<String> = arrayOf("Nguyen Van Tien", "Bui Cong Minh", "Nguyen Pha Le", "Phan Tuan Sy", "Dang Tieu Binh")
        IsStudent=findViewById(R.id.IsStudents)
        var arr = ArrayAdapter(this, R.layout.template_layout,strList)
        IsStudent.setAdapter(arr)
        IsStudent.setOnItemClickListener { parent, view, position, id ->
            val element = arr.getItem(position)
            Toast.makeText(this, element, Toast.LENGTH_SHORT).show()
        }

        strList.set(1, "Testing")
        arr = ArrayAdapter(this, R.layout.template_layout, strList)
        IsStudent.setAdapter(arr)
    }
}
