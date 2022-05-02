/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 6. 30 오전 11:21
 */

package com.joas.ocpp.stack;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.joas.ocpp.msg.IdTagInfo;
import com.joas.utils.LogWrapper;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AuthorizeCache {
    public static final String TAG = "AuthorizeCache";

    OCPPDbOpenHelper dbHelper;

    public AuthorizeCache(OCPPDbOpenHelper helper) {
        this.dbHelper = helper;
    }

    public void saveAuthInfo(String authIdTag, IdTagInfo tagInfo) {
        ContentValues values = new ContentValues();
        values.put(OCPPDatabase.AuthCacheTable.IDTAG, authIdTag);
        if ( tagInfo.getParentIdTag() != null ) {
            values.put(OCPPDatabase.AuthCacheTable.PARENTID, tagInfo.getParentIdTag());
        }
        if ( tagInfo.getExpiryDate() !=  null ) {
            values.put(OCPPDatabase.AuthCacheTable.EXPRIRED, tagInfo.getExpiryDate().getTimeInMillis());
        }
        if ( tagInfo.getStatus().equals("Accepted") == true ) {
            values.put(OCPPDatabase.AuthCacheTable.STATUS, "1");
        }
        else {
            values.put(OCPPDatabase.AuthCacheTable.STATUS, "0");
        }

        dbHelper.mDB.replace(OCPPDatabase.AuthCacheTable.TABLENAME, null, values);
    }

    public void deleteAuthInfo(String authIdTag) {
        dbHelper.mDB.delete(OCPPDatabase.AuthCacheTable.TABLENAME, OCPPDatabase.AuthCacheTable.IDTAG + "=?", new String[] { authIdTag });
    }

    public void clearAuthInfo() {
        dbHelper.mDB.delete(OCPPDatabase.AuthCacheTable.TABLENAME, null, null);
    }


    public IdTagInfo searchCacheAuthInfo(String authIdTag) {
        IdTagInfo retIdTag = null;
        Cursor c = dbHelper.mDB.query(OCPPDatabase.AuthCacheTable.TABLENAME, null, OCPPDatabase.AuthCacheTable.IDTAG + "=?", new String[] {authIdTag}, null, null, null, null);

        try {
            if (c.moveToNext()) {
                Long exprired = c.getLong(c.getColumnIndex(OCPPDatabase.AuthCacheTable.EXPRIRED));
                Calendar tmpTime = null;
                if (exprired != null) {
                    tmpTime = Calendar.getInstance();
                    if (exprired < tmpTime.getTimeInMillis()) {
                        Log.v(TAG, "Expired User:" + authIdTag + ", date:" + exprired);
                        //Delete User
                        deleteAuthInfo(authIdTag);
                        return null;
                    }
                }

                retIdTag = new IdTagInfo();
                retIdTag.setStatus(IdTagInfo.Status.ACCEPTED);
                retIdTag.setParentIdTag(c.getString(c.getColumnIndex(OCPPDatabase.AuthCacheTable.PARENTID)));
                if (tmpTime != null) tmpTime.setTimeInMillis(exprired);
                retIdTag.setExpiryDate(tmpTime);
                Log.v(TAG, "Auth Ok. id:" + c.getInt(c.getColumnIndex("id")) + ", idtag:" + c.getString(c.getColumnIndex(OCPPDatabase.AuthCacheTable.IDTAG)));
            }
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "searchCacheAuthInfo ex:"+e.toString());
        }
        finally {
            if (c != null) c.close();
        }

        return retIdTag;
    }

    public Map<String, IdTagInfo> getAllCacheIdTag() {

        Map<String, IdTagInfo> list = new HashMap<>();
        Cursor c = dbHelper.mDB.query(OCPPDatabase.AuthCacheTable.TABLENAME, null, null, null, null, null, null, null);
//        Cursor c = dbHelper.mDB.query(OCPPDatabase.LocalAuthTable.TABLENAME, null, null, null, null, null, null, null);

        try {
//            if (c.moveToNext()) {
            while (c.moveToNext() != false) {
                Long exprired = c.getLong(c.getColumnIndex(OCPPDatabase.AuthCacheTable.EXPRIRED));
                Calendar tmpTime = null;
                if (exprired != null) {
                    tmpTime = Calendar.getInstance();
                }

                IdTagInfo idTagInfo = new IdTagInfo();
                idTagInfo.setStatus(IdTagInfo.Status.ACCEPTED);
                idTagInfo.setParentIdTag(c.getString(c.getColumnIndex(OCPPDatabase.AuthCacheTable.PARENTID)));
                if (tmpTime != null) tmpTime.setTimeInMillis(exprired);
                idTagInfo.setExpiryDate(tmpTime);

                list.put(c.getString(c.getColumnIndex(OCPPDatabase.AuthCacheTable.IDTAG)), idTagInfo);
            }
        } catch (Exception e) {
            LogWrapper.e(TAG, "searchCacheAuthInfo ex:" + e.toString());
        } finally {
            if (c != null) c.close();
        }

        return list;
    }
}
