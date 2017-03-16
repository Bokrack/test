package com.example.user.bookdream;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

/**
 * 프로젝트명 : Book:DREAM
 * 시      기 : 성공회대학교 글로컬IT학과 2016년도 2학기 실무프로젝트
 * 팀      원 : 200934013 서동형, 201134031 최형근, 201434031 이보라미
 *
 * 설정을 하는 프래그먼트
 * 사용자는 이 프래그먼트에서 만든이 정보, 문의메일 보내기를 할 수 있다.
 **/
public class SettingFragment extends Fragment {
    ListView settingListview;
    String[] settingList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.layout_setting_fragment, container, false);
        settingListview = (ListView)rootView.findViewById(R.id.setting_listView);
        settingList = getResources().getStringArray(R.array.setting_list);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, settingList);
        settingListview.setAdapter(adapter);

        // 사용자가 어떤 리스트 아이템을 선택하는지 확인하는 리스너
        settingListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    if (position == 0) { // 첫 번째 아이템를 선택한 경우, 만든이 정보를 띄워준다.
                        getInformation();
                    } else { // 두 번째 아이템를 선택한 경우, 문의 메일 보내기를 띄워준다.
                        getEmail();
                    }
                } catch(Exception e) {
                    Toast.makeText(getActivity(), "Error 발생", Toast.LENGTH_SHORT).show();
                    Log.e("ERR", "Setting  ERR : " + e.getMessage());
                }
            }
        });

        return rootView;
    }

    /*
        사용자가 만든이 정보를 선택한 경우, 만든이 정보 다이얼로그를 띄워준다.
     */
    private void getInformation() {
        // Dialog에서 보여줄 입력화면 View 객체 생성 작업
        // Layout xml 리소스 파일을 View 객체로 부불려 주는(inflate) LayoutInflater 객체 생성
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // res폴더>>layout폴더>>layout_writing_custom_dialog.xml 레이아웃 리소스 파일로 View 객체 생성
        // Dialog의 listener에서 사용하기 위해 final로 참조변수 선언
        final View dialogView = inflater.inflate(R.layout.activity_made_by_dialog, null);

        AlertDialog.Builder buider = new AlertDialog.Builder(getContext()); //AlertDialog.Builder 객체 생성
        buider.setView(dialogView); //위에서 inflater가 만든 dialogView 객체 세팅 (Customize)
        buider.setNegativeButton("확인", new DialogInterface.OnClickListener() {
            //Dialog에 "확인"라는 타이틀의 버튼을 설정
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        //설정한 값으로 AlertDialog 객체 생성
        AlertDialog dialog = buider.create();
        //Dialog의 바깥쪽을 터치했을 때 Dialog를 없앨지 설정
        dialog.setCanceledOnTouchOutside(false);//없어지지 않도록 설정
        //Dialog 보이기
        dialog.show();
    }

    /*
        사용자가 문의메일 보내기를 선택한 경우, 문의 내용을 적는 다이얼로그를 띄운다.
     */
    private void getEmail() {
        // Dialog에서 보여줄 입력화면 View 객체 생성 작업
        // Layout xml 리소스 파일을 View 객체로 부불려 주는(inflate) LayoutInflater 객체 생성
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // res폴더>>layout폴더>>layout_writing_custom_dialog.xml 레이아웃 리소스 파일로 View 객체 생성
        // Dialog의 listener에서 사용하기 위해 final로 참조변수 선언
        final View dialogView = inflater.inflate(R.layout.layout_sending_email_dialog, null);

        AlertDialog.Builder buider = new AlertDialog.Builder(getContext()); //AlertDialog.Builder 객체 생성
        buider.setView(dialogView); //위에서 inflater가 만든 dialogView 객체 세팅 (Customize)
        buider.setPositiveButton("보내기", new DialogInterface.OnClickListener() {
            //Dialog에 "확인"라는 타이틀의 버튼을 설정
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String contents = null;
                try {
                    EditText content = (EditText)dialogView.findViewById(R.id.mail_content);
                    contents = content.getText().toString();
                } catch(Exception e) {
                    Toast.makeText(getActivity(), "Error 발생", Toast.LENGTH_SHORT).show();
                    Log.e("ERR", "Sending Button  ERR : " + e.getMessage());
                }

                if (contents.length() > 0) { // 사용자가 원하는 문의 내용을 적은 경우,
                    try {
                        sendingEmail(contents); // 사용자가 원하는 앱을 선택하는 메소드로 연결한 후
                        dialog.dismiss();       // 이 다이얼로그를 종료시킨다.
                    } catch (Exception e) {
                        Toast.makeText(getActivity(), "Error 발생", Toast.LENGTH_SHORT).show();
                        Log.e("ERR", "Sending  ERR : " + e.getMessage());
                    }
                } else { // 사용자가 아무 내용을 적지 않는 경우 문의 내용을 다시 입력해달라는 메세지를 띄운다.
                    Toast.makeText(getActivity(), "문의 내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        buider.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        //설정한 값으로 AlertDialog 객체 생성
        AlertDialog dialog = buider.create();
        //Dialog의 바깥쪽을 터치했을 때 Dialog를 없앨지 설정
        dialog.setCanceledOnTouchOutside(false);//없어지지 않도록 설정
        //Dialog 보이기
        dialog.show();
    }

    /*
        실제로 이메일을 보내는 메소드
        사용자가 보내기 버튼을 누른다면, 사용자가 지정된 이메일 앱에 연동되어 "받는이, 보내는이, 제목, 내용"이 자동 완성된다.
        (사용자의 이메일 아이디를 이용해 문의메일을 보낸다)
     */
    public void sendingEmail(String contents) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"pandorlee@naver.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Book:DREAM 문의메일");
        intent.putExtra(Intent.EXTRA_TEXT, contents);
        intent.setType("message/rfc822");
        startActivity(Intent.createChooser(intent, "Select an Email Client:"));
    }
}
