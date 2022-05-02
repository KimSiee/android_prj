/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 3. 18 오후 1:56
 *
 */

package com.joas.posco_comm;


import com.joas.utils.ByteUtil;
import java.net.InetAddress;
import java.util.Calendar;

public class PoscoPacket {
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
     * Fill Basic Field: Charger Mode, Status, meter value
     * @param vdData Variable Data in EvPacket
     * @param info Charger Info Data
     * @return size of fields
     */
    public static int fillBasicField(byte[] vdData, PoscoChargerInfo info) {
        int idx = 0;

        ByteUtil.wordToByteArray(info.cpMode, vdData, idx);
        idx += 2;

        ByteUtil.longToByteArray(info.cpStatus, vdData, 8, idx);
        idx += 8;

        ByteUtil.intToByteArray(info.meterVal, vdData, idx);
        idx += 4;
        return idx;
    }

    public static int fillChargingStatus(byte[] vdData, int idx, PoscoChargerInfo info) {

        if (info.isRemoteCharge) safeArrayCopy(info.remote_cardNum.getBytes(), 0, vdData, idx, 16);
        else safeArrayCopy(info.cardNum.getBytes(), 0, vdData, idx, 16);
        idx += 16;

        vdData[idx++] = (byte) info.reqAmoundSel;

        ByteUtil.intToByteArray(info.reqAmountKwh, vdData, idx);
        idx += 4;

        ByteUtil.intToByteArray(info.reqAmountCost, vdData, idx);
        idx += 4;

        vdData[idx++] = (byte) info.payMethod;

        ByteUtil.wordToByteArray(info.socBatt, vdData, idx);
        idx += 2;

        ByteUtil.intToByteArray(info.curChargingKwh, vdData, idx);
        idx += 4;

        ByteUtil.intToByteArray(info.curChargingCost, vdData, idx);
        idx += 4;

        ByteUtil.intToByteArray(info.curChargingCostUnit, vdData, idx);
        idx += 4;

        vdData[idx++] = (byte) info.battStatus;

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

        int remainTime = info.remainTime;
        try {
            byte[] arr_remtime = new byte[2];
            arr_remtime[0] = (byte) (remainTime / 3600);
            arr_remtime[1] = (byte) ((remainTime % 3600) / 60);
            byte[] bcdData = new byte[2];
            ByteUtil.GetDectoHexBCDFormat(arr_remtime);
            vdData[idx++] = arr_remtime[0];
            vdData[idx++] = arr_remtime[1];
        } catch (Exception e) {

        }
//        long remainTime = info.remainTime/60 * 100 + info.remainTime%60;
//        byte[] hexRemainTime = ByteUtil.decimalToBcd(remainTime,2);
//        vdData[idx++] = hexRemainTime[0];
//        vdData[idx++] = hexRemainTime[1];

        System.arraycopy(info.dbCode, 0, vdData, idx, 8);
        idx += 8;
        return idx;
    }

    public static int fillInstallationInfo(byte[] vdData, int idx, PoscoChargerInfo info) {
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
        } catch (Exception e) {
        }

        safeArrayCopy(info.paymentPortNumber.getBytes(), 0, vdData, idx, 4);
        idx += 4;

        try {
            InetAddress ip = InetAddress.getByName(info.paymentServerIp);
            byte[] rawIpAddr = ip.getAddress();
            System.arraycopy(rawIpAddr, 0, vdData, idx, 4);
            idx += 4;
        } catch (Exception e) {
        }

        String mdnNum = info.mtoPhoneNumber + "     ";
        safeArrayCopy(mdnNum.getBytes(), 0, vdData, idx, 16);
        idx += 16;

        //h1 모뎀번호정보 공백일 경우 h1 재전송 위한 플래그 설정 - add by si.200903
        if(info.mtoPhoneNumber.equals("           ")) {
            info.h1_mdnNumIsSpace = true;
        }
        else {
            info.h1_mdnNumIsSpace = false;
        }

        safeArrayCopy(info.gpsLocInfo.getBytes(), 0, vdData, idx, 25);
        idx += 25;

        vdData[idx++] = info.manufacturerCode;
        vdData[idx++] = info.mtomManufacturerCode;
        vdData[idx++] = info.rfManufacturerCode;

        safeArrayCopy(info.chg_sw_version.getBytes(), 0, vdData, idx, 40);
        idx += 40;

        return idx;
    }

    /**
     * BasicPacket for 1A, 1M
     */
    public static class BasicResponse {
        public static final int VD_DATA_SIZE = 14;

        public byte[] encode(PoscoChargerInfo info) {
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

        public byte[] encode(PoscoChargerInfo info, int ackCode, int reasonCode) {
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

        public byte[] encode(PoscoChargerInfo info) {
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
        public int modeKind = 0;
        public int soundVol = 0;
        public int chargingAmp = 0;

        public void decode(byte[] vdData) {
            int idx = 0;

            mode = ByteUtil.makeWord(vdData[idx], vdData[idx+1]);
            idx += 2;

            modeKind = vdData[idx++];       //이카플러그 전용

            soundVol = ByteUtil.makeInt(vdData, idx, true);     //이카플러그 전용
            idx += 4;

            chargingAmp = ByteUtil.makeInt(vdData, idx, true);  //이카플러그 전용
            idx += 4;
        }
    }

    public static class ChargerModeChangeAck_1C {
        public static final int VD_DATA_SIZE = 17;

        public byte[] encode(PoscoChargerInfo info) {
            byte[] vdData = new byte[VD_DATA_SIZE];

            int idx = fillBasicField(vdData, info);

            ByteUtil.wordToByteArray(info.cpMode , vdData, idx);
            idx += 2;

            vdData[idx++] = (byte)info.mode2;

            return vdData;
        }
    }

    /**
     *  단가정보 다운로드 message class for G1, 1j
     */
    public static class CostInfoDown_G1_1j {
        public String memberCostApplyDate = "";
        public String nonMemberCostApplyDate = "";
        public int memberCost = 0;
        public int nonMemberCost = 0;

        public void decode(byte[] vdData) {
            int idx = 0;

            memberCostApplyDate = ByteUtil.byteArrayToHexString(vdData, idx, 7);
            idx += 7;

            memberCost = ByteUtil.makeInt(vdData, idx, true);
            idx += 4;

            nonMemberCostApplyDate = ByteUtil.byteArrayToHexString(vdData, idx, 7);
            idx += 7;

            nonMemberCost = ByteUtil.makeInt(vdData, idx, true);
            idx += 4;
        }
    }

    /**
     *  리셋요청 message class for I1
     */
    public static class ResetRequest_I1{
        public int kind = 0;

        public void decode(byte[] vdData) {
            kind = vdData[0];
        }
    }

    /**
     *  정보 다운로드 message class for K1
     */
    public static class DataDown_K1{
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

            dest1 = vdData[idx++];
            dest2 = vdData[idx++];

            programVer = new String(vdData, idx, 8).replace("\0","");
            idx += 8;

            div = vdData[idx++];
            //파일명
            if ( div == (byte)0xF0 ) {
                filename = new String(vdData, idx, 12).replace("\0","");
                idx += 12;

                String strSize = new String(vdData, idx, 9).replace("\0","");
                idx += 9;
                filesize = Integer.parseInt(strSize);
            }
            //데이터 시작/중간
            else if ( (div == (byte)0xF1) || (div == (byte)0xFF) ) {
                blockIdx = ByteUtil.makeWord(vdData[idx], vdData[idx+1]);
                idx += 2;

                int rawDataSize = vdData.length - idx - 2;
                rawData = new byte[rawDataSize];
                System.arraycopy(vdData, idx, rawData, 0, rawDataSize);
                idx += rawDataSize;
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
     * 충전기 원격 다운로드 및 회원정보 class for K2
     */
    public static class DataDown_K2{
        public byte dest1 = 0;
        public byte dest2 = 0;
        public String remoteURL = "";

        public void decode(byte[] vdData) {
            int idx = 0;
            //destination 1
            dest1 = vdData[idx++];
            //destination 2
            dest2 = vdData[idx++];
            //program version(skip)
            idx += 8;
            //remote url + filename
            remoteURL = new String(vdData, idx, 100);
            //나머지 사용안함(skip 256 + 9)
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

        public byte[] encode(PoscoChargerInfo info) {
            byte[] vdData = new byte[VD_DATA_SIZE];

            int idx = fillBasicField(vdData, info);

            idx = fillChargingStatus(vdData, idx, info);

            return vdData;
        }
    }

    /**
     * 설치 정보 요청 응답 message class for 1U
     */
    public static class ChargerInstallationInfoAck_1U {
        public static final int VD_DATA_SIZE = 124;

        public byte[] encode(PoscoChargerInfo info) {
            byte[] vdData = new byte[VD_DATA_SIZE];

            int idx = fillBasicField(vdData, info);

            idx = fillInstallationInfo(vdData, idx, info);

            return vdData;
        }
//        public static final int VD_DATA_SIZE = 110;
//
//        public byte[] encode(PoscoChargerInfo info) {
//            byte[] vdData = new byte[VD_DATA_SIZE];
//
//            int idx = fillBasicField(vdData, info);
//
//            idx = fillInstallationInfo(vdData, idx, info);
//
//            return vdData;
//        }
    }


    public static class Authentication_a1 {
        public static final int VD_DATA_SIZE = 30;

        public byte[] encode(PoscoChargerInfo info) {
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
        public byte memberKindCode = 0;
        public String memberKind = "";
        public int costUnit = 10000; // x 0.01

        public void decode(byte[] vdData) {
            int idx = 0;
            cardNum = new String(vdData, idx, 16);
            idx += 16;

            authResult = (vdData[idx++] == 0x01);
            memberKindCode = vdData[idx++];
            memberKind = new String(vdData, idx, 2);
            idx += 2;

            costUnit = ByteUtil.makeInt(vdData, idx, true);
        }
    }

    public static class ChargerStatus_b1 {
        public static final int VD_DATA_SIZE = 57;

        public byte[] encode(PoscoChargerInfo info) {
            byte[] vdData = new byte[VD_DATA_SIZE];

            int idx = fillBasicField(vdData, info);

            safeArrayCopy(info.mtomRssi.getBytes(), 0, vdData, idx, 4);
            idx += 4;

            vdData[idx++] = 0; // Sound(이카)

            vdData[idx++] = 0; // 충전전류량(이카)
            vdData[idx++] = 0; //
            vdData[idx++] = 0; //
            vdData[idx++] = 0; //

            safeArrayCopy(info.chargingAvalTime.getBytes(), 0, vdData, idx, 30);
            idx += 30;

            //add by si. 200608 차지비 멤버수 전송
            int memnum = info.totMemnum;
            ByteUtil.intToByteArray(memnum, vdData, idx);
            idx+=4;

            return vdData;
        }
    }


    public static class AlarmInfo_c1 {
        public static final int VD_DATA_SIZE = 25;

        public byte[] encode(PoscoChargerInfo info, int alarmCode, byte alarmStatus) {
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

    public static class StartCharging_d1 {
        public static final int VD_DATA_SIZE = 223;

        public byte[] encode(PoscoChargerInfo info) {
            byte[] vdData = new byte[VD_DATA_SIZE];

            int idx = fillBasicField(vdData, info);

            if (info.isRemoteCharge)
                safeArrayCopy(info.remote_cardNum.getBytes(), 0, vdData, idx, 16);
            else safeArrayCopy(info.cardNum.getBytes(), 0, vdData, idx, 16);
            idx += 16;

            vdData[idx++] = (byte) info.reqAmoundSel;

            ByteUtil.intToByteArray(info.reqAmountKwh, vdData, idx);
            idx += 4;

            ByteUtil.intToByteArray(info.reqAmountCost, vdData, idx);
            idx += 4;

            vdData[idx++] = (byte) info.payMethod;

            vdData[idx++] = (byte) info.battStatus;

            ByteUtil.wordToByteArray(info.socBatt, vdData, idx);
            idx += 2;

            ByteUtil.intToByteArray(info.battTotalAmount, vdData, idx);
            idx += 4;

            ByteUtil.intToByteArray(info.curBattRemain, vdData, idx);
            idx += 4;

            ByteUtil.intToByteArray(info.battVoltage, vdData, idx);
            idx += 4;

            System.arraycopy(info.BMSVer.getBytes(), 0, vdData, idx, 4);
            idx += 4;

//            long remainTime = info.remainTime/60 * 100 + info.remainTime%60;
//            byte[] hexRemainTime = ByteUtil.decimalToBcd(remainTime, 2);
//            vdData[idx++] = hexRemainTime[0];
//            vdData[idx++] = hexRemainTime[1];
            int remainTime = info.remainTime;
            try {
                byte[] arr_remtime = new byte[2];
                arr_remtime[0] = (byte) (remainTime / 3600);
                arr_remtime[1] = (byte) ((remainTime % 3600) / 60);
                byte[] bcdData = new byte[2];
                ByteUtil.GetDectoHexBCDFormat(arr_remtime);
                vdData[idx++] = arr_remtime[0];
                vdData[idx++] = arr_remtime[1];
            } catch (Exception e) {
            }

            ByteUtil.intToByteArray(info.availableLineCurrent, vdData, idx);
            idx += 4;

            //현장결제 추가
            vdData[idx++] = info.paymentCompany; // 스마트로:0x01
            safeArrayCopy(info.paymentStoreId.getBytes(), 0, vdData, idx, 20);
            idx += 20;

            safeArrayCopy(info.paymentPreDealId.getBytes(), 0, vdData, idx, 60);
            idx += 60;

            safeArrayCopy(info.paymentPreDealSerialNo.getBytes(), 0, vdData, idx, 30);
            idx += 30;

            safeArrayCopy(info.paymentPreDealApprovalNo.getBytes(), 0, vdData, idx, 20);
            idx += 20;

            try {
                byte[] bcdTime = ByteUtil.decimalToBcd(Long.valueOf(info.paymentPreDealApprovalTime), 7);
                safeArrayCopy(bcdTime, 0, vdData, idx, 7);
            } catch (Exception e) {
            }
            idx += 7;

            ByteUtil.intToByteArray(info.paymentPreDealApprovalCost, vdData, idx);
            idx += 4;

            safeArrayCopy(info.paymentNoMemberPhone.getBytes(), 0, vdData, idx, 20);
            idx += 20;

            return vdData;
        }
    }

    public static class StartChargingAck_1d {
        public byte retCode = 0;
        public byte reasonCode = 0;
        public byte[] dbCode = new byte[8];

        public void decode(byte[] vdData) {
            int idx = 0;
            retCode = vdData[idx++];

            System.arraycopy(vdData, idx, dbCode, 0, 8);
            idx += 8;

            reasonCode = vdData[idx++];
        }
    }


    public static class ChargingStatus_e1 {
        public static final int VD_DATA_SIZE = 100;
        public byte[] encode(PoscoChargerInfo info) {
            byte[] vdData = new byte[VD_DATA_SIZE];

            int idx = fillBasicField(vdData, info);

            idx = fillChargingStatus(vdData, idx, info);

            return vdData;
        }
    }

    public static class FinishCharging_f1 {
        public static final int VD_DATA_SIZE = 424;

        public byte[] encode(PoscoChargerInfo info) {
            byte[] vdData = new byte[VD_DATA_SIZE];

            int idx = fillBasicField(vdData, info);

            if(info.isRemoteCharge) safeArrayCopy(info.remote_cardNum.getBytes(), 0, vdData, idx, 16);
            else safeArrayCopy(info.cardNum.getBytes(), 0, vdData, idx, 16);
            idx += 16;

            ByteUtil.intToByteArray(info.curChargingKwh, vdData, idx);
            idx += 4;

            //공급시간 - add by si.
            int chargingTime = info.chargingTime;
            try {
                byte[] arr_cgtime = new byte[3];
                arr_cgtime[0] = (byte) (chargingTime / 3600);
                arr_cgtime[1] = (byte) ((chargingTime % 3600) / 60);
                arr_cgtime[2] = (byte) (chargingTime % 60);

                ByteUtil.GetDectoHexBCDFormat(arr_cgtime);
                safeArrayCopy(arr_cgtime, 0, vdData, idx, 3);

            }catch (Exception e){}

            idx+=3;
//            byte[] bcdData;
//            // 충전 시간
//            try {
//                bcdData = ByteUtil.decimalToBcd((long) info.chargingTime, 3);
//                safeArrayCopy(bcdData, 0, vdData, idx, 3);
//            }
//            catch(Exception e) {}
//            idx += 3;

            ByteUtil.intToByteArray(info.curChargingCost, vdData, idx);
            idx += 4;

            vdData[idx++] = (byte)info.payMethod;

            vdData[idx++] = info.chargingFinishStatus;

            byte[] bcdData;

            if ( info.chargingStartTime != null ) {
                bcdData = ByteUtil.dateToBCD(info.chargingStartTime, "yyyyMMddHHmmss");
                safeArrayCopy(bcdData, 0, vdData, idx, 7);
            }
            idx += 7;

            System.arraycopy(info.dbCode, 0, vdData, idx, 8);
            idx += 8;

            //현장결제(카카오향, 부분취소, 결제방법 0x02)
            if(info.payMethod == 0x02){
                //결제업체
                vdData[idx++] = info.paymentCompany; // 스마트로:0x01

                //상점ID
                safeArrayCopy(info.paymentStoreId.getBytes(), 0, vdData, idx, 20);
                idx += 20;

                // 적용단가
                ByteUtil.intToByteArray(info.nonMemberCostUnit, vdData, idx);
                idx += 4;

                //한도승인 거래번호
                safeArrayCopy(info.paymentPreDealId.getBytes(), 0, vdData, idx, 60);
                idx += 60;

                // 한도승인 거래일련번호
                safeArrayCopy(info.paymentPreDealSerialNo.getBytes(), 0, vdData, idx, 30);
                idx += 30;

                //한도승인 승인번호
                safeArrayCopy(info.paymentPreDealApprovalNo.getBytes(), 0, vdData, idx, 20);
                idx += 20;

                //한도승인 승인일시
                byte[] bcdTime;
                try {
                    bcdTime = ByteUtil.decimalToBcd(Long.valueOf(info.paymentPreDealApprovalTime), 7);
                    safeArrayCopy(bcdTime, 0, vdData, idx, 7);
                }
                catch (Exception e) {}
                idx += 7;

                //한도승인 금액
                ByteUtil.intToByteArray(info.paymentPreDealApprovalCost, vdData, idx);
                idx += 4;

                // 취소 거래번호
                safeArrayCopy(info.paymentDealCancelId.getBytes(), 0, vdData, idx, 60);
                idx += 60;

                // 취소 승인번호
                safeArrayCopy(info.paymentDealCancelNo.getBytes(), 0, vdData, idx, 20);
                idx += 20;

                // 취소일시
                try {
                    bcdTime = ByteUtil.decimalToBcd(Long.valueOf(info.paymentDealCancelTime), 7);
                    safeArrayCopy(bcdTime, 0, vdData, idx, 7);
                }
                catch (Exception e) {}
                idx += 7;

                //취소금액
                ByteUtil.intToByteArray(info.paymentDealCancelApprovalCost, vdData, idx);
                idx += 4;

                //휴대폰번호호
               safeArrayCopy(info.paymentNoMemberPhone.getBytes(), 0, vdData, idx, 16);
                idx += 16;
            }
            else{       //현장결제(차지비향, 승승취, 결제방법 0x01)
                //결제업체
                vdData[idx++] = info.paymentCompany; // 스마트로:0x01

                //상점ID
                safeArrayCopy(info.paymentStoreId.getBytes(), 0, vdData, idx, 20);
                idx += 20;

                safeArrayCopy(info.paymentDealId.getBytes(), 0, vdData, idx, 60);
                idx += 60;

                safeArrayCopy(info.paymentDealSerialNo.getBytes(), 0, vdData, idx, 30);
                idx += 30;

                safeArrayCopy(info.paymentDealApprovalNo.getBytes(), 0, vdData, idx, 20);
                idx += 20;

                byte[] bcdTime;
                try {
                    bcdTime = ByteUtil.decimalToBcd(Long.valueOf(info.paymentDealApprovalTime), 7);
                    safeArrayCopy(bcdTime, 0, vdData, idx, 7);
                }
                catch (Exception e) {}
                idx += 7;

                //한도승인 취소 거래번호
                safeArrayCopy(info.paymentDealCancelId.getBytes(), 0, vdData, idx, 60);
                idx += 60;

                // 한도승인 취소번호
                safeArrayCopy(info.paymentDealCancelNo.getBytes(), 0, vdData, idx, 20);
                idx += 20;

                // 한도승인 취소일시
                try {
                    bcdTime = ByteUtil.decimalToBcd(Long.valueOf(info.paymentDealCancelTime), 7);
                    safeArrayCopy(bcdTime, 0, vdData, idx, 7);
                }
                catch (Exception e) {}
                idx += 7;

                // 적용단가
                ByteUtil.intToByteArray(info.nonMemberCostUnit, vdData, idx);
                idx += 4;

                //한도승인 거래번호
                safeArrayCopy(info.paymentPreDealId.getBytes(), 0, vdData, idx, 60);
                idx += 60;

                // 한도승인 거래일련번호
                safeArrayCopy(info.paymentPreDealSerialNo.getBytes(), 0, vdData, idx, 30);
                idx += 30;

                //한도승인 승인번호
                safeArrayCopy(info.paymentPreDealApprovalNo.getBytes(), 0, vdData, idx, 20);
                idx += 20;

                //한도승인 승인일시
                try {
                    bcdTime = ByteUtil.decimalToBcd(Long.valueOf(info.paymentPreDealApprovalTime), 7);
                    safeArrayCopy(bcdTime, 0, vdData, idx, 7);
                }
                catch (Exception e) {}
                idx += 7;

                ByteUtil.intToByteArray(info.paymentPreDealApprovalCost, vdData, idx);
                idx += 4;

                safeArrayCopy(info.paymentNoMemberPhone.getBytes(), 0, vdData, idx, 16);
                idx += 16;
            }


            return vdData;
        }
    }

    public static class FinishChargingAck_1f {
        public byte retCode = 0;
        public byte reasonCode = 0;
        public int chargingCost = 0;

        public void decode(byte[] vdData) {
            int idx = 0;

            chargingCost = ByteUtil.makeInt(vdData, idx, true);
            idx += 4;

            retCode = vdData[idx++];
            reasonCode = vdData[idx++];
        }
    }

    public static class Unplug_g1 {
        public static final int VD_DATA_SIZE = 29;

        public byte[] encode(PoscoChargerInfo info) {
            byte[] vdData = new byte[VD_DATA_SIZE];

            int idx = fillBasicField(vdData, info);

            byte[] arrayTime = ByteUtil.dateToBCD(Calendar.getInstance().getTime(), "yyyyMMddHHmmss");
            safeArrayCopy(arrayTime, 0, vdData, idx, 7);
            idx += 7;

            safeArrayCopy(info.dbCode, 0, vdData, idx, 8);
            idx += 8;
            return vdData;
        }
    }

    public static class ChargerInstallationInfo_h1 {
        public static final int VD_DATA_SIZE = 124;

        public byte[] encode(PoscoChargerInfo info) {
            byte[] vdData = new byte[VD_DATA_SIZE];

            int idx = fillBasicField(vdData, info);

            idx = fillInstallationInfo(vdData, idx, info);

            return vdData;
        }
    }

    public static class NonTransPacket_i1  {
        public byte[] encode(PoscoChargerInfo info, int dataIndex, byte[] rawData) {
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
        public String ins_vd = "";
        public byte[] dbCode = new byte[8];

        public void decode(byte[] vdData) {
            int idx = 0;

            retCode = vdData[idx++];
            reserved = vdData[idx++];
            if ( vdData.length >= 11 ) {
                ins_vd = new String(vdData, 1, 2);
                idx++;

                System.arraycopy(vdData, idx, dbCode, 0, 8);
                idx += 8;
            }
        }
    }

    public static class CostInfoDownReq_j1 {
        public static final int VD_DATA_SIZE = 14;

        public byte[] encode(PoscoChargerInfo info) {
            byte[] vdData = new byte[VD_DATA_SIZE];
            int idx = fillBasicField(vdData, info);
            return vdData;
        }
    }

    public static class ChargerFirmwareReq_m1 {
        public static final int VD_DATA_SIZE = 17;

        public byte[] encode(PoscoChargerInfo info, byte dest1, byte dest2) {
            byte[] vdData = new byte[VD_DATA_SIZE];
            int idx = fillBasicField(vdData, info);

            vdData[idx++] = dest1;
            vdData[idx++] = dest2;
            vdData[idx++] = 0x30; // '0'

            return vdData;
        }
    }

    public static class ChargerVersionReq_r1 {
        public static final int VD_DATA_SIZE = 14;

        public byte[] encode(PoscoChargerInfo info) {
            byte[] vdData = new byte[VD_DATA_SIZE];
            int idx = fillBasicField(vdData, info);

//            vdData[idx++] = 0x00;

            return vdData;
        }
    }

    public static class ChargerVersionReqAck_1r {
        public String verinfo;

        public void decode(byte[] vdData) {
            int idx = 0;

            verinfo = new String(vdData, idx, 40);
            idx += 40;
        }
    }

    public static class AbnormalTermination_t1 {
        public static final int VD_DATA_SIZE = 30;

        public byte[] encode(PoscoChargerInfo info) {
            byte[] vdData = new byte[VD_DATA_SIZE];
            int idx = fillBasicField(vdData, info);

            safeArrayCopy(info.remote_cardNum.getBytes(), 0, vdData, idx, 16);
            idx += 16;

            return vdData;
        }
    }

    // ToDo p1, q1 구현필요(결제시)
    //q1
    public static class MissingPaymentCompleteReq_q1{
        public static final int VD_DATA_SIZE = 271;

        public byte[] encode(PoscoChargerInfo info){
            byte[] vdData = new byte[VD_DATA_SIZE];
            int idx = fillBasicField(vdData, info);

            //비회원 전화번호
            safeArrayCopy(info.paymentNoMemberPhone.getBytes(), 0, vdData, idx, 16);
            idx += 16;
            //결제업체
            vdData[idx++] = info.paymentCompany; // 스마트로:0x01
            //상점번호
            safeArrayCopy(info.paymentStoreId.getBytes(), 0, vdData, idx, 20);
            idx += 20;
            //한도승인 취소 거래번호
            safeArrayCopy(info.paymentDealCancelId.getBytes(), 0, vdData, idx, 60);
            idx += 60;
            // 한도승인 취소번호
            safeArrayCopy(info.paymentDealCancelNo.getBytes(), 0, vdData, idx, 20);
            idx += 20;

            byte[] bcdTime;
            // 한도승인 취소일시
            try {
                bcdTime = ByteUtil.decimalToBcd(Long.valueOf(info.paymentDealCancelTime), 7);
                safeArrayCopy(bcdTime, 0, vdData, idx, 7);
            }
            catch (Exception e) {}
            idx += 7;

            //한도승인 취소 금액
            ByteUtil.intToByteArray(info.paymentDealCancelApprovalCost, vdData, idx);
            idx += 4;

            //실승인 거래번호
            safeArrayCopy(info.paymentDealId.getBytes(), 0, vdData, idx, 60);
            idx += 60;

            //실승인 거래일련번호(pgnum)
            safeArrayCopy(info.paymentDealSerialNo.getBytes(), 0, vdData, idx, 30);
            idx += 30;

            //실승인 승인번호
            safeArrayCopy(info.paymentDealApprovalNo.getBytes(), 0, vdData, idx, 20);
            idx += 20;

            //실승인 거래일시
            try {
                bcdTime = ByteUtil.decimalToBcd(Long.valueOf(info.paymentDealApprovalTime), 7);
                safeArrayCopy(bcdTime, 0, vdData, idx, 7);
            }
            catch (Exception e) {}
            idx += 7;

            //실승인 승인금액
            ByteUtil.intToByteArray(info.paymentDealApprovalCost, vdData, idx);
            idx += 4;

            //결제 DB코드(1p응답 값)
            System.arraycopy(info.payment_dbcode, 0, vdData, idx, 8);
            idx += 8;

            return vdData;
        }
    }
    //1q
    public static class MissingPaymentCompleteRes_1q{
        public String recvCellnum = "0000000000000000";
        public byte authResult = 0x01;      //인증결과 (0x00 : 정상, 0x01 : 한도승인 취소만 없음, 0x02 : 한도승인 취소 및 실승인 없음, 0x03 : 부분취소없음(부분취소 사용일경우)
        public byte recvPaymentCompany = 0x01;  //결제업체(0x01 : 스마트로)
        public String recvMarketnum = "00000000000000000000";       //상점번호
        public String recvPrepayUninum = "000000000000000000000000000000000000000000000000000000000000";    //최초결제 거래번호
        public String recvPrepayApprovalNum = "00000000000000000000";       //최초결제 승인번호
        public String recvPrepayDatetime = "0000000";  //최초결제 승인일시(BCD)
        public int recvPrepayPrice = 0;     //최초결제 승인금액
        public int recvusePrice = 0;        //사용금액(실결제 용도)
        public byte[] dbcode = new byte[8];         //1p응답에 대해 미처리 건을 처리 후 q1으로 전달할 때 필요한 dbcode

        public void decode(byte[] vdData) {
            int idx = 0;
            recvCellnum = new String(vdData,idx,16);
            idx+=16;
            authResult = vdData[idx++];
            recvPaymentCompany = vdData[idx++];
            recvMarketnum = new String(vdData,idx,20);
            idx+=20;
            recvPrepayUninum = new String(vdData,idx,60);
            idx+=60;
            recvPrepayApprovalNum = new String(vdData,idx,20);
            idx+=20;
            recvPrepayDatetime = ByteUtil.byteArrayToHexString(vdData, idx, 7);
            idx+=7;
            recvPrepayPrice = ByteUtil.makeInt(vdData,idx,true);
            idx+=4;
            recvusePrice = ByteUtil.makeInt(vdData,idx,true);
            idx+=4;
            System.arraycopy(vdData,idx,dbcode,0,8);
            idx+=8;
        }
    }
    //p1
    public static class CellNumAuthRequest_p1{
        public static final int VD_DATA_SIZE = 30;

        public byte[] encode(PoscoChargerInfo info) {
            byte[] vdData = new byte[VD_DATA_SIZE];
            int idx = fillBasicField(vdData, info);

            //비회원 전화번호
            safeArrayCopy(info.paymentNoMemberPhone.getBytes(), 0, vdData, idx, 16);
            idx += 16;

            return vdData;
        }
    }
    //1p
    public static class CellNumAuthRequestAck_1p {

        public String recvCellnum = "0000000000000000";
        public String recvAuthnum = "00";
        public byte authResult = 0x01;      //인증결과 (0x00 : 정상, 0x01 : 한도승인 취소만 없음, 0x02 : 한도승인 취소 및 실승인 없음, 0x03 : 부분취소없음(부분취소 사용일경우)
        public byte recvPaymentCompany = 0x01;  //결제업체(0x01 : 스마트로)
        public String recvMarketnum = "00000000000000000000";       //상점번호
        public String recvPrepayUninum = "000000000000000000000000000000000000000000000000000000000000";    //최초결제 거래번호
        public String recvPrepayApprovalNum = "00000000000000000000";       //최초결제 승인번호
        public String recvPrepayDatetime = "0000000";  //최초결제 승인일시(BCD)
        public int recvPrepayPrice = 0;     //최초결제 승인금액
        public int recvusePrice = 0;        //사용금액(실결제 용도)
        public byte[] dbcode = new byte[8];         //1p응답에 대해 미처리 건을 처리 후 q1으로 전달할 때 필요한 dbcode
        public String recvPrepaySerialNum = "000000000000000000000000000000";     //한도승인 거래일련번호

        public void decode(byte[] vdData) {
            int idx = 0;
            recvCellnum = new String(vdData,idx,16);
            idx+=16;
            recvAuthnum = new String(vdData,idx,2);
            idx+=2;
            authResult = vdData[idx++];
            recvPaymentCompany = vdData[idx++];
            recvMarketnum = new String(vdData,idx,20);
            idx+=20;
            recvPrepayUninum = new String(vdData,idx,60);
            idx+=60;
            recvPrepayApprovalNum = new String(vdData,idx,20);
            idx+=20;
            recvPrepayDatetime = ByteUtil.byteArrayToHexString(vdData, idx, 7);
            idx+=7;
            recvPrepayPrice = ByteUtil.makeInt(vdData,idx,true);
            idx+=4;
            recvusePrice = ByteUtil.makeInt(vdData,idx,true);
            idx+=4;
            System.arraycopy(vdData,idx,dbcode,0,8);
            idx+=8;
            recvPrepaySerialNum = new String(vdData,idx,30);
            idx+=30;
        }
    }

}
