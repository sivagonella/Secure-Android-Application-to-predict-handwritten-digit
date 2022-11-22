package com.example.myapplication;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import okio.Timeout;

public class SuccessScreenActivity extends AppCompatActivity {
    int number;
    TextView textView;

    ActivityResultLauncher<Intent> launchAnotherActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {

                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success_screen);
        Bundle bundle = getIntent().getExtras();
        number = (int) bundle.get("number");
        String displayMessage = "Uploaded number is: " + number;
        textView = findViewById(R.id.successText);
        textView.setText(displayMessage);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent changeActivityIntent = new Intent(SuccessScreenActivity.this, MainActivity.class);
                launchAnotherActivity.launch(changeActivityIntent);
            }
        }, 2000);
    }
}