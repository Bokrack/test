package com.example.user.bookdream;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * 프로젝트명 : Book:DREAM
 * 시      기 : 성공회대학교 글로컬IT학과 2016년도 2학기 실무프로젝트
 * 팀      원 : 200934013 서동형, 201134031 최형근, 201434031 이보라미
 *
 * 안드로이드의 서비스를 백그라운드로 돌려서 소켓서버와 계속 연결상태를 유지한다.
 * 서버가 보낼 데이터가 있으면 전송하고 연결된 소켓 클라이언트의 리시브 쓰레드를 통해 사진을 받는다.
 * 사진을 받고 다시 재연결을 시도해 연결상태를 유지한다.
 **/

public class MainService extends Service {
    private static int port = 5001;
    private String ipText; // IP지정으로 사용시에 쓸 코드
    private SocketClient client;
    private ReceiveThread receive;
    private Socket socket;
    private LinkedList<SocketClient> threadList;

    /*
        백그라운드에서 DB와 상호작용하는 것을 컨트롤하는 핸들러
     */
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            Bundle b = msg.getData();
            String title=b.getString("title")+" DREAM:니다!";

            NotificationManager manager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(MainService.this);
            builder.setSmallIcon(R.drawable.appicon)
                    .setContentTitle(title)
                    .setContentText("메시지가 왔어요!")
                    .setAutoCancel(true) // 알림바에서 자동 삭제
                    .setVibrate(new long[]{1000,2000,1000,3000,1000,4000}); // vibrate : "쉬고, 울리고" 패턴 (기준단위 : ms)
                                                                            // AndroidManifest.xml에 진동 권한 필요

            // 알람 클릭시 MainActivity를 화면에 띄운다.
            Intent intent = new Intent(getApplicationContext(),MatchActivity.class);
            if (intent == null) {
                intent = new Intent();
            }
            intent.putExtra("title", b.getString("title"));
            intent.putExtra("id", b.getString("id"));
            intent.putExtra("name", b.getString("name"));
            intent.putExtra("date", b.getString("date"));
            intent.putExtra("time", b.getString("time"));
            intent.putExtra("place", b.getString("place"));
            intent.putExtra("content", b.getString("content"));
            intent.putExtra("phone", b.getString("phone"));
            PendingIntent pIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(pIntent);
            Context hostActivity = MainService.this;
            SharedPreferences sf = PreferenceManager.getDefaultSharedPreferences(hostActivity);
            String ring = sf.getString("phone_ring", "");
            if (!ring.equals("")) {
                if (!ring.equals("no")) {
                    builder.setSound( Uri.parse(ring));
                }
             }else {
                Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                builder.setSound(alarmSound);
            }
            manager.notify(1, builder.build());
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("서비스 테스트", "onStartCommand 시작");
        threadList = new LinkedList<SocketClient>();
        client = new SocketClient(ipText, port+"");
        client.start();
        Log.i("서비스 테스트", "onStartCommand");
        return super.onStartCommand(intent, flags,startId);
    }

    @Override
    public void onCreate(){
        ipText=getString(R.string.ip_address);
    }

    @Override
    public void onDestroy() {
        Log.i("서비스 테스트", "삭제됬다 ㅜㅜ");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    class SocketClient extends Thread {
        boolean threadAlive;
        String ip;
        String port;

        public SocketClient(String ip, String port) {
            threadAlive = true;
            this.ip = ip;
            this.port = port;
            Log.i("서비스 테스트", "onStartCommand 연결준비 ");
        }

        @Override
        public void run() {
            Log.i("서비스 테스트", "onStartCommand 연결ㄱㄱ ");
            try {
                // 연결후 바로 ReceiveThread 시작
                socket = new Socket(ip, Integer.parseInt(port));
                Log.i("서비스 테스트", "onStartCommand 연결완료 ");
                receive = new ReceiveThread(socket);
                receive.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*
        서버로 부터 보내는 사진을 수신하는 쓰레드
     */
    class ReceiveThread extends Thread {
        private Socket socket = null;
        DataInputStream input;
        public ReceiveThread(Socket socket) {
            this.socket = socket;
            try {
                // 소켓의 inputsteam을 따로 변수에 저장
                input = new DataInputStream(socket.getInputStream());
            } catch (Exception e) {}
        }

        public void run() {
            Log.d("go", "시작");

            try {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

            final DBManager dbManager = new DBManager(getApplicationContext(), "App_Data.db", null, 1);
            final HashMap<String, String> dataMap = dbManager.getResult();
                oos.writeObject(dataMap);
                oos.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
            while (true) {
                try {

                    ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                    HashMap<String, String> stringDataMap = (HashMap<String, String>)ois.readObject();
                    Log.d("Socket_cnt", stringDataMap.size()+"");
                    if(stringDataMap.size() == 0) {     // 서버로 부터 데이터가 없으면 바로 continue
                        /*
                            지속적으로 소켓서버의 상태를 확인
                         */
                        continue;
                    }
                    Message msg = mHandler.obtainMessage();     // Message 객체 생성
                    Bundle b = new Bundle();
                    b.putString("title", stringDataMap.get("title"));
                    b.putString("date", stringDataMap.get("date"));
                    b.putString("time", stringDataMap.get("time"));
                    b.putString("place", stringDataMap.get("place"));
                    b.putString("id", stringDataMap.get("supply_id"));
                    b.putString("name", stringDataMap.get("supply_name"));
                    b.putString("content", stringDataMap.get("content"));
                    b.putString("phone", stringDataMap.get("phone"));
                    msg.setData(b);
                    mHandler.sendMessage(msg); // 핸들러로 데이터 전송
                    /*Thread.sleep(5000);   // 100초 슬립*/
                } catch (Exception e) {
                    e.printStackTrace();
            }
            }
        }
    }
}
