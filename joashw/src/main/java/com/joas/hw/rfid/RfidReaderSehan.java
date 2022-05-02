/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 8. 22 오전 8:47
 */

package com.joas.hw.rfid;

import com.joas.utils.LogWrapper;
import com.joas.utils.SerialPort;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Created by user on 2017-08-22.
 */

public class RfidReaderSehan extends RfidReader implements Runnable {
    public static final String TAG = "RfidReaderSehan";

    public enum RFID_CMD {
        RFID_STATUS,
        RFID_TMONEY,
        RFID_SCANMODE,
        RFID_AUTO_RELEASE,
        RFID_AUTO_MYFARE,
        RFID_AUTO_TMONEY
    };

    private static final int HEADER_SIZE = 3; // STX, CMD, LEN

    private byte[] SIO_TX_Buff = new byte[6];
    boolean isOpened = false;
    private InputStream serialInputStream;
    private OutputStream serialOutputStream;
    private String serialDev;

    private SerialPort mSerialPort;
    private byte[] src_data = new byte[8];
    private byte[] dest_data = new byte[16];
    private boolean bEndFlag = false;

    private RFID_CMD cardReadCmd = RFID_CMD.RFID_AUTO_TMONEY; // T Money


    Thread recvThread;

    public RfidReaderSehan(String serDev, RFID_CMD rfid_cmd) {
        serialDev = serDev;
        cardReadCmd = rfid_cmd;
        try {
            mSerialPort = new SerialPort(new File(serialDev), 115200, 0);
            if ( mSerialPort == null ) {
                LogWrapper.v("MainAct", "SerialPort Open Fail!");
                return;
            }

            serialInputStream = mSerialPort.getInputStream();
            serialOutputStream = mSerialPort.getOutputStream();

            isOpened = true;

            recvThread = new Thread(this);
            recvThread.start();
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
        sendSerialCard(RFID_CMD.RFID_AUTO_RELEASE);
    }

    public synchronized boolean sendSerialCard(RFID_CMD cmd) {
        if (isOpened)
        {
            try
            {
                SIO_Buff_Make(cmd);                            //명령
                serialOutputStream.write(SIO_TX_Buff);
                //spc.Write(SIO_TX_Buff, 0, SIO_TX_Buff.Length);
                LogWrapper.d(TAG, "SendSerialCard:"+cmd.name());
            }
            catch (Exception err)
            {
                //serStatus = "Error in read event: " + err.Message;
                return false;
            }
            return true;
        }
        else
        {
            //serStatus = "Serial port not open";
            return false;
        }
    }

    private void SIO_Buff_Make(RFID_CMD cmd)
    {
        byte CRC = 0; //xor

        // STX
        SIO_TX_Buff[0] = 0x02;

        switch (cmd)// Command Code
        {

            case RFID_STATUS: SIO_TX_Buff[1] = (byte)0xA0; break;   //상태
            case RFID_SCANMODE: SIO_TX_Buff[1] = (byte)0xA1; break;   //스캔모드
            case RFID_TMONEY: SIO_TX_Buff[1] = (byte)0xA2; break;   //티머니 1회리드
            case RFID_AUTO_RELEASE: SIO_TX_Buff[1] = (byte)0xB0; break;   //오토모드
            case RFID_AUTO_MYFARE: SIO_TX_Buff[1] = (byte)0xB0; break;   //오토모드
            case RFID_AUTO_TMONEY: SIO_TX_Buff[1] = (byte)0xB0; break;   //오토모드
        }

        // LEN
        SIO_TX_Buff[2] = 0x01;   //length

        switch (cmd) // Data
        {
            case RFID_STATUS: SIO_TX_Buff[3] = (byte)0x00; break;   //상태
            case RFID_SCANMODE: SIO_TX_Buff[3] = (byte)0x00; break;   //스캔모드
            case RFID_TMONEY: SIO_TX_Buff[3] = (byte)0x00; break;   //티머니 1회리드
            case RFID_AUTO_RELEASE: SIO_TX_Buff[3] = (byte)0x00; break;   //오토모드 해지
            case RFID_AUTO_MYFARE: SIO_TX_Buff[3] = (byte)0x01; break;   //오토모드 마이패어
            case RFID_AUTO_TMONEY: SIO_TX_Buff[3] = (byte)0x02; break;   //오토모드 티머니
        }

        // ETX
        SIO_TX_Buff[4] = (byte)0x03;

        CRC = 0;
        for (int i = 0; i < 5; i++)
        {
            CRC ^= SIO_TX_Buff[i]; //xor
        }

        SIO_TX_Buff[5] = CRC; //lv
    }

    @Override
    public void stopThread() {
        bEndFlag = true;
        recvThread.interrupt();
    }

    @Override
    public void run()
    {
        int aval = 0;
        byte[] readData = new byte[512];
        int timeoutCnt = 0;

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

                int len = readData[2];

                aval = serialInputStream.available();

                if ( readData[0] != 0x02 )  {
                    Thread.sleep(100);
                    serialInputStream.skip(aval);
                }
                else {
                    timeoutCnt = 0;
                    while ( aval < (len+2) ) {
                        aval = serialInputStream.available();
                        Thread.sleep(10);
                        if ( timeoutCnt++ > 100) {
                            break;
                        }
                    }
                }
                if ( aval >= (len+2) ) {
                    serialInputStream.read(readData, HEADER_SIZE, len+2 );
                    processCardData(readData, HEADER_SIZE+len+2);
                }

            }catch (Exception e) {
                LogWrapper.e(TAG, e.getMessage());
                try { Thread.sleep(100); } catch(Exception ex){}
            }
        }
    }

    public void processCardData(byte[] response, int responesize)
    {
        Arrays.fill(src_data, (byte)0xFF);
        Arrays.fill(dest_data, (byte)0);

        //수신 메세지
        if (checkSum(response, responesize) == true && response[0] == 0x02 && response[responesize - 2] == 0x03)  //체크섬/STX/ETX
        {
            //카드번호를 얻어 저장한다.
            if (response[1] == (byte)0xE1 || response[1] == (byte)0xE2)
            {
                if (response[1] == (byte)0xE1)  //마이패어
                {
                    System.arraycopy(response, 4, src_data, 4, 4);
                    ASCII_2_HEX_conversion(src_data, dest_data );

                    try
                    {
                        String cardId = new String(dest_data, 8, dest_data.length/2);
                        if ( rfidReaderListener != null ) rfidReaderListener.onRfidDataReceive(cardId, true);
                    }
                    catch(Exception e){ }
                }
                else if (response[1] == (byte)0xE2)  //티머니
                {
                    System.arraycopy(response, 4, dest_data, 0, 16); //실제 카드번호 ASCII

                    try
                    {
                        String cardId = new String(dest_data, 0, dest_data.length);
                        if ( rfidReaderListener != null ) rfidReaderListener.onRfidDataReceive(cardId, true);
                    }
                    catch(Exception e) { }
                }
            }
        }
        else {
            if ( rfidReaderListener != null ) rfidReaderListener.onRfidDataReceive("0000000000000000", false);
        }
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
