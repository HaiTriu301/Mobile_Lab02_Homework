package com.example.lab02_homework;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private ConstraintLayout mainLayout;
    private EditText inputText;
    private Button btnPredict;
    private TextView emojiText;
    private TextView resultText;
    private ProgressBar progressBar;

    private final OkHttpClient client = new OkHttpClient();
    private static final String API_URL = "http://192.168.2.16:5000/predict";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        mainLayout = findViewById(R.id.mainLayout);
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        inputText = findViewById(R.id.inputText);
        btnPredict = findViewById(R.id.btnPredict);
        emojiText = findViewById(R.id.emojiText);
        resultText = findViewById(R.id.resultText);
        progressBar = findViewById(R.id.progressBar);

        btnPredict.setOnClickListener(v -> predictSentiment());
    }

    private void predictSentiment() {
        String text = inputText.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Nhập văn bản", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        resultText.setText("Loading...");

        new Thread(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("text", text);

                RequestBody body = RequestBody.create(
                        json.toString(),
                        MediaType.parse("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url(API_URL)
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        runOnUiThread(() -> handleResponse(responseBody));
                    } else {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            resultText.setText("");
                            Toast.makeText(MainActivity.this,
                                    "Error from server: " + response.code(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            } catch (IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    resultText.setText("");
                    Toast.makeText(MainActivity.this,
                            "Connection error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void handleResponse(String responseBody) {
        progressBar.setVisibility(View.GONE);
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

            String sentiment = json.get("sentiment").getAsString().toUpperCase();

            String emoji;
            int bgColor;

            if (sentiment.contains("NEG")) {
                emoji = "☹️";
                bgColor = Color.parseColor("#8B0000");
                sentiment = "NEGATIVE";
            } else {
                emoji = "😃";
                bgColor = Color.parseColor("#2E7D32");
                sentiment = "POSITIVE";
            }

            mainLayout.setBackgroundColor(bgColor);
            emojiText.setText(emoji);
            resultText.setText(sentiment);

        } catch (Exception e) {
            Toast.makeText(this, "Error in processing results: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}