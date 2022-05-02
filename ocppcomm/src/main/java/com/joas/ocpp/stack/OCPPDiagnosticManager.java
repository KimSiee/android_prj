/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 8. 27 오후 3:57
 *
 */

package com.joas.ocpp.stack;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.joas.ocpp.msg.DiagnosticsStatusNotification;
import com.joas.ocpp.msg.GetDiagnostics;
import com.joas.utils.LogWrapper;
import com.joas.utils.SimpleFTPUpload;
import com.joas.utils.SimpleFTPUploadListener;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;
import com.joas.utils.ZipUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import okio.Timeout;

public class OCPPDiagnosticManager implements SimpleFTPUploadListener {
    public enum DiagnosticType {
        Fault,
        Error,
        System,
        Comm,
        Debug,
        Verbose
    };

    DiagnosticsStatusNotification.Status diagnosticStatus = DiagnosticsStatusNotification.Status.IDLE;

    private static final String TAG = "OCPPDiagnosticManager";
    private static final String DATABASE_PATH = "/Database/";
    private static final String DIAGNOSTIC_PATH = "/Diagnostic/";
    private static final String DATABASE_NAME = "ocpp_diagnostic.db";
    private static final int DATABASE_VERSION = 1;

    OCPPStack ocppStack;
    public static SQLiteDatabase mDB;
    private DatabaseHelper mDBHelper;
    private Context mCtx;
    private String mDbPath="";
    private String mbasePath="";
    private static final SimpleDateFormat formatter =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    Calendar startTime;
    Calendar stopTime;
    String location;
    int reqRetry = 1;
    int reqRetryInterval = 30;
    int retryCnt = 0;

    private String respFileName = "";

    SimpleFTPUpload simpleFTPUpload;

    private class DatabaseHelper extends SQLiteOpenHelper {

        // 생성자
        public DatabaseHelper(Context context, String name,
                              SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        // 최초 DB를 만들때 한번만 호출된다.
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(OCPPDatabase.DiagnosticLogTable.CREATE_TABLE);
        }

        // 버전이 업데이트 되었을 경우 DB를 다시 만들어 준다.
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS "+OCPPDatabase.DiagnosticLogTable.TABLENAME);
            onCreate(db);
        }
    }

    public OCPPDiagnosticManager(Context context, String basePath, OCPPStack stack){
        this.mCtx = context;
        this.mbasePath = basePath;
        this.mDbPath = basePath + DATABASE_PATH;
        this.ocppStack = stack;
    }

    public OCPPDiagnosticManager open() throws SQLException {

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

    void setDiagnosticStatus(DiagnosticsStatusNotification.Status status) {
        diagnosticStatus = status;
        ocppStack.sendDiagnosticsStatusNotification(diagnosticStatus);
    }

    DiagnosticsStatusNotification.Status getDiagnosticStatus() { return diagnosticStatus; }

    public void addLog(DiagnosticType type, String tag, String content) {
        String timestamp = formatter.format(new Date());
        addLog(timestamp, type, tag, content);
    }

    public void addLog(String timestamp, DiagnosticType type, String tag, String content) {
        ContentValues values = new ContentValues();
        values.put(OCPPDatabase.DiagnosticLogTable.TIMESTAMP, timestamp);
        values.put(OCPPDatabase.DiagnosticLogTable.TYPE, type.name());
        values.put(OCPPDatabase.DiagnosticLogTable.TAG, tag);
        values.put(OCPPDatabase.DiagnosticLogTable.CONTENT, content);

        mDB.insert(OCPPDatabase.DiagnosticLogTable.TABLENAME, null, values);
    }

    public void removePastLog() {
        Calendar curTime = Calendar.getInstance();
        curTime.add(Calendar.DATE, -1 * ocppStack.ocppConfiguration.JoasDiagnosticsLogDays );

        String query = String.format("DELETE FROM %s WHERE %s <= '%s'",
                OCPPDatabase.DiagnosticLogTable.TABLENAME,
                OCPPDatabase.DiagnosticLogTable.TIMESTAMP,
                formatter.format(curTime.getTime())
                );
        mDB.execSQL(query);
    }

    public String onRecvGetDiagnostics(GetDiagnostics getDiagnostics) {
        // 이미 FTP 업로드 중이라면 Return함
        if (diagnosticStatus != DiagnosticsStatusNotification.Status.IDLE || simpleFTPUpload != null) return "";

        respFileName = "diagnostics_cpid_" +
                ocppStack.ocppProperty.cpid + "_" +
                new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());


        startTime = getDiagnostics.getStartTime();
        stopTime = getDiagnostics.getStopTime();
        location = getDiagnostics.getLocation().toString();
        reqRetry = getDiagnostics.getRetries() == null ? 1 : getDiagnostics.getRetries().intValue();
        reqRetryInterval = getDiagnostics.getRetryInterval() == null ? 30 : getDiagnostics.getRetryInterval().intValue();
        retryCnt = 0;

        TimeoutTimer timer = new TimeoutTimer(100, new TimeoutHandler() {
            @Override
            public void run() {
                doUploadDiagnosticsLog();
            }
        });
        timer.startOnce();

        setDiagnosticStatus(DiagnosticsStatusNotification.Status.UPLOADING);

        return respFileName+".zip";
    }

    void doUploadDiagnosticsLog() {
        String query = "";

        if ( startTime == null || startTime == null ) {
            // 금일에 해당하는 로그를 얻어온다.

            SimpleDateFormat formatterLike =
                    new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String timeWhere = formatterLike.format(new Date()) + "%";

            query = String.format("SELECT %s || '|' || %s || '|' || %s || '|' || %s FROM %s WHERE %s LIKE '%s'",
                    OCPPDatabase.DiagnosticLogTable.TIMESTAMP,
                    OCPPDatabase.DiagnosticLogTable.TYPE,
                    OCPPDatabase.DiagnosticLogTable.TAG,
                    OCPPDatabase.DiagnosticLogTable.CONTENT,
                    OCPPDatabase.DiagnosticLogTable.TABLENAME,
                    OCPPDatabase.DiagnosticLogTable.TIMESTAMP,
                    timeWhere
                    );
        }
        else {
            startTime.setTimeZone(TimeZone.getDefault());
            stopTime.setTimeZone(TimeZone.getDefault());

            String strStart = formatter.format(startTime.getTime());
            String strStop = formatter.format(stopTime.getTime());

            query = String.format("SELECT %s || '|' || %s || '|' || %s || '|' || %s FROM %s WHERE %s >= '%s' and %s <= '%s'",
                    OCPPDatabase.DiagnosticLogTable.TIMESTAMP,
                    OCPPDatabase.DiagnosticLogTable.TYPE,
                    OCPPDatabase.DiagnosticLogTable.TAG,
                    OCPPDatabase.DiagnosticLogTable.CONTENT,
                    OCPPDatabase.DiagnosticLogTable.TABLENAME,
                    OCPPDatabase.DiagnosticLogTable.TIMESTAMP,
                    strStart,
                    OCPPDatabase.DiagnosticLogTable.TIMESTAMP,
                    strStop);
        }

        LogWrapper.d(TAG, "onRecvGetDiagnostics query:"+query);

        File logFile = new File(mbasePath+DIAGNOSTIC_PATH+respFileName+".log");

        File parent = new File(mbasePath+DIAGNOSTIC_PATH);
        if (!parent.exists()) {
            parent.mkdirs();
        }

        try
        {
            logFile.createNewFile();
        }
        catch (IOException e)
        {
            LogWrapper.e(TAG, "Diagnostics File Create("+logFile.getAbsolutePath()+") error:"+e.toString());
            uploadErrorProcess();
            return;
        }

        try
        {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, false));

            String value = null;
            Cursor cursor = null;

            try {
                cursor = mDB.rawQuery(query, null);
                while ( cursor.moveToNext() ) {
                    value = cursor.getString(0);
                    buf.append(value);
                    buf.newLine();
                }
            } catch(Exception e) {
                LogWrapper.e(TAG, "DB:"+OCPPDatabase.DiagnosticLogTable.TABLENAME+" Version Query "+query+" ERR!!:"+e.toString());
                uploadErrorProcess();
                return;
            }
            finally {
                if (cursor != null) cursor.close();
            }

            buf.close();
        }
        catch (IOException e)
        {
            LogWrapper.e(TAG, "Diagnostics File Write error:"+e.toString());
            uploadErrorProcess();
            return;
        }
        // zip으로 생성
        try {
            ZipUtils.zip(mbasePath + DIAGNOSTIC_PATH + respFileName + ".log", mbasePath + DIAGNOSTIC_PATH + respFileName + ".zip");

            simpleFTPUpload = new SimpleFTPUpload(this);
            simpleFTPUpload.execute(location, mbasePath+DIAGNOSTIC_PATH+respFileName+".zip", respFileName+".zip");
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "Zip Process Error");
            uploadErrorProcess();
        }
    }

    void uploadErrorProcess() {
        simpleFTPUpload = null;
        setDiagnosticStatus(DiagnosticsStatusNotification.Status.UPLOAD_FAILED);
        retryCnt++;

        if ( reqRetry > retryCnt ) {
            LogWrapper.d(TAG, "Error Retry Uploading diagnostics:"+retryCnt);

            TimeoutTimer timer = new TimeoutTimer(reqRetryInterval * 1000, new TimeoutHandler() {
                @Override
                public void run() {
                    setDiagnosticStatus(DiagnosticsStatusNotification.Status.UPLOADING);
                    doUploadDiagnosticsLog();
                }
            });
            timer.startOnce();
        }
        else {
            setDiagnosticStatus(DiagnosticsStatusNotification.Status.IDLE);
        }
    }

    void deleteRespFile() {
        try {
            File file = new File(mbasePath+DIAGNOSTIC_PATH+respFileName+".log");
            if ( file.exists()) {
                file.delete();
            }

            file = new File(mbasePath+DIAGNOSTIC_PATH+respFileName+".zip");
            if ( file.exists()) {
                file.delete();
            }
        }catch (Exception e) {}
    }

    @Override
    public void onSimpleFTPUploadFileError(String err) {
        deleteRespFile();

        uploadErrorProcess();
    }

    @Override
    public void onSimpleFTPUploadFinished() {
        simpleFTPUpload = null;

        deleteRespFile();

        setDiagnosticStatus(DiagnosticsStatusNotification.Status.UPLOADED);
        setDiagnosticStatus(DiagnosticsStatusNotification.Status.IDLE);
    }
}

