package com.devicebridge;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnPlay = findViewById(R.id.btn_play);
        Button btnOpenControl = findViewById(R.id.btn_open_control);

        btnPlay.setOnClickListener(v ->
                startActivity(new Intent(this, GameActivity.class)));

        btnOpenControl.setOnClickListener(v ->
                startActivity(new Intent(this, ControlPanelActivity.class)));
    }
}
