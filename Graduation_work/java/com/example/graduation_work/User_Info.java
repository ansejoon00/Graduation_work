package com.example.graduation_work;

import android.Manifest;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.app.Activity;
import android.view.View;
import android.widget.Toast;
import android.content.Context;

import android.content.Intent;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.app.AlertDialog;
import android.content.DialogInterface;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.annotation.NonNull;

import android.view.ViewGroup;

import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;

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
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class User_Info {
    private static final String IP_ADDRESS = "180.70.44.9";
    private static final String IP_PORTS = "11111";
    private static final String TAG = "User_Info";

    public static void insert_search_data(Context context, String userid, String data) {
        new InsertUserRecordTask(context, userid, data).execute("http://" + IP_ADDRESS + ":" + IP_PORTS + "/insert_data.php");
    }

    public static void showInfoPopup(Context context) {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.user_info);

        String userId = GlobalSettings.getUserId();
        String userEmail = GlobalSettings.getUserEmail();
        String userPhone = GlobalSettings.getUserPhone();
        String userDate = GlobalSettings.getUserDate();
        String userName = GlobalSettings.getUserName();

        TextView userInfoTextView = dialog.findViewById(R.id.userINFOTextView);
        userInfoTextView.setText("ID: " + userId + " / 이름: " + userName + "\n" + "Email: " + userEmail + "\n" + "전화번호: " + userPhone + "\n" + "생일: " + userDate);

        Button userRecordsButton = dialog.findViewById(R.id.userRecordsButton);
        Button userLogoutButton = dialog.findViewById(R.id.userlogoutButton);
        Button userDeleteAccountButton = dialog.findViewById(R.id.userdeleteAccountButton);

        userRecordsButton.setOnClickListener(v -> {
            showSearchRecordPopup(context);
            dialog.dismiss();
        });

        userLogoutButton.setOnClickListener(v -> {
            showConfirmationDialog(context, "로그아웃하시겠습니까?", () -> logout(context));
            dialog.dismiss();
        });

        userDeleteAccountButton.setOnClickListener(v -> {
            showConfirmationDialog(context, "회원 탈퇴하시겠습니까?", () -> deleteAccount(context));
            dialog.dismiss();
        });

        dialog.show();
    }

    public static void showSearchRecordPopup(Context context) {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.user_record);
        dialog.setCancelable(false);

        RecyclerView recyclerViewSearchHistory = dialog.findViewById(R.id.recyclerViewSearchHistory);
        recyclerViewSearchHistory.setLayoutManager(new LinearLayoutManager(context));

        String userid = GlobalSettings.getUserId();
        RecordSearchTask recordSearchTask = new RecordSearchTask(dialog);
        recordSearchTask.execute("http://" + IP_ADDRESS + ":" + IP_PORTS + "/search_record.php", userid);

        Button btnClosePopup = dialog.findViewById(R.id.btnClosePopup);
        btnClosePopup.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private static void showConfirmationDialog(Context context, String message, Runnable onConfirm) {
        new AlertDialog.Builder(context)
                .setTitle("확인")
                .setMessage(message)
                .setPositiveButton("예", (dialog, which) -> onConfirm.run())
                .setNegativeButton("아니요", null)
                .show();
    }

    public static void logout(Context context) {
        GlobalSettings.clearUser();
        Toast.makeText(context, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show();
        context.startActivity(new Intent(context, Login.class));
        if (context instanceof Activity) {
            ((Activity) context).finish();
        }
    }

    public static void deleteAccount(Context context) {
        String deleteId = GlobalSettings.getUserId();
        new DeleteUserTask(context, deleteId).execute("http://" + IP_ADDRESS + ":" + IP_PORTS + "/drop.php");
    }

    static class InsertUserRecordTask extends AsyncTask<String, Void, String> {
        private final Context context;
        private final String userID;
        private final String data;
        private ProgressDialog progressDialog;

        public InsertUserRecordTask(Context context, String userID, String data) {
            this.context = context;
            this.userID = userID;
            this.data = data;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(context, "데이터를 전송 중입니다...", null, true, true);
        }

        @Override
        protected void onPostExecute(String result) {
            progressDialog.dismiss();
            if (result.contains("Insert Data Success")) {
                Toast.makeText(context, "데이터가 성공적으로 삽입되었습니다.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "데이터 삽입 실패.", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected String doInBackground(String... params) {
            String serverURL = params[0];

            String postParameters = "userid=" + userID + "&data=" + data;

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
                return sb.toString();
            } catch (Exception e) {
                Log.d(TAG, "InsertUserRecordTask Error : " + e.getMessage());
                return "InsertUserRecordTask Error : " + e.getMessage();
            }
        }
    }

    static class DeleteUserTask extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;
        private final Context context;
        private final String userid;

        public DeleteUserTask(Context context, String userid) {
            this.context = context;
            this.userid = userid;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(context, "사용자 정보 삭제 중...", null, true, true);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressDialog.dismiss();
            Log.d(TAG, "POST response  - " + result);

            if (result.contains("delete success")) {
                Toast.makeText(context, "회원탈퇴에 성공하셨습니다.", Toast.LENGTH_SHORT).show();
                context.startActivity(new Intent(context, first_page.class));
                if (context instanceof Activity) {
                    ((Activity) context).finish();
                }
            } else {
                Toast.makeText(context, "회원탈퇴에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected String doInBackground(String... params) {
            String serverURL = params[0];

            String postParameters = "useremail=" + userid;

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
                return sb.toString();
            } catch (Exception e) {
                Log.d(TAG, "DeleteUserTask Error : " + e.getMessage());
                return "DeleteUserTask Error : " + e.getMessage();
            }
        }
    }

    static class RecordSearchTask extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;
        Dialog dialog;
        RecyclerView recyclerView;
        TextView emptyMessage;

        public RecordSearchTask(Dialog dialog) {
            this.dialog = dialog;
            this.recyclerView = dialog.findViewById(R.id.recyclerViewSearchHistory);
            this.emptyMessage = dialog.findViewById(R.id.emptyMessage);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(dialog.getContext(), "기록 갖고 오는 중...", null, true, true);
        }

        @Override
        protected String doInBackground(String... params) {
            String serverURL = params[0];
            String userid = params[1];
            StringBuilder result = new StringBuilder();

            try {
                String getUrl = serverURL + "?userid=" + userid;
                URL url = new URL(getUrl);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setReadTimeout(5000);

                InputStream inputStream = httpURLConnection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                reader.close();
                inputStream.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

            return result.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressDialog.dismiss();
            Log.d(TAG, "POST response - " + result);

            List<SearchRecordHistory> searchHistoryList = new ArrayList<>();
            if (result.contains("record search success")) {
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    JSONArray jsonArray = jsonObject.getJSONArray("records");

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject recordObject = jsonArray.getJSONObject(i);
                        String data = recordObject.getString("data");
                        String time = recordObject.getString("time");

                        searchHistoryList.add(new SearchRecordHistory(data, time));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(dialog.getContext(), "JSON 파싱 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(dialog.getContext(), "기록 검색에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            }

            if (searchHistoryList.isEmpty()) {
                emptyMessage.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyMessage.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                SearchHistoryAdapter searchHistoryAdapter = new SearchHistoryAdapter(searchHistoryList);
                recyclerView.setAdapter(searchHistoryAdapter);
            }
        }
    }

    static class SearchHistoryAdapter extends RecyclerView.Adapter<SearchHistoryAdapter.SearchHistoryViewHolder> {

        private List<SearchRecordHistory> searchHistoryList;

        public SearchHistoryAdapter(List<SearchRecordHistory> searchHistoryList) {
            this.searchHistoryList = searchHistoryList;
        }

        @NonNull
        @Override
        public SearchHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_record_info, parent, false);
            return new SearchHistoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SearchHistoryViewHolder holder, int position) {
            holder.bind(searchHistoryList.get(position));
        }

        @Override
        public int getItemCount() {
            return searchHistoryList.size();
        }

        static class SearchHistoryViewHolder extends RecyclerView.ViewHolder {
            private TextView data;
            private TextView time;

            public SearchHistoryViewHolder(@NonNull View itemView) {
                super(itemView);
                data = itemView.findViewById(R.id.Data);
                time = itemView.findViewById(R.id.Time);
            }

            public void bind(SearchRecordHistory searchHistory) {
                data.setText(searchHistory.getData());
                time.setText(searchHistory.getTime());
            }
        }
    }

    static class SearchRecordHistory {
        private String data;
        private String time;

        public SearchRecordHistory(String data, String time) {
            this.data = data;
            this.time = time;
        }

        public String getData() {
            return data;
        }

        public String getTime() {
            return time;
        }
    }
}