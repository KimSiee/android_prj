/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 10. 15 오후 4:56
 *
 */

package com.joas.kevcscomm;

import android.database.Cursor;

import com.joas.utils.ByteUtil;

public class KevcsLostTransManager {

    KevcsCommDbHelper dbHelper;

    public KevcsLostTransManager(KevcsCommDbHelper commDbHelper) {
        this.dbHelper = commDbHelper;
    }

    // 모든 데이터를 삭제한다.
    public void eraseAll()
    {
        try
        {
            String strQuery = "DELETE FROM "+KevcsCommDatabase.LostTrnsData.TABLENAME;
            dbHelper.mDB.execSQL(strQuery);
        }
        catch (Exception ex) {}

    }

    public void addTransPacket(KevcsPacket packet) {
        String rawStrMsgId = ByteUtil.byteArrayToHexString(packet.messageID, 0, packet.messageID.length);
        String rawStrData = ByteUtil.byteArrayToHexString(packet.raw, 0, packet.raw.length);

        try
        {
            // Insert를 할경우 기존에 데이터가 있으면 에러가 난다. 이때 REPLACE는 기존에 데이터가 있으면 교체한다.
            String strQuery  = String.format("REPLACE INTO %s(%s, %s) VALUES('%s','%s')",
                    KevcsCommDatabase.LostTrnsData.TABLENAME,
                    KevcsCommDatabase.LostTrnsData.MSGID,
                    KevcsCommDatabase.LostTrnsData.DATA,
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
                    KevcsCommDatabase.LostTrnsData.TABLENAME,
                    KevcsCommDatabase.LostTrnsData.MSGID,
                    rawStrMsgId
                    );

            dbHelper.mDB.execSQL(strQuery);
        }
        catch (Exception ex) { }
    }

    public boolean isPacketBackupNeed(int cmd) {
        boolean ret = false;

        if ( cmd == KevcsProtocol.KevcsCmd.EV_CHARGE_END_35.getID() ||
                cmd == KevcsProtocol.KevcsCmd.GLOB_CARD_APRV_38.getID() ||
                cmd == KevcsProtocol.KevcsCmd.GLOB_FAULT_EVENT_16.getID()) ret = true;

        return ret;
    }

    public KevcsPacket[] getListPacket() {
        KevcsPacket[] list = null;
        Cursor cursor = null;

        try
        {
            String strQuery  = String.format("SELECT %s, %s FROM %s order by %s asc",
                    KevcsCommDatabase.LostTrnsData.MSGID,
                    KevcsCommDatabase.LostTrnsData.DATA,
                    KevcsCommDatabase.LostTrnsData.TABLENAME,
                    KevcsCommDatabase.LostTrnsData.MSGID
                    );
            cursor = dbHelper.mDB.rawQuery(strQuery, null);

            if ( cursor.getCount() > 0 ) {
                list = new KevcsPacket[cursor.getCount()];
                int idx = 0;
                if ( cursor.moveToFirst() ) {
                    do {
                        String data = cursor.getString(1); // Data
                        byte[] rawData = ByteUtil.hexStringToByteArray(data);
                        list[idx++] = new KevcsPacket(rawData, rawData.length);
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
