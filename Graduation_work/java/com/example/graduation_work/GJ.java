package com.example.graduation_work;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.util.ArrayList;
import java.util.Locale;

public class GJ extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;
    private static final String TAG = "GJ";

    private TextView textTranscription;
    private StringBuilder recognizedText = new StringBuilder();
    private TextView transcriptionText, sentimentText;
    private TextView textSentiment;
    private Button buttonStartRecognition;
    private Button buttonStopRecognition;
    private Button buttonAnalyzeSentiment;
    private Button buttonAnalyzePlaylist;
    private SpeechRecognizer speechRecognizer;

    private static final String API_KEY = "AIzaSyDjQ_qtQqHeA9rip3dpfbLoVuRFuLrEsDo";
    private static final String API_URL = "https://language.googleapis.com/v1/documents:analyzeSentiment?key=" + API_KEY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gj);

        textTranscription = findViewById(R.id.text_transcription);
        textSentiment = findViewById(R.id.text_sentiment);
        buttonStartRecognition = findViewById(R.id.button_start_recognition);
        buttonStopRecognition = findViewById(R.id.button_stop_recognition);
        buttonAnalyzeSentiment = findViewById(R.id.button_analyze_sentiment);
        Button buttonAnalyzePlaylist = findViewById(R.id.button_analyze_palylist);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }

        initializeSpeechRecognizer();

        buttonStartRecognition.setOnClickListener(v -> startVoiceRecognition());

        buttonStopRecognition.setOnClickListener(v -> stopVoiceRecognition());

        buttonAnalyzeSentiment.setOnClickListener(v -> {
            String text = textTranscription.getText().toString();
            Log.d(TAG, "분석 텍스트: " + text);
            if (!text.isEmpty()) {
                analyzeSentiment(text);
            } else {
                textSentiment.setText("분석할 텍스트가 없습니다.");
            }
        });

        buttonAnalyzePlaylist.setOnClickListener(v -> {
            String text = textTranscription.getText().toString();
            if (!text.isEmpty()) {
                analyzeSentimentAndPlay(text);
            } else {
                Toast.makeText(GJ.this, "분석할 텍스트를 입력하세요.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void analyzeSentimentAndPlay(String text) {
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... params) {
                try {
                    OkHttpClient client = new OkHttpClient();

                    JsonObject document = new JsonObject();
                    document.addProperty("type", "PLAIN_TEXT");
                    document.addProperty("content", params[0]);

                    JsonObject requestBodyJson = new JsonObject();
                    requestBodyJson.add("document", document);

                    RequestBody body = RequestBody.create(
                            requestBodyJson.toString(),
                            MediaType.get("application/json; charset=utf-8")
                    );

                    Request request = new Request.Builder()
                            .url(API_URL)
                            .post(body)
                            .build();

                    Response response = client.newCall(request).execute();

                    if (response.isSuccessful()) {
                        String result = response.body() != null ? response.body().string() : null;
                        Log.d(TAG, "감정 분석 결과: " + result);
                        return result;
                    } else {
                        Log.e(TAG, "API 요청 실패: " + response.code());
                        return "API 요청 실패: " + response.code();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "오류 발생: " + e.getMessage(), e);
                    return "오류 발생: " + e.getMessage();
                }
            }

            @Override
            protected void onPostExecute(String result) {
                if (result != null) {
                    handleSentimentAndPlayMusic(result);
                } else {
                    Toast.makeText(GJ.this, "감정 분석 결과를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute(text);
    }

    private void handleSentimentAndPlayMusic(String jsonResponse) {
        try {
            JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();
            JsonObject sentiment = jsonObject.getAsJsonObject("documentSentiment");

            float score = sentiment.get("score").getAsFloat();
            float magnitude = sentiment.get("magnitude").getAsFloat();

            JsonObject textObject = jsonObject.getAsJsonArray("sentences").get(0).getAsJsonObject();
            String text = textObject.getAsJsonObject("text").get("content").getAsString();

            float adjustedMagnitude = adjustMagnitude(text, magnitude);

            String emotion = determineEmotion(score);

            String playlistUri = getPlaylistByEmotion(emotion, adjustedMagnitude);

            textSentiment.setText("감정 점수: " + score +
                    "\n원본 강도: " + magnitude +
                    "\n감정 강도: " + adjustedMagnitude +
                    "\n플레이리스트 URI: " + playlistUri);

            openSpotifyTrack(playlistUri);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "플레이리스트 재생 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private String determineEmotion(float score) {
        if (score > 0.5) {
            return "positive";
        } else if (score < -0.5) {
            return "negative";
        } else {
            return "neutral";
        }
    }

    public void openSpotifyTrack(String trackUri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(trackUri));

        String spotifyPackageName = "com.spotify.music";

        PackageManager packageManager = getPackageManager();
        try {
            packageManager.getPackageInfo(spotifyPackageName, 0);
            intent.setPackage(spotifyPackageName);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(this, "Spotify 앱이 설치되어 있지 않습니다.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Spotify 앱이 설치되어 있지 않습니다.", e);

            try {
                Intent storeIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + spotifyPackageName));
                startActivity(storeIntent);
            } catch (android.content.ActivityNotFoundException anfe) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + spotifyPackageName));
                startActivity(browserIntent);
            }
        }
    }

    private String getPlaylistByEmotion(String emotion, float adjustedMagnitude) {
        if (emotion.equals("positive")) {
            if (adjustedMagnitude <= 1.0) {
                return "spotify:playlist:0dXD363ANExFR9jef9aJdS";
            } else if (adjustedMagnitude <= 3.0) {
                return "spotify:playlist:55cVwqa0o5taJ035oSFGvX";
            } else {
                return "spotify:playlist:6QYsQ7KtxQ5CQN5SmvxYs0";
            }
        } else if (emotion.equals("negative")) {
            if (adjustedMagnitude <= 1.0) {
                return "spotify:playlist:102rwYtlszP9nks5BpWOyo";
            } else if (adjustedMagnitude <= 3.0) {
                return "spotify:playlist:6k0VGCu8TbRebQ8jJ6lOoZ";
            } else {
                return "spotify:playlist:0jHVoE10oVUywSHUsQ6Xma";
            }
        } else if (emotion.equals("neutral")) {
            return "spotify:playlist:0CU1hw0Ihb0zGxeqWtkpI0";
        }
        return null;
    }

    private void playPlaylist(String playlistId) {
        Toast.makeText(this, "플레이리스트 재생: " + playlistId, Toast.LENGTH_SHORT).show();
    }

    private void initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Toast.makeText(GJ.this, "음성 인식을 시작합니다.", Toast.LENGTH_SHORT).show();
                recognizedText.setLength(0);
            }

            @Override
            public void onBeginningOfSpeech() {
                Toast.makeText(GJ.this, "음성이 감지되었습니다.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                Toast.makeText(GJ.this, "음성 입력이 종료되었습니다.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(int error) {
                String errorMessage;
                switch (error) {
                    case SpeechRecognizer.ERROR_AUDIO:
                        errorMessage = "오디오 오류";
                        break;
                    case SpeechRecognizer.ERROR_CLIENT:
                        errorMessage = "클라이언트 오류";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK:
                        errorMessage = "네트워크 오류";
                        break;
                    case SpeechRecognizer.ERROR_NO_MATCH:
                        errorMessage = "일치하는 텍스트 없음";
                        break;
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                        errorMessage = "음성 인식기가 바쁨";
                        break;
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        errorMessage = "음성이 입력되지 않음";
                        break;
                    default:
                        errorMessage = "알 수 없는 오류";
                }
                Toast.makeText(GJ.this, "오류 발생: " + errorMessage, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    recognizedText.append(matches.get(0)).append(" ");
                    textTranscription.setText(recognizedText.toString().trim());
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> partialMatches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (partialMatches != null && !partialMatches.isEmpty()) {
                    String partialResult = partialMatches.get(0);

                    String currentText = recognizedText.toString();
                    if (!currentText.endsWith(partialResult)) {
                        textTranscription.setText(currentText + partialResult);
                    }
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void startVoiceRecognition() {
        if (speechRecognizer == null) {
            initializeSpeechRecognizer();
        }
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 15000);
        speechRecognizer.startListening(intent);
        Toast.makeText(this, "음성 인식을 시작합니다.", Toast.LENGTH_SHORT).show();
    }

    private void stopVoiceRecognition() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            Toast.makeText(this, "음성 인식을 중지합니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void analyzeSentiment(String text) {
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... params) {
                try {
                    OkHttpClient client = new OkHttpClient();

                    JsonObject document = new JsonObject();
                    document.addProperty("type", "PLAIN_TEXT");
                    document.addProperty("content", params[0]);

                    JsonObject requestBodyJson = new JsonObject();
                    requestBodyJson.add("document", document);

                    RequestBody body = RequestBody.create(
                            requestBodyJson.toString(),
                            MediaType.get("application/json; charset=utf-8")
                    );

                    Request request = new Request.Builder()
                            .url(API_URL)
                            .post(body)
                            .build();

                    Response response = client.newCall(request).execute();

                    if (response.isSuccessful()) {
                        return response.body() != null ? response.body().string() : null;
                    } else {
                        return "API 요청 실패: " + response.code();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return "오류 발생: " + e.getMessage();
                }
            }

            @Override
            protected void onPostExecute(String result) {
                if (result != null) {
                    parseSentimentResult(result);
                } else {
                    textSentiment.setText("결과를 가져오지 못했습니다.");
                }
            }
        }.execute(text);
    }

    private void parseSentimentResult(String jsonResponse) {
        try {
            JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();
            JsonObject sentiment = jsonObject.getAsJsonObject("documentSentiment");

            float score = sentiment.get("score").getAsFloat();
            float magnitude = sentiment.get("magnitude").getAsFloat();

            JsonObject textObject = jsonObject.getAsJsonArray("sentences").get(0).getAsJsonObject();
            String text = textObject.getAsJsonObject("text").get("content").getAsString();

            float adjustedMagnitude = adjustMagnitude(text, magnitude);

            float positive = Math.max(score, 0);
            float negative = Math.max(-score, 0);
            float neutral = 1 - (positive + negative);

            String overallSentiment;
            if (score > 0.5) {
                overallSentiment = "긍정적 😊";
            } else if (score < -0.5) {
                overallSentiment = "부정적 😞";
            } else {
                overallSentiment = "중립적 😐";
            }

            String resultText = "전체 문장 감정: " + overallSentiment +
                    "\n긍정 점수: " + positive +
                    "\n중립 점수: " + neutral +
                    "\n부정 점수: " + negative +
                    "\n감정 강도: " + adjustedMagnitude;

            textSentiment.setText(resultText);

        } catch (Exception e) {
            e.printStackTrace();
            textSentiment.setText("결과를 분석하지 못했습니다.");
        }
    }
    private float adjustMagnitude(String text, float magnitude) {
        int textLength = text.length();
        if (textLength > 15 && textLength <= 35) {
            return magnitude + 0.5f;
        } else if (textLength > 35) {
            return magnitude + 1.0f;
        }
        return magnitude;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "음성 인식을 위해 오디오 권한이 필요합니다.", Toast.LENGTH_LONG).show();
            }
        }
    }
}