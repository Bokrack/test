package com.example.user.bookdream;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * 프로젝트명 : Book:DREAM
 * 시      기 : 성공회대학교 글로컬IT학과 2016년도 2학기 실무프로젝트
 * 팀      원 : 200934013 서동형, 201134031 최형근, 201434031 이보라미
 *
 * 처음 애플리케이션을 실행하면 보이는 화면인 로그인 액티비티
 * 사용자는 이 액티비티에서 회원가입을 할 수 있고, 로그인(자동로그인 포함)을 할 수 있다.
 * 로그인에 성공 한다면, 현재의 로그인 액티비티는 바로 파괴되고 메인 액티비티가 불려진다.
 **/
public class LoginActivity extends AppCompatActivity {
    private TextView join;
    private EditText textViewId, textViewPw;
    private CheckBox autoLogin;
    private SharedPreferences loginPref;
    private SharedPreferences.Editor editor;

    private TextInputLayout hintJoinId, hintJoinPw, hintJoinPwCheck, hintJoinName, hintJoinEmail;
    private EditText enterJoinId, enterJoinPw, enterJoinPwCheck, enterJoinName, enterJoinEmail;
    private TextView joinCheckId;
    private ArrayList<String> memberIdList =new ArrayList<>();
    private boolean IdFlag = false;
    private AddMemberAsyncThread backgroundAddThread;
    private LoadMemberInfoAsyncThread backgroundLoadThread;
    private LoginAsyncThread backgroundLoginThread;

    /*
        사용자가 입력한 아이디, 비밀번호 정보를 DB에 저장된 회원정보와 비교해서 로그인을 하는 핸들러
     */
    final Handler handler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            if(msg.obj.equals("success")) { // 로그인에 성공한 경우
                if(autoLogin.isChecked()) {
                    Bundle b = msg.getData();
                    loginPref = getSharedPreferences("login_info", MODE_PRIVATE);
                    editor = loginPref.edit();
                    editor.putString("id", b.getString("id")); // 사용자에게 입력받은 ID를 저장
                    editor.putString("pw", b.getString("pw")); // 사용자에게 입력받은 PW를 저장
                    editor.commit();
                }
                Toast.makeText(getApplicationContext(), "로그인 되었습니다.", Toast.LENGTH_SHORT).show();
            }
            else { // 로그인에 실패한 경우 실패 메세지를 띄운다.
                Toast.makeText(getApplicationContext(), "학번과 비밀번호를 잘못입력하셨습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        // Book:DREAM 애플리케이션을 사용하기 위해서 필요한 권한을 사용자에게 요청한다.
        // 만약 권한이 허가 되지 않으면 애플리케이션의 정상적인 구동이 안된다.
        PermissionListener permissionlistener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {}

            @Override
            public void onPermissionDenied(ArrayList<String> deniedPermissions) {
                Toast.makeText(getApplicationContext(), "애플리케이션 권한을 확인해주세요.\n" + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
                finish();
            }
        };

        // 사용자에게 띄워지는 권한 허가 메세지 [READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE, CALL_PHONE]
        new TedPermission(this)
                .setPermissionListener(permissionlistener)
                .setRationaleMessage("Book:DREAM을 사용하기 위해서는 저장공간에 대한 권한이 있어야 합니다.")
                .setRationaleConfirmText("허락")
                .setDeniedCloseButtonText("거절")
                .setDeniedMessage("거부하시면 정상적인 어플리케이션 사용이 불가능합니다.\n[설정] > [권한]에서도 권한을 허용할 수 있습니다.")
                .setPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CALL_PHONE)
                .check();

        join = (TextView)findViewById(R.id.join);
        join.setOnClickListener(joinClickListener);

        textViewId = (EditText)findViewById(R.id.textView_id);
        textViewPw = (EditText)findViewById(R.id.textView_pw);

        autoLogin = (CheckBox) findViewById(R.id.auto_login_check);

        // 사용자가 자동로그인을 한 경우, 첫 로그인 이후 애플리케이션 구동을 한다면 자동로그인이 된다.
        loginPref = getSharedPreferences("login_info", MODE_PRIVATE);
        if(!loginPref.getString("id", "").equals("") && !loginPref.getString("pw", "").equals("")) {
            Toast.makeText(getApplicationContext(), "자동로그인 되었습니다.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    /*
        어플리케이션 종료시 쓰레드의 종료를 요청하는 메소드
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (backgroundAddThread.getStatus() == AsyncTask.Status.RUNNING) {
                backgroundAddThread.cancel(true);
            }

            if (backgroundLoadThread.getStatus() == AsyncTask.Status.RUNNING) {
                backgroundLoadThread.cancel(true);
            }

            if (backgroundLoginThread.getStatus() == AsyncTask.Status.RUNNING) {
                backgroundLoginThread.cancel(true);
            }
        } catch (Exception e) {}
    }

    /*
        사용자가 로그인 버튼을 누른 경우, 입력된 아이디와 비밀번호를 체크한다.
     */
    public void enter_log_in(View view) {
        String enteredId = textViewId.getText().toString();
        String enteredPw = textViewPw.getText().toString();

        if (enteredId != null && enteredId.length() > 0 && enteredPw != null && enteredPw.length() > 0) {
                // 로그인 정보가 맞나 확인 후 로그인해야 함.
                backgroundLoginThread = new LoginAsyncThread();
                backgroundLoginThread.execute(enteredId, enteredPw);
        } else {
            Toast.makeText(getApplicationContext(), "학번과 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
        }
    }

    /*
        사용자가 회원가입 버튼을 누른 경우, 회원 가입 다이얼로그가 띄워진다.
     */
    TextView.OnClickListener joinClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            LayoutInflater inflater = LoginActivity.this.getLayoutInflater();
            View alertLayout = inflater.inflate(R.layout.layout_join, null);

            hintJoinId = (TextInputLayout)alertLayout.findViewById(R.id.hint_join_id);
            hintJoinPw = (TextInputLayout)alertLayout.findViewById(R.id.hint_join_pw);
            hintJoinPwCheck = (TextInputLayout)alertLayout.findViewById(R.id.hint_join_pw_check);
            hintJoinName = (TextInputLayout)alertLayout.findViewById(R.id.hint_join_name);
            hintJoinEmail = (TextInputLayout)alertLayout.findViewById(R.id.hint_join_email);

            enterJoinId = (EditText)alertLayout.findViewById(R.id.enter_join_id);
            enterJoinPw = (EditText)alertLayout.findViewById(R.id.enter_join_pw);
            enterJoinPwCheck = (EditText)alertLayout.findViewById(R.id.enter_join_pw_check);
            enterJoinName = (EditText)alertLayout.findViewById(R.id.enter_join_name);
            enterJoinEmail = (EditText)alertLayout.findViewById(R.id.enter_join_email);

            joinCheckId = (TextView)alertLayout.findViewById(R.id.join_check_id);
            joinCheckId.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        String id = enterJoinId.getText().toString();

                        // 사용자가 아이디 중복확인을 누른 경우 가입된 회원 리스트에서 중복 된 사용자가 없는지 체크한다.
                        if (joinIdCheck(id)) {
                            IdFlag = true;
                            String msg = "회원가입이 가능한 학번입니다.";
                            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                        } else {
                            IdFlag = false;
                            String msg = "입력된 학번은 사용중입니다. 확인 후 관리자에게 문의해주세요.";
                            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        IdFlag = false;
                        Toast.makeText(getApplicationContext(), "Error 발생", Toast.LENGTH_SHORT).show();
                        Log.e("ERR", "ID CHECK ERR : " + e.getMessage());
                    }
                }
            });

            AlertDialog.Builder buider = new AlertDialog.Builder(LoginActivity.this); //AlertDialog.Builder 객체 생성
            buider.setCancelable(false);
            buider.setView(alertLayout); //위에서 inflater가 만든 dialogView 객체 세팅 (Customize)

            buider.setPositiveButton("확인", null); // 회원 가입에 대한 리스너는 아래서 붙여준다.
            buider.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            final AlertDialog dialog = buider.create();

            // 회원 가입에 대한 리스너
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(final DialogInterface d) {
                    backgroundLoadThread = new LoadMemberInfoAsyncThread();
                    backgroundLoadThread.execute();
                    Button postive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    postive.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            boolean flag = true;
                            String dbId = null, dbPw = null, dbName = null, dbEmail = null;
                            try {
                                if (flag) { // 아이디 중복체크를 했는지 체크
                                    if (IdFlag) {
                                        flag = true;
                                    } else {
                                        flag = false;
                                        hintJoinId.setError("중복 확인");
                                        requestFocus(enterJoinId);
                                    }
                                }

                                if (flag) { // 아이디는 성공회대학교 글로컬IT학과 학번이기 때문에, 정확히 9자 인지 체크
                                    dbId = enterJoinId.getText().toString();
                                    if(dbId.length() == 9) {
                                        hintJoinId.setErrorEnabled(false);
                                        flag = true;
                                    } else {
                                        flag = false;
                                        hintJoinId.setError("학번");
                                        requestFocus(enterJoinId);
                                    }
                                }

                                if (flag) { // 비밀번호 입력을 했는지 체크
                                    if (enterJoinPw.getText().toString().length() > 0) {
                                        flag = true;
                                        hintJoinPw.setErrorEnabled(false);
                                    } else {
                                        flag = false;
                                        hintJoinPw.setError("비밀번호");
                                        requestFocus(enterJoinPw);
                                    }
                                }

                                if (flag) { // 비밀번호 확인을 입력했는지 체크
                                    if (enterJoinPwCheck.getText().toString().length() > 0) {
                                        flag = true;
                                    } else {
                                        flag = false;
                                        hintJoinPwCheck.setError("비밀번호 확인");
                                        requestFocus(enterJoinPwCheck);
                                    }
                                }

                                if (flag) { // 비밀번호와 비밀번호확인이 동일한지 체크
                                    if (enterJoinPwCheck.getText().toString().equals(enterJoinPw.getText().toString())) {
                                        flag = true;
                                        dbPw = enterJoinPw.getText().toString();
                                        hintJoinPwCheck.setErrorEnabled(false);
                                    } else {
                                        flag = false;
                                        hintJoinPwCheck.setError("비밀번호가 동일하지 않습니다.");
                                        requestFocus(enterJoinPwCheck);
                                    }
                                }

                                if (flag) { // 이름을 입력했는지 체크
                                    dbName = enterJoinName.getText().toString();
                                    if (dbName.length() > 0) {
                                        flag = true;
                                        hintJoinName.setErrorEnabled(false);
                                    } else {
                                        flag = false;
                                        hintJoinName.setError("이름");
                                        requestFocus(enterJoinName);
                                    }
                                }

                                if (flag) { // 이메일 입력과 이메일 형식으로 입력했는지 체크
                                    dbEmail = enterJoinEmail.getText().toString();
                                    if(dbEmail.length() > 0 && android.util.Patterns.EMAIL_ADDRESS.matcher(dbEmail).matches()) {
                                        flag = true;
                                        hintJoinEmail.setErrorEnabled(false);
                                    } else {
                                        flag = false;
                                        hintJoinEmail.setError("메일 주소");
                                        requestFocus(enterJoinEmail);
                                    }
                                }

                                if(flag) { // 모든 입력이 정상적이라면 회원 DB에 입력 후 회원가입 다이얼로그를 종료한다.
                                    insertMemberDB(dbId, dbPw, dbName, dbEmail);
                                    dialog.dismiss();
                                }
                            } catch (Exception e) {
                                Toast.makeText(getApplicationContext(), "Error 발생", Toast.LENGTH_SHORT).show();
                                Log.e("ERR", "JOIN ERR : " + e.getMessage());
                            }
                        }
                    });
                }
            });

            //Dialog의 바깥쪽을 터치했을 때 Dialog를 없앨지 설정
            dialog.setCanceledOnTouchOutside(false);//없어지지 않도록 설정

            //Dialog 보이기
            dialog.show();
        }
    };

    /*
        사용자가 회원가입 시 잘못된 입력을 한 경우, 사용자에게 알려주기 위해 표시하는 메소드드     */
    private void requestFocus(View view) {
        if (view.requestFocus()) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    /*
        회원 중복이 있는지 체크하는 메소드
     */
    public boolean joinIdCheck(String id) {
        if( memberIdList.size() ==0) {
            return true;
        }

        for(int i = 0; i < memberIdList.size(); i++)
        {
            String userId = memberIdList.get(i);
            if(userId.equals(id)) {
                return false;
            }
        }
        return true;
    }

    /*
        사용자가 회원가입을 모두 마친 경우, 회원 정보를 DB에 입력하는 메소드
     */
    public void insertMemberDB(String id, String pw, String name, String email) {
        Toast.makeText(getApplicationContext(), "회원가입 성공", Toast.LENGTH_SHORT).show();
        backgroundAddThread = new AddMemberAsyncThread();
        backgroundAddThread.execute(id, pw, name, email);
    }

    public class AddMemberAsyncThread extends AsyncTask<String, String, String> {
        // Thread를 시작하기 전에 호출되는 함수
        protected void onPreExecute() {
            super.onPreExecute();

        }

        // Thread의 주요 작업을 처리 하는 함수
        // Thread를 실행하기 위해 excute(~)에서 전달한 값을 인자로 받습니다.
        protected String doInBackground(String... args) {
            URL url = null;
            HttpURLConnection conn = null;
            String urlStr = "";
            String param = null;

            urlStr = "http://"+getString(R.string.ip_address)+":8080/BookDreamServerProject/addMemberInfo";

            try {
                url = new URL(urlStr);
                Log.d("test", urlStr);

                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Cache-Control", "no-cache");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                HashMap<String, String> stringDataMap = new HashMap<String, String>();
                stringDataMap.put("id", args[0]);
                stringDataMap.put("pw", args[1]);
                stringDataMap.put("name", args[2]);
                stringDataMap.put("email",args[3]);
                ObjectOutputStream oos =new ObjectOutputStream(conn.getOutputStream());
                oos.writeObject(stringDataMap);
                oos.flush();
                oos.close();

                if (conn.getResponseMessage().equals("OK")) { // 서버가 받았다면
                    Log.d("test", "gogo");
                }
                conn.disconnect();
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Error 발생", Toast.LENGTH_SHORT).show();
                Log.e("ERR", "AddMemberAsyncThread ERR : " + e.getMessage());
            }

            return "";
        }

        // doInBackground(~)에서 호출되어 주로 UI 관련 작업을 하는 함수
        protected void onProgressUpdate(String... progress) {}

        // Thread를 처리한 후에 호출되는 함수
        // doInBackground(~)의 리턴값을 인자로 받습니다.
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }

        // AsyncTask.cancel(true) 호출시 실행되어 thread를 취소 합니다.
        protected void onCancelled() {
            super.onCancelled();
        }
    }

    public class LoadMemberInfoAsyncThread extends AsyncTask<Void, String, String> {
        // Thread를 시작하기 전에 호출되는 함수
        protected void onPreExecute() {
            super.onPreExecute();
        }

        // Thread의 주요 작업을 처리 하는 함수
        // Thread를 실행하기 위해 excute(~)에서 전달한 값을 인자로 받습니다.
        protected String doInBackground(Void...args) {
            URL url = null;
            HttpURLConnection conn = null;
            String urlStr = "";

            urlStr = "http://"+getString(R.string.ip_address)+":8080/BookDreamServerProject/requestMemberInfo";

            try {
                url = new URL(urlStr);
                Log.d("test", urlStr);

                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Cache-Control", "no-cache");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                int responseCode = conn.getResponseCode();
                Log.d("D", responseCode+"");
                if (responseCode == HttpURLConnection.HTTP_OK) {    // 송수신이 잘되면 - 데이터를 받은 것입니다.
                    Log.d("coded", "들어옴");
                    ObjectInputStream ois = new ObjectInputStream(conn.getInputStream());
                    ArrayList<String> idList = (ArrayList<String>)ois.readObject();
                    ois.close();

                    for(int i=0; i<idList.size(); i++) {
                        memberIdList.add(idList.get(i));
                    }
                    conn.disconnect();
                }
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Error 발생", Toast.LENGTH_SHORT).show();
                Log.e("ERR", "LoadMemberInfoAsyncThread ERR : " + e.getMessage());
            }

            return "";
        }

        // doInBackground(~)에서 호출되어 주로 UI 관련 작업을 하는 함수
        protected void onProgressUpdate(String... progress) {}

        // Thread를 처리한 후에 호출되는 함수
        // doInBackground(~)의 리턴값을 인자로 받습니다.
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }

        // AsyncTask.cancel(true) 호출시 실행되어 thread를 취소 합니다.
        protected void onCancelled() {
            super.onCancelled();
        }
    }

    public class LoginAsyncThread extends AsyncTask<String, String, String> {
        // Thread를 시작하기 전에 호출되는 함수
        protected void onPreExecute() {
            super.onPreExecute();
        }

        // Thread의 주요 작업을 처리 하는 함수
        // Thread를 실행하기 위해 excute(~)에서 전달한 값을 인자로 받습니다.
        protected String doInBackground(String... args) {
            URL url = null;
            HttpURLConnection conn = null;
            String urlStr = "";

            urlStr = "http://"+getString(R.string.ip_address)+":8080/BookDreamServerProject/requestLogin";

            try {
                String message="";
                url = new URL(urlStr);
                Log.d("test", urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Cache-Control", "no-cache");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                int responseCode = conn.getResponseCode();
                Log.d("D", responseCode + "");
                if (responseCode == HttpURLConnection.HTTP_OK) {    // 송수신이 잘되면 - 데이터를 받은 것입니다.
                    Log.d("coded", "들어옴");

                    ObjectInputStream ois = new ObjectInputStream(conn.getInputStream());
                    ArrayList<String> idList = (ArrayList<String>) ois.readObject();
                    ArrayList<String> pwList = (ArrayList<String>) ois.readObject();
                    ArrayList<String> nameList = (ArrayList<String>) ois.readObject();
                    ArrayList<String> emailList = (ArrayList<String>) ois.readObject();
                    ois.close();

                    Bundle loginInfo= new Bundle();        // id,pw를 저장하는 임시 저장소
                    for(String pw:pwList) {
                        Log.d("pwList", pw);
                    }
                    int i = 0;
                    for (String id : idList) {
                        if (id.equals(args[0])) {
                            Log.d("id", id +i);
                            if (pwList.get(i).equals(args[1])) {
                                loginInfo.putString("id", id);
                                loginInfo.putString("pw", pwList.get(i));
                                final DBManager dbManager = new DBManager(getApplicationContext(), "App_Data.db", null, 1);
                                Log.d("pw", pwList.get(i));
                                dbManager.insert(idList.get(i), nameList.get(i), emailList.get(i));
                                Intent intent = new Intent(getApplicationContext(), MainActivity.class);

                                message="success";
                                startActivity(intent);
                                finish();
                            }
                            break;
                        }
                        i++;
                    }
                    Message msg = handler.obtainMessage();
                    if(loginInfo.size()>0) {
                        msg.setData(loginInfo);
                    }
                    msg.obj= message;
                    handler.sendMessage(msg);
                    conn.disconnect();
                }
            }  catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Error 발생", Toast.LENGTH_SHORT).show();
                Log.e("ERR", "LoginAsyncThread ERR : " + e.getMessage());
            }
            return "";
        }

        // doInBackground(~)에서 호출되어 주로 UI 관련 작업을 하는 함수
        protected void onProgressUpdate(String... progress) {}

        // Thread를 처리한 후에 호출되는 함수
        // doInBackground(~)의 리턴값을 인자로 받습니다.
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }

        // AsyncTask.cancel(true) 호출시 실행되어 thread를 취소 합니다.
        protected void onCancelled() {
            super.onCancelled();
        }
    }
}