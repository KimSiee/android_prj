/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 6. 22 오전 8:15
 *
 */

package com.joas.minsu_comm;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;

public class MemberDbOpenHelper {
    private static final String DATABASE_NAME = "member.db";
    private static final int DATABASE_VERSION = 1;
    public static SQLiteDatabase mDB;
    private DatabaseHelper mDBHelper;
    private Context mCtx;
    private String mDbPath="";

    private class DatabaseHelper extends SQLiteOpenHelper {

        // 생성자
        public DatabaseHelper(Context context, String name,
                              SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        // 최초 DB를 만들때 한번만 호출된다.
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(MemberDatabase.MemberTable.CREATE_TABLE);
        }

        // 버전이 업데이트 되었을 경우 DB를 다시 만들어 준다.
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS "+MemberDatabase.MemberTable.TABLENAME);
            onCreate(db);
        }
    }

    public MemberDbOpenHelper(Context context, String basePath){
        this.mCtx = context;
        this.mDbPath = basePath;
    }

    public MemberDbOpenHelper open() throws SQLException {

        File parent = new File(mDbPath);
        if (!parent.exists()) {
            parent.mkdirs();
        }

        mDBHelper = new DatabaseHelper(mCtx, mDbPath+DATABASE_NAME, null, DATABASE_VERSION);
        mDB = mDBHelper.getWritableDatabase();
        return this;
    }

    public void close(){
        mDB.close();
    }
}
