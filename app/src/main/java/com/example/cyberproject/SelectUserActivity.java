package com.example.cyberproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class SelectUserActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_user);

        Button btn1 = findViewById(R.id.btnUser1);
        Button btn2 = findViewById(R.id.btnUser2);
        Button btn3 = findViewById(R.id.btnUser3);

        btn1.setOnClickListener(v -> openChat("compte 1"));
        btn2.setOnClickListener(v -> openChat("compte 2"));
        btn3.setOnClickListener(v -> openChat("comptePIRATE"));
    }

    private void openChat(String userId) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("userId", userId);
        startActivity(intent);
        finish();
    }
}
