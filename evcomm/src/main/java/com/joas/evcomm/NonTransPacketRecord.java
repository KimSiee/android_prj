/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 4. 8 오전 11:08
 *
 */

package com.joas.evcomm;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.joas.utils.ByteUtil;
import com.joas.utils.LogWrapper;

import java.util.Calendar;

public class NonTransPacketRecord {
    public static final String TAG = "NonTransPacketRecord";
    EVCommDbOpenHelper dbHelper;

    public NonTransPacketRecord(EVCommDbOpenHelper db, String basePath) {
        dbHelper = db;
    }

    public void addRecord(EvPacket evPacket) {
        dbHelper.mDB.beginTransaction();

        try {
            ContentValues values = new ContentValues();
            values.put(EVCommDatabase.NonTransPacketTable.ID, evPacket.getUniqueId());
            values.put(EVCommDatabase.NonTransPacketTable.INS, evPacket.ins);
            byte[] rawData = evPacket.encode();
            String strData = ByteUtil.byteArrayToHexString(rawData, 0, rawData.length);
            values.put(EVCommDatabase.NonTransPacketTable.DATA, strData);

            dbHelper.mDB.insert(EVCommDatabase.NonTransPacketTable.TABLENAME, null, values);
            dbHelper.mDB.setTransactionSuccessful();
        }
        finally {
            dbHelper.mDB.endTransaction();
        }
    }

    public void removeRecord(String id) {
        dbHelper.mDB.delete(EVCommDatabase.NonTransPacketTable.TABLENAME, EVCommDatabase.NonTransPacketTable.ID+"='"+id+"'", null);
    }

    public EvPacket getNextRecord() {
        EvPacket evPacket = null;
        Cursor c = dbHelper.mDB.query(EVCommDatabase.NonTransPacketTable.TABLENAME, null, null, null, null, null,
                            EVCommDatabase.NonTransPacketTable.ID+" asc", "1");

        try {
            if (c.moveToNext()) {
                String strData = c.getString(c.getColumnIndex(EVCommDatabase.NonTransPacketTable.DATA));
                byte[] rawData = ByteUtil.hexStringToByteArray(strData);
                evPacket = new EvPacket(rawData, 0, rawData.length);

                //LogWrapper.v(TAG, "NonTransRecord Get:" + strData);
            }
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "getNextRecord e:"+e.toString());
        }
        finally {
            if (c != null) c.close();
        }

        return evPacket;
    }

    public void removeAll() {
        dbHelper.mDB.delete(EVCommDatabase.NonTransPacketTable.TABLENAME, null, null);
    }
}
