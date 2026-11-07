package com.example.week8

import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnRead = findViewById<Button>(R.id.btnRead)
        val btnReadText = findViewById<Button>(R.id.btnReadText)
        val btnWriteExternal = findViewById<Button>(R.id.btnWriteExternal)
        val txtInfor = findViewById<TextView>(R.id.txtInfor)

        btnSave.setOnClickListener {
            val file = openFileOutput("test.txt", MODE_APPEND)
            val message = "Hello World\n"
            file.write(message.toByteArray())
            file.close()
            Toast.makeText(this, "Đã ghi file", Toast.LENGTH_SHORT).show()
        }

        btnRead.setOnClickListener {
            try {
                val file = openFileInput("test.txt")
                val data = ByteArray(1024)
                val bytesRead = file.read(data)
                if (bytesRead > 0) {
                    txtInfor.text = String(data, 0, bytesRead, Charsets.UTF_8)
                } else {
                    txtInfor.text = ""
                }
                file.close()
            } catch (_: Exception) {
                txtInfor.text = "Không tìm thấy file hoặc lỗi đọc file"
            }
        }

        btnReadText.setOnClickListener {
            try {
                val file = File(applicationContext.filesDir, "test.txt")
                if (file.exists()) {
                    val contents = file.readText() // Read file
                    txtInfor.text = contents
                } else {
                    txtInfor.text = "Không tìm thấy file"
                }
            } catch (_: Exception) {
                txtInfor.text = "Không tìm thấy file hoặc lỗi đọc file"
            }
        }

        btnWriteExternal.setOnClickListener {
            try {
                val file = File(Environment.getExternalStorageDirectory(), "/Documents/test.txt")
                file.createNewFile()
                file.writeText("Hello External Storage")
                Toast.makeText(this, "Đã ghi External Storage", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Lỗi ghi file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        val file = File(Environment.getExternalStorageDirectory(), "/Documents/test.txt")
        txtInfor.text = file.readText()

        val file_b = File(Environment.getExternalStorageDirectory(), "/Documents/test2.txt").exists()
        if (file_b) {
            val file = File(Environment.getExternalStorageDirectory(), "/Documents/test2.txt")
            txtInfor.text = file.readText()
        }
        else
            Toast.makeText(this, "Không tìm thấy file", Toast.LENGTH_SHORT).show()
    }
}