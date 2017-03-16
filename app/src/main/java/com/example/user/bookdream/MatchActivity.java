package com.example.user.bookdream;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
/**
 * 프로젝트명 : Book:DREAM
 * 시      기 : 성공회대학교 글로컬IT학과 2016년도 2학기 실무프로젝트
 * 팀      원 : 200934013 서동형, 201134031 최형근, 201434031 이보라미
 *
 * 매칭에 사용되는 액티비티.
 * 요청 또는 드림에서 책 매칭이 완료된 경우 이 액티비티를 통해서 매칭 처리가 됨
 **/
public class MatchActivity extends AppCompatActivity {
    private Button confirmBtn,cancelBtn;
    private TextView tx;
    private MatchPrecentCondionInfoAsyncThread backgroundMatchPrecentConditionThread;


    final Handler handler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            Toast.makeText(getApplicationContext(), "완료처리 되었습니다!", Toast.LENGTH_LONG).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match);
        tx = (TextView) findViewById(R.id.match_message_tx);
        final Intent intent = getIntent();
        tx.setText(intent.getStringExtra("id") + " " +intent.getStringExtra("name") +"님이\n"+ intent.getStringExtra("title")+" 책을 제공한다고 합니다.\n"
                + "일시 : "+intent.getStringExtra("date") +" 시간 : "+ intent.getStringExtra("time") +"\n 장소 : " +intent.getStringExtra("place") +"\n"
                + " 내용 : " + intent.getStringExtra("content") + "\n 연락처 : " + intent.getStringExtra("phone")) ;

        confirmBtn = (Button) findViewById(R.id.confirm_btn);
        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backgroundMatchPrecentConditionThread = new MatchPrecentCondionInfoAsyncThread();
                backgroundMatchPrecentConditionThread.execute(intent.getStringExtra("no"));
                finish();
            }
        });
        cancelBtn = (Button) findViewById(R.id.cancel_btn);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    public class MatchPrecentCondionInfoAsyncThread extends AsyncTask<String, String, String> {
        // Thread를 시작하기 전에 호출되는 함수
        protected void onPreExecute() {
            super.onPreExecute();
        }

        // Thread의 주요 작업을 처리 하는 함수
        // Thread를 실행하기 위해 excute(~)에서 전달한 값을 인자로 받습니다.
        protected String doInBackground(String...args) {
            URL url = null;
            HttpURLConnection conn = null;
            String urlStr = "";
            ArrayList<String> list = new ArrayList<>();
            list.add(args[0]);
            urlStr = "http://"+getString(R.string.ip_address)+":8080/BookDreamServerProject/removeCompletePresentConditionInfo";

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

                ObjectOutputStream oos =new ObjectOutputStream(conn.getOutputStream());
                oos.writeObject(list);
                oos.flush();
                oos.close();
                if (conn.getResponseMessage().equals("OK")) { // 서버가 받았다면
                    int responseCode = conn.getResponseCode();
                    Log.d("D", responseCode+"");
                    if (responseCode == HttpURLConnection.HTTP_OK) {    // 송수신이 잘되면 - 데이터를 받은 것입니다.
                        Log.d("coded", "들어옴");
                        Message msg = handler.obtainMessage();
                        handler.sendMessage(msg);
                    }
                    conn.disconnect();
                }
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Error 발생", Toast.LENGTH_SHORT).show();
                Log.e("ERR", "MatchPrecentCondionInfoAsyncThread ERR : " + e.getMessage());
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
