/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 12. 14 오후 5:45
 *
 */

package com.joas.j14_touch_2ch_comm;


import com.joas.utils.ByteUtil;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.Calendar;

public class j14_touch_2ch_Packet {
    /**
     *  범위를 벗어나지 않는 범위에서 안전하게 배열을 복사한다.(나머지는 0x00)
     * @param src 소스
     * @param srcidx 소스 오프셋
     * @param dst 목적지
     * @param dstIdx 목적지 오프셋
     * @param size 크기
     */
    public static void safeArrayCopy(byte[] src, int srcidx, byte[] dst, int dstIdx, int size) {
        byte[] tmpBuf = new byte[size];

        if ( src != null ) {
            int srcSize = (src.length - srcidx) > size ? size : (src.length - srcidx);
            System.arraycopy(src, srcidx, tmpBuf, 0, srcSize);
        }

        int dstSize = (dst.length - dstIdx) > size ? size : (dst.length - dstIdx);
        System.arraycopy(tmpBuf, 0, dst, dstIdx, dstSize);
    }

    /**
     *  범위를 벗어나지 않는 범위에서 안전하게 배열을 복사한다.(나머지는 0x20)
     * @param src 소스
     * @param srcidx 소스 오프셋
     * @param dst 목적지
     * @param dstIdx 목적지 오프셋
     * @param size 크기
     */
    public static void safeArrayCopy20(byte[] src, int srcidx, byte[] dst, int dstIdx, int size) {
        byte[] tmpBuf = new byte[size];

        for (int i = 0; i < size; i++) {
            tmpBuf[i] = 0x20;
        }

        if ( src != null ) {
            int srcSize = (src.length - srcidx) > size ? size : (src.length - srcidx);
            System.arraycopy(src, srcidx, tmpBuf, 0, srcSize);
        }

        int dstSize = (dst.length - dstIdx) > size ? size : (dst.length - dstIdx);
        System.arraycopy(tmpBuf, 0, dst, dstIdx, dstSize);
    }


    /**
     * Fill Basic Field: Charger Mode, Status, meter value
     * @param vdData Variable Data in EvPacket
     * @param info Charger Info Data
     * @return size of fields
     */
    public static int fillBasicField(byte[] vdData, j14_touch_2ch_ChargerInfo info) {
        int idx = 0;

        ByteUtil.wordToByteArray(info.cpMode, vdData, idx);
        idx += 2;

        ByteUtil.longToByteArray(info.cpStatus, vdData, 8, idx);
        idx += 8;

        ByteUtil.intToByteArray(info.meterVal, vdData, idx);
        idx += 4;
        return idx;
    }

    public static int fillChargingStatus(byte[] vdData, int idx, j14_touch_2ch_ChargerInfo info) {
        safeArrayCopy(info.cardNum.getBytes(), 0, vdData, idx, 16);
        idx += 16;

        vdData[idx++] = (byte)info.reqAmoundSel;

        ByteUtil.intToByteArray(info.reqAmountKwh, vdData, idx);
        idx += 4;

        ByteUtil.intToByteArray(info.reqAmountCost, vdData, idx);
        idx += 4;

        vdData[idx++] = (byte)info.payMethod;

        ByteUtil.wordToByteArray(info.socBatt , vdData, idx);
        idx += 2;

        ByteUtil.intToByteArray(info.curChargingKwh, vdData, idx);
        idx += 4;

        ByteUtil.intToByteArray(info.curChargingCost, vdData, idx);
        idx += 4;

        ByteUtil.intToByteArray(info.curChargingCostUnit, vdData, idx);
//        ByteUtil.intToByteArray((int)(info.curChargingCostUnit*100), vdData, idx);        //단가 타입 변환

        idx += 4;

        vdData[idx++] = (byte)info.battStatus;

        ByteUtil.intToByteArray(info.battTotalAmount, vdData, idx);
        idx += 4;

        ByteUtil.intToByteArray(info.curBattRemain, vdData, idx);
        idx += 4;

        ByteUtil.intToByteArray(info.battVoltage, vdData, idx);
        idx += 4;

        ByteUtil.intToByteArray(info.battAmpare, vdData, idx);
        idx += 4;

        ByteUtil.intToByteArray(info.battTemperature, vdData, idx);
        idx += 4;

        System.arraycopy(info.BMSVer.getBytes(), 0, vdData, idx, 4);
        idx += 4;

        long remainTime = info.remainTime/60 * 100 + info.remainTime%60;
        byte[] hexRemainTime = ByteUtil.decimalToBcd(remainTime,2);
        vdData[idx++] = hexRemainTime[0];
        vdData[idx++] = hexRemainTime[1];

        System.arraycopy(info.dbCode, 0, vdData, idx, 2);
        idx += 2;
        return  idx;
    }

    public static int fillInstallationInfo(byte[] vdData, int idx, j14_touch_2ch_ChargerInfo info) {
        safeArrayCopy(info.stationId.getBytes(), 0, vdData, idx, 8);
        idx += 8;

        safeArrayCopy(info.chargerId.getBytes(), 0, vdData, idx, 2);
        idx += 2;

        safeArrayCopy(info.portNumber.getBytes(), 0, vdData, idx, 4);
        idx += 4;

        try {
            InetAddress ip = InetAddress.getByName(info.serverIp);
            byte[] rawIpAddr = ip.getAddress();
            System.arraycopy(rawIpAddr, 0, vdData, idx, 4);
            idx += 4;
        }
        catch (Exception e) {}

        safeArrayCopy(info.paymentPortNumber.getBytes(), 0, vdData, idx, 4);
        idx += 4;

        try {
            InetAddress ip = InetAddress.getByName(info.paymentServerIp);
            byte[] rawIpAddr = ip.getAddress();
            System.arraycopy(rawIpAddr, 0, vdData, idx, 4);
            idx += 4;
        }
        catch (Exception e) {}

        safeArrayCopy20(info.mtoPhoneNumber.getBytes(), 0, vdData, idx, 16);
        idx += 16;

        safeArrayCopy20(info.gpsLocInfo.getBytes(), 0, vdData, idx, 25);
        idx += 25;

        vdData[idx++] = info.manufacturerCode;
        vdData[idx++] = info.mtomManufacturerCode;
        vdData[idx++] = info.rfManufacturerCode;

//        safeArrayCopy(info.gpsLocInfo.getBytes(), 0, vdData, idx, 40);
//        idx += 40;

        return idx;
    }

    /**
     * BasicPacket for 1A, 1M
     */
    public static class BasicResponse {
        public static final int VD_DATA_SIZE = 14;

        public byte[] encode(j14_touch_2ch_ChargerInfo info) {
            byte[] vdData = new byte[VD_DATA_SIZE];
            fillBasicField(vdData, info);
            return vdData;
        }
    }

    /**
     * BasicResponse for 1G, 1I, 1K, 1L, 1N
     */
    public static class BasicResponseCode {
        public static final int VD_DATA_SIZE = 16;

        public byte[] encode(j14_touch_2ch_ChargerInfo info, int ackCode, int reasonCode) {
            byte[] vdData = new byte[VD_DATA_SIZE];

            int idx = fillBasicField(vdData, info);

            vdData[idx++] = (byte)ackCode;
            vdData[idx++] = (byte)reasonCode;

            return vdData;
        }
    }


    /**
     * 층전기 모드 요청 응답 message class for 1B
    */
    public static class ChargerModeRequestAck_1B {
        public static final int VD_DATA_SIZE = 17;

        public byte[] encode(j14_touch_2ch_ChargerInfo info) {
            byte[] vdData = new byte[VD_DATA_SIZE];

            int idx = fillBasicField(vdData, info);

            ByteUtil.wordToByteArray(info.cpMode , vdData, idx);

            idx += 2;
            vdData[idx++] = (byte)info.mode2;

            return vdData;
        }
    }

    /**
     * 충전기 모드 변경 message class for C1 / 1C
     */
    public static class ChargerModeChange_C1 {
        public int mode = 0;

        public void decode(byte[] vdData) {
            int idx = 0;

            //충전기 Mode
            mode = ByteUtil.makeWord(vdData[idx], vdData[idx+1]);
            idx += 2;

            //Reserved
            idx += 1; // Skip
        }
    }

    public static class ChargerModeChangeAck_1C {
        public static final int VD_DATA_SIZE = 17;

        public byte[] encode(j14_touch_2ch_ChargerInfo info) {
            byte[] vdData = new byte[VD_DATA_SIZE];

            int idx = fillBasicField(vdData, info);

            ByteUtil.wordToByteArray(info.cpMode , vdData, idx);
            idx += 2;

            vdData[idx++] = (byte)info.mode2;

            return vdData;
        }
    }


    /**
     * 버전 정보 전송
     *
     * by Lee 20200716
     */
    public static class VersionInfo_E1 {
        public String ProgramVer = "";
        public String VoiceVer = "";
        public String SMSInfoVer = "";
        public String UIVer = "";
        public String operateParameterVer = "";
        public String standardUnitCostVer = "";
        public String encryptionKeyVer = "";

        public String unitCostVer = "";
        public String M2MVer = "";
        public String rfReaderVer = "";
        public String memberInfoVer = "";


        public void decode(byte[] vdData) {
            int idx = 0;

            // 연계 항목 1
            idx += 5;

            // 프로그램 Ver.
            try {
                //파싱이 안되어 조정함
                ProgramVer = asciiToString(vdData);
            }
            catch (Exception e)
            { }
            safeArrayCopy(ProgramVer.getBytes(), 0, vdData, idx, 8);

            idx += 8;

            //  연계 항목 2
            idx += 5;

            // 음성정보 Ver.
            safeArrayCopy(VoiceVer.getBytes(), 0, vdData, idx, 8);
            idx += 8;

            //  연계 항목 3
            idx += 5;

            // 문자정보 Ver.
            safeArrayCopy(SMSInfoVer.getBytes(), 0, vdData, idx, 8);
            idx += 8;

            //  연계 항목 4
            idx += 5;

            // UI Ver.
            safeArrayCopy(UIVer.getBytes(), 0, vdData, idx, 8);
            idx += 8;

            //  연계 항목 5
            idx += 5;

            // 운영 파라미터 Ver.
            safeArrayCopy(operateParameterVer.getBytes(), 0, vdData, idx, 8);
            idx += 8;

            //  연계 항목 6
            idx += 5;

            // 기본 단가요금 Ver.
            safeArrayCopy(standardUnitCostVer.getBytes(), 0, vdData, idx, 8);
            idx += 8;

            //  연계 항목 7
            idx += 5;

            // 암호화 키 Ver.
            safeArrayCopy(encryptionKeyVer.getBytes(), 0, vdData, idx, 8);
            idx += 8;

            //  연계 항목 8
            idx += 5;

            // 판매 단가 요금 Ver.
            safeArrayCopy(unitCostVer.getBytes(), 0, vdData, idx, 8);
            idx += 8;

            //  연계 항목 9
            idx += 5;

            // M2M 단말기 Ver.
            safeArrayCopy(M2MVer.getBytes(), 0, vdData, idx, 8);
            idx += 8;

            //  연계 항목 10
            idx += 5;

            // RF 단말기 Ver.
            safeArrayCopy(rfReaderVer.getBytes(), 0, vdData, idx, 8);
            idx += 8;

            //  연계 항목 11
            idx += 5;

            // 회원정보 Ver.
            safeArrayCopy(memberInfoVer.getBytes(), 0, vdData, idx, 8);
            idx += 8;

            //Reserved
            idx += 1; // Skip
        }

        /**
         * 아스키 -> String 문자열
         *
         * by Lee 20200901
         * @param vdData
         * @return
         */
        public String asciiToString(byte vdData[])
        {
            byte num[] = new byte[8];
            int j = 0;
            for (int i = 5; i < 13; i++)
            {
                num[j] = vdData[i];
                j++;
            }
            String str = "";

            try {
                str = new String(num, "UTF-8");
            }
            catch (UnsupportedEncodingException uee)
            { }

            System.out.println(str);
            return str;
        }

    }



    public static class VersionInfoAck_1E {
        public static final int VD_DATA_SIZE = 16;

        public byte[] encode(j14_touch_2ch_ChargerInfo info) {
            byte[] vdData = new byte[VD_DATA_SIZE];

            int idx = fillBasicField(vdData, info);

            vdData[idx++] = (byte) 0x06;
            vdData[idx++] = (byte) 0x00;

            return vdData;
        }
    }


    /**
     *  단가정보 다운로드 message class for G1, 1j
     */
    public static class CostInfoDown_G1_1j {
        public String costVersion = "";
        public String memberCostApplyDate = "";
        public String nonMemberCostApplyDate = "";

        public int[] memberCost = new int[24];
        public int[] nonMemberCost = new int[24];

        public void decode(byte[] vdData) {
//            int idx = 0;      //충전기 종류 인덱스
            int idx = 1;        //단가 적용일자 인덱스 시작

            // 버전정보 (8)
            costVersion = new String(vdData, idx, 8);
            idx += 8;

            // 회원단가
            // 충전기 종류 (1)
            idx += 1;

            // 적용일자 (7)
            memberCostApplyDate = ByteUtil.byteArrayToHexString(vdData, idx, 7);
            idx += 7;

            // 충전요금 단가 (4x24)
            for ( int i=0; i<24; i++) {
                memberCost[i] = ByteUtil.makeInt(vdData, idx, true);
                idx += 4;
            }

            try
            {
                // 비회원단가
                // 충전기 종류 (1)
                idx += 1;

                // 적용일자 (7)
                nonMemberCostApplyDate = ByteUtil.byteArrayToHexString(vdData, idx, 7);
                idx += 7;

                // 충전요금 단가 (4x24)
                for ( int i=0; i<24; i++) {
                    nonMemberCost[i] = ByteUtil.makeInt(vdData, idx, true);
                    idx += 4;
                }
            }
            catch (Exception e1)
            { }

        }
    }

    /**
     *  단가요청 message class for H1
     */
    public static class CostInfoRequest_H1{
        public byte kind = 0;

        public void decode(byte[] vdData) {
            kind = vdData[0];
        }
    }

    /**
     * 단가 응답 message class for 1H
     */
    public static class CostInfoAck_1H {
        public static final int VD_DATA_SIZE = 120;

        public byte[] encode(j14_touch_2ch_ChargerInfo info, byte kind) {
            byte[] vdData = new byte[VD_DATA_SIZE];

            int idx = fillBasicField(vdData, info);

            vdData[idx++] = kind;
            safeArrayCopy(info.costVersion.getBytes(), 0, vdData, idx, 8);
            idx += 8;


            for (int i = 0; i < 24; i++) {
                int nMemberConstUnit = (int)(info.memberCostUnit[i]*100);
                ByteUtil.intToByteArray(nMemberConstUnit, vdData, idx);

                idx += 4;
            }

            return vdData;
        }
    }

    /**
     *  리셋요청 message class for I1
     */
    public static class ResetRequest_I1 {
        public int kind = 0;

        public void decode(byte[] vdData) {
            kind = vdData[0];
        }
    }

    /**
     *  정보 다운로드 message class for K1
     */
    public static class DataDown_K1_1m {
        public byte dest1 = 0;
        public byte dest2 = 0;
        public String programVer = "";
        public byte div = 0;
        public String filename = "";
        public int filesize = 0;
        public int blockIdx = 0;
        public byte[] rawData;
        public int rawDataCrc = 0;
        public boolean isLastDown = false;

        public void decode(byte[] vdData) {
            int idx = 0;

            // Destination 1 [1], 2 [1] HEX
            dest1 = vdData[idx++];
            dest2 = vdData[idx++];

            // 프로그램 버전 [8] ASC
            programVer = new String(vdData, idx, 8).replace("\0","");
            idx += 8;

            // 구분자 [1] HEX
            div = vdData[idx++];

            // Data
            if ( div == (byte)0xF0 ) {
                // 파일명 [12] ASC
                filename = new String(vdData, idx, 12).replace("\0","");
                idx += 12;

                // Download 할 File 크기 [9] ASC
                String strSize = new String(vdData, idx, 9).replace("\0","");
                idx += 9;
                filesize = Integer.parseInt(strSize);
            }
            //데이터 시작/중간
            else if ( (div == (byte)0xF1) || (div == (byte)0xFF) ) {

                // 각 데이터의 블록의 Index [2] HEX
                // 각 데이터 블록의 Index 시작은 0 베이스
                blockIdx = ByteUtil.makeWord(vdData[idx], vdData[idx+1]);
                idx += 2;

                // Binary Data [1024] Hex
                int rawDataSize = vdData.length - idx - 2;
                rawData = new byte[rawDataSize];
                System.arraycopy(vdData, idx, rawData, 0, rawDataSize);
                idx += rawDataSize;

                // CRC16 [2] HEX
                rawDataCrc = ByteUtil.makeWord(vdData[idx], vdData[idx+1]);
                idx += 2;

                // 마지막 블럭
                if ( div == (byte)0xFF ) {
                    isLastDown = true;
                }
            }

        }
    }

    /**
     *  충전기 운영시간 설정 message class for L1
     */
    public static class OpenCloseTime_L1 {
        public String openTime = "";
        public String closeTime = "";

        public void decode(byte[] vdData) {
            int idx = 0;

            openTime = ByteUtil.byteArrayToHexString(vdData, idx, 7);
            idx += 7;

            closeTime = ByteUtil.byteArrayToHexString(vdData, idx, 7);
            idx += 7;

        }
    }

    /**
     *  충전기 정보 변경 message class for N1
     */
    public static class ChargerInfoChange_N1 {
        public byte kind = 0;
        public String changeVal = "";

        public void decode(byte[] vdData) {
            int idx = 0;

            kind = vdData[idx++];
            changeVal = new String(vdData, idx, 30);
            idx += 30;
        }
    }

    /**
     *  충전기 충전 시작 종료 요청 message class for Q1
     */
    public static class RemoteStartStop_Q1 {
        public String cardNum = "";
        public String orderNum = "";
        public byte chargingCmd = 0;
        public int chargingTime = 0;

        public void decode(byte[] vdData) {
            int idx = 0;

            cardNum = new String(vdData, idx, 16);
            idx += 16;

            orderNum = new String(vdData, idx, 16);
            idx += 16;

            chargingCmd = vdData[idx++];
            int hh = Integer.parseInt(ByteUtil.byteArrayToHexString(vdData, idx, 1));
            idx++;
            int mm = Integer.parseInt(ByteUtil.byteArrayToHexString(vdData, idx, 1));
            idx++;
            int ss = Integer.parseInt(ByteUtil.byteArrayToHexString(vdData, idx, 1));
            idx++;

            chargingTime = (hh*60*60) + (mm*60) + ss;
        }
    }

    /**
     *  충전 진행 상태 요청 message classs for 1R
     */
    public static class ChargingStatusRequestAck_1R {
        public static final int VD_DATA_SIZE = 100;

        public byte[] encode(j14_touch_2ch_ChargerInfo info) {
            byte[] vdData = new byte[VD_DATA_SIZE];

            int idx = fillBasicField(vdData, info);

            idx = fillChargingStatus(vdData, idx, info);

            return vdData;
        }
    }


    /**
     *  암호화 Key 다운로드 message classs for 1S
     */
    public static class EncryptionKeyRequestAck_1S {
        public static final int VD_DATA_SIZE = 16;

        public byte[] encode(j14_touch_2ch_ChargerInfo info) {
            byte[] vdData = new byte[VD_DATA_SIZE];

            int idx = fillBasicField(vdData, info);

            // 응답코드
            vdData[idx] = (byte) 0x06;
            // 사유코드
            vdData[idx++] = (byte) 0x00;

            return vdData;
        }
    }




    /**
     *  충전기 운영 파라미터 message classs for 1T
     */
    public static class ChargerParameterRequestAck_1T {
        public static final int VD_DATA_SIZE = 16;

        public byte[] encode(j14_touch_2ch_ChargerInfo info) {
            byte[] vdData = new byte[VD_DATA_SIZE];

            int idx = fillBasicField(vdData, info);

            // 응답코드
            vdData[idx] = (byte) 0x06;
            // 사유코드
            vdData[idx++] = (byte) 0x00;

            return vdData;
        }
    }




    /**
     * 설치 정보 요청 응답 message class for 1U
     */
    public static class ChargerInstallationInfoAck_1U {
        public static final int VD_DATA_SIZE = 110;

        public byte[] encode(j14_touch_2ch_ChargerInfo info) {
            byte[] vdData = new byte[VD_DATA_SIZE];

            int idx = fillBasicField(vdData, info);

            idx = fillInstallationInfo(vdData, idx, info);

            return vdData;
        }
    }


    public static class Authentication_a1 {
        public static final int VD_DATA_SIZE = 30;

        public byte[] encode(j14_touch_2ch_ChargerInfo info) {
            byte[] vdData = new byte[VD_DATA_SIZE];

            int idx = fillBasicField(vdData, info);

            safeArrayCopy(info.cardNum.getBytes(), 0, vdData, idx, 16);
            idx += 16;

            return vdData;
        }
    }

    public static class AuthenticationAck_1a {

        public String cardNum = "0000000000000000";
        public boolean authResult = false;

        public void decode(byte[] vdData) {
            int idx = 0;
            cardNum = new String(vdData, idx, 16);
            idx += 16;

            authResult = (vdData[idx++] == 0x01);
        }
    }

    public static class ChargerStatus_b1 {
        public static final int VD_DATA_SIZE = 14;

        public byte[] encode(j14_touch_2ch_ChargerInfo info) {
            byte[] vdData = new byte[VD_DATA_SIZE];

            int idx = fillBasicField(vdData, info);
            return vdData;
        }
    }


    public static class AlarmInfo_c1 {
        public static final int VD_DATA_SIZE = 25;

        public byte[] encode(j14_touch_2ch_ChargerInfo info, int alarmCode, byte alarmStatus) {
            byte[] vdData = new byte[VD_DATA_SIZE];

            int idx = fillBasicField(vdData, info);

            // 발생시각
            byte[] arrayTime = ByteUtil.dateToBCD(Calendar.getInstance().getTime(), "yyyyMMddHHmmss");
            safeArrayCopy(arrayTime, 0, vdData, idx, 7);
            idx += 7;

            ByteUtil.wordToByteArray(alarmCode, vdData, idx);
            idx += 2;

            vdData[idx++] = alarmStatus;
            vdData[idx++] = info.manufacturerCode;

            return vdData;
        }
    }

    public int chargingStartTime = 0;       //충전시작 시간
    public static class StartCharging_d1 {
        public static final int VD_DATA_SIZE = 100;

        public byte[] encode(j14_touch_2ch_ChargerInfo info) {
            byte[] vdData = new byte[VD_DATA_SIZE];

            int idx = fillBasicField(vdData, info);

            safeArrayCopy(info.cardNum.getBytes(), 0, vdData, idx, 16);
            idx += 16;

            vdData[idx++] = (byte)info.reqAmoundSel;

            ByteUtil.intToByteArray(info.reqAmountKwh, vdData, idx);
            idx += 4;

            ByteUtil.intToByteArray(info.reqAmountCost, vdData, idx);
            idx += 4;

            vdData[idx++] = (byte)info.payMethod;

            vdData[idx++] = (byte)info.battStatus;

            ByteUtil.wordToByteArray(info.socBatt , vdData, idx);
            idx += 2;

            ByteUtil.intToByteArray(info.battTotalAmount, vdData, idx);
            idx += 4;

            ByteUtil.intToByteArray(info.curBattRemain, vdData, idx);
            idx += 4;

            ByteUtil.intToByteArray(info.battVoltage, vdData, idx);
            idx += 4;

            System.arraycopy(info.BMSVer.getBytes(), 0, vdData, idx, 4);
            idx += 4;

            long remainTime = info.remainTime/60 * 100 + info.remainTime%60;
            byte[] hexRemainTime = ByteUtil.decimalToBcd(remainTime, 2);
            vdData[idx++] = hexRemainTime[0];
            vdData[idx++] = hexRemainTime[1];

            ByteUtil.intToByteArray(info.availableLineCurrent, vdData, idx);
            idx += 4;

            return vdData;
        }
    }

    public static class StartChargingAck_1d {
        public byte retCode = 0;
        public byte reasonCode = 0;
        public byte[] dbCode = new byte[2];

        public void decode(byte[] vdData) {
            int idx = 0;
            retCode = vdData[idx++];

            System.arraycopy(vdData, idx, dbCode, 0, 2);
            idx += 2;

            reasonCode = vdData[idx++];
        }
    }


    public static class ChargingStatus_e1 {
        public static final int VD_DATA_SIZE = 100;
        public byte[] encode(j14_touch_2ch_ChargerInfo info) {
            byte[] vdData = new byte[VD_DATA_SIZE];

            int idx = fillBasicField(vdData, info);

            idx = fillChargingStatus(vdData, idx, info);

            return vdData;
        }
    }

    public static class FinishCharging_f1 {
        public static final int VD_DATA_SIZE = 100;

        public byte[] encode(j14_touch_2ch_ChargerInfo info) {
            byte[] vdData = new byte[VD_DATA_SIZE];

            int idx = fillBasicField(vdData, info);

            //회원카드번호
            safeArrayCopy(info.cardNum.getBytes(), 0, vdData, idx, 16);
            idx += 16;

            //충전전력량
            ByteUtil.intToByteArray(info.curChargingKwh, vdData, idx);
            idx += 4;

            byte[] bcdData;
            // 충전 시간
            try {
                String chargedTime = ConvertChargingTime(info.chargingTime);

                //데이터 포맷이 안맞아 수정함
                //hhmmss
                //by jmlee 20200527
                byte[] btChargingTime = ByteUtil.hexStringToByteArray(chargedTime);
                safeArrayCopy(btChargingTime, 0, vdData, idx, 3);
            }
            catch(Exception e) {}
            idx += 3;

            ByteUtil.intToByteArray(info.curChargingCost, vdData, idx);
            idx += 4;

            vdData[idx++] = (byte)info.payMethod;

            vdData[idx++] = info.chargingFinishStatus;

            if ( info.chargingStartTime != null ) {
                bcdData = ByteUtil.dateToBCD(info.chargingStartTime, "yyyyMMddHHmmss");
                safeArrayCopy(bcdData, 0, vdData, idx, 7);
            }
            idx += 7;

            System.arraycopy(info.dbCode, 0, vdData, idx, 2);
            idx += 8;

            return vdData;
        }

        /**
         * 충전시간 변환 및 계산용 함수
         * int (sec) -> string (time)
         * 예시) 014116 (1시간 41분 16초)
         *
         * by jmLee 20200527
         */
        private String ConvertChargingTime(int chargingTimeSec)
        {
            int hour = (chargingTimeSec / 60) / 60;
            int min = (chargingTimeSec / 60) % 60;
            int sec = chargingTimeSec % 60;

            return String.format("%02d", hour) + String.format("%02d", min) + String.format("%02d", sec);
        }


    }


    public static class FinishChargingAck_1f {
        public byte retCode = 0;
        public byte reasonCode = 0;

        public void decode(byte[] vdData) {
            int idx = 0;
            retCode = vdData[idx++];
            reasonCode = vdData[idx++];
        }
    }

    public static class Unplug_g1 {
        public static final int VD_DATA_SIZE = 23;

        public byte[] encode(j14_touch_2ch_ChargerInfo info) {
            byte[] vdData = new byte[VD_DATA_SIZE];

            int idx = fillBasicField(vdData, info);

            //수정 (충전시작시간)
            // by Lee 20200527
            if ( info.chargingStartTime != null ) {
                byte bcdData[] = ByteUtil.dateToBCD(info.chargingStartTime, "yyyyMMddHHmmss");
                safeArrayCopy(bcdData, 0, vdData, idx, 7);
            }
            idx += 7;

//            byte[] arrayTime = ByteUtil.dateToBCD(Calendar.getInstance().getTime(), "yyyyMMddHHmmss");
//            safeArrayCopy(arrayTime, 0, vdData, idx, 7);
//            idx += 7;

            safeArrayCopy(info.dbCode, 0, vdData, idx, 2);
            idx += 2;
            return vdData;
        }
    }

    public static class ChargerInstallationInfo_h1 {
        public static final int VD_DATA_SIZE = 110;

        public byte[] encode(j14_touch_2ch_ChargerInfo info) {
            byte[] vdData = new byte[VD_DATA_SIZE];

            int idx = fillBasicField(vdData, info);

            idx = fillInstallationInfo(vdData, idx, info);

            return vdData;
        }
    }

    public static class NonTransPacket_i1  {
        public byte[] encode(j14_touch_2ch_ChargerInfo info, int dataIndex, byte[] rawData) {
            byte[] vdData = new byte[16+rawData.length];

            int idx = fillBasicField(vdData, info);

            ByteUtil.wordToByteArray(dataIndex, vdData, idx);
            idx += 2;

            System.arraycopy(rawData, 0, vdData, idx, rawData.length);
            idx += rawData.length;

            return vdData;
        }
    }

    public static class NonTransPacketAck_1i  {
        public byte retCode = 0;
        public byte reserved = 0;

        public void decode(byte[] vdData) {
            int idx = 0;

            retCode = vdData[idx++];
            reserved = vdData[idx++];
        }
    }

    public static class CostInfoDownReq_j1 {
        public static final int VD_DATA_SIZE = 15;

        public byte[] encode(j14_touch_2ch_ChargerInfo info) {
            byte[] vdData = new byte[VD_DATA_SIZE];
            int idx = fillBasicField(vdData, info);

            vdData[idx++] = (byte) 0x01;

            return vdData;
        }
    }

    public static class InfoDownReq_m1 {
        public static final int VD_DATA_SIZE = 18;

        public byte[] encode(j14_touch_2ch_ChargerInfo info, byte dest1, byte dest2) {
            byte[] vdData = new byte[VD_DATA_SIZE];
            int idx = fillBasicField(vdData, info);

            ByteUtil.wordToByteArray(info.m1_idx, vdData, idx);
            idx += 2;

            vdData[idx++] = dest1;
            vdData[idx++] = dest2;

            return vdData;
        }
    }


    /**
     *  암호화 key 요청 (k1)
     *
     *  by Lee 20200716
     */
    public static class EncryptionKey_k1 {
        public static final int VD_DATA_SIZE = 14;

        public byte[] encode(j14_touch_2ch_ChargerInfo info) {
            byte[] vdData = new byte[VD_DATA_SIZE];
            int idx = fillBasicField(vdData, info);

            return vdData;
        }
    }

    /**
     *  암호화 key 다운로드 (S1)
     *  암호화 key 응답 (1k)
     *
     *  by Lee 20200716
     */
    public static class EncryptionKeyReqAck_1k {
        public String encryptionKey;
        public String versionInfo;

        public void decode(byte[] vdData) {
            int idx = 0;

            safeArrayCopy(versionInfo.getBytes(), 0, vdData, idx, 8);
            idx += 8;

            encryptionKey = new String(vdData, idx, 16);

            idx = 16;

        }
    }


    /**
     *  충전기 운영 파라미터 요청
     *
     *  by Lee 20200716
     */
    public static class ChargerParameter_l1 {
        public static final int VD_DATA_SIZE = 14;

        public byte[] encode(j14_touch_2ch_ChargerInfo info) {
            byte[] vdData = new byte[VD_DATA_SIZE];
            int idx = fillBasicField(vdData, info);

            return vdData;
        }
    }


    /**
     *  충전기 운영 파라미터 요청
     *
     *  by Lee 20200716
     */
    public static class ChargerParameterReqAck_1l {
        public String versionInfo;
        public String dayFinishTime;        // 일 마감 시간

        public void decode(byte[] vdData) {
            int idx = 0;

            // 8byte 버전 정보
            safeArrayCopy(versionInfo.getBytes(), 0, vdData, idx, 8);
            idx += 8;

            // 2byte 유휴 시 상태

            idx += 2;

            // 2byte 유휴 시 상태

            idx += 2;

            // 2byte 충전 시 상태

            idx += 2;

            // 2byte 충전 시 상태

            idx += 2;

            // 10byte 가맹점주 성명

            idx += 10;

            // 11byte 가맹점주 전화번호
            idx += 11;

            // 100byte 한글 충전소명
            idx += 100;

            // 100byte 영어 충전소명
            idx += 100;

            // 3byte 일 마감시간
            byte[] dayFinishData = new byte[3];
            for (int i = 0; i < 3; i++)
            {
                dayFinishData[i] = vdData[idx+i];
            }

            dayFinishTime = ByteUtil.bcdToString(dayFinishData);
            idx += 3;

            // 36byte 일 마감시간
            for (int i = 0; i < 36; i++)
            {
                vdData[idx+i] = (byte) 0x00;
            }
            idx += 36;
        }
    }



    public static class ChargerVersionReq_r1 {
        public static final int VD_DATA_SIZE = 14;

        public byte[] encode(j14_touch_2ch_ChargerInfo info) {
            byte[] vdData = new byte[VD_DATA_SIZE];
            int idx = fillBasicField(vdData, info);

            return vdData;
        }
    }

    public static class ChargerVersionReqAck_1r {
        public String swVerInfo;
        public String costVerInfo;

        public void decode(byte[] vdData) {
            int idx = 5;

            swVerInfo = new String(vdData, idx, 8);

            idx = 91;

            costVerInfo = new String(vdData, idx, 8);
        }
    }


    public static class DayFinishReq_s1 {
        public static final int VD_DATA_SIZE = 52;

        public byte[] encode(j14_touch_2ch_ChargerInfo info) {
            byte[] vdData = new byte[VD_DATA_SIZE];
            int idx = fillBasicField(vdData, info);

            // 7byte 마감일자
            byte bcdData[] = ByteUtil.dateToBCD(Calendar.getInstance().getTime(), "yyyyMMddHHmmss");
            safeArrayCopy(bcdData, 0, vdData, idx, 7);
            idx += 7;

            // 2byte 총 충전건수
            ByteUtil.wordToByteArray(info.dayAllChargingNum, vdData, idx);
            idx += 2;

            // 4byte 총 충전금액
            ByteUtil.intToByteArray(info.dayAllChargingCost, vdData, idx);
            idx += 4;

            // 4byte 총 충전 전력량
            ByteUtil.intToByteArray(info.dayAllChargingKwh, vdData, idx);
            idx += 4;

            // 21byte Reserved
            for (int i = 0; i < 21; i++)
            {
                vdData[idx+i] = (byte) 0x00;
            }
            idx += 21;

            InitDayFinishData(info);

            return vdData;
        }

        /**
         * 일 마감 전송 시, 일 마감 자료 적산 데이터 초기화
         *
         * by Lee 20200715
         * @param info
         */
        private void InitDayFinishData(j14_touch_2ch_ChargerInfo info)
        {
            // 총 충전 건수 초기화
            info.dayAllChargingNum = 0;

            // 총 충전 금액 초기화
            info.dayAllChargingCost = 0;

            // 총 충전 전력량 초기화
            info.dayAllChargingKwh = 0;
        }
    }



    public static class DayFinishReqAck_1s {
        public String swVerInfo;
        public String costVerInfo;

        public void decode(byte[] vdData) {
//            int idx = 5;
//
//            swVerInfo = new String(vdData, idx, 8);
//
//            idx = 91;
//
//            costVerInfo = new String(vdData, idx, 8);
        }
    }

    public static class AbnormalTermination_t1 {
        public static final int VD_DATA_SIZE = 30;

        public byte[] encode(j14_touch_2ch_ChargerInfo info) {
            byte[] vdData = new byte[VD_DATA_SIZE];
            int idx = fillBasicField(vdData, info);

            safeArrayCopy(info.cardNum.getBytes(), 0, vdData, idx, 16);
            idx += 16;

            return vdData;
        }
    }

    // ToDo p1, q1 구현필요(결제시)
}
