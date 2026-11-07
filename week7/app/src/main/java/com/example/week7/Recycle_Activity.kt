package com.example.week7

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageView

class Recycle_Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.recycle_layout)

        val recyclerView =
            findViewById<RecyclerView>(R.id.recyclerStudents)

        ViewCompat.setOnApplyWindowInsetsListener(recyclerView) { v, insets ->
            val systemBars =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        val students = listOf(
            DataClass(R.drawable.fakelogo, "Saddam Hussein"),
            DataClass(R.drawable.broslogo, "Bui Cong Minh"),
            DataClass(R.drawable.xlogo, "Nguyen Pha Le"),
            DataClass(R.drawable.vins, "Phan Tuan Sy"),
            DataClass(R.drawable.fakelogo, "Dang Tieu Binh")
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = RecyclerViewAdapter(students)
    }
}

data class DataClass(var imgProfire: Int, var StudentNames: String)

class RecyclerViewAdapter(
    private val mList: List<DataClass>
) : RecyclerView.Adapter<RecyclerViewAdapter.ViewHolderClass>() {

    class ViewHolderClass(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val textView: TextView = itemView.findViewById(R.id.txtName)
        val imageView: ImageView = itemView.findViewById(R.id.imgPro)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolderClass {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.template_re_layout, parent, false)

        return ViewHolderClass(itemView)
    }

    override fun onBindViewHolder(
        holder: ViewHolderClass,
        position: Int
    ) {
        val student = mList[position]
        holder.textView.text = student.StudentNames
        holder.imageView.setImageResource(student.imgProfire)

        holder.itemView.setOnClickListener { view ->
            Toast.makeText(
                view.context,
                student.StudentNames,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun getItemCount(): Int = mList.size
}