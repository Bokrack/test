package com.example.user.bookdream;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

/**
 * 프로젝트명 : Book:DREAM
 * 시      기 : 성공회대학교 글로컬IT학과 2016년도 2학기 실무프로젝트
 * 팀      원 : 200934013 서동형, 201134031 최형근, 201434031 이보라미
 *
 * 사용자가 로그인을 한 후 구현되는 메인 액티비티.
 * 이 액티비티 안에서 프래그먼트를 교체하는 형식으로 애플리케이션이 구동된다.
 **/
public class MainActivity extends AppCompatActivity {
    private final long	FINSH_INTERVAL_TIME = 2000; // 2초안에 Back 버튼을 2번 누르면 앱 종료 -> 2초
    private long backPressedTime = 0;

    private int[] imageResId = {
            R.drawable.home,
            R.drawable.demand,
            R.drawable.supply,
            R.drawable.qna,
            R.drawable.settings
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent serviceIntent = new Intent(MainActivity.this, MainService.class);
        startService(serviceIntent);    // 서비스 시작

        // 각 5개의 탭 구성 - 각 프래그먼트를 구별하는 용도
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText("메인").setIcon(imageResId[0]));
        tabLayout.addTab(tabLayout.newTab().setText("요청").setIcon(imageResId[1]));
        tabLayout.addTab(tabLayout.newTab().setText("드림").setIcon(imageResId[2]));
        tabLayout.addTab(tabLayout.newTab().setText("정보").setIcon(imageResId[3]));
        tabLayout.addTab(tabLayout.newTab().setText("설정").setIcon(imageResId[4]));
        tabLayout.getBackground().setColorFilter(Color.parseColor("#3247B2"), PorterDuff.Mode.SRC);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        // 5개의 탭에 맞게 프래그먼트 구성
        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        final PagerAdapter adapter = new PagerAdapter (getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

     /*
        뒤로가기 버튼을 2초내로 2번 누를 시 Application 종료
     */
    @Override
    public void onBackPressed() {
        long tempTime = System.currentTimeMillis();
        long intervalTime = tempTime - backPressedTime;

        if ( 0 <= intervalTime && FINSH_INTERVAL_TIME >= intervalTime ) {
            super.onBackPressed();
        } else {
            backPressedTime = tempTime;
            Toast.makeText(getApplicationContext(), "\'뒤로\'버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT).show();
        }
    }
}
