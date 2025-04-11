package com.example.graduation_work;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Map_Mode extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng currentLocation;

    private EditText cityEditText;
    private Button searchButton;
    private String accessToken;

    private RadioGroup locationToggleGroup;
    private boolean displayCityName = true;
    private boolean displayDistrictName = false;
    private boolean displayNeighborhoodName = false;
    private boolean displayIslandName = false;

    private static final String TAG = "Map_Mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);

        cityEditText = findViewById(R.id.city);
        searchButton = findViewById(R.id.searchButton);

        locationToggleGroup = findViewById(R.id.radioGroup);
        locationToggleGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioButton1) {
                displayCityName = true;
                displayDistrictName = false;
                displayNeighborhoodName = false;
                displayIslandName = false;
            } else if (checkedId == R.id.radioButton2) {
                displayCityName = false;
                displayDistrictName = true;
                displayNeighborhoodName = false;
                displayIslandName = false;
            } else if (checkedId == R.id.radioButton3) {
                displayCityName = false;
                displayDistrictName = false;
                displayNeighborhoodName = true;
                displayIslandName = false;
            } else if (checkedId == R.id.radioButton4) {
                displayCityName = false;
                displayDistrictName = false;
                displayNeighborhoodName = false;
                displayIslandName = true;
            }
            updateLocation();
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.maps);
        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        searchButton.setOnClickListener(v -> {
            String searchQuery = cityEditText.getText().toString();
            if (!searchQuery.isEmpty()) {
                if (accessToken == null) {
                    requestAccessTokenAndSearch(searchQuery);
                } else {
                    searchPlaylists(searchQuery);
                }
            } else {
                Toast.makeText(Map_Mode.this, "검색어를 입력하세요.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, 1);
            return;
        }

        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setMapToolbarEnabled(true);

        updateLocation();
    }

    private void updateLocation() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.addMarker(new MarkerOptions().position(currentLocation).title("현재 위치"));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));

                        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                        try {
                            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                            if (addresses != null && !addresses.isEmpty()) {
                                Address address = addresses.get(0);
                                String locationName = null;

                                if (displayCityName) {
                                    locationName = address.getLocality();
                                    if (locationName != null && locationName.endsWith("시")) {
                                        locationName = locationName.substring(0, locationName.length() - 1);
                                    }
                                } else if (displayDistrictName) {
                                    locationName = address.getSubLocality();
                                    if (locationName != null && locationName.endsWith("구")) {
                                        locationName = locationName.substring(0, locationName.length() - 1);
                                    }
                                } else if (displayNeighborhoodName) {
                                    locationName = address.getThoroughfare();
                                    if (locationName != null && locationName.endsWith("동")) {
                                        locationName = locationName.substring(0, locationName.length() - 1);
                                        locationName = locationName.replaceAll("\\d", "");
                                    }
                                } else if (displayIslandName) {
                                    locationName = address.getFeatureName();
                                    if (locationName != null && locationName.endsWith("섬")) {
                                        locationName = locationName.substring(0, locationName.length() - 1);
                                    }
                                }

                                if (locationName != null && !locationName.isEmpty()) {
                                    cityEditText.setText(locationName);
                                } else {
                                    cityEditText.setText("위치를 찾을 수 없습니다.");
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            cityEditText.setText("위치를 찾는 중 오류가 발생했습니다.");
                        }
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateLocation();
            }
        }
    }

    private void requestAccessTokenAndSearch(String query) {
        String clientId = BuildConfig.SPOTIFY_CLIENT_ID;
        String clientSecret = BuildConfig.SPOTIFY_CLIENT_SECRET;
        String credentials = clientId + ":" + clientSecret;
        String basicAuth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://accounts.spotify.com/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        SpotifyAuthService authService = retrofit.create(SpotifyAuthService.class);

        Call<AccessTokenResponse> call = authService.getAccessToken(basicAuth, "client_credentials");
        call.enqueue(new Callback<AccessTokenResponse>() {
            @Override
            public void onResponse(Call<AccessTokenResponse> call, Response<AccessTokenResponse> response) {
                if (response.isSuccessful()) {
                    AccessTokenResponse tokenResponse = response.body();
                    if (tokenResponse != null) {
                        accessToken = tokenResponse.getAccessToken();
                        searchPlaylists(query);
                    }
                } else {
                    Toast.makeText(Map_Mode.this, "Failed to get access token", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AccessTokenResponse> call, Throwable t) {
                Toast.makeText(Map_Mode.this, "Request failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchPlaylists(String query) {
        if (accessToken == null) {
            Toast.makeText(this, "Access token is not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.spotify.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        SpotifyService service = retrofit.create(SpotifyService.class);
        Call<SearchResponse> call = service.searchPlaylists(query, "playlist", "Bearer " + accessToken);
        call.enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                if (response.isSuccessful()) {
                    SearchResponse searchResponse = response.body();
                    if (searchResponse != null && searchResponse.getPlaylists().getItems().size() > 0) {
                        String playlistUri = searchResponse.getPlaylists().getItems().get(0).getUri();
                        openSpotifyTrack(playlistUri);
                    } else {
                        Toast.makeText(Map_Mode.this, "No playlists found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(Map_Mode.this, "Search request failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                Toast.makeText(Map_Mode.this, "Request failed", Toast.LENGTH_SHORT).show();
            }
        });
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
}