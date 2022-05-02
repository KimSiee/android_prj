/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 8. 22 오전 8:47
 */

package com.joas.hw.rfid;

import android.util.Log;

import com.joas.utils.ByteUtil;
import com.joas.utils.LogWrapper;
import com.joas.utils.SerialPort;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * 사용 명령
 * 1. 초기화
 * 무음 02 6B 06 00 00 00 00 01 00 00 00 E0 00 00 21 01 8F 23 03
 * 모드 셀렉트(이벤트시 RX 데이터 발생): 02 6B 02 00 00 00 00 01 00 00 00 44 80 AC 03
 *
 * 2. 카드 reqeust
 * 모드 셀렉트(이벤트시 RX 데이터 발생): 02 6B 02 00 00 00 00 01 00 00 00 44 80 AC 03
 *
 * 3. 카드 이벤트
 * 카드 IN : 02 50 03 53 03
 * 카드 OUT : 02 50 02 52 03
 *
 * 4. UID 요청
 * 02 6F 05 00 00 00 00 01 00 00 00 FF CA 00 00 00 5E 03
 * 상태 응답 : 02 00 00 03
 * 데이터 응답([]안이 카드 데이터) :02 80 06 00 00 00 00 01 02 81 00 [A2 9F 47 15] 90 00 FB 03
 *
 * 5. 티머니, 레일플러스 교통카드 요청
 * 02 6F 0C 00 00 00 00 01 00 00 00 00 A4 04 00 07 A0 00 00 04 52 00 01 32 03
 * 상태 응답 : 02 00 00 03
 * 데이터 응답 ([]안이 카드 데이터) : 028053000000001C008100 6F4F8407A0000004520001A54450020100470200074301081105904C0000044F07D41000000300019F1003E30034
 * 5F24 02 2303 12 08 [1010010118324938] BF0C1101010250000000000000000000000000009000C103
 * 유효기간 TAG정보 5F24 길이 02 데이터 2303
 * 일련번호 TAG정보 12 길이 08 데이터 1010010118324938
 *
 */

public class RfidReaderACM1281S extends RfidReader implements Runnable {
    public static final String TAG = "RfidReaderACM1281S";

    public enum RFID_CMD {
        RFID_MYFARE,
        RFID_TMONEY,
        RFID_AUTO_RELEASE
    };

    private static final int HEADER_SIZE = 4; // MIN Status 4 byte

    boolean isOpened = false;
    private InputStream serialInputStream;
    private OutputStream serialOutputStream;
    private String serialDev;

    private SerialPort mSerialPort;
    private byte[] src_data = new byte[8];
    private byte[] dest_data = new byte[16];
    private boolean bEndFlag = false;

    private int seq = 1;

    private RFID_CMD cardReadCmd = RFID_CMD.RFID_MYFARE; // T Money

    Thread recvThread;
    Thread sendThread;

    boolean isCardDetected = false;
    boolean isSoundReq = false;

    public RfidReaderACM1281S(String serDev, RFID_CMD rfid_cmd) {
        serialDev = serDev;
        cardReadCmd = rfid_cmd;
        try {
            mSerialPort = new SerialPort(new File(serialDev), 9600, 0);
            if ( mSerialPort == null ) {
                LogWrapper.v("MainAct", "SerialPort Open Fail!");
                return;
            }

            serialInputStream = mSerialPort.getInputStream();
            serialOutputStream = mSerialPort.getOutputStream();

            isOpened = true;

            recvThread = new Thread(this);
            recvThread.start();

            sendThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    sendProcess();
                }
            });

            sendThread.start();
        }
        catch (Exception e) {
            LogWrapper.e("MainAct", e.getMessage());
        }
    }

    @Override
    public void destory() {
        if ( recvThread != null ) recvThread.interrupt();
    }

    @Override
    public void rfidReadRequest() {
        sendSerialCard(cardReadCmd);
    }

    @Override
    public void rfidReadRelease() {
//        sendSerialCard(RFID_CMD.RFID_AUTO_RELEASE);
    }

    private int getSeq() {
        int ret = seq;
        seq = (seq + 1) % 255;
        return ret;
    }

    private byte calXor(byte[] data, int start, int end) {
        byte ret = 0;

        for ( int i=start; i <= end; i++) {
            ret ^= data[i];
        }

        return ret;
    }

    public void sendBeepSet() {
        //02 6B 06 00 00 00 00 01 00 00 00 E0 00 00 21 01 FF 53 03
        byte[] data = new byte[19];

        data[0] = 0x02; //STX

        data[1] = 0x6B; // Type
        // Length
        data[2] = 0x06;
        data[3] = 0x00;
        data[4] = 0x00;
        data[5] = 0x00;

        data[6] = 0x00; // Slot
        data[7] = (byte)getSeq(); // Seq

        data[8] = 0;

        data[9] = 0;
        data[10] = 0;

        //APDU
        data[11] = (byte)0xE0;
        data[12] = (byte)0x00;
        data[13] = (byte)0x00;
        data[14] = (byte)0x21;
        data[15] = (byte)0x01;
        data[16] = (byte)0x8F;

        // CheckSum(XOR)
        data[17] = calXor(data, 1, 16);
        data[18] = 0x03; // ETX

        String hexData = ByteUtil.byteArrayToHexString(data, 0, data.length);  //헥사코드로 보임
        LogWrapper.d(TAG, "Send Beep Set Data> " + hexData);

        try {
            serialOutputStream.write(data);
        }
        catch (Exception err) {
            Log.e(TAG, "Send Err> " + err.toString());
        }
    }
    public void sendRFPowerSetChange(){
        //인식거리 : 60mm
        //02 6B 18 00 00 00 00 01 00 00 00 E0 00 00 2F 13 06 8F AF 85 80 8F 8F 8F 8F 72 53 32 12 76 52 32 12 3F 3F 88 03
        //인식거리 : 20mm
        //02 6B 18 00 00 00 00 01 00 00 00 E0 00 00 2F 13 06 8F AF 85 80 8F 8F 8F 8F 72 53 32 12 76 52 32 12 08 08 80 03

        String cmd_60mm = "0001000000E000002F13068FAF85808F8F8F8F72533212765232123F3F";
        String cmd_20mm = "0001000000E000002F13068FAF85808F8F8F8F72533212765232120808";
        byte[] apdu = hexStringToByteArray(cmd_20mm);

        byte[] data = new byte[37];
        data[0] = 0x02;
        data[1] = 0x6B;
        data[2] = 0x18;
        data[3] = 0x00;
        data[4] = 0x00;
        data[5] = 0x00;

        //apdu
        System.arraycopy(apdu,0,data,6,apdu.length);

        // CheckSum(XOR)
        data[35] = calXor(data, 1, 34);
        data[36] = 0x03; // ETX

        String hexData = ByteUtil.byteArrayToHexString(data, 0, data.length);  //헥사코드로 보임
        LogWrapper.d(TAG, "Send PowerControl data> " + hexData);

        try {
            serialOutputStream.write(data);
        }
        catch (Exception err) {
            Log.e(TAG, "sendRFPowerSetChange:Send Err> " + err.toString());
        }

    }
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public void sendModeSetChange() {
        //02 6B 02 00 00 00 00 01 00 00 00 44 80 AC 03
        byte[] data = new byte[15];
        data[0] = 0x02; //STX

        data[1] = 0x6B; // Type
        // Length
        data[2] = 0x02;
        data[3] = 0x00;
        data[4] = 0x00;
        data[5] = 0x00;

        data[6] = 0x00; // Slot
        data[7] = (byte)getSeq(); // Seq

        data[8] = 0;

        data[9] = 0;
        data[10] = 0;

        //APDU
        data[11] = (byte)0x44;
        data[12] = (byte)0x80; // Message IN & 9600bps

        // CheckSum(XOR)
        data[13] = calXor(data, 1, 12);
        data[14] = 0x03; // ETX

        String hexData = ByteUtil.byteArrayToHexString(data, 0, data.length);  //헥사코드로 보임
        LogWrapper.d(TAG, "Send ModeSetChangeReq> " + hexData);


        try {
            serialOutputStream.write(data);
        }
        catch (Exception err) {
            Log.e(TAG, "sendModeSetChange:Send Err> " + err.toString());
        }
    }

    public void sendBuzzerSound(byte delay) {
        //02 6B 06 00 00 00 00 01 00 00 00 E0 00 00 28 01 0A AF 03
        byte[] data = new byte[19];
        data[0] = 0x02; //STX

        data[1] = 0x6B; // Type
        // Length
        data[2] = 0x06;
        data[3] = 0x00;
        data[4] = 0x00;
        data[5] = 0x00;

        data[6] = 0x00; // Slot
        data[7] = (byte)getSeq(); // Seq

        data[8] = 0;

        data[9] = 0;
        data[10] = 0;

        //APDU
        data[11] = (byte)0xE0;
        data[12] = (byte)0x00;
        data[13] = (byte)0x00;
        data[14] = (byte)0x28;
        data[15] = (byte)0x01;
        data[16] = (byte)delay;

        // CheckSum(XOR)
        data[17] = calXor(data, 1, 16);
        data[18] = 0x03; // ETX

        String hexData = ByteUtil.byteArrayToHexString(data, 0, data.length);  //헥사코드로 보임
        LogWrapper.d(TAG, "sendBuzzerSound> " + hexData);


        try {
            serialOutputStream.write(data);
        }
        catch (Exception err) {
            Log.e(TAG, "sendBuzzerSound:Send Err> " + err.toString());
        }
    }

    public void sendRequestUID() {
        //02 6F 05 00 00 00 00 01 00 00 00 FF CA 00 00 00 5E 03
        byte[] data = new byte[18];
        data[0] = 0x02; //STX

        data[1] = 0x6F; // Type
        // Length
        data[2] = 0x05;
        data[3] = 0x00;
        data[4] = 0x00;
        data[5] = 0x00;

        data[6] = 0x00; // Slot
        data[7] = (byte)getSeq(); // Seq

        data[8] = 0;

        data[9] = 0;
        data[10] = 0;

        //APDU
        data[11] = (byte)0xFF;
        data[12] = (byte)0xCA;
        data[13] = (byte)0x00;
        data[14] = (byte)0x00;
        data[15] = (byte)0x00;

        // CheckSum(XOR)
        data[16] = calXor(data, 1, 15);
        data[17] = 0x03; // ETX

        String hexData = ByteUtil.byteArrayToHexString(data, 0, data.length);  //헥사코드로 보임
        LogWrapper.d(TAG, "sendRequestUID> " + hexData);

        try {
            serialOutputStream.write(data);
        }
        catch (Exception err) {
            Log.e(TAG, "sendBeepSet:Send Err> " + err.toString());
        }
    }

    public void sendRequestTMoney() {
        // 02 6F 0C 00 00 00 00 01 00 00 00
        // 00 A4 04 00 07 A0 00 00 04 52 00 01 32 03
        byte[] data = new byte[25];
        data[0] = 0x02; //STX

        data[1] = 0x6F; // Type
        // Length
        data[2] = 0x0C;
        data[3] = 0x00;
        data[4] = 0x00;
        data[5] = 0x00;

        data[6] = 0x00; // Slot
        data[7] = (byte)getSeq(); // Seq

        data[8] = 0;

        data[9] = 0;
        data[10] = 0;

        //APDU
        data[11] = (byte)0x00;
        data[12] = (byte)0xA4;
        data[13] = (byte)0x04;
        data[14] = (byte)0x00;
        data[15] = (byte)0x07;
        data[16] = (byte)0xA0;
        data[17] = (byte)0x00;
        data[18] = (byte)0x00;
        data[19] = (byte)0x04;
        data[20] = (byte)0x52;
        data[21] = (byte)0x00;
        data[22] = (byte)0x01;

        // CheckSum(XOR)
        data[23] = calXor(data, 1, 22);
        data[24] = 0x03; // ETX

        String hexData = ByteUtil.byteArrayToHexString(data, 0, data.length);  //헥사코드로 보임
        LogWrapper.d(TAG, "SendRequestTmoney Data> " + hexData);

        try {
            serialOutputStream.write(data);
        }
        catch (Exception err) {
            Log.e(TAG, "sendRequestTMoney:Send Err> " + err.toString());
        }

    }

    public synchronized boolean sendSerialCard(RFID_CMD cmd) {
        cardReadCmd = cmd;
        sendModeSetChange();
        sendRFPowerSetChange();
        return true;
    }

    @Override
    public void stopThread() {
        bEndFlag = true;
        recvThread.interrupt();
    }

    public void sendProcess() {
        try {

            Thread.sleep(100);
            sendBeepSet();
            Thread.sleep(100);
            sendModeSetChange();

            while ( !bEndFlag && !Thread.currentThread().isInterrupted()) {
                if ( isCardDetected ) {
                    Thread.sleep(100);
                    if (cardReadCmd == RFID_CMD.RFID_MYFARE) sendRequestUID();
                    else sendRequestTMoney();

                    isCardDetected = false;
                }
                else if ( isSoundReq ){
                    isSoundReq = false;
                    sendBuzzerSound((byte)10); // 100ms
                }

                Thread.sleep(10);
            }
        }
        catch (Exception e) {
            LogWrapper.e(TAG, e.getMessage());
            try { Thread.sleep(100); } catch(Exception ex){}
        }
    }

    @Override
    public void run()
    {
        int aval = 0;
        byte[] readData = new byte[512];
        int timeoutCnt = 0;
        int idx;

        while ( !bEndFlag && !Thread.currentThread().isInterrupted()) {
            try {
                Arrays.fill(readData, (byte)0);

                aval = serialInputStream.available();

                if ( aval >= HEADER_SIZE ) {
                    serialInputStream.read(readData, 0, 1 );
                }
                else {
                    Thread.sleep(10);
                    continue;
                }

                if ( readData[0] != 0x02 ) continue;

                serialInputStream.read(readData, 1, HEADER_SIZE-1 );

                // 만약 Status bit이면 continue
                if ( readData[HEADER_SIZE-1] == 0x03 && readData[1] == readData[2] ) {
                    continue;
                }

                //카드 In Message 처리
                //02 50 03 53 03 // 카드 IN
                //02 50 02 52 03 // 카드 OUT
                if ( readData[0] == 0x02 && readData[1] == 0x50 ) {
                    int timeout = 10;
                    do {
                        aval = serialInputStream.available();
                        if ( aval < 1 ) Thread.sleep(10);
                    } while (aval < 1 && timeout-- > 0);

                    if ( timeout <= 0 ) {
                        // 타임아웃인 경우 비움
                        serialInputStream.skip(aval);
                        continue;
                    }

                    serialInputStream.read(readData, 0, 1 );

                    // 카드 IN
                    if ( readData[2] == 0x03 ) {
                        isCardDetected = true;
                        LogWrapper.d(TAG, "Card In!!");
                    }
                    // 카드 OUT
                    else if ( readData[2] == 0x02 ) {
                        LogWrapper.d(TAG, "Card Out!!");
                    }
                }

                // APDU Message 처리
                //최소 Length 가 HEADER_SIZE + 2 가 되어야 함.(STX + Type + Length(4))
                int timeout = 10;
                do {
                    aval = serialInputStream.available();
                    if ( aval < 2 ) Thread.sleep(10);
                } while(aval < 2 && timeout-- > 0);

                if ( timeout <= 0 ) {
                    // 타임아웃인 경우 비움
                    serialInputStream.skip(aval);
                    continue;
                }

                serialInputStream.read(readData, HEADER_SIZE, 2 );
                int type = readData[1];
                int len = ByteUtil.makeInt(readData[5], readData[4], readData[3], readData[2]);

                // 5 + Length + CHK + ETX 까지 Read
                int remain = 5 + len + 1 + 1;

                aval = serialInputStream.available();
                timeout = 10;
                do {
                    aval = serialInputStream.available();
                    if ( aval < remain ) Thread.sleep(10);
                } while(aval < remain && timeout-- > 0);
                if ( timeout <= 0 ) {
                    // 타임아웃인 경우 비움
                    serialInputStream.skip(aval);
                    continue;
                }

                serialInputStream.read(readData, HEADER_SIZE+2, remain );

                String hexData = ByteUtil.byteArrayToHexString(readData, 0, HEADER_SIZE+2+remain);  //헥사코드로 보임
//                LogWrapper.d(TAG, "Read :"+hexData);
                byte xorData = calXor(readData, 1, HEADER_SIZE+2+remain-3);
                if ( xorData != readData[HEADER_SIZE+2+remain-2]) {
                    LogWrapper.e(TAG, "XOR Error: "+ String.format("%02X", xorData));
                }

                // Data Parsing
                if ( processCardData(readData, HEADER_SIZE+2+remain, len) ) {
                    aval = serialInputStream.available();
                    serialInputStream.skip(aval);
                }

            }catch (Exception e) {
                LogWrapper.e(TAG, e.getMessage());
                try { Thread.sleep(100); } catch(Exception ex){}
            }
        }
    }

    public boolean processCardData(byte[] response, int responesize, int extDataLen)
    {
        int idx = 1 + 10;
        if ( extDataLen < 6 ) return false;

        Arrays.fill(src_data, (byte)0xFF);
        Arrays.fill(dest_data, (byte)0);

        if ( cardReadCmd == RFID_CMD.RFID_MYFARE ) {
            if ( response[idx+4] == (byte)0x90 && response[idx+5] == 0x00) {
                System.arraycopy(response, idx, src_data, 4, 4);
                ASCII_2_HEX_conversion(src_data, dest_data );

                isSoundReq = true;

                try
                {
                    //String cardId = new String(dest_data, 8, dest_data.length/2);
                    String cardId = new String(dest_data, 0, dest_data.length); // 앞에 FFFFFFFF를 삽입함
                    if ( rfidReaderListener != null ) rfidReaderListener.onRfidDataReceive(cardId, true);
                }
                catch(Exception e){ }

                return true;
            }
        }
        else {
            if ( response[responesize-4] == (byte)0x90 && response[responesize-3] == 0x00 && response[idx] == 0x6F) {
                for ( int i=idx+1; i<responesize-19; i++) {
                    // 카드번호 찾기: 유효기간 테그 0x5F24 길이 0x02, 유효기간 2byte, 일변번호 테그:0x12, 길이 0x08
                    if ( response[i] == 0x5F && response[i+1] == 0x24 && response[i+2] == 0x02 && response[i+5] == 0x12 && response[i+6] == 0x08) {
                        System.arraycopy(response, i+7, src_data, 0, 8);
                        ASCII_2_HEX_conversion(src_data, dest_data );

                        isSoundReq = true;

                        try
                        {
                            String cardId = new String(dest_data, 0, dest_data.length);
                            if ( rfidReaderListener != null ) rfidReaderListener.onRfidDataReceive(cardId, true);
                        }
                        catch(Exception e){ }

                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean checkSum(byte[] response, int responesize)
    {
        boolean rescrc = false;
        byte CRC = 0; //xor

        try
        {
            responesize = 5 + response[2]; //길이 찾기(5+가변데이터)

            for (int i = 0; i < responesize - 1; i++)
            {
                CRC ^= response[i]; //xor
            }

            if (CRC == response[responesize - 1])
            {
                rescrc = true;
            }
            else
            {
                rescrc = false;
            }
        }
        catch(Exception e)
        {
            rescrc = false;
        }

        return rescrc;
    }

    private void ASCII_2_HEX_conversion(byte[] src, byte[] dest)
    {
        int n = 0;

        for (int i = 0; i < 8; i++)
        {
            if (((src[i] >> 4) & 0x0F) > 0x09)
            {
                dest[n++] = (byte)(((src[i] >> 4) & 0x0F) + 0x37);
            }
            else
            {
                dest[n++] = (byte)(((src[i] >> 4) & 0x0F) + 0x30);
            }

            if ((src[i] & 0x0F) > 0x09)
            {
                dest[n++] = (byte)((src[i] & 0x0F) + 0x37);
            }
            else
            {
                dest[n++] = (byte)((src[i] & 0x0F) + 0x30);
            }
        }
    }
}
