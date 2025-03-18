package com.example.graduation_work;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class User_Find {
    private static final String IP_ADDRESS = "180.70.44.9";
    private static final String IP_PORTS = "11111";
    private static final String TAG = "User_Find";

    private Context context;

    public User_Find(Context context) {
        this.context = context;
    }

    public static void showIDPopup(Context context) {
        if (context instanceof AppCompatActivity && !((AppCompatActivity) context).isFinishing()) {
            Dialog dialog = new Dialog(context);
            dialog.setContentView(R.layout.forgot_id);

            EditText forgot_id_name = dialog.findViewById(R.id.forgot_ID_Name);
            EditText forgot_id_text = dialog.findViewById(R.id.forgot_ID_Text);
            Button find_id_button = dialog.findViewById(R.id.find_ID_Button);

            find_id_button.setOnClickListener(v -> {
                String name = forgot_id_name.getText().toString().trim();
                String text = forgot_id_text.getText().toString().trim();
                String status = isValidEmail(text);

                if (name.isEmpty() || text.isEmpty()) {
                    Toast.makeText(context, "모든 정보를 입력해주세요.", Toast.LENGTH_SHORT).show();
                } else {
                    FindIDTask findidtask = new FindIDTask(context, name, text, status);
                    findidtask.execute("http://" + IP_ADDRESS + ":" + IP_PORTS + "/find_ID.php");
                    Log.d(TAG, "FindIDTask Constructor - Name: " + name + ", NUM/EMAIL: " + text + ", Status: " + status);
                }
            });
            dialog.show();
        }
    }

    public static class FindIDTask extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;
        private final Context context;
        private final String userIDName;
        private final String userIDText;
        private final String status;

        public FindIDTask(Context context, String userIDName, String userIDText, String status) {
            this.context = context;
            this.userIDName = userIDName;
            this.userIDText = userIDText;
            this.status = status;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (context instanceof AppCompatActivity && !((AppCompatActivity) context).isFinishing()) {
                progressDialog = ProgressDialog.show(context, "아이디 찾는 중...", null, true, true);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressDialog.dismiss();
            Log.d(TAG, "POST response - " + result);
            try {
                JSONObject jsonResponse = new JSONObject(result);
                String status = jsonResponse.getString("status");

                if (status.equals("success")) {
                    String userID = jsonResponse.getString("userID");

                    if (this.status.equals("Email")) {

                    } else if (this.status.equals("Phone")) {
                        requestSmsPermissionForPassword(context, userIDText, userID);
                    }
                } else {
                    Toast.makeText(context, "회원정보가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                Log.e(TAG, "JSON parsing error: " + e.getMessage());
                Toast.makeText(context, "데이터를 처리하는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        }


        @Override
        protected String doInBackground(String... params) {
            String serverURL = params[0];

            String postParameters = "userName=" + userIDName + "&userText=" + userIDText + "&status=" + status;

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
                Log.d("Find ID php 값 :", sb.toString());
                return sb.toString();
            } catch (Exception e) {
                Log.d(TAG, "Find ID Error : " + e.getMessage());
                return "Find ID Error : " + e.getMessage();
            }
        }
    }

    public static void showPWPopup(Context context) {
        if (context instanceof AppCompatActivity && !((AppCompatActivity) context).isFinishing()) {
            Dialog dialog = new Dialog(context);
            dialog.setContentView(R.layout.forgot_password);

            EditText forgot_pw_id = dialog.findViewById(R.id.forgot_PW_ID);
            EditText forgot_pw_name = dialog.findViewById(R.id.forgot_PW_Name);
            EditText forgot_pw_text = dialog.findViewById(R.id.forgot_PW_Text);
            Button find_pw_button = dialog.findViewById(R.id.find_PW_Button);

            find_pw_button.setOnClickListener(v -> {
                String id = forgot_pw_id.getText().toString().trim();
                String name = forgot_pw_name.getText().toString().trim();
                String text = forgot_pw_text.getText().toString().trim();
                String status = isValidEmail(text);

                if (id.isEmpty() || name.isEmpty() || text.isEmpty()) {
                    Toast.makeText(context, "모든 정보를 입력해주세요.", Toast.LENGTH_SHORT).show();
                } else {
                    FindPasswordTask findPasswordTask = new FindPasswordTask(context, id, name, text, status);
                    findPasswordTask.execute("http://" + IP_ADDRESS + ":" + IP_PORTS + "/find_PassWord.php");
                    Log.d(TAG, "FindPasswordTask Constructor - ID: " + id + ", Name: " + name + ", Num/EMAIL: " + text + ", Status: " + status);
                }
            });
            dialog.show();
        }
    }

    public static class FindPasswordTask extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;
        private final Context context;
        private final String userPWID;
        private final String userPWName;
        private final String userPWText;
        private final String status;

        public FindPasswordTask(Context context, String userPWID, String userPWName, String userPWText, String status) {
            this.context = context;
            this.userPWID = userPWID;
            this.userPWName = userPWName;
            this.userPWText = userPWText;
            this.status = status;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(context, "비밀번호 찾는 중...", null, true, true);
        }

        //여기!!
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressDialog.dismiss();
            Log.d(TAG, "POST response - " + result);
            try {
                JSONObject jsonResponse = new JSONObject(result);
                String status = jsonResponse.getString("status");

                if (status.equals("success")) {
                    String userPassword = jsonResponse.getString("userPassword");

                    if (this.status.equals("Email")) {

                    } else if (this.status.equals("Phone")) {
                        requestSmsPermissionForPassword(context, userPWText, userPassword);
                    }
                } else {
                    Toast.makeText(context, "회원정보가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                Log.e(TAG, "JSON parsing error: " + e.getMessage());
                Toast.makeText(context, "데이터를 처리하는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected String doInBackground(String... params) {
            String serverURL = params[0];

            String postParameters = "userID=" + userPWID + "&userName=" + userPWName + "&userText=" + userPWText + "&status=" + status;

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
                Log.d("Find Password php 값 :", sb.toString());
                return sb.toString();
            } catch (Exception e) {
                Log.d(TAG, "Find Password Error : " + e.getMessage());
                return "Find Password Error : " + e.getMessage();
            }
        }

    }

    public static String isValidEmail(String text) {
        if (text != null && android.util.Patterns.EMAIL_ADDRESS.matcher(text).matches()) {
            return "Email"; // '@'가 있으면 0 반환
        } else {
            return "Phone"; // '@'가 없으면 1 반환
        }
    }

    public static void requestSmsPermissionForID(Context context, String userText, String userID_Send) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((AppCompatActivity) context, new String[]{android.Manifest.permission.SEND_SMS}, 1);
        } else {
            sendIDViaSMS(userText, userID_Send);
        }
    }

    private static void requestSmsPermissionForPassword(Context context, String userText, String userPassword_Send) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((AppCompatActivity) context, new String[]{Manifest.permission.SEND_SMS}, 2);
        } else {
            sendPasswordViaSMS(userText, userPassword_Send);
        }
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        if (requestCode == 1) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                Toast.makeText(context, "SMS 권한이 승인되었습니다.", Toast.LENGTH_SHORT).show();
//                sendIDViaSMS(userText, userID_Send);
//            } else {
//                Toast.makeText(context, "SMS 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show();
//            }
//        } else if (requestCode == 2) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                Toast.makeText(context, "SMS 권한이 승인되었습니다.", Toast.LENGTH_SHORT).show();
//                sendPasswordViaSMS(userText, userPassword_Send);
//            } else {
//                Toast.makeText(context, "SMS 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }

    // SMS Send
    public static void sendIDViaSMS(String phoneNumber, String userID) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, "Your ID is: " + userID, null, null);
            Log.d(TAG, "SMS 전송 성공");
        } catch (Exception e) {
            Log.e(TAG, "SMS 전송 실패: " + e.getMessage());
        }
    }

    private static void sendPasswordViaSMS(String phoneNumber, String userPassword) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, "Your password is: " + userPassword, null, null);
            Log.d(TAG, "SMS 전송 성공");
        } catch (Exception e) {
            Log.e(TAG, "SMS 전송 실패: " + e.getMessage());
        }
    }
}
