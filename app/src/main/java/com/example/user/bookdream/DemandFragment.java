package com.example.user.bookdream;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

/**
 * 프로젝트명 : Book:DREAM
 * 시      기 : 성공회대학교 글로컬IT학과 2016년도 2학기 실무프로젝트
 * 팀      원 : 200934013 서동형, 201134031 최형근, 201434031 이보라미
 *
 * 요청 프래그먼트를 구성
 * 후배가 요청 글을 쓰는 경우, 선배가 후배의 요청을 보고 책을 주는 경우와 관련된 모든 로직을 구성한다.
 **/
public class DemandFragment extends Fragment {
    private ListView demandListView = null;
    private ListViewAdapter dAdapter = null;
    private SwipeRefreshLayout mSwipeRefreshLayout = null;

    private Spinner titleSp, periodSp;
    private EditText edtContent;

    private TextView demandWrite;

    private TextView view_title, view_user, view_period, dview_content, dview_date, requestingBtn;

    private WriteAsyncThread backgroundWriteThread;
    private InitAsyncThread backgroundInitThread;
    private RemoveAsyncThread backgroundRemoveThread;
    private  CompletePrecentCondionInfoAsyncThread  backgroundCompletePrecentConditionThread;

    /*
        후배가 요청 게시판에 글을 올린 경우, DB와 상호작용 하는 핸들러
     */
    final Handler handler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            Bundle b = msg.getData();

            if (b.getString("present_condition") != null) {
                if (b.getString("result").equals("success")) {
                    Toast.makeText(getActivity(), "BOOK:DREAM을 완료했습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "이미 처리 하신 상태입니다.", Toast.LENGTH_SHORT).show();
                }
            } else {
                dAdapter.addItem(b.getString("no"), b.getString("title"), b.getString("date"), b.getString("user"), b.getString("period"), b.getString("content"));
                dAdapter.dataChange();
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.layout_demand_fragment, container, false);

        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_layout);
        mSwipeRefreshLayout.setOnRefreshListener(refreshListener);

        demandListView = (ListView) rootView.findViewById(R.id.demand_listView);

        titleSp = (Spinner) rootView.findViewById(R.id.title_sp);
        periodSp = (Spinner) rootView.findViewById(R.id.period_sp);
        demandWrite = (TextView) rootView.findViewById(R.id.demand_write);
        demandWrite.setOnClickListener(writtingClickListener);

        dAdapter = new ListViewAdapter(getContext());
        demandListView.setAdapter(dAdapter);

        backgroundInitThread = new InitAsyncThread();
        backgroundInitThread.execute();

        // 사용자가 선택한 글의 세부 정보를 DB에서 받아서 다이얼로그로 띄운다
        demandListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                getItemDialog(position);
            }
        });

        // 사용자가 글을 오래 클릭한 경우 : 1. 글쓴이와 동일 인물인지를 확인 2. 동일 인물일 시 요청 리스트와 DB에서 해당 글을 삭제
        demandListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                final DBManager dbManager = new DBManager(getContext(), "App_Data.db", null, 1);
                final HashMap <String, String> dataMap = dbManager.getResult();
                String userName = dataMap.get("id") +" " +dataMap.get("name");
                DListData dData = (DListData) dAdapter.getItem(position);
                if (!dData.mUser.equals(userName)) {    // 해당 글 작성자가 아닌 경우 실패 메세지를 띄우고 삭제 취소
                    Log.d("user", dData.mUser);
                    Log.d("userInfo", userName);
                    Toast.makeText(getContext(), "글 작성자가 아닙니다. 확인 후 다시 시도해주세요.", Toast.LENGTH_LONG).show();
                    return true;
                }
                AlertDialog.Builder alert = new AlertDialog.Builder(view.getContext());
                alert.setTitle("확인창")
                     .setMessage("정말로 삭제하시겠습니까?")
                     .setPositiveButton("확인", new DialogInterface.OnClickListener(){
                         @Override
                         public void onClick(DialogInterface dialog, int which) {
                             Log.d("d","번호 : " + position);
                             dAdapter.clear();

                             backgroundRemoveThread = new RemoveAsyncThread();
                             backgroundRemoveThread.execute(position+"");   // DB에서 삭제
                         }
                     })
                     .setNegativeButton("취소", new DialogInterface.OnClickListener(){
                         @Override
                         public void onClick(DialogInterface dialog, int which) {
                             dialog.dismiss();
                         }
                     }).show();
                return true;
            }
        });
        return rootView;
    }

    /*
        어플리케이션 종료시 쓰레드의 종료를 요청하는 메소드
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (backgroundWriteThread.getStatus() == AsyncTask.Status.RUNNING) {
                backgroundWriteThread.cancel(true);
            }

            if (backgroundInitThread.getStatus() == AsyncTask.Status.RUNNING) {
                backgroundInitThread.cancel(true);
            }

            if (backgroundRemoveThread.getStatus() == AsyncTask.Status.RUNNING) {
                backgroundRemoveThread.cancel(true);
            }

            if (backgroundCompletePrecentConditionThread.getStatus() == AsyncTask.Status.RUNNING) {
                backgroundCompletePrecentConditionThread.cancel(true);
            }
        } catch (Exception e) {}
    }

    /*
        DListData에서 해당 정보를 찾아 다이얼로그에 정보를 띄운다.
    */
    public void getItemDialog(int position) {
        DListData dData = dAdapter.mListData.get(position);

        final String gNo = dData.mNo;
        final String gTitle = dData.mTitle;
        String gDate = dData.mDate;
        final String gUser = dData.mUser;
        String gPeriod = dData.mSemester;
        String cContent = dData.mContent;

        // Dialog에서 보여줄 입력화면 View 객체 생성 작업
        // Layout xml 리소스 파일을 View 객체로 부불려 주는(inflate) LayoutInflater 객체 생성
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // res폴더>>layout폴더>>layout_demand_view_dialog.xml 레이아웃 리소스 파일로 View 객체 생성
        // Dialog의 listener에서 사용하기 위해 final로 참조변수 선언
        final View dialogView = inflater.inflate(R.layout.layout_demand_view_dialog, null);

        AlertDialog.Builder buider = new AlertDialog.Builder(getContext()); // AlertDialog.Builder 객체 생성

        buider.setView(dialogView); // 위에서 inflater가 만든 dialogView 객체 세팅 (Customize)

        buider.setNegativeButton("확인", new DialogInterface.OnClickListener() { // Dialog에 "확인"라는 타이틀의 버튼을 설정
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss(); // 확인 버튼을 누르면 다이얼로그가 꺼짐
            }
        });

        view_title = (TextView) dialogView.findViewById(R.id.dview_title);
        view_user = (TextView) dialogView.findViewById(R.id.dview_user);
        view_period = (TextView) dialogView.findViewById(R.id.dview_period);
        dview_content = (TextView) dialogView.findViewById(R.id.dview_content);
        dview_date = (TextView) dialogView.findViewById(R.id.dview_date);
        requestingBtn = (TextView) dialogView.findViewById(R.id.requestDBtn);

        // 선배가 후배에게 책을 주려는 의도가 있는 경우, 새로운 다이얼로그를 띄워 약속 시간 등을 정한다.
        requestingBtn.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) { // 선배 : 후배가 올린 드림 글의 요청에 응한 경우 (선배가 후배에게 책을 주고싶은 경우)
                final DBManager dbManager = new DBManager(getContext(), "App_Data.db", null, 1);
                final HashMap <String, String> dataMap = dbManager.getResult();
                String userName = dataMap.get("id") +" " +dataMap.get("name");
                if(gUser.equals(userName)) { // 자기 자신의 드림 요청에 응하는것 방지
                    Toast.makeText(getContext(), "자신한테 DREAM을 할 수 없습니다.", Toast.LENGTH_LONG).show();
                    return ;
                }

                LayoutInflater inflater = LayoutInflater.from(getContext());
                final View dialogView= inflater.inflate(R.layout.layout_supply_precent_condition_dialog,null);

                final DatePicker datePicker = (DatePicker)  dialogView.findViewById(R.id.datePicker);
                final TimePicker timePicker = (TimePicker)  dialogView.findViewById(R.id.timePicker);
                final EditText edit_where= (EditText)dialogView.findViewById(R.id.edit_where);
                final EditText edit_content= (EditText)dialogView.findViewById(R.id.edit_content);
                final EditText edit_phone= (EditText)dialogView.findViewById(R.id.edit_phone);
                edit_phone.addTextChangedListener(new PhoneNumberFormattingTextWatcher());

                // 멤버의 세부내역 입력 Dialog 생성 및 보이기
                final android.support.v7.app.AlertDialog.Builder builder= new android.support.v7.app.AlertDialog.Builder(getContext()); //AlertDialog.Builder 객체 생성
                builder.setTitle("BOOK:DREAM"); // Dialog 제목
                builder.setIcon(android.R.drawable.ic_menu_add); // 제목옆의 아이콘 이미지(원하는 이미지 설정)
                builder.setView(dialogView); // 위에서 inflater가 만든 dialogView 객체 세팅 (Customize)
                // Dialog에 "DREAM"라는 타이틀의 버튼을 설정
                builder.setPositiveButton("DREAM", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TextView에 추가작업을 완료 하였기에 '완료'했다는 메세지를 Toast로 출력
                        Toast.makeText(getContext(), datePicker.toString(), Toast.LENGTH_SHORT).show();
                        // 선배가 설정한 약속 날짜를 datePicker로부터 저장
                        String date = String.format("%d - %d - %d", datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                        // 선배가 설정한 약속 시간을 timePicker로부터 저장
                        String time = "";
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            time = String.format("%d 시 %d 분", timePicker.getHour(), timePicker.getMinute());
                        } else {
                            time = String.format("%d 시 %d 분", timePicker.getCurrentHour(), timePicker.getCurrentMinute());
                        }

                        // 선배의 정보와 드림 메세지(선배 이름, 연락처, 약속 장소 등)을 보냄
                        String[] demandUserInfo = view_user.getText().toString().split(" ");
                        backgroundCompletePrecentConditionThread = new CompletePrecentCondionInfoAsyncThread();
                        backgroundCompletePrecentConditionThread.execute(
                                gNo,gTitle,demandUserInfo[0], demandUserInfo[1],
                                date, time, edit_where.getText().toString(),
                                edit_content.getText().toString(), edit_phone.getText().toString());
                    }
                });

                // Dialog에 "Cancel"이라는 타이틀의 버튼을 설정
                builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss(); // 다이얼로그 종료
                    }
                });

                // 설정한 값으로 AlertDialog 객체 생성
                android.support.v7.app.AlertDialog completeDialog=builder.create();
                // Dialog의 바깥쪽을 터치했을 때 Dialog를 없앨지 설정
                completeDialog.setCanceledOnTouchOutside(false);//없어지지 않도록 설정
                // Dialog 보이기
                completeDialog.show();
            }
        });

        // 해당 글의 정보를 다이얼로그에 띄움
        try {
            view_title.setText(gTitle);
            view_user.setText(gUser);
            view_period.setText(gPeriod);
            dview_content.setText(cContent);
            dview_date.setText(gDate);
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Error 발생", Toast.LENGTH_SHORT).show();
            Log.e("ERR", "requestingBtn.setOnClickListener ERR : " + e.getMessage());
        }

        //설정한 값으로 AlertDialog 객체 생성
        AlertDialog dialog = buider.create();

        //Dialog의 바깥쪽을 터치했을 때 Dialog를 없앨지 설정
        dialog.setCanceledOnTouchOutside(false);//없어지지 않도록 설정

        //Dialog 보이기
        dialog.show();
    }

    /*
        후배가 요청 글을 올리는 경우의 리스너 설정
        원하는 책의 이름, 기간을 스피너에서 설정하고 간단한 메세지를 등록할 수 있게 한다.
     */
    TextView.OnClickListener writtingClickListener = new View.OnClickListener() {
        String title = null, period = null, content = null;
        boolean flag = true;

        @Override
        public void onClick(View v) {
            // Dialog에서 보여줄 입력화면 View 객체 생성 작업
            // Layout xml 리소스 파일을 View 객체로 부불려 주는(inflate) LayoutInflater 객체 생성
            LayoutInflater inflater = getActivity().getLayoutInflater();

            // res폴더>>layout폴더>>layout_demand_writing_dialog.xml 레이아웃 리소스 파일로 View 객체 생성
            // Dialog의 listener에서 사용하기 위해 final로 참조변수 선언
            final View dialogView = inflater.inflate(R.layout.layout_demand_writing_dialog, null);

            titleSp = (Spinner) dialogView.findViewById(R.id.title_sp);
            periodSp = (Spinner) dialogView.findViewById(R.id.period_sp);
            edtContent = (EditText) dialogView.findViewById(R.id.edt_content);

            String[] subject = getResources().getStringArray(R.array.title_sp_array);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, subject);
            titleSp.setAdapter(adapter);
            titleSp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    title = parent.getItemAtPosition(position).toString();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            Calendar cal = Calendar.getInstance();
            int nowMonth = cal.get(Calendar.MONTH); // 현재 월
            int nowYear = cal.get(Calendar.YEAR);   // 현재 연도

            // 1. nowYear : 현재 년도로 부터 3년 후의 학기까지 명시된 대여기간으로 표기하고 나머지는 기타로 표기한다.
            // 2. nowMonth : 6월을 기준으로 1월 ~ 6월을 1학기, 7월 ~ 12월을 2학기로 계산한다. 대여 기간 표기를 해당 학기 이후로 표기한다.
            final String[] settingYear;
            if (nowMonth < 7) {
                settingYear = new String[]{"선    택",
                        String.valueOf(nowYear) + "-1", String.valueOf(nowYear) + "-2",
                        String.valueOf(nowYear + 1) + "-1", String.valueOf(nowYear + 1) + "-2",
                        String.valueOf(nowYear + 2) + "-1", String.valueOf(nowYear + 2) + "-2",
                        String.valueOf(nowYear + 3) + "-1", String.valueOf(nowYear + 3) + " -2", "기타"};
            } else {
                settingYear = new String[]{"선    택",
                        String.valueOf(nowYear) + "-2",
                        String.valueOf(nowYear + 1) + "-1", String.valueOf(nowYear + 1) + "-2",
                        String.valueOf(nowYear + 2) + "-1", String.valueOf(nowYear + 2) + "-2",
                        String.valueOf(nowYear + 3) + "-1", String.valueOf(nowYear + 3) + "-2", "기타"};
            }

            ArrayAdapter<String> adapter2 = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, settingYear);
            periodSp.setAdapter(adapter2);
            periodSp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    period = parent.getItemAtPosition(position).toString();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            AlertDialog.Builder buider = new AlertDialog.Builder(getContext()); // AlertDialog.Builder 객체 생성
            buider.setView(dialogView); // 위에서 inflater가 만든 dialogView 객체 세팅 (Customize)
            buider.setPositiveButton("확인", null);  // Dialog에 "확인"라는 타이틀의 버튼을 설정, 리스너는 바로 아래에서 설정
            buider.setNegativeButton("취소", new DialogInterface.OnClickListener() {  // Dialog에 "취소"라는 타이틀의 버튼을 설정
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            //설정한 값으로 AlertDialog 객체 생성
            final AlertDialog dialog = buider.create();
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface d) {
                    // 다이얼로그의 확인 버튼을 누른 경우를 Overriding
                    Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    positive.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            flag = true;

                            // 과목을 선택하지 않는 경우 요청 불가 설정
                            String[] tmp = getResources().getStringArray(R.array.title_sp_array);
                            if (title == null || title.equals(tmp[0])) {
                                flag = false;
                                Toast.makeText(getActivity(), "과목을 선택 해주세요.", Toast.LENGTH_LONG).show();
                            } else {
                                flag = true;
                            }

                            // 설명을 쓰지 않는 경우 요청 불가 설정
                            if (flag) {
                                try {
                                    content = edtContent.getText().toString();
                                    if (content.length() == 0) {
                                        flag = false;
                                        Toast.makeText(getActivity(), "해당 과목에 대한 설명을 써주세요.", Toast.LENGTH_LONG).show();
                                    } else {
                                        flag = true;
                                    }
                                } catch (Exception e) {
                                    flag = false;
                                    Toast.makeText(getActivity(), "Demand Content ERR : " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }

                            // 기간을 선택하지 않는 경우 요청 불가 설정
                            if (flag) {
                                if (period == null || period.equals("선    택")) {
                                    flag = false;
                                    Toast.makeText(getActivity(), "기간을 선택 해주세요.", Toast.LENGTH_LONG).show();
                                } else {
                                    flag = true;
                                }
                            }

                            // 모든 요청 데이터가 채워진 경우, 요청 완료(DB 업로드, List 업로드) 후 다이얼로그 종료
                            if (flag) {
                                final DBManager dbManager = new DBManager(getContext(), "App_Data.db", null, 1);
                                final HashMap <String, String> dataMap = dbManager.getResult();
                                String userName = dataMap.get("id") +" " +dataMap.get("name");
                                Calendar cal = Calendar.getInstance();
                                String date = (cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DATE));
                                int n = dAdapter.getCount(); //현재리스트 숫자로 만드는 uniqueNum
                                dAdapter.addItem(n+"", title, date, userName, period, content);
                                dAdapter.dataChange();
                                insertDatabase(n, title, date, userName, period, content);
                                dialog.dismiss();
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
       ViewHolder란, 이름 그대로 뷰들을 홀더에 꼽아놓듯이 보관하는 객체를 말한다.
       각각의 Row를 그려낼 때 그 안의 위젯들의 속성을 변경하기 위해 findViewById를 호출하는데,
       이것의 비용이 큰것을 줄이기 위해 사용한다. 여기서 게시판의 정보들을 ViewHolder를 이용해 삽입한다.
     */
    private class ViewHolder {
        public TextView mNo;
        public TextView mTitle;
        public TextView mUser;
        public TextView mSemester;
    }

    /*
        요청 프래그먼트의 리스트뷰를 관리하는 메소드
     */
    private class ListViewAdapter extends BaseAdapter {
        private Context mContext = null;
        private ArrayList<DListData> mListData = new ArrayList<>();

        public ListViewAdapter(Context mContext) {
            super();
            this.mContext = mContext;
        }

        // 총 몇개의 리스트가 있는지 반환
        @Override
        public int getCount() {
            return mListData.size();
        }

        // 사용자가 선택한 아이템을 반환
        @Override
        public Object getItem(int position) {
            return mListData.get(position);
        }

        // ID(몇 번째 아이템인지) 반환
        @Override
        public long getItemId(int position) {
            return position;
        }

        // 사용자가 선택한 아이템 데이터를 layout_demand_board_item 형태에 맞춰 반환한다.
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            DemandFragment.ViewHolder holder;
            if (convertView == null) {
                holder = new DemandFragment.ViewHolder();

                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.layout_demand_board_item, null);

                holder.mNo = (TextView) convertView.findViewById(R.id.no);
                holder.mTitle = (TextView) convertView.findViewById(R.id.title);
                holder.mUser = (TextView) convertView.findViewById(R.id.user);
                holder.mSemester = (TextView) convertView.findViewById(R.id.semester);

                convertView.setTag(holder);
            } else {
                holder = (DemandFragment.ViewHolder) convertView.getTag();
            }

            DListData mData = mListData.get(position); // DListData로부터 해당 아이템의 데이터를 받아온다.

            holder.mNo.setText(mData.mNo);
            holder.mTitle.setText(mData.mTitle);
            holder.mUser.setText(mData.mUser);
            holder.mSemester.setText(mData.mSemester);

            return convertView;
        }

        /*
            리스트에 아이템을 추가하는 메소드
        */
        public void addItem(String mNo, String mTitle, String mDate, String mUser, String mSemester, String mContent) {
            DListData addInfo = null;
            addInfo = new DListData();
            addInfo.mNo = mNo;
            addInfo.mTitle = mTitle;
            addInfo.mDate = mDate;
            addInfo.mUser = mUser;
            addInfo.mSemester = mSemester;
            addInfo.mContent = mContent;

            mListData.add(addInfo);
        }

        // 리스트를 새로고침 하는 메소드
        public void clear(){
            mListData.clear();
        }

        // 데이터가 바뀌었음을 DB에 알려주는 메소드
        public void dataChange() {
            dAdapter.notifyDataSetChanged();
        }
    }

    /*
        사용자가 리스트를 아래로 끌어당겨 새로고침을 하는 경우, 데이터를 새로고침 하는 메소드
     */
    SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            dAdapter.clear();
            backgroundInitThread = new InitAsyncThread(); // DB로부터 데이터를 새로 받아온다.
            backgroundInitThread.execute();
        }
    };

    /*
        DB에 새로운 데이터를 추가하는 메소드
     */
    private void insertDatabase(int uniqueNum, String title, String date, String userName, String period, String content) {
        backgroundWriteThread = new WriteAsyncThread();
        backgroundWriteThread.execute(uniqueNum+"", title, date, userName, period, content);
    }

    public class WriteAsyncThread extends AsyncTask<String, String, String> {
        // Thread를 시작하기 전에 호출되는 함수
        protected void onPreExecute() {
            super.onPreExecute();

        }

        // Thread의 주요 작업을 처리 하는 함수
        // Thread를 실행하기 위해 excute(~)에서 전달한 값을 인자로 받는다.
        protected String doInBackground(String... args) {
            try {
                URL url = null;
                HttpURLConnection conn = null;
                String urlStr = "";

                urlStr = "http://"+getString(R.string.ip_address)+":8080/BookDreamServerProject/addDemandBulletinBoardInfo";
                url = new URL(urlStr);
                Log.d("test", urlStr);
                OutputStream os = null;
                InputStream is = null;
                ByteArrayOutputStream baos = null;
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Cache-Control", "no-cache");
                conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                HashMap<String, String> params = new HashMap<>();
                params.put("no", args[0]);
                params.put("title", args[1]);
                params.put("date", args[2]);
                params.put("user", args[3]);
                params.put("period", args[4]);
                params.put("content", args[5]);
                ObjectOutputStream oos = new ObjectOutputStream(conn.getOutputStream());
                oos.writeObject(params);
                oos.flush();
                oos.close();
                Log.d("test", "write");
                if (conn.getResponseMessage().equals("OK")) { // 서버가 받았다면
                    Log.d("Test","DD");
                }
                conn.disconnect();
                return "OK";
            } catch (Exception e) {
                Toast.makeText(getActivity(), "Error 발생", Toast.LENGTH_SHORT).show();
                Log.e("ERR", "WriteAsyncThread ERR : " + e.getMessage());
            }
            return "";
        }

        protected void onProgressUpdate(String... progress) {}

        // Thread를 처리한 후에 호출되는 함수
        // doInBackground(~)의 리턴값을 인자로 받는다.
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }

        // AsyncTask.cancel(true) 호출시 실행되어 thread를 취소한다.
        protected void onCancelled() {
            super.onCancelled();
        }
    }

    public class InitAsyncThread extends AsyncTask<Void, String, String> {
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

            urlStr = "http://"+getString(R.string.ip_address)+":8080/BookDreamServerProject/requestDemandBulletinBoardInitInfo";

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
                if (responseCode == HttpURLConnection.HTTP_OK) {    // 송수신이 잘되면 - 데이터를 받은 것
                    Log.d("coded", "들어옴");
                    ObjectInputStream ois = new ObjectInputStream(conn.getInputStream());
                    HashMap<String, HashMap<String, String>> dataMap = (HashMap<String, HashMap<String, String>>)ois.readObject();
                    ois.close();

                    for(int i=0; i<dataMap.size(); i++) {
                        HashMap<String, String> stringDataMap = dataMap.get(i+"");
                        Message msg = handler.obtainMessage();
                        Bundle b = new Bundle();
                        b.putString("no", i+"");
                        b.putString("title" , stringDataMap.get("title"));
                        Log.d("DD", stringDataMap.get("title"));
                        b.putString("date" , stringDataMap.get("date"));
                        b.putString("user", stringDataMap.get("user"));
                        b.putString("period", stringDataMap.get("period"));
                        b.putString("content", stringDataMap.get("content"));
                        msg.setData(b);
                        handler.sendMessage(msg);
                    }

                    conn.disconnect();
                }
            } catch (Exception e) {
                Toast.makeText(getActivity(), "Error 발생", Toast.LENGTH_SHORT).show();
                Log.e("ERR", "InitAsyncThread ERR : " + e.getMessage());
            }

            return "";
        }

        // doInBackground(~)에서 호출되어 주로 UI 관련 작업을 하는 함수
        protected void onProgressUpdate(String... progress) {}

        // Thread를 처리한 후에 호출되는 함수
        // doInBackground(~)의 리턴값을 인자로 받는다.
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            mSwipeRefreshLayout.setRefreshing(false); //  새로고침이 완료 되었음을 표시
        }

        // AsyncTask.cancel(true) 호출시 실행되어 thread를 취소 합니다.
        protected void onCancelled() {
            super.onCancelled();
        }
    }

    public class RemoveAsyncThread extends AsyncTask<String, String, String> {
        // Thread를 시작하기 전에 호출되는 함수
        protected void onPreExecute() {
            super.onPreExecute();
        }

        // Thread의 주요 작업을 처리 하는 함수
        // Thread를 실행하기 위해 excute(~)에서 전달한 값을 인자로 받습니다.
        protected String doInBackground(String...args) {
            try {
                URL url = null;
                HttpURLConnection conn = null;
                String urlStr = "";

                urlStr = "http://"+getString(R.string.ip_address)+":8080/BookDreamServerProject/removeDemandBulletinBoardInfo";
                url = new URL(urlStr);
                Log.d("test", urlStr);

                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Cache-Control", "no-cache");
                conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                HashMap<String, String> params = new HashMap<>();
                params.put("no", args[0]);
                ObjectOutputStream oos = new ObjectOutputStream(conn.getOutputStream());
                oos.writeObject(params);
                oos.flush();
                oos.close();
                Log.d("test", "remove_write");
                if (conn.getResponseMessage().equals("OK")) { // 서버가 받았다면
                    Log.d("coded", "들어옴");
                    ObjectInputStream ois = new ObjectInputStream(conn.getInputStream());
                    HashMap<String, HashMap<String, String>> dataMap = (HashMap<String, HashMap<String, String>>)ois.readObject();
                    ois.close();

                    for(int i = 0; i < dataMap.size(); i++) {
                        HashMap<String, String> stringDataMap = dataMap.get(i+"");
                        Message msg = handler.obtainMessage();
                        Bundle b = new Bundle();
                        b.putString("no", i+"");
                        b.putString("title" , stringDataMap.get("title"));
                        Log.d("DD", stringDataMap.get("title"));
                        b.putString("date" , stringDataMap.get("date"));
                        b.putString("user", stringDataMap.get("user"));
                        b.putString("period", stringDataMap.get("period"));
                        b.putString("content", stringDataMap.get("content"));
                        msg.setData(b);
                        handler.sendMessage(msg);
                    }

                    Log.d("Test","DD");
                }
                conn.disconnect();
                return "OK";
            }  catch (Exception e) {
                Toast.makeText(getActivity(), "Error 발생", Toast.LENGTH_SHORT).show();
                Log.e("ERR", "RemoveAsyncThread ERR : " + e.getMessage());
            }
            return "";
        }

        // doInBackground(~)에서 호출되어 주로 UI 관련 작업을 하는 함수
        protected void onProgressUpdate(String... progress) {}

        // Thread를 처리한 후에 호출되는 함수
        // doInBackground(~)의 리턴값을 인자로 받는다.
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }

        // AsyncTask.cancel(true) 호출시 실행되어 thread를 취소한다.
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
        // Thread를 실행하기 위해 excute(~)에서 전달한 값을 인자로 받는다.
        protected String doInBackground(String...args) {
            URL url = null;
            HttpURLConnection conn = null;
            String urlStr = "";
            final DBManager dbManager = new DBManager(getContext(), "App_Data.db", null, 1);
            final HashMap <String, String> appDataMap = dbManager.getResult();
            HashMap<String, String> dataMap = new HashMap<>();
            dataMap.put("type", "demand");
            dataMap.put("no", args[0]);
            dataMap.put("title", args[1]);
            dataMap.put("supply_id", appDataMap.get("id"));
            dataMap.put("supply_name", appDataMap.get("name"));
            dataMap.put("demand_id",args[2]);
            dataMap.put("demand_name", args[3]);
            dataMap.put("date", args[4]);
            dataMap.put("time", args[5]);
            dataMap.put("where", args[6]);
            dataMap.put("content", args[7]);
            dataMap.put("phone", args[8]);

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
                    Log.d("test_cnt",stringDataMap.size()+"");
                    if(stringDataMap .size() == 0){
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
        // doInBackground(~)의 리턴값을 인자로 받는다
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }

        // AsyncTask.cancel(true) 호출시 실행되어 thread를 취소한다.
        protected void onCancelled() {
            super.onCancelled();
        }
    }
}