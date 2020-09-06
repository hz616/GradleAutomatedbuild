package com.example.gradledemo

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var mBtn: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mBtn = findViewById(R.id.btn_click)
        mBtn.setOnClickListener {

        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}