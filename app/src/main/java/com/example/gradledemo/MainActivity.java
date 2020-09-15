package com.example.gradledemo;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {
    private final String TAG = "";

    private Button mBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBtn = findViewById(R.id.btn_click);
        mBtn.setOnClickListener(view -> {
            System.out.println("");
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
