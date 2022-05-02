/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 6. 29 오전 10:37
 */

package com.joas.ocpp.stack;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;

public class OCPPDbOpenHelper {
    private static final String DATABASE_PATH = "/Database/";
    private static final String DATABASE_NAME = "ocppstack.db";
    private static final int DATABASE_VERSION = 6;
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
            db.execSQL(OCPPDatabase.AuthCacheTable.CREATE_TABLE);
            db.execSQL(OCPPDatabase.AuthCacheTable.CREATE_INDEX);

            db.execSQL(OCPPDatabase.LocalAuthTable.CREATE_TABLE);
            db.execSQL(OCPPDatabase.LocalAuthTable.CREATE_INDEX);

            db.execSQL(OCPPDatabase.LocalConfigTable.CREATE_TABLE);
            db.execSQL(OCPPDatabase.LocalConfigTable.CREATE_INDEX);

            db.execSQL(OCPPDatabase.LostTransactionMessage.CREATE_TABLE);
            db.execSQL(OCPPDatabase.LostTransactionMessage.CREATE_INDEX);

            LocalConfig.initLocalConfigTable(db);
        }

        // 버전이 업데이트 되었을 경우 DB를 다시 만들어 준다.
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS "+OCPPDatabase.AuthCacheTable.TABLENAME);
            db.execSQL("DROP TABLE IF EXISTS "+OCPPDatabase.LocalAuthTable.TABLENAME);
            db.execSQL("DROP TABLE IF EXISTS "+OCPPDatabase.LocalConfigTable.TABLENAME);
            db.execSQL("DROP TABLE IF EXISTS "+OCPPDatabase.LostTransactionMessage.TABLENAME);
            onCreate(db);
        }
    }

    public OCPPDbOpenHelper(Context context, String basePath){
        this.mCtx = context;
        this.mDbPath = basePath + DATABASE_PATH;
    }

    public OCPPDbOpenHelper open() throws SQLException {

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
