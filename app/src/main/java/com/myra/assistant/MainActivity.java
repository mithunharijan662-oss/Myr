package com.myra.assistant;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.text.InputType;
import android.speech.SpeechRecognizer;
import android.speech.RecognizerIntent;
import android.speech.RecognitionListener;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.Manifest;

import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.*;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private LinearLayout messages;
    private EditText input;
    private ScrollView scroll;
    private ExecutorService executor;
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private boolean voiceEnabled = true;

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

        Button mic = new Button(this);
        mic.setText("🎙️");
        bottom.addView(mic);

        Button send = new Button(this);
        send.setText("SEND");
        send.setTextColor(Color.BLACK);
        send.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(Color.CYAN));
        bottom.addView(send);

        Button voice = new Button(this);
        voice.setText("🔊");
        bottom.addView(voice);

        root.addView(title);
        root.addView(subtitle);
        root.addView(status);
        root.addView(scroll,
            new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(bottom);

        setContentView(root);

        addMessage("MYRA",
            "Namaste! Main MYRA hoon. Aap mujhse bolkar ya likhkar baat kar sakte hain.",
            false);

        setupTTS();
        setupSpeechRecognizer();

        send.setOnClickListener(v -> sendMessage());

        input.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });

        mic.setOnClickListener(v -> startListening());

        voice.setOnClickListener(v -> {
            voiceEnabled = !voiceEnabled;
            voice.setText(voiceEnabled ? "🔊" : "🔇");
            if (!voiceEnabled && tts != null) {
                tts.stop();
            }
            Toast.makeText(this,
                voiceEnabled ? "Voice ON" : "Voice OFF",
                Toast.LENGTH_SHORT).show();
        });
    }

    private void setupTTS() {
        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) {
                int languageResult =
                    tts.setLanguage(new Locale("hi", "IN"));

                if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
                    languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.US);
                }

                if (Build.VERSION.SDK_INT >= 21 && tts.getVoices() != null) {
                    for (Voice v : tts.getVoices()) {
                        String name = v.getName().toLowerCase(Locale.ROOT);
                        if (name.contains("female") &&
                            !v.isNetworkConnectionRequired()) {
                            tts.setVoice(v);
                            break;
                        }
                    }
                }

                tts.setSpeechRate(0.95f);
                ttsReady = true;
            }
        });
    }

    private void speak(String text) {
        if (ttsReady && voiceEnabled && tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "MYRA_REPLY");
        }
    }

    private void setupSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) {
                    runOnUiThread(() -> addMessage("MYRA", "Sun rahi hoon...", false));
                }

                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() {}

                @Override
                public void onError(int error) {
                    runOnUiThread(() -> Toast.makeText(
                        MainActivity.this,
                        "Voice recognition error: " + error,
                        Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches =
                        results.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION);

                    if (matches != null && !matches.isEmpty()) {
                        runOnUiThread(() -> {
                            input.setText(matches.get(0));
                            sendMessage();
                        });
                    }
                }

                @Override public void onPartialResults(Bundle results) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });
        }
    }

    private void startListening() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[]{Manifest.permission.RECORD_AUDIO}, 101);
            return;
        }

        if (speechRecognizer == null) {
            Toast.makeText(this,
                "Speech recognition phone mein available nahi hai.",
                Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);

        try {
            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            Toast.makeText(this,
                "Microphone start nahi hua: " + e.getMessage(),
                Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
        String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 101 && grantResults.length > 0 &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListening();
        } else if (requestCode == 101) {
            Toast.makeText(this,
                "Voice ke liye microphone permission zaroori hai.",
                Toast.LENGTH_LONG).show();
        }
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
                if (count > 0) messages.removeViewAt(count - 1);
                addMessage("MYRA", finalReply, false);
                speak(finalReply);
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (executor != null) executor.shutdown();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
