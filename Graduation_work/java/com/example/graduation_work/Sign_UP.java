package com.example.graduation_work;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Sign_UP extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private static String IP_ADDRESS = "180.70.44.9";
    private static String IP_PORTS = "11111";
    private static String TAG = "Sign_UP";

    private ImageView back;

    private EditText signup_id;
    private EditText signup_email;
    private EditText signup_pwd;
    private EditText signup_pwd2;
    private EditText signup_phone;
    private EditText signup_name;
    private EditText signup_date;
    private Spinner spinner;
    private EditText signup_question;

    private Button signup_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup_page);

        spinner = findViewById(R.id.spinner1);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.question, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        signup_id = (EditText) findViewById(R.id.signup_id);
        signup_email = (EditText) findViewById(R.id.signup_email);
        signup_pwd = (EditText) findViewById(R.id.signup_pwd);
        signup_pwd2 = (EditText) findViewById(R.id.signup_pwd2);
        signup_phone = (EditText) findViewById(R.id.signup_phone);
        signup_name = (EditText) findViewById(R.id.signup_name);
        signup_date = (EditText) findViewById(R.id.signup_date);
        signup_question = (EditText) findViewById(R.id.question);
        signup_button = (Button) findViewById(R.id.signup_button);
        back = (ImageView) findViewById(R.id.back);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        signup_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String id = signup_id.getText().toString().trim();
                String email = signup_email.getText().toString().trim();
                String pwd = signup_pwd.getText().toString().trim();
                String pwdcheck = signup_pwd2.getText().toString().trim();
                String phone = signup_phone.getText().toString().trim();
                String name = signup_name.getText().toString().trim();
                String date = signup_date.getText().toString().trim();
                String question = signup_question.getText().toString().trim();
                String spinner1 = spinner.getSelectedItem().toString().trim();

                if (id.isEmpty() || email.isEmpty() || pwd.isEmpty() || pwdcheck.isEmpty() || phone.isEmpty() || name.isEmpty() || date.isEmpty() || spinner1.equals("- 질문을 선택해주세요 -") || question.isEmpty()) {
                    Toast.makeText(Sign_UP.this, "정보를 입력해주세요.", Toast.LENGTH_SHORT).show();
                } else {
                    if (pwd.equals(pwdcheck)) {
                        if (!email.contains("@") || !email.contains(".com")) {
                            Toast.makeText(Sign_UP.this, "이메일에 @ 및 .com을 포함시키세요.", Toast.LENGTH_SHORT).show();
                        } else if (pwd.length() <= 5) {
                            Toast.makeText(Sign_UP.this, "비밀번호를 6자리 이상 입력해주세요.", Toast.LENGTH_SHORT).show();
                        } else if (phone.contains("-") || !(Integer.parseInt(String.valueOf(phone.charAt(1))) == 1)) {
                            Toast.makeText(Sign_UP.this, "올바른 전화번호 형식으로 입력해주세요..", Toast.LENGTH_SHORT).show();
                        } else if (date.length() <= 7 || Integer.parseInt(String.valueOf(date.charAt(0))) >= 3 || Integer.parseInt(String.valueOf(date.charAt(4))) > 1 || Integer.parseInt(String.valueOf(date.charAt(5))) == 0 || Integer.parseInt(String.valueOf(date.charAt(6))) > 3 || (Integer.parseInt(String.valueOf(date.charAt(6))) == 3 && Integer.parseInt(String.valueOf(date.charAt(7))) > 1 || (Integer.parseInt(String.valueOf(date.charAt(4))) == 1 && Integer.parseInt(String.valueOf(date.charAt(5))) > 2) || Integer.parseInt(String.valueOf(date.charAt(7))) == 0)) {
                            Toast.makeText(Sign_UP.this, "생년월일 8자 이상 및 올바르게 입력하세요.", Toast.LENGTH_SHORT).show();
                        } else {
                            Sign_UP_Task sign_up_task = new Sign_UP_Task();
                            sign_up_task.execute("http://" + IP_ADDRESS + ":" + IP_PORTS + "/sign_up.php", id, email, pwd, phone, name, date, spinner1, question);
                        }
                    } else {
                        Toast.makeText(Sign_UP.this, "비밀번호가 일치 하지 않습니다.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    class Sign_UP_Task extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(Sign_UP.this, "Please Wait", null, true, true);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressDialog.dismiss();
            Log.d(TAG, "POST response  - " + result);

            if (result.contains("sign up success")) {
                Toast.makeText(Sign_UP.this, "회원가입에 성공하셨습니다.", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(Sign_UP.this, "회원가입에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected String doInBackground(String... params) {
            String serverURL = (String) params[0];
            String userid = (String) params[1];
            String useremail = (String) params[2];
            String userpw = (String) params[3];
            String userphone = (String) params[4];
            String username = (String) params[5];
            String userdate = (String) params[6];
            String userquestion = (String) params[7];
            String useranswer = (String) params[8];

            String postParameters = "userid=" + userid + "&useremail=" + useremail + "&userpw=" + userpw + "&userphone=" + userphone + "&username=" + username + "&userdate=" + userdate + "&userquestion=" + userquestion + "&useranswer=" + useranswer;

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
                Log.d("sign up php 값 :", sb.toString());
                return sb.toString();
            } catch (Exception e) {
                Log.d(TAG, "Sign up Error : " + e.getMessage());
                return "Sign up Error : " + e.getMessage();
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }
}