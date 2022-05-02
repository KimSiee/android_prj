/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 8. 18 오전 8:35
 */

package com.joas.hw.dsp2;

import com.joas.utils.BitUtil;
import com.joas.utils.ByteUtil;
import com.joas.utils.CRC16;
import com.joas.utils.LogWrapper;
import com.joas.utils.SerialPort;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DSPControl2 extends Thread {
    public static final String TAG = "DSPControl2";

    public static final int DSP_VER_REG17_SIZE = 40;
//    public static final int DSP_VER_REG23_SIZE = 23;
    public static final int DSP_VER_REG23_SIZE = 30;

//    public static final int DSP_VER_TXREG_DEFAULT_SIZE = 23;
    public static final int DSP_VER_TXREG_DEFAULT_SIZE = 29;
    public static final int DSP_VER_TXREG_V28_SIZE = 31;        //카페이먼트 번지수 추가

    public static final int METER_TYPE_SLOW = 0;
    public static final int METER_TYPE_FAST = 1;
    public int meterValScale = 1;

    private int channel_max = 1;

    private DSPTxData2[] dspTxData;
    private DSPRxData2[] dspRxData;

    private DSPRxData2[] dspEventRxData, dspEventRxDataOld;

    private SerialPort mSerialPort;
    private String serialDev;
    private InputStream serialInputStream;
    private OutputStream serialOutputStream;

    private boolean bEndFlag = false;
    private boolean bMeterUse = false;
    private int meterType = METER_TYPE_SLOW;
    private long[] lastMeterValue;

    private DSPControl2Listener dspControlListener = null;
    private DSPMonitor2Listener dspMonitorListener = null;
    private DSPTXData2ExtInterface dsptxDataExtInterface = null;

    private int dspReadRegSize = DSP_VER_REG17_SIZE;
    private int dspWriteRegSize = DSP_VER_TXREG_DEFAULT_SIZE;

    private boolean dspCommError = false;
    private int dspCommErrorCnt = 0;

    /*
    public DSPControl(int chan_max, String serDev, DSPControlListener listener)
    {
        InitDspControl(chan_max, serDev, DSP_VER_REG17_SIZE, DSP_VER_TXREG_DEFAULT_SIZE, 1, listener);
    }

    public DSPControl(int chan_max, String serDev, int readRegSize, DSPControlListener listener)
    {
        InitDspControl(chan_max, serDev, readRegSize, DSP_VER_TXREG_DEFAULT_SIZE, 1, listener);
    }

    public DSPControl(int chan_max, String serDev, int readRegSize, int writeRegSize, DSPControlListener listener)
    {
        InitDspControl(chan_max, serDev, readRegSize, writeRegSize, 1, listener);
    }
    */

    public DSPControl2(int chan_max, String serDev, int readRegSize, int writeRegSize, int meterScale, DSPControl2Listener listener)
    {
        InitDspControl(chan_max, serDev, readRegSize, writeRegSize, meterScale, listener);
    }

    public void InitDspControl(int chan_max, String serDev, int readRegSize, int writeRegSize, int meterScale, DSPControl2Listener listener)
    {
        channel_max = chan_max;
        serialDev = serDev;
        this.meterValScale = meterScale;
        this.dspReadRegSize = readRegSize;
        this.dspWriteRegSize = writeRegSize;
        dspControlListener = listener;

        dspRxData = new DSPRxData2[this.channel_max];
        dspTxData = new DSPTxData2[this.channel_max];

        dspEventRxData = new DSPRxData2[this.channel_max];
        dspEventRxDataOld = new DSPRxData2[this.channel_max];

        lastMeterValue = new long[this.channel_max];

        for (int i=0; i<this.channel_max; i++) {
            dspRxData[i] = new DSPRxData2(i+1, dspReadRegSize, meterValScale);
            dspEventRxDataOld[i] = new DSPRxData2(i+1, dspReadRegSize, meterValScale);

            dspTxData[i] = new DSPTxData2(i+1, dspWriteRegSize);

            lastMeterValue[i] = 0;
        }

        try {
            mSerialPort = new SerialPort(new File(serialDev), 38400, 0);
            if ( mSerialPort == null ) {
                LogWrapper.v("DSPControl", "SerialPort Open Fail!");
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
            LogWrapper.e("DSPControl", e.toString());
        }
    }

    public void setDspMonitorListener(DSPMonitor2Listener listener) {
        dspMonitorListener = listener;
    }

    public void setDSPTXDataExtInterface(DSPTXData2ExtInterface extInterface) {
        dsptxDataExtInterface = extInterface;
    }

    public void setMeterUse(boolean tf) {
        bMeterUse = tf;
    }

    public void setMeterType(int type) {
        meterType = type;
    }

    public DSPTxData2 getDspTxData(int channel) {
        if ( channel >= channel_max ) return null;
        return dspTxData[channel];
    }

    public DSPRxData2 getDspRxData2(int channel) {
        if ( channel >= channel_max ) return null;
        DSPRxData2 data = new DSPRxData2(channel+1, dspReadRegSize, meterValScale);
        synchronized (dspRxData[channel].lock) {
            System.arraycopy(dspRxData[channel].rawData, 0, data.rawData, 0, data.rawData.length);
        }
        data.decode();
        return data;
    }

    public boolean getDspCommError() { return dspCommError; }

    public void stopThread() {bEndFlag = true;}

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
                synchronized (dspTxData[curChan].lock) {
                    if (bMeterUse && meterType == METER_TYPE_FAST)
                        dspTxData[curChan].meterUpdateCmdFast = 3; // 전력량계 얻어오는 명령

                    sendData = dspTxData[curChan].encode(curChan, dsptxDataExtInterface);
                }

                serialOutputStream.write(sendData);
                serialOutputStream.flush();

                // ReadOnly.. Lock을 하지 않는다....(디버깅용)
                try {
                    if ( dspMonitorListener != null ) dspMonitorListener.onDspTxDataEvent(curChan, dspTxData[curChan]);
                } catch (Exception e) {
                    LogWrapper.e(TAG+":MNTListner, TX:", e.toString());
                }

                // 4. Read Tx Data Ack
                getDSPData(curChan, 8, false);
                Thread.sleep(100);

                curChan = (curChan + 1) % channel_max;
            }
            catch (InterruptedException e) {
                LogWrapper.e(TAG, "Force Finish DSPControl:"+e.getMessage());
                return;
            }
            catch (Exception e) {
                LogWrapper.e(TAG, e.getMessage());
                try { Thread.sleep(100); }catch(Exception ex){}
            }
        }
        LogWrapper.e(TAG, "Finish DSPControl");
    }

    public void sendGetDataReq(int chan) {
        byte[] buf = new byte[8];
        int idx = 0;

        buf[idx++] = (byte)(chan+1);  // address  = chan + 1
        buf[idx++] = (byte)4; // Function Code 4

        // start address ( 400 )
        buf[idx++] = (byte)((DSPRxData2.START_REG_ADDR >> 8 ) & 0xff);
        buf[idx++] = (byte)(DSPRxData2.START_REG_ADDR & 0xff);

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

    public void getDSPData(int chan, int reqDataSize, boolean isRxData) {
        int waitCnt = 10; // 100 msd
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
                            synchronized (dspRxData[readChan].lock) {
                                System.arraycopy(readData, 0, dspRxData[readChan].rawData, 0, reqDataSize);
                                try {
                                    if (dspMonitorListener != null)
                                        dspMonitorListener.onDspRxDataEvent(readChan, dspRxData[readChan]);
                                } catch (Exception e) {
                                    LogWrapper.e(TAG+":MNTListner, RX:", e.toString());
                                }
                            }

                        }
                        else {
                            LogWrapper.e(TAG, "Read DSP CRC Err.:"+Integer.toHexString(readCrc)+"<>"+Integer.toHexString(crc));
                            LogWrapper.e(TAG, "DATA:"+ByteUtil.byteArrayToHexStringDiv(readData, 0, reqDataSize, ' '));
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
                    if ( dspControlListener != null ) dspControlListener.onDspCommErrorStstus(true);
                }
                dspCommError = true;
            }
        }
        else {
            dspCommErrorCnt = 0;
            if ( dspCommError == true ) {
                if ( dspControlListener != null ) dspControlListener.onDspCommErrorStstus(false);
            }
            dspCommError = false;
        }
    }

    //================================================
    // DSP Check
    //================================================
    void dspRxProcess(int channel) {
        // 상태값 변경
        if ( dspEventRxDataOld[channel].status400 != dspEventRxData[channel].status400 ) {
            // 두 값을 XOR 한다. 이후 남은 비트가 변경된 값
            int diff = dspEventRxDataOld[channel].status400 ^ dspEventRxData[channel].status400;

            if ( dspControlListener == null ) return;

            for (int i=0; i<16; i++) {
                if ( BitUtil.getBitBoolean(diff, i) ) {
                    dspControlListener.onDspStatusChange(channel, DSPRxData2.STATUS400.getValue(i), BitUtil.getBitBoolean(dspEventRxData[channel].status400, i));
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

    public long getLastMeterValue(int channel) {
        return lastMeterValue[channel];
    }

    public void dspRxThread() {
        int channel = 0;
        while ( true ) {
            try {
                dspEventRxData[channel] = getDspRxData2(channel);
                if (dspEventRxData != null) {
                    dspRxProcess(channel);
                    dspEventRxDataOld[channel] = dspEventRxData[channel];
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

    /**
     * slow chargertype setting func - add by si. 220418
     * @param channel
     * @param slowChargerType
     */
    public void setSlowChargerType(int channel, int slowChargerType) {
        synchronized (dspTxData[channel].lock) {
            dspTxData[channel].slowChargerType = slowChargerType;
        }
    }

    public void setUIState(int channel, DSPTxData2.DSP_UI_STATE state) {
        synchronized (dspTxData[channel].lock) {
            dspTxData[channel].uiState = state.getValue();
        }
    }

    public void setConnectorSelect(int channel, int select) {
        synchronized (dspTxData[channel].lock) {
            dspTxData[channel].chargeSelect = select;
        }
    }

    public void setState200(int channel, DSPTxData2.STATUS200 state, boolean tf) {
        synchronized (dspTxData[channel].lock) {
            dspTxData[channel].set200Reg(state, tf);
        }
    }

    public boolean getState200(int channel, DSPTxData2.STATUS200 state) {
        boolean tf = false;
        synchronized (dspTxData[channel].lock) {
            tf = dspTxData[channel].get200Reg(state);
        }
        return tf;
    }

    public void setVoltageCmd(int channel, float val) {
        synchronized (dspTxData[channel].lock) {
            dspTxData[channel].voltageCmd = val;
        }
    }

    public void setAmpareCmd(int channel, float val) {
        synchronized (dspTxData[channel].lock) {
            dspTxData[channel].ampareCmd = val;
        }
    }

    public void setPowerlimit(int channel, int val) {
        synchronized (dspTxData[channel].lock) {
            dspTxData[channel].powerLimit = val;
        }
    }

    /**
     * 신규 프로토콜 ver2.0 추가
     *
     * by Lee 20200521
     *
     * @param channel
     * @param val
     */
    public void setOutputVoltageAC(int channel, float val) {
        synchronized (dspTxData[channel].lock) {
            dspTxData[channel].outputVoltageAC = val;
        }
    }

    public void setOutputAmpareAC(int channel, float val) {
        synchronized (dspTxData[channel].lock) {
            dspTxData[channel].outputAmpareAC = val;
        }
    }

    public void setMeterAC(int channel, float val) {
        synchronized (dspTxData[channel].lock) {
            dspTxData[channel].meterAC = val;
        }
    }

    /***
     * by si 210914, 급속 전력량,출력전류,출력전압 값 세팅
     */
    public void setOutputVoltageDC(int channel, float val){
        synchronized (dspTxData[channel].lock){
            dspTxData[channel].outputVoltageDC = val;
        }
    }
    public void setOutputAmpareDC(int channel, float val) {
        synchronized (dspTxData[channel].lock) {
            dspTxData[channel].outputAmpareDC = val;
        }
    }

    public void setMeterDC(int channel, float val) {
        synchronized (dspTxData[channel].lock) {
            dspTxData[channel].meterDC = val;
        }
    }

    /**
     * 216번지 값 쓰기
     *
     * by Lee 20200515
     * @param channel
     * @param state
     * @param tf
     */
    public void setState216(int channel, DSPTxData2.STATUS216 state, boolean tf) {
        synchronized (dspTxData[channel].lock) {
            dspTxData[channel].set216Reg(state, tf);
        }
    }


    public void setDspTxData(int channel, int index, int value) {
        synchronized (dspTxData[channel].lock) {
            try {
                int idx = DSPTxData2.DATA_OFFSET+index*2;
                dspTxData[channel].rawData[idx++] = (byte)((value >> 8 ) & 0xff);
                dspTxData[channel].rawData[idx++] = (byte)((value >> 0 ) & 0xff);
            }
            catch (Exception e) {

            }
        }
    }
}
