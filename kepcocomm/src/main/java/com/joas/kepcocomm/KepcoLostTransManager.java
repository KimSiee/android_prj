/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 10. 15 오후 4:56
 *
 */

package com.joas.kepcocomm;

import android.database.Cursor;

import com.joas.utils.ByteUtil;

public class KepcoLostTransManager {

    KepcoCommDbHelper dbHelper;

    public KepcoLostTransManager(KepcoCommDbHelper commDbHelper) {
        this.dbHelper = commDbHelper;
    }

    // 모든 데이터를 삭제한다.
    public void eraseAll()
    {
        try
        {
            String strQuery = "DELETE FROM "+KepcoCommDatabase.LostTrnsData.TABLENAME;
            dbHelper.mDB.execSQL(strQuery);
        }
        catch (Exception ex) {}

    }

    public void addTransPacket(KepcoPacket packet) {
        String rawStrMsgId = ByteUtil.byteArrayToHexString(packet.messageID, 0, packet.messageID.length);
        String rawStrData = ByteUtil.byteArrayToHexString(packet.raw, 0, packet.raw.length);

        try
        {
            // Insert를 할경우 기존에 데이터가 있으면 에러가 난다. 이때 REPLACE는 기존에 데이터가 있으면 교체한다.
            String strQuery  = String.format("REPLACE INTO %s(%s, %s) VALUES('%s','%s')",
                    KepcoCommDatabase.LostTrnsData.TABLENAME,
                    KepcoCommDatabase.LostTrnsData.MSGID,
                    KepcoCommDatabase.LostTrnsData.DATA,
                    rawStrMsgId,
                    rawStrData
                    );

            dbHelper.mDB.execSQL(strQuery);
        }
        catch (Exception ex) { }
    }

    public void removeTransPacket(byte[] msgId) {
        String rawStrMsgId = ByteUtil.byteArrayToHexString(msgId, 0, msgId.length);
        try
        {
            String strQuery  = String.format("DELETE FROM %s WHERE %s='%s'",
                    KepcoCommDatabase.LostTrnsData.TABLENAME,
                    KepcoCommDatabase.LostTrnsData.MSGID,
                    rawStrMsgId
                    );

            dbHelper.mDB.execSQL(strQuery);
        }
        catch (Exception ex) { }
    }

    public boolean isPacketBackupNeed(int cmd) {
        boolean ret = false;

        if ( cmd == KepcoProtocol.KepcoCmd.EV_CHARGE_END_35.getID() ||
                cmd == KepcoProtocol.KepcoCmd.GLOB_CARD_APRV_38.getID() ||
                cmd == KepcoProtocol.KepcoCmd.GLOB_FAULT_EVENT_16.getID()) ret = true;

        return ret;
    }

    public KepcoPacket[] getListPacket() {
        KepcoPacket[] list = null;
        Cursor cursor = null;

        try
        {
            String strQuery  = String.format("SELECT %s, %s FROM %s order by %s asc",
                    KepcoCommDatabase.LostTrnsData.MSGID,
                    KepcoCommDatabase.LostTrnsData.DATA,
                    KepcoCommDatabase.LostTrnsData.TABLENAME,
                    KepcoCommDatabase.LostTrnsData.MSGID
                    );
            cursor = dbHelper.mDB.rawQuery(strQuery, null);

            if ( cursor.getCount() > 0 ) {
                list = new KepcoPacket[cursor.getCount()];
                int idx = 0;
                if ( cursor.moveToFirst() ) {
                    do {
                        String data = cursor.getString(1); // Data
                        byte[] rawData = ByteUtil.hexStringToByteArray(data);
                        list[idx++] = new KepcoPacket(rawData, rawData.length);
                    }
                    while ( cursor.moveToNext() );
                }
            }
        }
        catch (Exception ex) { }
        finally {
            if (cursor !=null) cursor.close();
        }
        return list;
    }
}
