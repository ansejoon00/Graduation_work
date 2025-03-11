package com.example.graduation_work;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Login extends AppCompatActivity {
    private static String IP_ADDRESS = "180.70.44.9";
    private static String IP_PORTS = "11111";
    private static String TAG = "LOGIN";

    private ImageView back;

    private EditText login_id;
    private EditText login_pwd;

    private Button login_button;
    private Button forgot_id_button;
    private Button forgot_password_button;

    String ID;
    String PASSWORD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_page);

        back = findViewById(R.id.back);

        login_id = findViewById(R.id.login_id);
        login_pwd = findViewById(R.id.login_pwd);

        login_button = findViewById(R.id.login_button);
        forgot_id_button = findViewById(R.id.forgot_id_button);
        forgot_password_button = findViewById(R.id.forgot_password_button);

        back.setOnClickListener(view -> finish());

        login_button.setOnClickListener(v -> {
            login_button.setBackgroundResource(R.drawable.color_button_click);

            String id = login_id.getText().toString().trim();
            String pwd = login_pwd.getText().toString().trim();

            if (id.isEmpty() || pwd.isEmpty()) {
                Toast.makeText(Login.this, "정보를 입력해주세요.", Toast.LENGTH_SHORT).show();
            } else {
                LoginTask logintask = new LoginTask(id, pwd);
                logintask.execute("http://" + IP_ADDRESS + ":" + IP_PORTS + "/login.php");
                Log.d(TAG, "LoginTask Constructor - ID: " + id + ", PWD: " + pwd);
            }
        });
        forgot_id_button.setOnClickListener(v -> User_Find.showIDPopup(Login.this));
        forgot_password_button.setOnClickListener(v -> User_Find.showPWPopup(Login.this));
    }

    class LoginTask extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;
        private final String login_ID;
        private final String login_PW;

        LoginTask(String login_ID, String login_PW) {
            this.login_ID = login_ID;
            this.login_PW = login_PW;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(Login.this, "로그인 중...", null, true, true);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressDialog.dismiss();
            Log.d(TAG, "POST response - " + result);

            if (result.contains("login success")) {
                GlobalSettings.setUserId(login_ID);
                String user_ID = GlobalSettings.getUserId();

                Toast.makeText(Login.this, "로그인에 성공하셨습니다.", Toast.LENGTH_SHORT).show();

                UserinfoTask userinfotask = new UserinfoTask();
                userinfotask.execute("http://" + IP_ADDRESS + ":" + IP_PORTS + "/info.php", user_ID);
                Log.d(TAG, "UserinfoTask Constructor - ID: " + user_ID);

                Intent intent = new Intent(Login.this, Mainmenu.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(Login.this, "로그인에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected String doInBackground(String... params) {
            String serverURL = params[0];

            String postParameters = "userid=" + login_ID + "&userpw=" + login_PW;

            try {
                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoOutput(true);
                httpURLConnection.connect();

                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();

                int responseStatusCode = httpURLConnection.getResponseCode();
                InputStream inputStream = (responseStatusCode == HttpURLConnection.HTTP_OK) ? httpURLConnection.getInputStream() : httpURLConnection.getErrorStream();

                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }

                bufferedReader.close();
                Log.d("Login php 값 :", sb.toString());
                return sb.toString();
            } catch (Exception e) {
                Log.d(TAG, "Login Error : " + e.getMessage());
                return "Login Error : " + e.getMessage();
            }
        }
    }

    class UserinfoTask extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(Login.this, "사용자 정보 동기화 중...", null, true, true);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressDialog.dismiss();
            Log.d(TAG, "POST response - " + result);

            try {
                JSONObject jsonResponse = new JSONObject(result);

                if (jsonResponse.getString("status").equals("userinfo success")) {
                    GlobalSettings.setUserEmail(jsonResponse.getString("UserEmail"));
                    GlobalSettings.setUserPhone(jsonResponse.getString("UserPhone"));
                    GlobalSettings.setUserName(jsonResponse.getString("UserName"));
                    GlobalSettings.setUserDate(jsonResponse.getString("Userdate"));

                    Toast.makeText(Login.this, "동기화에 성공하셨습니다.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(Login.this, Mainmenu.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(Login.this, "동기화에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                Log.e(TAG, "JSON parsing error: " + e.getMessage());
                Toast.makeText(Login.this, "데이터를 처리하는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected String doInBackground(String... params) {
            String serverURL = params[0];
            String userid = params[1];

            String postParameters = "userid=" + userid;

            try {
                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoOutput(true);
                httpURLConnection.connect();

                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();

                int responseStatusCode = httpURLConnection.getResponseCode();
                InputStream inputStream = (responseStatusCode == HttpURLConnection.HTTP_OK) ? httpURLConnection.getInputStream() : httpURLConnection.getErrorStream();

                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }

                bufferedReader.close();
                Log.d("Info php 값 :", sb.toString());
                return sb.toString();
            } catch (Exception e) {
                Log.d(TAG, "Info Error : " + e.getMessage());
                return "Info Error : " + e.getMessage();
            }
        }
    }
}