/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 10. 15 오후 12:10
 *
 */

package com.joas.hw.dsptest;

import com.joas.utils.BitUtil;
import com.joas.utils.ByteUtil;
import com.joas.utils.CRC16;
import com.joas.utils.LogWrapper;
import com.joas.utils.SerialPort;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DSPTestControl extends Thread{
    public static final String TAG = "DSPTestControl";

    public static final int DSP_TEST_REG17_SIZE = 13;       //RX
    public static final int DSP_TEST_REG23_SIZE = 4;        //TX

    public static final int METER_TYPE_SLOW = 0;
    public static final int METER_TYPE_FAST = 1;
    public int meterValScale = 1;

    private int channel_max = 1;

    private DSPTestTxData[] dspTestTxData;
    private DSPTestRxData[] dspTestRxData;

    private DSPTestRxData[] dspEventRxData_test, dspEventRxDataOld_test;

    private SerialPort mSerialPort;
    private String serialDev;
    private InputStream serialInputStream;
    private OutputStream serialOutputStream;

    private boolean bEndFlag = false;
    private boolean bMeterUse = false;
    private int meterType = METER_TYPE_SLOW;
    private long[] lastMeterValue;

    private DSPTestControlListener dspTestControlListener = null;
    private DSPTestMonitorListener dspTestMonitorListener = null;
    private DSPTestTXDataExInterface dspTesttxDataExtInterface = null;

    private int dspReadRegSize = DSP_TEST_REG17_SIZE;
    private int dspWriteRegSize = DSP_TEST_REG23_SIZE;

    private boolean dspCommError = false;
    private int dspCommErrorCnt = 0;



    public DSPTestControl(int chan_max, String serDev, int readRegSize, int writeRegSize, int meterScale, DSPTestControlListener listener)
    {
        InitDspControl(chan_max, serDev, readRegSize, writeRegSize, meterScale, listener);
    }

    public void InitDspControl(int chan_max, String serDev, int readRegSize, int writeRegSize, int meterScale, DSPTestControlListener listener)
    {
        channel_max = chan_max;
        serialDev = serDev;
        this.meterValScale = meterScale;
        this.dspReadRegSize = readRegSize;
        this.dspWriteRegSize = writeRegSize;
        dspTestControlListener = listener;

        dspTestRxData = new DSPTestRxData[this.channel_max];
        dspTestTxData = new DSPTestTxData[this.channel_max];

        dspEventRxData_test = new DSPTestRxData[this.channel_max];
        dspEventRxDataOld_test = new DSPTestRxData[this.channel_max];

        lastMeterValue = new long[this.channel_max];

        for (int i=0; i<this.channel_max; i++) {
            dspTestRxData[i] = new DSPTestRxData(i+1, dspReadRegSize, meterValScale);
            dspEventRxDataOld_test[i] = new DSPTestRxData(i+1, dspReadRegSize, meterValScale);

            dspTestTxData[i] = new DSPTestTxData(i+1, dspWriteRegSize);

            lastMeterValue[i] = 0;
        }

        try {
            mSerialPort = new SerialPort(new File(serialDev), 38400, 0);
            if ( mSerialPort == null ) {
                LogWrapper.v("DSPTestControl", "SerialPort Open Fail!");
                return;
            }

            serialInputStream = mSerialPort.getInputStream();
            serialOutputStream = mSerialPort.getOutputStream();

            new Thread(new Runnable() {
                @Override public void run() {
                    dspRxThread();
                }
            }).start();

        }
        catch (Exception e) {
            LogWrapper.e("DSPTestControl", e.toString());
        }
    }

    public void getDSPData(int chan, int reqDataSize, boolean isRxData) {
        int waitCnt = 10; // 100 ms
        int aval = 0;
        byte[] readData = new byte[256];
        boolean err = false;

        try {
            while (waitCnt-- > 0) {
                aval = serialInputStream.available();
                if ( aval >= reqDataSize ) {

                    if ( isRxData ) {
                        serialInputStream.read(readData, 0, reqDataSize);
                        int readCrc =  ByteUtil.makeWord(readData[reqDataSize-1], readData[reqDataSize-2]);
                        int crc = CRC16.calc(0xFFFF, readData, 0, reqDataSize-2);
                        if ( readCrc == crc ) {
                            int readChan = chan;
                            // 만약 읽은 데이터의 채널이 틀린 경우에 처리
                            if ( (readData[0]-1) != chan ) {
                                if ( readData[0] > channel_max || readData[0]== 0) break;
                                else readChan = (readData[0]-1);
                            }
                            // 해당 데이터에 대한 동기화
                            synchronized (dspTestRxData[readChan].lock) {
                                System.arraycopy(readData, 0, dspTestRxData[readChan].rawData, 0, reqDataSize);
                                try {
                                    if (dspTestMonitorListener != null)
                                        dspTestMonitorListener.onTestDspRxDataEvent(readChan, dspTestRxData[readChan]);
                                } catch (Exception e) {
                                    LogWrapper.e(TAG+":MNTListner(Test), RX:", e.toString());
                                }
                            }

                        }
                        else {
                            LogWrapper.e(TAG, "Read DSP CRC Err(Test).:"+Integer.toHexString(readCrc)+"<>"+Integer.toHexString(crc));
                            LogWrapper.e(TAG, "DATA(Test):"+ByteUtil.byteArrayToHexStringDiv(readData, 0, reqDataSize, ' '));
                            err = true;
                        }
                        //LogWrapper.v(TAG, "Read DSP Data:"+aval+" Done.");
                    }
                    else {
                        // TX Ack 데이터 처리..
                        //LogWrapper.v(TAG, "Read DSP Ack:"+aval+" Done.");
                    }
                    // 남아있는 여분의 데이터를 삭제한다.
                    aval = serialInputStream.available();
                    if (aval > 0) serialInputStream.skip(aval);
                    break;
                }
                Thread.sleep(10);
            }
        } catch (Exception e) {
            LogWrapper.e(TAG, e.toString());
        }

        // Timeout DSP Data
        if ( waitCnt <= 0 ) {
            //LogWrapper.e(TAG, "DSP Rx Timeout:"+aval);
            err = true;
        }

        // DSP 에러 처리
        if ( err == true ) {
            dspCommErrorCnt++;
            if ( dspCommErrorCnt > 100) {
                if ( dspCommError == false ) {
                    if ( dspTestControlListener != null ) dspTestControlListener.onTestDspCommErrorStstus(true);
                }
                dspCommError = true;
            }
        }
        else {
            dspCommErrorCnt = 0;
            if ( dspCommError == true ) {
                if ( dspTestControlListener != null ) dspTestControlListener.onTestDspCommErrorStstus(false);
            }
            dspCommError = false;
        }
    }


    //================================================
    // DSP Check
    //================================================
    void dspRxProcess(int channel) {
        // 상태값 변경
        if ( dspEventRxDataOld_test[channel].status500 != dspEventRxData_test[channel].status500 ) {
            // 두 값을 XOR 한다. 이후 남은 비트가 변경된 값
            int diff = dspEventRxDataOld_test[channel].status500 ^ dspEventRxData_test[channel].status500;

            if ( dspTestControlListener == null ) return;

            for (int i=0; i<16; i++) {
                if ( BitUtil.getBitBoolean(diff, i) ) {
                    dspTestControlListener.onTestDspStatusChange(channel, DSPTestRxData.STATUS500.getValue(i), BitUtil.getBitBoolean(dspEventRxData_test[channel].status500, i));
                }
            }
        }

        // Meter값 비교
        // 예전 프로토콜인 경우 (전력량계 통신 X)
//        if ( bMeterUse ) {
//            if ( meterType == METER_TYPE_SLOW) {
//                if (dspEventRxDataOld[channel].meterValue != dspEventRxData[channel].meterValue) {
//                    dspControlListener.onDspMeterChange(channel, dspEventRxData[channel].meterValue);
//                }
//                lastMeterValue[channel] = dspEventRxData[channel].meterValue;
//            }
//            else {
//                if (dspEventRxDataOld[channel].meterValueFast != dspEventRxData[channel].meterValueFast) {
//                    dspControlListener.onDspMeterChange(channel, dspEventRxData[channel].meterValueFast);
//                }
//                lastMeterValue[channel] = dspEventRxData[channel].meterValueFast;
//            }
//        }
    }

    public void dspRxThread() {
        int channel = 0;
        while ( true ) {
            try {
                dspEventRxData_test[channel] = getDspRxData2(channel);
                if (dspEventRxData_test != null) {
                    dspRxProcess(channel);
                    dspEventRxDataOld_test[channel] = dspEventRxData_test[channel];
                }
            }
            catch (Exception e) {
                LogWrapper.e(TAG, "DSP RX ERR:"+e.toString());
            }
            try {
                Thread.sleep(100); // 100ms 마다 동작
            } catch(Exception e){}

            channel = (channel + 1) % channel_max;
        }
    }

    public void setDspMonitorListener(DSPTestMonitorListener listener) {
        dspTestMonitorListener = listener;
    }

    public void setDSPTXDataExtInterface(DSPTestTXDataExInterface extInterface) {
        dspTesttxDataExtInterface = extInterface;
    }

    public void setMeterUse(boolean tf) {
        bMeterUse = tf;
    }

    public void setMeterType(int type) {
        meterType = type;
    }

    public DSPTestTxData getDspTxData(int channel) {
        if ( channel >= channel_max ) return null;
        return dspTestTxData[channel];
    }

    public DSPTestRxData getDspRxData2(int channel) {
        if ( channel >= channel_max ) return null;
        DSPTestRxData data = new DSPTestRxData(channel+1, dspReadRegSize, meterValScale);
        synchronized (dspTestRxData[channel].lock) {
            System.arraycopy(dspTestRxData[channel].rawData, 0, data.rawData, 0, data.rawData.length);
        }
        data.decode();
        return data;
    }

    public boolean getDspCommError() { return dspCommError; }

    public void stopThread() {bEndFlag = true;}


    public void sendGetDataReq(int chan) {
        byte[] buf = new byte[8];
        int idx = 0;

        buf[idx++] = (byte)(chan+1);  // address  = chan + 1
        buf[idx++] = (byte)4; // Function Code 4

        // start address ( 500 )
        buf[idx++] = (byte)((DSPTestRxData.START_REG_ADDR >> 8 ) & 0xff);
        buf[idx++] = (byte)(DSPTestRxData.START_REG_ADDR & 0xff);

        buf[idx++] = (byte)((dspReadRegSize >> 8 ) & 0xff);
        buf[idx++] = (byte)(dspReadRegSize & 0xff);

        int crc = CRC16.calc(0xFFFF, buf, 0, idx);
        // CRC
        buf[idx++] = (byte)(crc & 0xff);
        buf[idx++] = (byte)((crc >> 8 ) & 0xff);


        try {
            serialOutputStream.write(buf);
            serialOutputStream.flush();
        } catch (IOException e) {
            LogWrapper.e(TAG, e.getMessage());
        }
    }

    @Override
    public void run() {
        int curChan = 0;

        bEndFlag = false;
        if ( mSerialPort == null ) return;

        while ( !bEndFlag && !isInterrupted()) {
            try {
                // 1. Send GetDate Request Message
                sendGetDataReq(curChan);

                // 2. Read GetData
                getDSPData(curChan, 5 + 2 * dspReadRegSize, true);
                Thread.sleep(100);

                // 3. Send Tx Data
                byte[] sendData;
                synchronized (dspTestTxData[curChan].lock) {
                    if (bMeterUse && meterType == METER_TYPE_FAST)
                        dspTestTxData[curChan].meterUpdateCmdFast = 3; // 전력량계 얻어오는 명령

                    sendData = dspTestTxData[curChan].encode(curChan, dspTesttxDataExtInterface);
                }

                serialOutputStream.write(sendData);
                serialOutputStream.flush();

                // ReadOnly.. Lock을 하지 않는다....(디버깅용)
                try {
                    if ( dspTestMonitorListener != null ) dspTestMonitorListener.onTestDspTxDataEvent(curChan, dspTestTxData[curChan]);
                } catch (Exception e) {
                    LogWrapper.e(TAG+":MNTListner(Test), TX:", e.toString());
                }

                // 4. Read Tx Data Ack
                getDSPData(curChan, 8, false);
                Thread.sleep(100);

                curChan = (curChan + 1) % channel_max;
            }
            catch (InterruptedException e) {
                LogWrapper.e(TAG, "Force Finish DSPControl(Test):"+e.getMessage());
                return;
            }
            catch (Exception e) {
                LogWrapper.e(TAG, e.getMessage());
                try { Thread.sleep(100); }catch(Exception ex){}
            }
        }
        LogWrapper.e(TAG, "Finish DSPControl(Test)");
    }
}
