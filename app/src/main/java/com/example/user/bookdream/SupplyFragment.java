package com.example.user.bookdream;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import static android.app.Activity.RESULT_OK;

/**
 * 프로젝트명 : Book:DREAM
 * 시      기 : 성공회대학교 글로컬IT학과 2016년도 2학기 실무프로젝트
 * 팀      원 : 200934013 서동형, 201134031 최형근, 201434031 이보라미
 *
 * 드림 프래그먼트를 구성
 * 선배가 드림 가능한 책을 업로드 하는 경우, 후배가 선배의 드림 글을 보고 대여하는 로직을 구성한다.
 **/
public class SupplyFragment extends Fragment {
    private static final int PICK_FROM_CAMERA = 0;
    private static final int PICK_FROM_ALBUM = 1;
    private static final int CROP_FROM_iMAGE = 2;
    private SwipeRefreshLayout smSwipeRefreshLayout = null;
    private Uri mImageCaptureUri;
    private int xValue, yValue;

    private ListView mListView = null;
    private ListViewAdapter mAdapter = null;
    private Spinner writing_title;
    private EditText writing_grade;
    private TextView writing_image_upload, writing_take_photo;
    private ImageView writting_image;

    private ImageView view_image;
    private TextView view_title, view_grade, view_type, view_date, requestingBtn;
    private TextView writing;
    private RadioGroup rent_or_give;


    private Drawable uploadingImage = null;
    private String getTitle = null, getState = null, getGrade = null;
    private Drawable getImage = null;
    private WriteAsyncThread backgroundWriteThread;
    private InitAsyncThread backgroundInitThread;
    private RemoveAsyncThread backgroundRemoveThread;
    private AddSupplyPrecentConditionAsyncThread backgroundAddPrecentConditionThread;

    /*
        선배가 드림 게시판에 글을 올린 경우, DB와 상호 작용하는 리스너
     */
    final Handler handler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            Bundle b = msg.getData();
            if (b.getString("present_condition") !=null) {
                if (b.getString("result").equals("success")){
                    Toast.makeText(getActivity(), "DREAM을 신청했습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "이미 DREAM을 신청한 상태입니다.", Toast.LENGTH_SHORT).show();
                }
            } else {
                byte[] bytes = (byte[])msg.obj;
                ByteArrayInputStream is = new ByteArrayInputStream(bytes);
                Drawable icon = Drawable.createFromStream(is, "articleImage");
                mAdapter.addItem(icon, b.getString("no"), b.getString("title"), b.getString("date"),
                                 b.getString("state"), b.getString("grade"), b.getString("info"));
                mAdapter.dataChange();
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.layout_supply_fragment, container, false);

        smSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.sswipe_layout);
        smSwipeRefreshLayout.setOnRefreshListener(srefreshListener);

        mListView = (ListView) rootView.findViewById(R.id.listView);
        writing = (TextView) rootView.findViewById(R.id.supply_write);

        writing.setOnClickListener(writingClickListener);

        mAdapter = new ListViewAdapter(getContext());
        mListView.setAdapter(mAdapter);

        // 사용자가 선택한 글의 세부 정보를 DB에서 받아서 다이얼로그로 띄운다
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                getItemDialog(position);
            }
        });

        // 사용자가 글을 오래 클릭한 경우 : 1. 글쓴이와 동일 인물인지를 확인 2. 동일 인물일 시 요청 리스트와 DB에서 해당 글을 삭제
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                final DBManager dbManager = new DBManager(getContext(), "App_Data.db", null, 1);
                SListData sData = (SListData) mAdapter.getItem(position);
                HashMap<String, String> dataMap =dbManager.getResult();
                String userName = dataMap.get("id") +" " +dataMap.get("name");

                if(!sData.mInfo.equals(userName)){
                    Toast.makeText(getContext(), "글 작성자가 아닙니다. 확인 후 다시 시도해주세요.", Toast.LENGTH_LONG).show();
                    return true;
                }

                AlertDialog.Builder alert = new AlertDialog.Builder(view.getContext());
                alert.setTitle("확인창")
                        .setMessage("정말로 삭제하시겠습니까?")
                        .setPositiveButton("확인", new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mAdapter.clear();
                                backgroundRemoveThread = new RemoveAsyncThread();
                                backgroundRemoveThread.execute(position+"");
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

        backgroundInitThread = new InitAsyncThread();
        backgroundInitThread.execute();
        try {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            xValue = yValue = (displayMetrics.widthPixels / 2);
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Error 발생", Toast.LENGTH_SHORT).show();
            Log.e("ERR", "Scale  ERR : " + e.getMessage());
        }

        return rootView;
    }

    /*
        어플리케이션 종료시 쓰레드의 종료를 요청
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

            if (backgroundAddPrecentConditionThread.getStatus() == AsyncTask.Status.RUNNING) {
                backgroundAddPrecentConditionThread.cancel(true);
            }
        } catch (Exception e) {}
    }

    /*
        SListData에서 해당 정보를 찾아 다이얼로그에 정보를 띄운다.
    */
    public void getItemDialog(final int position) {
        SListData mData = mAdapter.mListData.get(position);

        Drawable vIcon = mData.mIcon;
        String vTitle = mData.mTitle;
        String vDate = mData.mDate;
        String vType = mData.mType;
        String vGrade = mData.mGrade;
        final String vInfo = mData.mInfo;

        // Dialog에서 보여줄 입력화면 View 객체 생성 작업
        // Layout xml 리소스 파일을 View 객체로 부불려 주는(inflate) LayoutInflater 객체 생성
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // res폴더>>layout폴더>>layout_writing_custom_dialog.xml 레이아웃 리소스 파일로 View 객체 생성
        // Dialog의 listener에서 사용하기 위해 final로 참조변수 선언
        final View dialogView = inflater.inflate(R.layout.layout_supply_view_dialog, null);

        AlertDialog.Builder buider = new AlertDialog.Builder(getContext()); // AlertDialog.Builder 객체 생성
        buider.setView(dialogView); // 위에서 inflater가 만든 dialogView 객체 세팅 (Customize)
        buider.setNegativeButton("확인", new DialogInterface.OnClickListener() {
            //Dialog에 "확인"라는 타이틀의 버튼을 설정

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        view_image = (ImageView) dialogView.findViewById(R.id.view_imageView);
        view_title = (TextView) dialogView.findViewById(R.id.view_title);
        view_grade = (TextView) dialogView.findViewById(R.id.view_grade);
        view_type = (TextView) dialogView.findViewById(R.id.view_type);
        view_date = (TextView) dialogView.findViewById(R.id.view_date);
        requestingBtn = (TextView) dialogView.findViewById(R.id.requestSBtn);
        requestingBtn.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final DBManager dbManager = new DBManager(getContext(), "App_Data.db", null, 1);
                final HashMap <String, String> dataMap = dbManager.getResult();
                String userName = dataMap.get("id") +" " +dataMap.get("name");
                if(vInfo.equals(userName)) { // 자기 자신의 드림 요청에 응하는것 방지
                    Toast.makeText(getContext(), "자신한테 DREAM을 신청할 수 없습니다.", Toast.LENGTH_LONG).show();
                    return ;
                }
                backgroundAddPrecentConditionThread = new AddSupplyPrecentConditionAsyncThread();
                backgroundAddPrecentConditionThread.execute(position+"", dataMap.get("id"), dataMap.get("name"));
            }
        });

        // 선배가 올린 책 사진 크기를 설정, 다이얼로그에 띄워준다.
        try {
            if ((vIcon == null) || !(vIcon instanceof BitmapDrawable)) {
                view_image.setImageDrawable(vIcon);
            } else {
                Bitmap b = ((BitmapDrawable) vIcon).getBitmap();
                Bitmap bitmapResized = Bitmap.createScaledBitmap(b, xValue, yValue, false);
                vIcon = new BitmapDrawable(getResources(), bitmapResized);
                view_image.setImageDrawable(vIcon);
            }
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Error 발생", Toast.LENGTH_SHORT).show();
            Log.e("ERR", "Image Setting  ERR : " + e.getMessage());
        }

        // 책 정보를 다이얼로그에 띄어준다.
        try {
            view_title.setText(vTitle);
            view_grade.setText(vGrade);
            view_type.setText(vType);
            view_date.setText(vDate);
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Error 발생", Toast.LENGTH_SHORT).show();
            Log.e("ERR", "Data Setting  ERR : " + e.getMessage());
        }

        //설정한 값으로 AlertDialog 객체 생성
        AlertDialog dialog = buider.create();

        //Dialog의 바깥쪽을 터치했을 때 Dialog를 없앨지 설정
        dialog.setCanceledOnTouchOutside(false);//없어지지 않도록 설정

        //Dialog 보이기
        dialog.show();
    }

    /*
       ViewHolder란, 이름 그대로 뷰들을 홀더에 꼽아놓듯이 보관하는 객체를 말한다.
       각각의 Row를 그려낼 때 그 안의 위젯들의 속성을 변경하기 위해 findViewById를 호출하는데,
       이것의 비용이 큰것을 줄이기 위해 사용한다. 여기서 게시판의 정보들을 ViewHolder를 이용해 삽입한다.
     */
    private class ViewHolder {
        public ImageView mIcon;
        public TextView mText;
        public TextView mDate;
        public TextView mInfo;
    }

    /*
        드림 프래그먼트의 리스트뷰를 관리하는 메소드
     */
    private class ListViewAdapter extends BaseAdapter {
        private Context mContext = null;
        private ArrayList<SListData> mListData = new ArrayList<>();

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

        // 사용자가 선택한 아이템 데이터를 layout_supply_board_item 형태에 맞춰 반환한다.
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();

                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.layout_supply_board_item, null);

                holder.mIcon = (ImageView) convertView.findViewById(R.id.mImage);
                holder.mText = (TextView) convertView.findViewById(R.id.mTitle);
                holder.mDate = (TextView) convertView.findViewById(R.id.mDate);
                holder.mInfo = (TextView) convertView.findViewById(R.id.mInfo);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            SListData mData = mListData.get(position); // SListData로부터 해당 아이템의 데이터를 받아온다.

            if (mData.mIcon != null) {
                holder.mIcon.setVisibility(View.VISIBLE);
                holder.mIcon.setImageDrawable(mData.mIcon);
            } else {
                holder.mIcon.setVisibility(View.GONE);
            }
            holder.mText.setText(mData.mTitle);
            holder.mDate.setText(mData.mDate);
            holder.mInfo.setText(mData.mInfo);

            return convertView;
        }

        /*
            리스트에 아이템을 추가하는 메소드
        */
        public void addItem(Drawable icon, String mUnique, String mTitle, String mDate, String mType, String mGrade, String mInfo) {
            SListData addInfo = null;
            addInfo = new SListData();
            addInfo.mIcon = icon;
            addInfo.mTitle = mTitle;
            addInfo.mDate = mDate;
            addInfo.mUnique = mUnique;
            addInfo.mType = mType;
            addInfo.mGrade = mGrade;
            addInfo.mInfo = mInfo;
            mListData.add(addInfo);
        }

        // 리스트를 새로고침 하는 메소드
        public void clear(){
            mListData.clear();
        }

        // 데이터가 바뀌었음을 DB에 알려주는 메소드
        public void dataChange() {
            mAdapter.notifyDataSetChanged();
        }
    }

    /*
        선배가 드림 글을 올리는 경우의 리스너 설정
        과목명, 대여/증여 여부, 책 상태, 책 사진을 등록할 수 있게 한다.
     */
    TextView.OnClickListener writingClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Dialog에서 보여줄 입력화면 View 객체 생성 작업
            // Layout xml 리소스 파일을 View 객체로 부불려 주는(inflate) LayoutInflater 객체 생성
            LayoutInflater inflater = getActivity().getLayoutInflater();

            // res폴더>>layout폴더>>layout_writing_custom_dialog.xml 레이아웃 리소스 파일로 View 객체 생성
            // Dialog의 listener에서 사용하기 위해 final로 참조변수 선언

            final View dialogView = inflater.inflate(R.layout.layout_supply_writing_dialog, null);

            writing_title = (Spinner) dialogView.findViewById(R.id.writing_title);
            writing_grade = (EditText) dialogView.findViewById(R.id.writing_grade);
            writing_image_upload = (TextView) dialogView.findViewById(R.id.image_upload);
            writing_take_photo = (TextView) dialogView.findViewById(R.id.image_take);
            rent_or_give = (RadioGroup) dialogView.findViewById(R.id.stateRadioBtn);
            writting_image = (ImageView) dialogView.findViewById(R.id.writting_image);
            writing_image_upload.setOnClickListener(uploadClickListener);
            writing_take_photo.setOnClickListener(takePhotoClickListener);

            String[] subject = getResources().getStringArray(R.array.title_sp_array);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_dropdown_item, subject);
            writing_title.setAdapter(adapter);
            writing_title.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    getTitle = parent.getItemAtPosition(position).toString();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            AlertDialog.Builder buider = new AlertDialog.Builder(getContext()); // AlertDialog.Builder 객체 생성
            buider.setView(dialogView); // 위에서 inflater가 만든 dialogView 객체 세팅 (Customize)
            buider.setPositiveButton("확인", null);  // Dialog에 "확인"라는 타이틀의 버튼을 설정, 리스너는 뒤에서 붙여줌
            buider.setNegativeButton("취소", new DialogInterface.OnClickListener() {  // Dialog에 "취소"라는 타이틀의 버튼을 설정
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            //설정한 값으로 AlertDialog 객체 생성
            final AlertDialog dialog = buider.create();

            /* 사용자가 확인버튼을 눌렀지만 일정 조건에 맞지 않을 경우 경고 Toast 메세지를 띄우고,
               다이얼로그를 종료하지 않기 위해서 Postive 버튼을 Override 한다. */
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(final DialogInterface d) {
                    Button postive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    postive.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            boolean flag = true;

                            if (flag) { // 사용자가 과목 명을 제대로 입력했는지를 체크
                                if (getTitle == null || getTitle.equals("선    택")) {
                                    flag = false;
                                    Toast.makeText(getActivity(), "과목을 선택 해주세요.", Toast.LENGTH_LONG).show();
                                } else {
                                    flag = true;
                                }
                            }

                            if (flag) { // 사용자가 대여 형태를 제대로 입력했는지를 체크
                                try {
                                    int checkedId = rent_or_give.getCheckedRadioButtonId();
                                    RadioButton selected = (RadioButton) dialogView.findViewById(checkedId);
                                    getState = selected.getText().toString();
                                } catch (Exception e) {
                                    Toast.makeText(getActivity(), "Error 발생", Toast.LENGTH_SHORT).show();
                                    Log.e("ERR", "State  ERR : " + e.getMessage());
                                    flag = false;
                                }
                            }

                            if (flag) { // 사용자가 책 상태를 제대로 입력했는지를 체크
                                try {
                                    if (writing_grade.getText().length() != 0) {
                                        getGrade = writing_grade.getText().toString();
                                    } else if (writing_grade.getText().length() > 50) {
                                        Toast.makeText(getContext(), "50자 미만으로 입력해주세요.", Toast.LENGTH_SHORT).show();
                                        flag = false;
                                    } else {
                                        Toast.makeText(getContext(), "책 상태를 입력해주세요.", Toast.LENGTH_SHORT).show();
                                        flag = false;
                                    }
                                } catch (Exception e) {
                                    Toast.makeText(getActivity(), "Error 발생", Toast.LENGTH_SHORT).show();
                                    Log.e("ERR", "Grade  ERR : " + e.getMessage());
                                    flag = false;
                                }
                            }

                            if (flag) { // 사용자가 이미지를 제대로 업로드 했는지를 체크
                                try {
                                    if (uploadingImage != null) {
                                        getImage = uploadingImage;
                                        flag = true;
                                    } else {
                                        flag = false;
                                        Toast.makeText(getContext(), "이미지를 선택해주세요.", Toast.LENGTH_SHORT).show();
                                    }
                                } catch (Exception e) {
                                    Toast.makeText(getActivity(), "Error 발생", Toast.LENGTH_SHORT).show();
                                    Log.e("ERR", "Image  ERR : " + e.getMessage());
                                    flag = false;
                                }
                            }

                            // 사용자가 입력한 정보가 모드 정상일 때 DB에 업로드 및 게시판 목록에 업로드
                            if (flag) {
                                final DBManager dbManager = new DBManager(getContext(), "App_Data.db", null, 1);
                                HashMap<String, String> dataMap =dbManager.getResult();

                                Calendar cal = Calendar.getInstance();
                                String getDate = (cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DATE));

                                // 게시판 목록에 내용 업로드
                                int n = mAdapter.getCount(); // 현재리스트 숫자로 만드는 uniqueNum
                                mAdapter.addItem(getImage, n+"", getTitle, getDate, getState, getGrade, dataMap.get("id") +" "+ dataMap.get("name"));
                                mAdapter.dataChange();

                                insertDatabase(n+"", getTitle, getDate, getState, getGrade, getImage, dataMap.get("id"), dataMap.get("name"));

                                dialog.dismiss();   // 글쓰기가 완료되면 다이얼로그 종료
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
        사진 업로드를 사용자 핸드폰의 갤러리로부터 하는 경우,
        갤러리로 이동시켜 사진을 선택할 수 있게 하는 리스너
     */
    TextView.OnClickListener uploadClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType(android.provider.MediaStore.Images.Media.CONTENT_TYPE);
            startActivityForResult(intent, PICK_FROM_ALBUM);
        }
    };

    /*
        사용자가 직접 사진을 찍는 경우, 사진 앱으로 연결해주는 리스너
     */
    TextView.OnClickListener takePhotoClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            // 임시로 사용할 파일의 경로를 생성
            String url = "tmp_" + String.valueOf(System.currentTimeMillis()) + ".jpg";
            mImageCaptureUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(), url));

            intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mImageCaptureUri);
            startActivityForResult(intent, PICK_FROM_CAMERA);
        }
    };

    /*
        사용자가 직접 사진을 찍거나 갤러리에서 특정 사진을 선택 한 후 이미지를 편집하는 메소드
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) {
            return;
        }

        switch (requestCode) {
            // 갤러리로 부터 사진을 선택한 경우, 이미지를 가져온다. 이후의 처리가 직접 사진을 찍은 경우와 같으므로 일단  break없이 진행한다.
            case PICK_FROM_ALBUM :
                mImageCaptureUri = data.getData();
                Log.d("BookDREAM", mImageCaptureUri.getPath().toString());
            // 직접 사진을 찍은 경우, 사진을 찍은 후 크기를 알맞게 크롭한다.
            case PICK_FROM_CAMERA:
                // 이미지를 가져온 이후의 리사이즈할 이미지 크기를 결정합니다.
                // 이후에 이미지 크롭 어플리케이션을 호출하게 됩니다.
                Intent intent = new Intent("com.android.camera.action.CROP");
                intent.setDataAndType(mImageCaptureUri, "image/*");

                // CROP할 이미지를 저장
                intent.putExtra("outputX", xValue); // CROP한 이미지의 x축 크기
                intent.putExtra("outputY", yValue); // CROP한 이미지의 y축 크기
                intent.putExtra("aspectX", 1); // CROP 박스의 X축 비율
                intent.putExtra("aspectY", 1); // CROP 박스의 Y축 비율
                intent.putExtra("scale", true);
                intent.putExtra("return-data", true);
                startActivityForResult(intent, CROP_FROM_iMAGE); // CROP_FROM_CAMERA case문 이동
                break;
            // 크롭된 이후의 이미지 설정, 이미지뷰에 이미지를 보여준다거나 부가적인 작업 이후에 임시 파일을 삭제한다.
            case CROP_FROM_iMAGE:
                if (resultCode != RESULT_OK) {
                    return;
                }
                final Bundle extras = data.getExtras();
                // CROP된 이미지를 저장하기 위한 FILE 경로
                String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/BookDREAM/" + System.currentTimeMillis() + ".jpg";
                if (extras != null) {
                    Bitmap photo = extras.getParcelable("data"); // CROP된 BITMAP
                    storeCropImage(photo, filePath); // CROP된 이미지를 외부저장소, 앨범에 저장한다.
                    uploadingImage = new BitmapDrawable(getResources(), photo);
                    writting_image.setImageDrawable(uploadingImage); // 레이아웃의 이미지칸에 CROP된 BITMAP을 보여줌
                    break;
                }

                // 임시 파일 삭제
                File f = new File(mImageCaptureUri.getPath());
                if (f.exists()) {
                    f.delete();
                }
        }
    }

    /*
        크롭할 이미지를 저장하는 메소드
    */
    private void storeCropImage(Bitmap bitmap, String filePath) {
        // SmartWheel 폴더를 생성하여 이미지를 저장하는 방식이다.
        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/BookDREAM/";
        File directory_bookDREAM = new File(dirPath);

        if (!directory_bookDREAM.exists()) { // BookDREAM 디렉터리에 폴더가 없다면 (새로 이미지를 저장할 경우에 속한다.)
            directory_bookDREAM.mkdir();
        }

        File copyFile = new File(filePath);
        BufferedOutputStream out = null;

        try {
            copyFile.createNewFile();
            out = new BufferedOutputStream(new FileOutputStream(copyFile));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);

            // sendBroadcast를 통해 Crop된 사진을 앨범에 보이도록 갱신한다.
            getActivity().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(copyFile)));

            out.flush();
            out.close();
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Error 발생", Toast.LENGTH_SHORT).show();
            Log.e("ERR", "Image Setting  ERR : " + e.getMessage());
        }
    }

    /*
        사용자가 리스트를 아래로 끌어당겨 새로고침을 하는 경우, 데이터를 새로고침 하는 메소드
     */
    SwipeRefreshLayout.OnRefreshListener srefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            mAdapter.clear();
            backgroundInitThread = new InitAsyncThread();
            backgroundInitThread.execute();
            //smSwipeRefreshLayout.setRefreshing(false); //  새로고침이 완료 되었음을 표시.
        }
    };

    /*
        DB에 새로운 데이터를 추가하는 메소드
     */
    public void insertDatabase(String getUniqueNum, String getTitle, String getDate, String getState, String getGrade, Drawable getImage, String getId, String getName) {
        Bundle b = new Bundle();
        b.putString("no", getUniqueNum);
        b.putString("title", getTitle);
        b.putString("date", getDate);
        b.putString("state", getState);
        b.putString("grade", getGrade);
        b.putString("id", getId);
        b.putString("name", getName);
        b.putString("file_name", getUniqueNum+"_"+getTitle+".png");
        Message msg= new Message();
        msg.setData(b);
        msg.obj = getImage;
        backgroundWriteThread = new WriteAsyncThread();
        backgroundWriteThread.execute(msg);
    }

    public class WriteAsyncThread extends AsyncTask<Message, String, String> {
        // Thread를 시작하기 전에 호출되는 함수
        protected void onPreExecute() {
            super.onPreExecute();
        }

        // Thread의 주요 작업을 처리 하는 함수
        // Thread를 실행하기 위해 excute(~)에서 전달한 값을 인자로 받습니다.
        protected String doInBackground(Message... args) {
            Message dataMsg = args[0];
            Bundle stringData = dataMsg.getData();
            URL url = null;
            HttpURLConnection conn = null;
            String urlStr = "";

            urlStr = "http://"+getString(R.string.ip_address)+":8080/BookDreamServerProject/writeSupplyBulletinBoardInfo";

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
                stringDataMap.put("no", stringData .getString("no"));
                stringDataMap.put("title",stringData .getString("title"));
                stringDataMap.put("date",stringData .getString("date"));
                stringDataMap.put("state",stringData .getString("state"));
                stringDataMap.put("grade",stringData .getString("grade"));
                stringDataMap.put("id",stringData .getString("id"));
                stringDataMap.put("name",stringData .getString("name"));
                stringDataMap.put("file_name",stringData .getString("file_name"));
                Bitmap bitmap = ((BitmapDrawable) dataMsg.obj).getBitmap();
                ArrayList<byte[]> list = new ArrayList<byte[]>();
                ByteArrayOutputStream stream = new ByteArrayOutputStream() ;
                bitmap.compress( Bitmap.CompressFormat.JPEG, 100, stream) ;
                list.add(stream.toByteArray());

                ObjectOutputStream oos =new ObjectOutputStream(conn.getOutputStream());
                oos.writeObject(stringDataMap);
                oos.flush();
                oos.writeObject(list);
                oos.flush();
                oos.close();

                if (conn.getResponseMessage().equals("OK")) { // 서버가 받았다면
                    Log.d("test", "gogo");
                }

                conn.disconnect();
            } catch (Exception e) {
                Toast.makeText(getActivity(), "Error 발생", Toast.LENGTH_SHORT).show();
                Log.e("ERR", "WriteAsyncThread ERR : " + e.getMessage());
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
            urlStr = "http://"+getString(R.string.ip_address)+":8080/BookDreamServerProject/requestSupplyBulletinBoardInitInfo";
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
                    ArrayList<byte[]> dataList = (ArrayList<byte[]>)ois.readObject();
                    ArrayList<String> titleList = (ArrayList<String>)ois.readObject();
                    ArrayList<String> dateList = (ArrayList<String>)ois.readObject();
                    ArrayList<String> stateList = (ArrayList<String>)ois.readObject();
                    ArrayList<String> gradeList = (ArrayList<String>)ois.readObject();
                    ArrayList<String> infoList = (ArrayList<String>)ois.readObject();
                    ois.close();

                    for(int i=0; i<dataList.size(); i++) {
                        Message msg = handler.obtainMessage();
                        Bundle b = new Bundle();
                        b.putString("no", i+"");
                        b.putString("title" , titleList.get(i));
                        b.putString("date", dateList.get(i));
                        b.putString("state", stateList.get(i));
                        b.putString("grade", gradeList.get(i));
                        b.putString("info", infoList.get(i));
                        msg.obj = dataList.get(i);
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
        // doInBackground(~)의 리턴값을 인자로 받습니다.
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            smSwipeRefreshLayout.setRefreshing(false); //  새로고침이 완료 되었음을 표시
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

                urlStr = "http://"+getString(R.string.ip_address)+":8080/BookDreamServerProject/removeSupplyBulletinBoardInfo";
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
                    ArrayList<byte[]> dataList = (ArrayList<byte[]>)ois.readObject();
                    ArrayList<String> titleList = (ArrayList<String>)ois.readObject();
                    ArrayList<String> dateList = (ArrayList<String>)ois.readObject();
                    ArrayList<String> stateList = (ArrayList<String>)ois.readObject();
                    ArrayList<String> gradeList = (ArrayList<String>)ois.readObject();
                    ois.close();

                    for(int i=0; i<dataList.size(); i++) {
                        Message msg = handler.obtainMessage();
                        Bundle b = new Bundle();
                        b.putString("no", i+"");
                        b.putString("title" , titleList.get(i));
                        b.putString("date", dateList.get(i));
                        b.putString("state", stateList.get(i));
                        b.putString("grade", gradeList.get(i));
                        msg.obj = dataList.get(i);
                        msg.setData(b);
                        handler.sendMessage(msg);
                    }

                    Log.d("Test","DD");
                }
                conn.disconnect();
                return "OK";
            } catch (Exception e) {
                Toast.makeText(getActivity(), "Error 발생", Toast.LENGTH_SHORT).show();
                Log.e("ERR", "RemoveAsyncThread ERR : " + e.getMessage());
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

    public class AddSupplyPrecentConditionAsyncThread extends AsyncTask<String, String, String> {
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
            urlStr = "http://"+getString(R.string.ip_address)+":8080/BookDreamServerProject/addSupplyPresentConditionInfo";

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
                stringDataMap.put("no", args[0]);
                stringDataMap.put("demand_id",args[1]);
                stringDataMap.put("demand_name", args[2]);

                ObjectOutputStream oos =new ObjectOutputStream(conn.getOutputStream());
                oos.writeObject(stringDataMap);
                oos.flush();
                oos.close();

                if (conn.getResponseMessage().equals("OK")) { // 서버가 받았다면
                    Log.d("test", "gogo");

                    ObjectInputStream ois = new ObjectInputStream(conn.getInputStream());
                    HashMap<String, String> dataMap = (HashMap<String, String>)ois.readObject();
                    ois.close();
                    Message msg = handler.obtainMessage();
                    Bundle b = new Bundle();
                    b.putString("present_condition", "data");
                    Log.d("test_cnt",dataMap.size()+"");
                    if(dataMap.size()==0){
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
                Log.e("ERR", "AddSupplyPrecentConditionAsyncThread ERR : " + e.getMessage());
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