package com.example.graduation_work;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Mainmenu extends AppCompatActivity {
    private static final String SPOTIFY_BASE_URL = "https://api.spotify.com/";
    private static final String ACCOUNTS_BASE_URL = "https://accounts.spotify.com/"; // 이 부분 추가
    private static String IP_ADDRESS = "180.70.44.9";
    private static String IP_PORTS = "11111";
    private String accessToken;
    private static final String TAG = "Mainmenu";
    private SpeechRecognizer speechRecognizer;

    // 추가된 부분
    private RadioGroup searchTypeRadioGroup;
    private RadioButton songRadioButton;
    private RadioButton playlistRadioButton;

    private EditText searchEditText;
    private ImageButton searchTextButton;
    private ImageButton searchVoiceButton;
    private Button searchMapButton;
    private Button searchTalkButton;

    private ImageButton userInfoButton;
    private TextView userINFOTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.mainmenu_page);

        // 음성 인식 권한 요청
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }

        searchEditText = findViewById(R.id.searchEditText);
        searchTextButton = findViewById(R.id.searchTextButton);
        searchVoiceButton = findViewById(R.id.searchVoiceButton);
        searchMapButton = findViewById(R.id.searchMapButton);
        searchTalkButton = findViewById(R.id.searchTalkButton);
        userInfoButton = findViewById(R.id.UserInfoButton);

        // RadioGroup과 RadioButton 초기화
        searchTypeRadioGroup = findViewById(R.id.searchTypeRadioGroup);
        songRadioButton = findViewById(R.id.songRadioButton);
        playlistRadioButton = findViewById(R.id.playlistRadioButton);

        String userid = GlobalSettings.getUserId();

        initializeUI();

        // 검색 버튼 클릭 시 처리
        searchTextButton.setOnClickListener(v -> {
            String searchQuery = searchEditText.getText().toString();
            Toast.makeText(Mainmenu.this, userid + searchQuery, Toast.LENGTH_SHORT).show();
            if (!searchQuery.isEmpty()) {
                // 액세스 토큰이 없는 경우 요청 후 검색 실행
                if (accessToken == null) {
                    requestAccessTokenAndSearch(searchQuery);
                } else {
                    // RadioGroup 리스너에서 검색 처리
                    int checkedId = searchTypeRadioGroup.getCheckedRadioButtonId();
                    if (checkedId == R.id.songRadioButton) {
                        User_Info.insert_search_data(this, userid, searchQuery);
                        searchTracks(searchQuery); // 노래 검색

                    } else if (checkedId == R.id.playlistRadioButton) {
                        User_Info.insert_search_data(this, userid, searchQuery);
                        searchPlaylists(searchQuery); // 플레이리스트 검색

                    }
                }
            } else {
                Toast.makeText(Mainmenu.this, "검색어를 입력하세요.", Toast.LENGTH_SHORT).show();
            }
        });

        // 음성 검색 버튼 클릭 시 처리
        searchVoiceButton.setOnClickListener(v -> startVoiceRecognition());

        searchMapButton.setOnClickListener(v -> {
            Intent intent = new Intent(Mainmenu.this, Map_Mode.class);
            startActivity(intent);
        });

        searchTalkButton.setOnClickListener(v -> {
            Intent intent = new Intent(Mainmenu.this, GJ.class);
            startActivity(intent);
        });

        userInfoButton.setOnClickListener(view -> User_Info.showInfoPopup(Mainmenu.this));
    }

    private void initializeUI() {
        searchEditText = findViewById(R.id.searchEditText);
        searchTextButton = findViewById(R.id.searchTextButton);
        searchVoiceButton = findViewById(R.id.searchVoiceButton);
        searchMapButton = findViewById(R.id.searchMapButton);
        searchTalkButton = findViewById(R.id.searchTalkButton);
        searchTypeRadioGroup = findViewById(R.id.searchTypeRadioGroup);
    }

    // Spotify 액세스 토큰을 요청하고 검색을 수행하는 메소드
    private void requestAccessTokenAndSearch(String query) {
        String clientId = BuildConfig.SPOTIFY_CLIENT_ID;
        String clientSecret = BuildConfig.SPOTIFY_CLIENT_SECRET;
        String credentials = clientId + ":" + clientSecret;
        String basicAuth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(logging).build();

        Retrofit retrofit = new Retrofit.Builder().baseUrl(ACCOUNTS_BASE_URL).client(client).addConverterFactory(GsonConverterFactory.create()).build();
        SpotifyAuthService authService = retrofit.create(SpotifyAuthService.class);

        Call<AccessTokenResponse> call = authService.getAccessToken(basicAuth, "client_credentials");
        call.enqueue(new Callback<AccessTokenResponse>() {
            @Override
            public void onResponse(Call<AccessTokenResponse> call, Response<AccessTokenResponse> response) {
                if (response.isSuccessful()) {
                    AccessTokenResponse tokenResponse = response.body();
                    if (tokenResponse != null) {
                        accessToken = tokenResponse.getAccessToken();
                        Log.d(TAG, "Access Token: " + accessToken);

                        // 토큰을 얻은 후, 사용자가 선택한 검색 유형에 따라 검색 실행
                        int checkedId = searchTypeRadioGroup.getCheckedRadioButtonId();
                        if (checkedId == R.id.songRadioButton) {
                            searchTracks(query); // 노래 검색
                        } else if (checkedId == R.id.playlistRadioButton) {
                            searchPlaylists(query); // 플레이리스트 검색
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to get access token: " + response.code() + " " + response.message());
                    Toast.makeText(Mainmenu.this, "Failed to get access token", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AccessTokenResponse> call, Throwable t) {
                t.printStackTrace();
                Toast.makeText(Mainmenu.this, "Request failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Spotify에서 노래 검색 수행
    private void performSearch(String query) {
        if (accessToken == null) {
            Toast.makeText(this, "Access token is not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Retrofit retrofit = new Retrofit.Builder().baseUrl(SPOTIFY_BASE_URL).addConverterFactory(GsonConverterFactory.create()).build();
        SpotifyService service = retrofit.create(SpotifyService.class);
        Call<SearchResponse> call = service.searchTracks(query, "track", "Bearer " + accessToken);

        call.enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                if (response.isSuccessful()) {
                    SearchResponse searchResponse = response.body();
                    if (searchResponse != null && searchResponse.getTracks().getItems().size() > 0) {
                        String trackUri = searchResponse.getTracks().getItems().get(0).getUri();
                        openSpotifyTrack(trackUri);
                    } else {
                        Toast.makeText(Mainmenu.this, "No tracks found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(Mainmenu.this, "Search request failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                t.printStackTrace();
                Toast.makeText(Mainmenu.this, "Request failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 음성 인식 시작
    private void startVoiceRecognition() {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        searchEditText.setText(matches.get(0));
                        performSearch(matches.get(0));
                    }
                }

                @Override
                public void onReadyForSpeech(Bundle params) {
                }

                @Override
                public void onBeginningOfSpeech() {
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                }

                @Override
                public void onBufferReceived(byte[] buffer) {
                }

                @Override
                public void onEndOfSpeech() {
                }

                @Override
                public void onError(int error) {
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                }

                @Override
                public void onEvent(int eventType, Bundle params) {
                }
            });
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "검색어를 말해 주세요.");

        speechRecognizer.startListening(intent);
    }

    // searchTracks() 메서드 추가
    private void searchTracks(String query) {
        if (accessToken == null) {
            Toast.makeText(this, "Access token is not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Retrofit retrofit = new Retrofit.Builder().baseUrl("https://api.spotify.com/").addConverterFactory(GsonConverterFactory.create()).build();

        SpotifyService service = retrofit.create(SpotifyService.class);
        Call<SearchResponse> call = service.searchTracks(query, "track", "Bearer " + accessToken);

        call.enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                if (response.isSuccessful()) {
                    // 검색 결과 처리
                    SearchResponse searchResponse = response.body();
                    if (searchResponse != null && searchResponse.getTracks().getItems().size() > 0) {
                        String trackUri = searchResponse.getTracks().getItems().get(0).getUri();
                        openSpotifyTrack(trackUri); // 검색 결과 Spotify에서 열기
                    } else {
                        Toast.makeText(Mainmenu.this, "No tracks found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(Mainmenu.this, "Search request failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                t.printStackTrace();
                Toast.makeText(Mainmenu.this, "Request failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "음성 인식 권한이 승인되었습니다.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "음성 인식 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void searchPlaylists(String query) {
        if (accessToken == null) {
            Toast.makeText(this, "Access token is not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Retrofit retrofit = new Retrofit.Builder().baseUrl("https://api.spotify.com/").addConverterFactory(GsonConverterFactory.create()).build();

        SpotifyService service = retrofit.create(SpotifyService.class);
        Call<SearchResponse> call = service.searchPlaylists(query, "playlist", "Bearer " + accessToken);
        call.enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                if (response.isSuccessful()) {
                    // 검색 결과 처리
                    SearchResponse searchResponse = response.body();
                    if (searchResponse != null && searchResponse.getPlaylists().getItems().size() > 0) {
                        String playlistUri = searchResponse.getPlaylists().getItems().get(0).getUri();

                        // URI 형식 확인 후 Spotify 재생
                        if (playlistUri.startsWith("spotify:playlist:")) {
                            openSpotifyTrack(playlistUri);
                        } else {
                            Toast.makeText(Mainmenu.this, "Invalid Spotify URI", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Toast.makeText(Mainmenu.this, "No playlists found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(Mainmenu.this, "Search request failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                t.printStackTrace();
                Toast.makeText(Mainmenu.this, "Request failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 음성 인식 시작


    // 특정 트랙이나 플레이리스트를 Spotify 앱에서 여는 메소드
    public void openSpotifyTrack(String trackUri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(trackUri));

        String spotifyPackageName = "com.spotify.music";

        // Spotify 앱이 설치되어 있는지 확인
        PackageManager packageManager = getPackageManager();
        try {
            packageManager.getPackageInfo(spotifyPackageName, 0);
            intent.setPackage(spotifyPackageName);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(this, "Spotify 앱이 설치되어 있지 않습니다.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Spotify 앱이 설치되어 있지 않습니다.", e);

            // Play Store로 이동하여 Spotify 앱 설치를 유도
            try {
                Intent storeIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + spotifyPackageName));
                startActivity(storeIntent);
            } catch (android.content.ActivityNotFoundException anfe) {
                // Play Store가 설치되지 않았을 때 예외 처리
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + spotifyPackageName));
                startActivity(browserIntent);
            }
        }
    }
}