package com.example.user.bookdream;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 프로젝트명 : Book:DREAM
 * 시      기 : 성공회대학교 글로컬IT학과 2016년도 2학기 실무프로젝트
 * 팀      원 : 200934013 서동형, 201134031 최형근, 201434031 이보라미
 *
 * 정보 프래그먼트를 구성
 * 글로컬IT학과의 책 정보를 보여주고, 사용자가 더 자세한 정보를 원하는 경우 네이버 책 링크로 연결해준다.
 **/
public class InformationFragment extends Fragment {
    public String[] mTitle;    // 제목 저장
    public int[] mImage;       // 책 이미지 ID 저장
    public String[] mContent;  // 책 요약 내용 저장
    public String[] mLink;     // 네이버 책 링크 저장

    TextView infoTitle, infoContent;
    LinearLayout infoGoToSite;
    ImageView infoImage;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.layout_infomation_fragment, container, false);

        mTitle = getResources().getStringArray(R.array.title_info_array);
        mImage = new int[]{
                R.drawable.book01, R.drawable.book02, R.drawable.book03,
                R.drawable.book04, R.drawable.book05, R.drawable.book06,
                R.drawable.book07, R.drawable.book08, R.drawable.book09,
                R.drawable.book10, R.drawable.book11, R.drawable.book12, R.drawable.book13};
        mContent = getResources().getStringArray(R.array.content_info_array);
        mLink = getResources().getStringArray(R.array.link_info_array);
        GridView gridview = (GridView)rootView.findViewById(R.id.bookList);
        gridview.setAdapter(new ImageAdapter(getActivity()));
        gridview.setOnItemClickListener(gridviewOnItemClickListener);

        return rootView;
    }

    /*
        사용자가 GridView에 있는 책 이미지를 클릭한 경우 상세 다이얼로그를 띄워준다.
     */
    private GridView.OnItemClickListener gridviewOnItemClickListener = new GridView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            String title = mTitle[position];
            String content = mContent[position];
            int imageID = mImage[position];
            final String link = mLink[position];

            // Dialog에서 보여줄 입력화면 View 객체 생성 작업
            // Layout xml 리소스 파일을 View 객체로 부불려 주는(inflate) LayoutInflater 객체 생성
            LayoutInflater inflater = getActivity().getLayoutInflater();

            // res폴더>>layout폴더>>layout_information_dialog.xml 레이아웃 리소스 파일로 View 객체 생성
            // Dialog의 listener에서 사용하기 위해 final로 참조변수 선언
            final View dialogView= inflater.inflate(R.layout.layout_information_dialog, null);

            infoTitle = (TextView)dialogView.findViewById(R.id.info_title);
            infoContent = (TextView)dialogView.findViewById(R.id.info_content);
            infoImage = (ImageView)dialogView.findViewById(R.id.info_image);
            infoGoToSite = (LinearLayout)dialogView.findViewById(R.id.info_goToSite);

            try {
                infoTitle.setText(title);
                infoContent.setText(content);

                DisplayMetrics displayMetrics = new DisplayMetrics();
                getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

                // 보여지는 이미지를 사용자의 디바이스 크기에 맞춰 조절한다.
                Drawable img = getResources().getDrawable(imageID);
                Bitmap b = ((BitmapDrawable)img).getBitmap();
                Bitmap bitmapResized = Bitmap.createScaledBitmap(b, (displayMetrics.widthPixels / 2), (displayMetrics.heightPixels / 3), false);
                img = new BitmapDrawable(getResources(), bitmapResized);

                infoImage.setImageDrawable(img);

                infoGoToSite.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
                    }
                });
            } catch (Exception e) {
                Toast.makeText(getActivity(), "Error 발생", Toast.LENGTH_SHORT).show();
                Log.e("ERR", "GridView.OnItemClickListener ERR : " + e.getMessage());
            }

            AlertDialog.Builder buider= new AlertDialog.Builder(getContext()); //AlertDialog.Builder 객체 생성

            buider.setView(dialogView); //위에서 inflater가 만든 dialogView 객체 세팅 (Customize)

            buider.setNegativeButton("확인", new DialogInterface.OnClickListener() {
                //Dialog에 "확인"라는 타이틀의 버튼을 설정
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            //설정한 값으로 AlertDialog 객체 생성
            final AlertDialog dialog = buider.create();

            //Dialog의 바깥쪽을 터치했을 때 Dialog를 없앨지 설정
            dialog.setCanceledOnTouchOutside(false);//없어지지 않도록 설정

            //Dialog 보이기
            dialog.show();
        }
    };

    public class ImageAdapter extends BaseAdapter {
        private Context context;
        public ImageAdapter(Context c) { context = c; }
        public int getCount() { return (null != mImage) ? mImage.length : 0;}
        public Object getItem(int position) { return (null != mImage) ? mImage[position] : 0;}
        public long getItemId(int position) { return position; }

        // 어댑터가 참조하는 각 항목에 대해 새 이미지뷰를 생성한다.
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;

            Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), mImage[position]);
            bmp = Bitmap.createScaledBitmap(bmp, 320, 240, false);

            if (convertView == null) {
                imageView = new ImageView(context);
                imageView.setAdjustViewBounds(true);
                imageView.setImageBitmap(bmp);
            } else {
                imageView = (ImageView)convertView;
            }
            imageView.setImageResource(mImage[position]);
            return imageView;
        }
    }
}