/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 2. 6 오전 10:59
 *
 */

package com.joas.ocpp.stack;

import android.content.ContentValues;
import android.database.Cursor;

import com.joas.ocpp.msg.IdTagInfo;
import com.joas.ocpp.msg.LocalAuthorizationList;
import com.joas.utils.LogWrapper;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalAuthList {
    public static final String TAG = "LocalAuthList";

    OCPPDbOpenHelper dbHelper;

    public LocalAuthList(OCPPDbOpenHelper helper) {
        this.dbHelper = helper;
    }

    public void updateList(List<LocalAuthorizationList> authList, boolean isFull) {
        if ( isFull ) {
            dbHelper.mDB.execSQL("DROP TABLE IF EXISTS " + OCPPDatabase.LocalAuthTable.TABLENAME);
            dbHelper.mDB.execSQL(OCPPDatabase.LocalAuthTable.CREATE_TABLE);
            dbHelper.mDB.execSQL(OCPPDatabase.LocalAuthTable.CREATE_INDEX);
        }

        dbHelper.mDB.beginTransaction();

        try {
            for ( LocalAuthorizationList info : authList ) {  // loop through your records

                ContentValues values = new ContentValues();
                values.put(OCPPDatabase.LocalAuthTable.IDTAG, info.getIdTag());
                IdTagInfo tagInfo = info.getIdTagInfo();
                if ( tagInfo.getParentIdTag() != null ) {
                    values.put(OCPPDatabase.LocalAuthTable.PARENTID, info.getIdTagInfo().getParentIdTag());
                }
                if ( tagInfo.getExpiryDate() !=  null ) {
                    values.put(OCPPDatabase.LocalAuthTable.EXPRIRED, tagInfo.getExpiryDate().getTimeInMillis());
                }
                if ( tagInfo.getStatus().equals("Accepted") == true ) {
                    values.put(OCPPDatabase.LocalAuthTable.STATUS, "1");
                }
                else {
                    values.put(OCPPDatabase.LocalAuthTable.STATUS, "0");
                }

                if ( isFull ) {
                    dbHelper.mDB.insert(OCPPDatabase.LocalAuthTable.TABLENAME, null, values);
                } else {
                    dbHelper.mDB.replace(OCPPDatabase.LocalAuthTable.TABLENAME, null, values);
                }
            }

            dbHelper.mDB.setTransactionSuccessful();
        }
        finally {
            dbHelper.mDB.endTransaction();
        }
    }

    public IdTagInfo searchLocalAuthInfo(String authIdTag) {
        IdTagInfo retIdTag = null;
        Cursor c = dbHelper.mDB.query(OCPPDatabase.LocalAuthTable.TABLENAME, null, OCPPDatabase.LocalAuthTable.IDTAG + "=?", new String[] {authIdTag}, null, null, null, null);

        try {
            if (c.moveToNext()) {
                Long exprired = c.getLong(c.getColumnIndex(OCPPDatabase.LocalAuthTable.EXPRIRED));
                Calendar tmpTime = null;
                if (exprired != null) {
                    tmpTime = Calendar.getInstance();
                    if (exprired < tmpTime.getTimeInMillis()) {
                        LogWrapper.v(TAG, "Expired User:" + authIdTag + ", date:" + exprired);
                        return null;
                    }
                }

                retIdTag = new IdTagInfo();
                retIdTag.setStatus(IdTagInfo.Status.ACCEPTED);
                retIdTag.setParentIdTag(c.getString(c.getColumnIndex(OCPPDatabase.LocalAuthTable.PARENTID)));
                if (tmpTime != null) tmpTime.setTimeInMillis(exprired);
                retIdTag.setExpiryDate(tmpTime);
                LogWrapper.v(TAG, "Auth Ok. id:" + c.getInt(c.getColumnIndex("id")) + ", idtag:" + c.getString(c.getColumnIndex(OCPPDatabase.LocalAuthTable.IDTAG)));
            }
        } catch (Exception e) {
            LogWrapper.e(TAG, "searchLocalAuthInfo ex:"+e.toString());
        }
        finally {
            if (c != null) c.close();
        }

        return retIdTag;
    }

    public Map<String, IdTagInfo> getAllLocalAuthList() {
        Map<String, IdTagInfo> list = new HashMap<>();
        Cursor c = dbHelper.mDB.query(OCPPDatabase.LocalAuthTable.TABLENAME, null, null, null, null, null, null, null);

        try {

            while(c.moveToNext()!=false){
                Long exprired = c.getLong(c.getColumnIndex(OCPPDatabase.LocalAuthTable.EXPRIRED));
                Calendar tmpTime = null;
                if (exprired != null) {
                    tmpTime = Calendar.getInstance();
                }

                IdTagInfo idTagInfo = new IdTagInfo();
                idTagInfo.setStatus(IdTagInfo.Status.ACCEPTED);
                idTagInfo.setParentIdTag(c.getString(c.getColumnIndex(OCPPDatabase.LocalAuthTable.PARENTID)));
                if (tmpTime != null) tmpTime.setTimeInMillis(exprired);
                idTagInfo.setExpiryDate(tmpTime);

                list.put(c.getString(c.getColumnIndex(OCPPDatabase.LocalAuthTable.IDTAG)), idTagInfo);
            }
//            if (c.moveToNext()) {
//                Long exprired = c.getLong(c.getColumnIndex(OCPPDatabase.LocalAuthTable.EXPRIRED));
//                Calendar tmpTime = null;
//                if (exprired != null) {
//                    tmpTime = Calendar.getInstance();
//                }
//
//                IdTagInfo idTagInfo = new IdTagInfo();
//                idTagInfo.setStatus(IdTagInfo.Status.ACCEPTED);
//                idTagInfo.setParentIdTag(c.getString(c.getColumnIndex(OCPPDatabase.LocalAuthTable.PARENTID)));
//                if (tmpTime != null) tmpTime.setTimeInMillis(exprired);
//                idTagInfo.setExpiryDate(tmpTime);
//
//                list.put(c.getString(c.getColumnIndex(OCPPDatabase.LocalAuthTable.IDTAG)), idTagInfo);
//            }
        } catch (Exception e) {
            LogWrapper.e(TAG, "searchLocalAuthInfo ex:"+e.toString());
        }
        finally {
            if (c != null) c.close();
        }

        return list;
    }
}
