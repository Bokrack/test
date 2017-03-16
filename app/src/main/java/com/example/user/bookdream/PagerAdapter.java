package com.example.user.bookdream;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

/**
 * 프로젝트명 : Book:DREAM
 * 시      기 : 성공회대학교 글로컬IT학과 2016년도 2학기 실무프로젝트
 * 팀      원 : 200934013 서동형, 201134031 최형근, 201434031 이보라미
 *
 * 하나의 엑티비티(MainActivity) 안에서 5개의 Fragment를 구동시키기 위해 필요한 어뎁터
 * 사용자의 선택에 따라 지정된 Fragment를 구동시킨다.
 **/
public class PagerAdapter extends FragmentStatePagerAdapter {
    int mNumOfTabs;

    public PagerAdapter(FragmentManager fm, int NumOfTabs) {
        super(fm);
        this.mNumOfTabs = NumOfTabs;
    }

    /*
        사용자가 탭을 선택함에 맞는 Fragment를 구동시키는 메소드
     */
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                HomeFragment homeTab = new HomeFragment();
                return homeTab;
            case 1:
                DemandFragment demandTab = new DemandFragment();
                return demandTab;
            case 2:
                SupplyFragment supplyTab = new SupplyFragment();
                return supplyTab;
            case 3 :
                InformationFragment qnaTab = new InformationFragment();
                return qnaTab;
            case 4 :
                SettingFragment settingTab = new SettingFragment();
                return settingTab;
            default:
                return null;
        }
    }

    /*
        텝의 갯수를 반환하는 메소드 -> return 5
     */
    @Override
    public int getCount() {
        return mNumOfTabs;
    }
}