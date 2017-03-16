package com.example.user.bookdream;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.HashMap;

/**
 * 프로젝트명 : Book:DREAM
 * 시      기 : 성공회대학교 글로컬IT학과 2016년도 2학기 실무프로젝트
 * 팀      원 : 200934013 서동형, 201134031 최형근, 201434031 이보라미
 *
 * Sqllite를 상속받아서 애플리케이션 전체적으로 데이터베이스를 이용하게 해준다.
 **/
public class DBManager extends SQLiteOpenHelper {
    public DBManager(Context context, String name, CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE APP_DATA(no INTEGER PRIMARY KEY AUTOINCREMENT, id TEXT,name TEXT, email TEXT);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    /*
        현재 로그인 된 회원 정보를 받아온다.
     */
    public void insert(String id, String name, String email) {
        SQLiteDatabase db = getWritableDatabase(); // 읽고 쓰기가 가능하게 DB 열기
        db.execSQL("INSERT INTO APP_DATA VALUES(null, '" + id + "', '" + name + "','" + email + "');"); // DB에 입력한 값으로 행 추가
        db.close();
    }

    /*
        로그아웃 하고 다시 로그인 하는 경우, 이전의 회원 정보를 삭제할 때 사용
     */
    public void delete(String id) {
        SQLiteDatabase db = getWritableDatabase(); // 입력한 항목과 일치하는 행 삭제
        db.execSQL("DELETE FROM APP_DATA WHERE id='" + id + "';");
        db.close();
    }

    /*
        모든 회원 정보를 DB로 부터 받아와 저장하는 메소드
    */
    public HashMap<String, String> getResult() {
        SQLiteDatabase db = getReadableDatabase(); // 읽기가 가능하게 DB 열기
        HashMap<String,String> result = new HashMap<>();

        // DB에 있는 데이터를 쉽게 처리하기 위해 Cursor를 사용하여 테이블에 있는 모든 데이터 출력
        Cursor cursor = db.rawQuery("SELECT * FROM APP_DATA", null);
        while (cursor.moveToNext()) {
            result.put("id", cursor.getString(1));
            result.put("name", cursor.getString(2));
            result.put("email", cursor.getString(3));
        }
        return result;
    }
}