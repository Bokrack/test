package com.example.user.bookdream;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import static android.content.Context.MODE_PRIVATE;

/**
 * 프로젝트명 : Book:DREAM
 * 시      기 : 성공회대학교 글로컬IT학과 2016년도 2학기 실무프로젝트
 * 팀      원 : 200934013 서동형, 201134031 최형근, 201434031 이보라미
 *
 * 메인 프래그먼트를 구성
 * 로그인된 회원 정보와 해당 사용자의 요청, 공급 현황을 관리한다.
 **/
public class HomeFragment extends Fragment {
    private TextView dreamTx, idTx, nameTx, emailTx, requestTx, supplyTx;
    private TextView dreamBtn, logoutBtn;
    private LoadAllPrecentCondionInfoAsyncThread backgroundAllLoadPrecentConditionThread;
    private LoadPrecentCondionInfoAsyncThread backgroundLoadPrecentConditionThread;
    private CompletePrecentCondionInfoAsyncThread backgroundCompletePrecentConditionThread;
    private SwipeRefreshLayout mSwipeRefreshLayout = null;

    /*
        사용자의 회원 정보, 현재 현황에 대해 DB와 상호작용 하는 핸들러
     */
    final Handler handler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            Bundle b = msg.getData();

            // 회원 정보 관련 상호작용
            if(b.size() == 0) {
                HashMap<String, String> infoDataMap = (HashMap<String, String>) msg.obj;
                requestTx.setText("요청 현황 : " +" " + infoDataMap.get("demand_cnt")+"건");
                supplyTx.setText("공급 현황 : " + " " + infoDataMap.get("supply_cnt")+"건");
                int dreamCnt = Integer.parseInt(infoDataMap.get("dream_cnt"));
                if (dreamCnt == 0) {
                    dreamBtn.setVisibility(View.GONE);
                }
                dreamTx.setText("DREAM: 현황" +" " + dreamCnt+"건");
                return;
            }

            // 현재 현황 관련 상호작용
            if(b.getString("present_condition") != null) {
                if (b.getString("result").equals("success")) {
                    Toast.makeText(getActivity(), "BOOK:DREAM을 완료했습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "이미 처리 하신 상태입니다.", Toast.LENGTH_SHORT).show();
                }
            } else {

                final int[] no = new int[b.size()];
                Log.d("size", b.size()+"");
                for(int i = 0; i < b.size(); i++) {
                    no[i] = Integer.parseInt(b.getString(i+""));
                }
                final int[] idx = {0};
                final String[] info = (String[])msg.obj;
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
                alertDialogBuilder.setTitle("책을 제공할 학생을 선택하세요.");
                alertDialogBuilder.setSingleChoiceItems(info, 0, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                idx[0] =id; // 누른 옵션 항목의 id를 저장
                            }
                }).setPositiveButton("완료", new DialogInterface.OnClickListener(){
                    // 완료 버튼을 누른 경우 layout_supply_precent_condition_dialog와 관련된 정보 컨트롤
                    @Override
                    public void onClick(DialogInterface dialog, int witch) {
                        LayoutInflater inflater = LayoutInflater.from(getContext());
                        final View dialogView= inflater.inflate(R.layout.layout_supply_precent_condition_dialog,null);
                        final DatePicker datePicker = (DatePicker)  dialogView.findViewById(R.id.datePicker);
                        final TimePicker timePicker = (TimePicker)  dialogView.findViewById(R.id.timePicker);
                        final EditText edit_where= (EditText)dialogView.findViewById(R.id.edit_where);
                        final EditText edit_content= (EditText)dialogView.findViewById(R.id.edit_content);
                        final EditText edit_phone= (EditText)dialogView.findViewById(R.id.edit_phone);
                        edit_phone.addTextChangedListener(new PhoneNumberFormattingTextWatcher());

                        // 멤버의 세부내역 입력 Dialog 생성 및 보이기 -> 선배가 후배에게 책을 주는 경우(from 드림 프래그먼트)
                        final AlertDialog.Builder builder= new AlertDialog.Builder(getContext()); // AlertDialog.Builder 객체 생성
                        builder.setTitle("BOOK:DREAM"); // Dialog 제목
                        builder.setMessage("각각의 정보를 작성해주세요.\n완료를 누르면 취소가 되지않습니다.");
                        builder.setIcon(android.R.drawable.ic_menu_add); // 제목옆의 아이콘 이미지(원하는 이미지 설정)
                        builder.setView(dialogView); // 위에서 inflater가 만든 dialogView 객체 세팅 (Customize)
                        builder.setPositiveButton("DREAM", new DialogInterface.OnClickListener() {
                            // Dialog에 "Complite"라는 타이틀의 버튼을 설정
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // 선배가 설정한 datePicker 데이터를 받아옴
                                String date = String.format("%d - %d - %d", datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                                // 선배가 설정한 timePicker 데이터를 받아옴
                                String time="";
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    time = String.format("%d 시 %d 분", timePicker.getHour(), timePicker.getMinute());
                                } else {
                                    time = String.format("%d 시 %d 분", timePicker.getCurrentHour(), timePicker.getCurrentMinute());
                                }

                                backgroundCompletePrecentConditionThread = new CompletePrecentCondionInfoAsyncThread();
                                backgroundCompletePrecentConditionThread.execute(no[idx[0]]+"",info[idx[0]],
                                        date, time, edit_where.getText().toString(),
                                        edit_content.getText().toString(), edit_phone.getText().toString());
                            }
                        });

                        // Dialog에 "CANCEL"이라는 타이틀의 버튼을 설정
                        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                        // 설정한 값으로 AlertDialog 객체 생성
                        AlertDialog completeDialog=builder.create();
                        // Dialog의 바깥쪽을 터치했을 때 Dialog를 없앨지 설정
                        completeDialog.setCanceledOnTouchOutside(false);//없어지지 않도록 설정
                        // Dialog 보이기
                        completeDialog.show();
                    }
                }).setNegativeButton("취소", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create().show();
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final DBManager dbManager = new DBManager(getContext(), "App_Data.db", null, 1);
        final HashMap <String, String> dataMap = dbManager.getResult();
        final View rootView = inflater.inflate(R.layout.layout_home_fragment, container, false);

        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.homeswipe_layout);
        mSwipeRefreshLayout.setOnRefreshListener(refreshListener);

        idTx = (TextView) rootView.findViewById(R.id.id_tx);
        nameTx = (TextView) rootView.findViewById(R.id.name_tx);
        emailTx = (TextView) rootView.findViewById(R.id.email_tx);

        idTx.setText(idTx.getText().toString() +" "+dataMap.get("id"));
        nameTx.setText(nameTx.getText().toString() +" "+dataMap.get("name"));
        emailTx.setText(emailTx.getText().toString() +" "+dataMap.get("email"));
        requestTx = (TextView) rootView.findViewById(R.id.request_tx);
        supplyTx = (TextView) rootView.findViewById(R.id.supply_tx);
        dreamTx =(TextView) rootView.findViewById(R.id.dream_tx);
        dreamBtn =(TextView) rootView.findViewById(R.id.dream_btn);
        dreamBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backgroundLoadPrecentConditionThread =  new LoadPrecentCondionInfoAsyncThread();
                backgroundLoadPrecentConditionThread.execute(dataMap.get("id")); // 후배에게 드림 메세지 전송
            }
        });

        logoutBtn = (TextView) rootView.findViewById(R.id.logout_btn);
        logoutBtn.setOnClickListener(new View.OnClickListener() { // 회원 로그아웃
            @Override
            public void onClick(View v) {
                final DBManager dbManager = new DBManager(getContext(), "App_Data.db", null, 1);
                SharedPreferences pref = getContext().getSharedPreferences("login_info", MODE_PRIVATE);
                dbManager.delete(pref.getString("id",""));
                Log.d("test", pref.getString("id",""));
                SharedPreferences.Editor editor = pref.edit();
                editor.clear();
                editor.commit();

                Toast.makeText(getContext(), "로그아웃 되었습니다. 다시 로그인 해주세요.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(getContext(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                getActivity().startActivity(intent);
                getActivity().finish();
            }
        });

        backgroundAllLoadPrecentConditionThread = new LoadAllPrecentCondionInfoAsyncThread();
        backgroundAllLoadPrecentConditionThread.execute(dataMap.get("id"), dataMap.get("name"));

        return rootView;
    }

    /*
        어플리케이션 종료시 쓰레드의 종료를 요청하는 메소드
     */
    @Override
    public void onDestroy() {        // 어플리케이션 종료시 쓰레드의 종료를 요청
        super.onDestroy();
        try {
            if (backgroundAllLoadPrecentConditionThread.getStatus() == AsyncTask.Status.RUNNING) {
                backgroundAllLoadPrecentConditionThread.cancel(true);
            }

            if (backgroundLoadPrecentConditionThread.getStatus() == AsyncTask.Status.RUNNING) {
                backgroundLoadPrecentConditionThread.cancel(true);
            }

            if (backgroundCompletePrecentConditionThread.getStatus() == AsyncTask.Status.RUNNING) {
                backgroundCompletePrecentConditionThread.cancel(true);
            }
        } catch (Exception e) {}
    }

    /*
        SListData에서 해당 정보를 찾아 다이얼로그에 정보를 띄운다.
    */
    SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            final DBManager dbManager = new DBManager(getContext(), "App_Data.db", null, 1);
            final HashMap <String, String> dataMap = dbManager.getResult();
            backgroundAllLoadPrecentConditionThread = new LoadAllPrecentCondionInfoAsyncThread();
            backgroundAllLoadPrecentConditionThread.execute(dataMap.get("id"), dataMap.get("name"));
        }
    };

    public class LoadPrecentCondionInfoAsyncThread extends AsyncTask<String, String, String> {
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
            urlStr = "http://"+getString(R.string.ip_address)+":8080/BookDreamServerProject/loadSupplyPresentConditionInfo";

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
                        ObjectInputStream ois = new ObjectInputStream(conn.getInputStream());
                        HashMap<String, String> dataMap = (HashMap<String, String>) ois.readObject();
                        ois.close();
                        Log.d("cnt", dataMap.size() + "");
                        if (dataMap.size() == 0)
                            return "";

                        final String[] info = new String[dataMap.size()/4];
                        Bundle b = new Bundle();
                        for(int i = 0; i < dataMap.size()/4; i++) {
                            b.putString(i+"", dataMap.get("no "+i));
                            info[i] = dataMap.get("title"+i)+ " " + dataMap.get("id"+i) + " " + dataMap.get("name"+i);
                        }
                        Message msg = handler.obtainMessage();
                        msg.setData(b);
                        msg.obj=info;
                        handler.sendMessage(msg);
                    }
                    conn.disconnect();
                }
            } catch (Exception e) {
                Toast.makeText(getActivity(), "Error 발생", Toast.LENGTH_SHORT).show();
                Log.e("ERR", "LoadPrecentCondionInfoAsyncThread ERR : " + e.getMessage());
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

    public class LoadAllPrecentCondionInfoAsyncThread extends AsyncTask<String, String, String> {
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
            HashMap<String, String> dataMap = new HashMap<>();
            dataMap.put("id", args[0]);
            dataMap.put("name", args[1]);
            urlStr = "http://"+getString(R.string.ip_address)+":8080/BookDreamServerProject/loadAllPresentConditionInfo";

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
                oos.writeObject(dataMap);
                oos.flush();
                oos.close();
                if (conn.getResponseMessage().equals("OK")) { // 서버가 받았다면
                    int responseCode = conn.getResponseCode();
                    Log.d("Dd", responseCode+"");
                    if (responseCode == HttpURLConnection.HTTP_OK) {    // 송수신이 잘되면 - 데이터를 받은 것입니다.
                        Log.d("coded", "들어옴");
                        ObjectInputStream ois = new ObjectInputStream(conn.getInputStream());
                        HashMap<String, String> infoDataMap = (HashMap<String, String>) ois.readObject();
                        ois.close();
                        Log.d("cnt", dataMap.size() + "");
                        if (dataMap.size() == 0)
                            return "";

                        Message msg = handler.obtainMessage();
                        Bundle b= new Bundle();
                        msg.setData(b);
                        msg.obj=infoDataMap;
                        handler.sendMessage(msg);
                      }
                    conn.disconnect();
                }
            } catch (Exception e) {
                Toast.makeText(getActivity(), "Error 발생", Toast.LENGTH_SHORT).show();
                Log.e("ERR", "LoadAllPrecentCondionInfoAsyncThread ERR : " + e.getMessage());
            }

            return "";
        }

        // doInBackground(~)에서 호출되어 주로 UI 관련 작업을 하는 함수
        protected void onProgressUpdate(String... progress) {}

        // Thread를 처리한 후에 호출되는 함수
        // doInBackground(~)의 리턴값을 인자로 받습니다.
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            mSwipeRefreshLayout.setRefreshing(false); // 새로고침이 완료 되었음을 표시
        }

        // AsyncTask.cancel(true) 호출시 실행되어 thread를 취소 합니다.
        protected void onCancelled() {
            super.onCancelled();
        }
    }

    public class CompletePrecentCondionInfoAsyncThread extends AsyncTask<String, String, String> {
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
            String[] values = args[1].split(" ");
            final DBManager dbManager = new DBManager(getContext(), "App_Data.db", null, 1);
            final HashMap <String, String> appDataMap = dbManager.getResult();
            HashMap<String, String> dataMap = new HashMap<>();
            dataMap.put("no", args[0]);
            dataMap.put("type", "supply");
            dataMap.put("supply_id", appDataMap.get("id"));
            dataMap.put("supply_name", appDataMap.get("name"));
            dataMap.put("title", values[0]);
            dataMap.put("demand_id", values[1]);
            dataMap.put("demand_name", values[2]);
            dataMap.put("date", args[2]);
            dataMap.put("time", args[3]);
            dataMap.put("where", args[4]);
            dataMap.put("content", args[5]);
            dataMap.put("phone", args[6]);

            urlStr = "http://"+getString(R.string.ip_address)+":8080/BookDreamServerProject/completePresentConditionInfo";
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
                oos.writeObject(dataMap);
                oos.flush();
                oos.close();
                if (conn.getResponseMessage().equals("OK")) { // 서버가 받았다면
                    ObjectInputStream ois = new ObjectInputStream(conn.getInputStream());
                    HashMap<String, String> stringDataMap = (HashMap<String, String>)ois.readObject();
                    ois.close();
                    Message msg = handler.obtainMessage();
                    Bundle b = new Bundle();
                    b.putString("present_condition", "data");
                    Log.d("test_cnt",dataMap.size()+"");
                    if(stringDataMap.size()==0){
                        b.putString("result", "fail");
                    } else {
                        b.putString("result", "success");
                    }
                    msg.setData(b);
                    handler.sendMessage(msg);
                }
                conn.disconnect();

            } catch (Exception e) {
                Toast.makeText(getActivity(), "Error 발생", Toast.LENGTH_SHORT).show();
                Log.e("ERR", "CompletePrecentCondionInfoAsyncThread ERR : " + e.getMessage());
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