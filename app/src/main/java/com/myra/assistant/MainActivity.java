package com.myra.assistant;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;
import android.text.InputType;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private LinearLayout messages;
    private EditText input;
    private ScrollView scroll;
    private ExecutorService executor;
    private final String API_URL =
        "https://spring-queen-35fb.mithunharijan662.workers.dev";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        executor = Executors.newSingleThreadExecutor();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(5, 10, 25));
        root.setPadding(18, 15, 18, 12);

        TextView title = new TextView(this);
        title.setText("MYRA");
        title.setTextSize(34);
        title.setTextColor(Color.CYAN);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, Typeface.BOLD);

        TextView subtitle = new TextView(this);
        subtitle.setText("YOUR PERSONAL AI ASSISTANT");
        subtitle.setTextSize(11);
        subtitle.setTextColor(Color.WHITE);
        subtitle.setGravity(Gravity.CENTER);

        TextView status = new TextView(this);
        status.setText("● AI SYSTEM ONLINE");
        status.setTextColor(Color.GREEN);
        status.setTextSize(13);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 12, 0, 12);

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        messages.setPadding(4, 12, 4, 12);

        scroll.addView(messages);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER_VERTICAL);

        input = new EditText(this);
        input.setHint("Ask MYRA anything...");
        input.setHintTextColor(Color.GRAY);
        input.setTextColor(Color.WHITE);
        input.setTextSize(15);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT |
                           InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setPadding(15, 10, 15, 10);
        input.setBackgroundColor(Color.rgb(25, 35, 55));

        LinearLayout.LayoutParams inputParams =
            new LinearLayout.LayoutParams(0, -2, 1);
        inputParams.setMargins(0, 8, 8, 8);
        bottom.addView(input, inputParams);

        Button send = new Button(this);
        send.setText("SEND");
        send.setTextColor(Color.BLACK);
        send.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(Color.CYAN));
        bottom.addView(send);

        root.addView(title);
        root.addView(subtitle);
        root.addView(status);
        root.addView(scroll,
            new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(bottom);

        setContentView(root);

        addMessage("MYRA",
            "Namaste! Main MYRA hoon. Aap mujhse kuch bhi pooch sakte hain.",
            false);

        send.setOnClickListener(v -> sendMessage());
        input.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }

    private void addMessage(String sender, String text, boolean user) {
        TextView bubble = new TextView(this);
        bubble.setText(sender + "\n" + text);
        bubble.setTextSize(15);
        bubble.setTextColor(Color.WHITE);
        bubble.setPadding(15, 12, 15, 12);
        bubble.setBackgroundColor(user
            ? Color.rgb(15, 75, 105)
            : Color.rgb(25, 40, 65));

        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 6, 0, 6);
        messages.addView(bubble, params);

        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private void sendMessage() {
        String message = input.getText().toString().trim();

        if (message.isEmpty()) return;

        input.setText("");
        ((InputMethodManager) getSystemService(
            Context.INPUT_METHOD_SERVICE))
            .hideSoftInputFromWindow(input.getWindowToken(), 0);

        addMessage("YOU", message, true);
        addMessage("MYRA", "Thinking...", false);

        executor.execute(() -> {
            String reply;

            try {
                URL url = new URL(API_URL);
                HttpURLConnection connection =
                    (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setRequestProperty(
                    "Content-Type", "application/json");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);
                connection.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("message", message);

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(body.toString().getBytes("UTF-8"));
                }

                int code = connection.getResponseCode();
                InputStream stream = (code >= 200 && code < 300)
                    ? connection.getInputStream()
                    : connection.getErrorStream();

                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, "UTF-8"));

                StringBuilder result = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                reader.close();

                JSONObject response =
                    new JSONObject(result.toString());

                reply = response.optString("reply",
                    response.optString("error", "No response received."));

                connection.disconnect();

            } catch (Exception e) {
                reply = "Connection error: " + e.getMessage();
            }

            final String finalReply = reply;

            runOnUiThread(() -> {
                int count = messages.getChildCount();
                if (count > 0) {
                    messages.removeViewAt(count - 1);
                }
                addMessage("MYRA", finalReply, false);
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) executor.shutdown();
    }
}
