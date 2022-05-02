/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 9. 30 오후 2:09
 *
 */

package com.joas.kevcscomm;

import android.database.Cursor;

public class KevcsMember {

    KevcsCommDbHelper dbHelper;

    public KevcsMember(KevcsCommDbHelper commDbHelper) {
        this.dbHelper = commDbHelper;
    }

    // 모든 데이터를 삭제한다.
    public void eraseAll()
    {
        try
        {
            String strQuery = "DELETE FROM "+KevcsCommDatabase.CardMember.TABLENAME;
            dbHelper.mDB.execSQL(strQuery);
        }
        catch (Exception ex) {}

    }

    // 사용자를 추가한다.
    public void addMembers(String[] card_no, String[] card_pass, String[] add_cl, int cnt)
    {
        try
        {
            // Insert를 할경우 기존에 데이터가 있으면 에러가 난다. 이때 REPLACE는 기존에 데이터가 있으면 교체한다.
            String strQuery  = String.format("REPLACE INTO %s(%s, %s, %s) VALUES",
                    KevcsCommDatabase.CardMember.TABLENAME,
                    KevcsCommDatabase.CardMember.CARD_NO,
                    KevcsCommDatabase.CardMember.CARD_PWD,
                    KevcsCommDatabase.CardMember.ADD_CL);

            for (int i = 0; i < cnt; i++)
            {
                strQuery += "('" + card_no[i] + "','" + card_pass[i] + "','" + add_cl[i] + "')";
                if (i != (cnt - 1)) strQuery += ",";
            }
            dbHelper.mDB.execSQL(strQuery);
        }
        catch (Exception ex) { }
    }

    // 번호를 사용하여 사용자를 지운다.
    public void removeMember(String card_no)
    {
        try
        {
            String strQuery  = String.format("DELETE FROM %s WHERE %s='%s'",
                    KevcsCommDatabase.CardMember.TABLENAME,
                    KevcsCommDatabase.CardMember.CARD_NO,
                    card_no);

            dbHelper.mDB.execSQL(strQuery);
        }
        catch (Exception ex) { }
    }

    public boolean searchMember(String card_no)
    {
        boolean ret = false;
        Cursor cursor = null;

        try
        {
            String strQuery  = String.format("SELECT %s, %s FROM %s WHERE %s='%s' and %s='1'",
                    KevcsCommDatabase.CardMember.CARD_NO,
                    KevcsCommDatabase.CardMember.CARD_PWD,
                    KevcsCommDatabase.CardMember.TABLENAME,
                    KevcsCommDatabase.CardMember.CARD_NO,
                    card_no,
                    KevcsCommDatabase.CardMember.ADD_CL);
            cursor = dbHelper.mDB.rawQuery(strQuery, null);

            if ( cursor.getCount() > 0 ) {
                ret = true;
            }
        }
        catch (Exception ex) { }
        finally {
            if (cursor !=null) cursor.close();
        }

        return ret;
    }

    public boolean searchMember(String card_no, String password)
    {
        boolean ret = false;
        Cursor cursor = null;

        try
        {
            String strQuery  = String.format("SELECT %s, %s FROM %s WHERE %s='%s' and %s='%s' and %s='1'",
                    KevcsCommDatabase.CardMember.CARD_NO,
                    KevcsCommDatabase.CardMember.CARD_PWD,
                    KevcsCommDatabase.CardMember.TABLENAME,
                    KevcsCommDatabase.CardMember.CARD_NO,
                    card_no,
                    KevcsCommDatabase.CardMember.CARD_PWD,
                    password,
                    KevcsCommDatabase.CardMember.ADD_CL);
            cursor = dbHelper.mDB.rawQuery(strQuery, null);

            if ( cursor.getCount() > 0 ) {
                ret = true;
            }
        }
        catch (Exception ex) { }
        finally {
            if (cursor !=null) cursor.close();
        }

        return ret;
    }
}
