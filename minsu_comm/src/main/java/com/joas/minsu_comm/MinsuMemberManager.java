/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 4. 4 오후 3:06
 *
 */

package com.joas.minsu_comm;

import android.content.Context;
import android.database.Cursor;

import com.joas.utils.ARIAEngine;
import com.joas.utils.ByteUtil;
import com.joas.utils.LogWrapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

public class MinsuMemberManager {
    public static String TAG = "MinsuMemberManager";
    public static String TAG_RESRV = "Chargev_Reservation";
    public static String RESERVED_INFO_FILENAME = "psreserv.txt";

    Vector<ReservedInfo> reservedInfoList  = new Vector<>();

    String infoFilePath = "";

    MinsuChargerInfo minsuchargerinfo;

    MemberDbOpenHelper dbHelper;

    String strMemberInfoDown = "";
    int pre_index = -1;

    public MinsuMemberManager(String filePath, MinsuChargerInfo info, Context context) {
        infoFilePath = filePath;

        dbHelper = new MemberDbOpenHelper(context, filePath);
        dbHelper.open();

        updateReservedInfoList();

        this.minsuchargerinfo = info;
    }

    void memberInfoDown(byte dest2, byte sector, byte[] buffer, int blockIdx)
    {
        // 인덱스 초기화
        if (sector == (byte)0xF0)
        {
            strMemberInfoDown = "";
            pre_index = -1;
        }

        // 회원정보 전체 다운로드 시에 첫 시작일 때
        if (dest2 == (byte)0x05 && sector == (byte)0xF0)
        {
            // 필요한 경우 전체삭제함
            removeMemberAll();
            return;
        }

        if (sector == (byte)0xF1 || sector == (byte)0xFF)
        {
            try
            {
                //중복처리 knw170830
                int index = blockIdx;

                if (pre_index != index)
                {
                    strMemberInfoDown += new String(buffer, 0, buffer.length);

                    // 줄바꿈 테그로 String를 나눈다.
                    String[] listCard = strMemberInfoDown.split("\n");
                    String[] cardNum = new String[listCard.length];
                    int listCnt = 0;
                    for (int i = 0; i < listCard.length; i++)
                    {
                        listCard[i] = listCard[i].replace("\r", "");
                        if (listCard[i].length() >= 32)
                        {
                            cardNum[listCnt++] = listCard[i].substring(0, 32);
                        }
                    }

                    if (dest2 == (byte)0x07)
                    {
                        if (listCard[listCard.length - 1].length() != 32)
                        {
                            strMemberInfoDown = listCard[listCard.length - 1];
                        }
                        else {
                            strMemberInfoDown = "";
                        }
                        deleteMembers(cardNum, listCnt);
                    }
                    else
                    {
                        if (listCard[listCard.length - 1].length() != 34)
                        {
                            strMemberInfoDown = listCard[listCard.length - 1];
                        }
                        else {
                            strMemberInfoDown = "";
                        }
                        addMembers(cardNum, listCnt);
                    }
                }
                pre_index = index;
            }
            catch (Exception e)
            {
                LogWrapper.e(TAG, "memberInfoDown:"+e.toString());
            }
        }

    }

    public void removeMemberAll() {
        try
        {
            String strQuery = "DELETE FROM "+MemberDatabase.MemberTable.TABLENAME;
            dbHelper.mDB.execSQL(strQuery);
        }
        catch (Exception ex) {
            LogWrapper.e(TAG, "removeMemberAll:"+ex.toString());
        }
    }

    // 사용자를 추가한다.
    public void addMembers(String[] card_no, int cnt)
    {
        try {
            // Insert를 할경우 기존에 데이터가 있으면 에러가 난다. 이때 REPLACE는 기존에 데이터가 있으면 교체한다.
            String start_strQuery = String.format("REPLACE INTO %s(%s) VALUES ",
                    MemberDatabase.MemberTable.TABLENAME,
                    MemberDatabase.MemberTable.CARD_NO);
            String querycmd = "";
            StringBuilder srb = new StringBuilder();

            for (int i = 0; i < cnt; i++) {
                srb.append("('" + card_no[i] + "')");
                if (i != 0 && i % 400 == 0) {       //400개 단위로 data 저장, sqlite select 한번 실행가능 최대 항목수 500개 이기 떄문
                    querycmd = start_strQuery + srb.toString();
                    dbHelper.mDB.execSQL(querycmd);
                    srb.setLength(0);
                } else {
                    if (i == cnt - 1) {
                        //마지막일 경우 쿼리 실행
                        querycmd = start_strQuery + srb.toString();
                        dbHelper.mDB.execSQL(querycmd);
                    }
                    else srb.append(",");
                }
            }
        }
        catch (Exception ex) {
            LogWrapper.e(TAG, "addMembers:"+ex.toString());
        }
    }

    // 번호를 사용하여 사용자를 지운다.
    public void deleteMembers(String[] card_no, int cnt)
    {
        try
        {
            // Insert를 할경우 기존에 데이터가 있으면 에러가 난다. 이때 REPLACE는 기존에 데이터가 있으면 교체한다.
            String strQuery  = String.format("DELETE FROM %s WHERE ",
                    MemberDatabase.MemberTable.TABLENAME);

            for (int i = 0; i < cnt; i++)
            {
                strQuery += " " + MemberDatabase.MemberTable.CARD_NO + "= '" + card_no[i] + "'";
                if (i != (cnt - 1)) strQuery += " OR ";
            }
            dbHelper.mDB.execSQL(strQuery);
        }
        catch (Exception ex) {
            LogWrapper.e(TAG, "deleteMembers:"+ex.toString());
        }
    }

    // 사용자를 검색하여 password를 리턴한다. 없으면 null를 리턴한다.

    public boolean searchMember(String card_no)
    {
        boolean ret = false;
        Cursor cursor = null;

        try
        {
            String strQuery  = String.format("SELECT %s FROM %s WHERE %s='%s'",
                    MemberDatabase.MemberTable.CARD_NO,
                    MemberDatabase.MemberTable.TABLENAME,
                    MemberDatabase.MemberTable.CARD_NO,
                    card_no);
            cursor = dbHelper.mDB.rawQuery(strQuery, null);

            if ( cursor.getCount() > 0 ) {
                ret = true;
            }
        }
        catch (Exception ex) {
            LogWrapper.e(TAG, "searchMember:"+ex.toString());
        }
        finally {
            if (cursor !=null) cursor.close();
        }

        return ret;
    }

    public int getMemberNumber(){
        int ret = 0;
        Cursor cursor = null;

        try
        {
            String strQuery  = String.format("SELECT COUNT(*) FROM %s", MemberDatabase.MemberTable.TABLENAME);
            cursor = dbHelper.mDB.rawQuery(strQuery, null);
            if ( cursor.moveToFirst() ) {
                ret = cursor.getInt(0);
            }
        }
        catch (Exception ex) {
            LogWrapper.e(TAG, "getMemberNumber:"+ex.toString());
        }
        finally {
            if (cursor !=null) cursor.close();
        }

        return ret;
    }

    //예약명령이 내려오면 호출됨
    public void updateReservedInfoList() {
        reservedInfoList = new Vector<>(); // 초기화

        FileInputStream is;
        BufferedReader reader;
        File file = new File(infoFilePath, RESERVED_INFO_FILENAME);

        try {
            if (file.exists()) {
                is = new FileInputStream(file);
                reader = new BufferedReader(new InputStreamReader(is));
                String line = reader.readLine();
                while (line != null) {
                    try {
                        ReservedInfo info = ReservedInfo.Parse(line);
                        if ( info != null ) reservedInfoList.add(info);
                    }
                    catch (Exception e) {
                        LogWrapper.d(TAG, "readMemberFile Data:("+line+") Conflict:"+e.toString());
                    }
                    line = reader.readLine();
                }

                LogWrapper.d(TAG_RESRV, "Reservation updated..");
            }
        }catch (Exception e) {
            LogWrapper.d(TAG, "updateReservedInfoList:"+e.toString());
        }
    }

    /**
     * add by si. 200605 UID 복호화 함수(차지비)
     * @param uid 암호화된 UID 값
     * @return 복호화된 UID Stringdfd
     */
    public String DecryptChargevMemberUid(String uid) {
        String ret = "";
        try {
            ARIAEngine engine = new ARIAEngine(128, "Success ChargEV!");
            byte[] org = ByteUtil.hexStringToByteArray(uid);
            byte[] out = new byte[16];
            engine.decrypt(org, 0, out, 0);
            String decryptUID = new String(out);
            ret = decryptUID;
        } catch (Exception e) {
            LogWrapper.d(TAG, "DecryptUID:" + e.toString());
        }

        return ret;
    }

    public boolean localAuthentication(String cardNum) {
        boolean ret = false;
        try {
            ARIAEngine engine = new ARIAEngine(128, "Success ChargEV!");
            byte[] org = cardNum.getBytes();
            byte[] out = engine.encrypt(org, 0);

            String key = ByteUtil.byteArrayToHexString(out, 0, 16);

            ret = searchMember(key);

            /*
            // 1. Search Member
            ret = memberInfoList.containsKey(key);

            // 2. Search Member Add if not found member
            if ( ret == false ) ret = memberAddInfoList.containsKey(key);

            // 3. Search Member Del if found member (if found del member, return false)
            if ( ret == true ) ret = !memberDelInfoList.containsKey(key);
            */

            //LogWrapper.d("OUT", ByteUtil.byteArrayToHexString(out, 0, 16));
        }
        catch (Exception e) {
            LogWrapper.d(TAG, "localAuthentication:"+e.toString());
        }
        return ret;
    }

    public long calcReservStartTime(ReservedInfo info) {
        long diff = 0;
        long min = 0;

        try {
            //현재시간
            Date now = Calendar.getInstance().getTime();

            //예약시작시간
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            //예약 시작시간 문자열 변환
            String str_rsv_starttime = dateFormat.format(info.startDateTime);
            //예약시작시간을 Date로 parsing후 time 가져오기
            Date rsvStartTime = dateFormat.parse(str_rsv_starttime);
            long rsv_start_time = rsvStartTime.getTime();

            //시간차 구하기(분단위)
            diff = now.getTime() - rsv_start_time;
            min = (diff / 1000) / 60;

        } catch (Exception e) {
            LogWrapper.d(TAG, "checkReservDuration:" + e.toString());
        }

        return min;
    }

    public void getNextReservedInfo() {
        ReservedInfo ret = null;

        try {

            int reservedMemberCnt = reservedInfoList.size();

            if(reservedMemberCnt == 0) {
                minsuchargerinfo.rsv_flag = 0;
            }
            else {
                for (ReservedInfo info : reservedInfoList) {
                    //1. 예약시작시간으로부터 10분 경과했을 경우 해당 예약리스트 삭제
                    long min = calcReservStartTime(info);
                    if (min > 10) {
                        reservedInfoList.remove(0);
//                    continue;
                        break;
                    }


                    //2. 그외에는 그 다음 항목이 해당 다음 예약(혹은 현재)임
                    ret = info;

                    minsuchargerinfo.rsv_orderNum = ret.orderNum;
                    minsuchargerinfo.rsv_startDay = ret.startDay;
                    minsuchargerinfo.rsv_startTime = ret.startTime;
                    minsuchargerinfo.rsv_chargingTimeMin = ret.chargingTimeMin;
                    String rawUid = ret.uid;
                    minsuchargerinfo.rsv_uid = DecryptChargevMemberUid(rawUid);
                    minsuchargerinfo.rsv_startDateTime = ret.startDateTime;
                    minsuchargerinfo.rsv_endDateTime = ret.endDateTime;


                    //예약충전까지 잔여시간(플래그별 구분)
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    //예약 시작시간 문자열 변환
                    String str_rsv_starttime = dateFormat.format(minsuchargerinfo.rsv_startDateTime);

                    //현재시간
                    Date curDate = new Date();

                    //예약시작시간을 Date로 parsing후 time 가져오기
                    Date rsvStartTime = dateFormat.parse(str_rsv_starttime);
                    long rsv_start_time = rsvStartTime.getTime();

                    //현재시간을 Date로 parsing후 time 가져오기
                    curDate = dateFormat.parse(dateFormat.format(curDate));
                    long cur_date_time = curDate.getTime();

                    //시간차이를 분으로 표현
                    minsuchargerinfo.rsv_leftMin = (rsv_start_time - cur_date_time) / 60000;


                    if (minsuchargerinfo.rsv_leftMin > 30) {
                        //예약시간까지 30분 이상 남았을 경우
                        minsuchargerinfo.rsv_flag = 1;
                    } else if (minsuchargerinfo.rsv_leftMin <= 10) {    //예약시간까지 10분이하 남을경우, 예약자만 사용가능,
                        minsuchargerinfo.rsv_flag = 3;
                    } else
                        minsuchargerinfo.rsv_flag = 2;        //예약시간까지 10분이상 30분 미만일 경우, 비예약자 남은시간동안 충전 가능
                    break;
                }
            }
        }catch (Exception e)
        {
            LogWrapper.d(TAG,"Get ReservedInfo:" + e.toString());
        }
    }

    public static class ReservedInfo {
        public String orderNum = "";
        public String startDay = "";
        public String startTime = "";
        public int chargingTimeMin = 0;
        public String uid = "";

        public Date startDateTime;
        public Date endDateTime;

        static public ReservedInfo Parse(String data) {
            String[] list = data.split(",");
            if ( list.length < 5 ) return null;

            ReservedInfo ret = new ReservedInfo();
            ret.orderNum = list[0];
            ret.startDay = list[1];
            ret.startTime = list[2];
            ret.chargingTimeMin = Integer.parseInt(list[3]);
            ret.uid = list[4];
            String strDate = ret.startDay + " " + ret.startTime + ":00";
            try {
                ret.startDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(strDate);
                Calendar cal = Calendar.getInstance();
                cal.setTime(ret.startDateTime);
                cal.add(Calendar.MINUTE, ret.chargingTimeMin);
                ret.endDateTime = cal.getTime();
            }
            catch (Exception e ) {
                LogWrapper.d(TAG,"ReservedInfo Parse:" + e.toString());
                ret = null;
            }

            return ret;
        }
    }
}
