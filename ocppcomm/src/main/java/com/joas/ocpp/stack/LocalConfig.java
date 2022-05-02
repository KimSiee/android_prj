/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 2. 6 오후 5:22
 *
 */

package com.joas.ocpp.stack;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.joas.utils.LogWrapper;

import org.json.JSONArray;

import java.util.Arrays;

public class LocalConfig {
    public static final String KEY_AUTH_VERSION = "local_auth_version";
    public static final String KEY_CONNECTOR_AVAILABLE = "key_connector_avail";

    public static final String TAG = "LocalConfig";

    public static final int TYPE_LOCAL = 0;
    public static final int TYPE_OCPP = 1;

    OCPPDbOpenHelper dbHelper;

    public LocalConfig(OCPPDbOpenHelper helper) {
        this.dbHelper = helper;
    }

    // 저장되는 로컬 파라미터값을 저장한다.
    static void initLocalConfigTable(SQLiteDatabase db) {
        db.execSQL(String.format("INSERT INTO %s(%s,%s,%s) VALUES('%s','0', '%d')",
                OCPPDatabase.LocalConfigTable.TABLENAME, OCPPDatabase.LocalConfigTable.KEY, OCPPDatabase.LocalConfigTable.VALUE, OCPPDatabase.LocalConfigTable.TYPE,
                KEY_AUTH_VERSION, TYPE_LOCAL));
    }

    static public String getWhereKey(String key) {
        return " WHERE "+OCPPDatabase.LocalConfigTable.KEY+"='"+key+"'";
    }

    public int getLocalAuthVersion() {
        int version = -1;
        String strQuery = "SELECT "+OCPPDatabase.LocalConfigTable.VALUE +" FROM "+OCPPDatabase.LocalConfigTable.TABLENAME + getWhereKey(KEY_AUTH_VERSION);
        Cursor cursor = null;

        try {
            cursor = dbHelper.mDB.rawQuery(strQuery, null);
            if ( cursor.moveToFirst() ) {
                version = Integer.parseInt(cursor.getString(0));
            }
        } catch(Exception e) {
            LogWrapper.e(TAG, "DB:"+OCPPDatabase.LocalConfigTable.TABLENAME+" Version Query "+strQuery+" ERR!!:"+e.toString());
        }
        finally {
            if (cursor != null) cursor.close();
        }

        return version;
    }

    void replaceKeyValue(String key, String value, int type) {
        String strQuery = String.format("REPLACE INTO %s(%s,%s,%s) VALUES ('%s','%s','%d')",
                OCPPDatabase.LocalConfigTable.TABLENAME,
                OCPPDatabase.LocalConfigTable.KEY,
                OCPPDatabase.LocalConfigTable.VALUE,
                OCPPDatabase.LocalConfigTable.TYPE,
                key, value, type);
        dbHelper.mDB.execSQL(strQuery);
    }

    public void setLocalAuthVersion(int version) {
        replaceKeyValue(KEY_AUTH_VERSION, ""+version, TYPE_LOCAL);
    }

    public void getConnectorAvalList(boolean[] avalList) {
        String strQuery = "SELECT "+OCPPDatabase.LocalConfigTable.VALUE +" FROM "+OCPPDatabase.LocalConfigTable.TABLENAME + getWhereKey(KEY_CONNECTOR_AVAILABLE);
        String value = null;
        Cursor cursor = null;
        try {
            cursor = dbHelper.mDB.rawQuery(strQuery, null);
            if ( cursor.moveToFirst() ) {
                value = cursor.getString(0);
            }
        } catch(Exception e) {
            LogWrapper.e(TAG, "DB:"+OCPPDatabase.LocalConfigTable.TABLENAME+" Version Query "+strQuery+" ERR!!:"+e.toString());
        }
        finally {
            if (cursor != null) cursor.close();
        }

        if ( value == null ) {
            initConnectorAvalList(avalList);
        }
        else {
            try {
                JSONArray array = new JSONArray(value);
                for ( int i=0; i<avalList.length; i++ ) avalList[i] = array.getBoolean(i);
            }
            catch (Exception e) {
                LogWrapper.e(TAG, "Data:"+value+" Parse avalList ERR!!:"+e.toString());
                initConnectorAvalList(avalList);
            }
        }
    }

    void initConnectorAvalList(boolean[] avalList) {
        for ( int i=0; i< avalList.length; i++ ) avalList[i] = true;
        setConnectorAvalList(avalList);
    }

    public void setConnectorAvalList(boolean[] avalList) {
        String strValue = Arrays.toString(avalList);
        replaceKeyValue(KEY_CONNECTOR_AVAILABLE, strValue, TYPE_LOCAL);
    }

    public void saveOcppConfiguration(String key, String value) {
        replaceKeyValue(key, value, TYPE_OCPP);
    }

    public void loadOcppConfiguration(OCPPConfiguration ocppConfiguration) {
        String strQuery= String.format("SELECT %s, %s FROM %s where type='%d'",
                OCPPDatabase.LocalConfigTable.KEY, OCPPDatabase.LocalConfigTable.VALUE, OCPPDatabase.LocalConfigTable.TABLENAME, TYPE_OCPP);

        String key= null;
        String value = null;

        Cursor cursor = null;
        try {
            cursor = dbHelper.mDB.rawQuery(strQuery, null);
            if (cursor != null && cursor.getCount() != 0){
                cursor.moveToFirst();

                do {
                    key = cursor.getString(0);
                    value = cursor.getString(1);
                    ocppConfiguration.setConfigration(key, value);
                    LogWrapper.d(TAG, "Load OCPP CFG: "+ key +" = " + value);
                } while (cursor.moveToNext());
            }
        } catch(Exception e) {
            LogWrapper.e(TAG, "DB:"+OCPPDatabase.LocalConfigTable.TABLENAME+" Version Query "+strQuery+" ERR!!:"+e.toString());
        }
        finally {
            if (cursor != null) cursor.close();
        }
    }
}
