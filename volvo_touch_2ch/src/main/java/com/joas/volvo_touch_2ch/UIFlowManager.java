/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 7. 21. 오후 4:36
 *
 */

package com.joas.volvo_touch_2ch;

import android.app.AlarmManager;
import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.joas.evcomm.AlarmCode;
import com.joas.evcomm.EvCommManager;
import com.joas.hw.dsp2.DSPControl2;
import com.joas.hw.dsp2.DSPControl2Listener;
import com.joas.hw.dsp2.DSPRxData2;
import com.joas.hw.dsp2.DSPTxData2;
import com.joas.hw.rfid.RfidReaderListener;
import com.joas.utils.FileUtil;
import com.joas.utils.LogWrapper;
import com.joas.utils.RemoteUpdater;
import com.joas.utils.ZipUtils;
import com.joas.volvo_touch_2ch.page.PageEvent;
import com.joas.volvo_touch_2ch.page.PageID;
import com.joas.volvo_touch_2ch_comm.PoscoModemCommManager;
import com.joas.volvo_touch_2ch_comm.PoscoModemCommManagerListener;
import com.joas.volvo_touch_2ch_comm.Volvo2chChargerInfo;
import com.joas.volvo_touch_2ch_comm.VolvoCommManager;
import com.joas.volvo_touch_2ch_comm.VolvoCommManagerListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

public class UIFlowManager implements RfidReaderListener, DSPControl2Listener, UpdateManagerListener, VolvoCommManagerListener, PoscoModemCommManagerListener {
    public static final String TAG = "UIFlowManager";

    @Override
    public void onUpdateStatus(UpdateManager.UpdateState state) {
        //사용안함
    }

    @Override
    public void onSystemTimeUpdate(Date syncTime) {
        Date curTime = new Date();
        AlarmManager am = (AlarmManager) mainActivity.getSystemService(Context.ALARM_SERVICE);

        // 현재 시각과 서버 시각이 일정이상 차이가 나면 현재 시간을 갱신한다.
        if (Math.abs(curTime.getTime() - syncTime.getTime()) > TypeDefine.TIME_SYNC_GAP_MS) {
            am.setTime(syncTime.getTime());
            LogWrapper.v(TAG, "TimeSync : " + syncTime.toString());
        }
    }

    @Override
    public void onCommConnected(int ch) {
        mainActivity.setCommConnStatus(true);
        dspControl.setState216(chargeData.dspChannel, DSPTxData2.STATUS216.SERVER_COMM, true);     //서버통신상태
        LogWrapper.v(TAG, "CH:" + ch + "Server Connected!!");
    }

    @Override
    public void onCommDisconnected(int ch) {
        mainActivity.setCommConnStatus(false);
        dspControl.setState216(chargeData.dspChannel, DSPTxData2.STATUS216.SERVER_COMM, false);     //서버통신상태
        LogWrapper.v(TAG, "CH:" + ch + "Server Disconnected!!");
    }

    @Override
    public void onAuthResultEvent(boolean isSuccess, int reason, int costUnit, int ch) {
        onAuthResultEvent(isSuccess, reason);
    }

    @Override
    public void onResetRequest(int kind) {
        // 충전기 리셋
        if ( kind == 0x01 || kind == 0x04 ) {
            multiChannelUIManager.onResetRequest(true);
        }
    }

    @Override
    public void onChargerInfoChangeReq(int kind, String value, int ch) {
        String value_N1 = value.trim();

        if(!value_N1.equals(""))
        {
            if(kind == 0x01)
            {
                //충전기 서버IP 설정값 변경 및 적용
                getCpConfig().serverIP = value_N1;
                getCpConfig().saveConfig(mainActivity);
                //리셋진행
                multiChannelUIManager.onResetRequest(true);
            }
            else if(kind == 0x02)
            {
                //충전기 설정시간 관련 값 저장
                String[] setTimeVal = value_N1.split(",");
                volvo2chChargerInfo.settimeInterval = setTimeVal[0];
                volvo2chChargerInfo.minsetTime = setTimeVal[1];
                volvo2chChargerInfo.maxsetTime = setTimeVal[2];
                volvo2chChargerInfo.saveCpSetTimeInfo(mainActivity.getBaseContext(),Environment.getExternalStorageDirectory()+TypeDefine.CP_CONFIG_PATH,TypeDefine.CHGSETTIME_INFO_FILENAME);

                //add by ksi. 20200529 - b1전문에 포함된 충전기 설정시간 전송값 저장
                volvo2chChargerInfo.chargingAvalTime = value;
            }
            volvoCommManager.sendChargerStatus();
        }
    }

    /**
     * 원격 시작 및 종료
     * @param cmd 0x01: 완속 CType | 급속 AC3상, 0x02: 완속 Btype | 급속 DC차데모, 0x03: 종료, 0x04: 급속 DC콤보, 0x05: 즉시종료
     * @return 성공여부
     */
    @Override
    public boolean onRemoteStartStop(int cmd, int ch) {
        boolean ret = false;

        if(cmd == 0x05) {
            ret = true;     //예약취소일경우, 충전중,대기중 상관없이 무조건 1Q true 리턴
        }
        else {
            if ((meterConfig.lcdType.equals("None") == false && getUIFlowState() == UIFlowState.UI_CARD_TAG)
                    || (meterConfig.lcdType.equals("None") && getUIFlowState() == UIFlowState.UI_READY))        //카드태깅화면(대기중)일때만 원격신호 받고 원격충전 진행하도록
            {
                if (cmd == 0x01 || cmd == 0x02) {
                    remote_lastCardnum = volvo2chChargerInfo.remote_cardNum;
                    volvo2chChargerInfo.payMethod = volvo2chChargerInfo.PAY_METHOD_BY_SERVER;
                    chargeData.authType = TypeDefine.AuthType.MEMBER;
                    volvo2chChargerInfo.authType = Volvo2chChargerInfo.CommAuthType.MEMBER;
                    volvo2chChargerInfo.paymentCompany = 0x00;
                    volvo2chChargerInfo.payMethod = 0x00;
                    volvo2chChargerInfo.isRemoteCharge = true;
//                    setUIFlowState(UIFlowState.UI_CONNECTOR_WAIT);
                    if(cmd == 0x01) {
                        chargeData.connectorType = TypeDefine.ConnectorType.CTYPE;
                        dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData2.CHARGER_SELECT_SLOW_CTYPE);
                    }
                    else if(cmd == 0x02){
                        chargeData.connectorType = TypeDefine.ConnectorType.BTYPE;
                        dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData2.CHARGER_SELECT_SLOW_BTYPE);
                    }
                    doAuthComplete();
                    LogWrapper.v(TAG, "Remote Charging Request Event:" + volvo2chChargerInfo.remote_cardNum);

                    ret = true;
                } else ret = false;
            } else {
                if (cmd == 0x03) {
                    if (remote_lastCardnum.equals(volvo2chChargerInfo.remote_cardNum)) {
                        LogWrapper.v(TAG, "Stop by Remote Signal");

                        if(getUIFlowState() == UIFlowState.UI_CHARGING) onChargingStop();
                        else onPageStartEvent();

//                        dispMeteringString(new String[]{"Stoping...", "Wait a second"});
                        ret = true;
                    } else {
                        LogWrapper.v(TAG, "Stop by Remote Signal : Card Not Match");
//                        dispTempMeteringString(new String[]{"Card Not Match"}, 4000);
                        ret = false;
                    }
                } else ret = false;
            }
        }
        return ret;
    }

    /**
     * 서버에서 충전시작(d1)에 대한 응답이 nack이면 시작된 충전을 종료시켜야 한다.
     * 종료후에는 f1을 전송해줘야 함.
     * @param rspCode 0x06 충전가능, 0x15 충전불가
     * @param rspReason 거절사유 : (0x11 : UID인증에러, 0x12 : 충전Credit단가 확인에러, 0x13:Credit부족에러, 0x14:충전시간 에러, 0x15:예약시간 에러, 0x16:충전소에러, 0x17:충전기에러)
     */
    @Override
    public void onRecvStartChargingResp(byte rspCode, byte rspReason, int ch) {
        if(rspCode == 0x06 || isLocalAuth)  //로컬인증일 경우 1d응답 상관없이 진행행
        {
            //1d 정상응답일 경우 아무것도 하지 않음
        }
        else if(rspCode == 0x15) {
            if (getUIFlowState() == UIFlowState.UI_CHARGING) {
                onChargingStop();
            }
        }
    }

    /**
     * 서버에서 충전 종료 메시지를 받음
     * @param cost 계산된 실제 금액
     */
    @Override
    public void onRecvFinishChargingResp(int cost, int ch) {
        //1f응답에 담긴 충전요금 저장
        volvo2chChargerInfo.curChargingCost = cost;
    }

    /**
     * add by ksi. 20200529
     * 1r로 응답된 버전이 충전기 버전과 다르면 서버에 update요청 진행
     * @param version
     */
    @Override
    public void onRecvVersionResp(String version, int ch) {
        String serverVersion = version.replaceAll("\\p{Z}", "");
        String chargerVersion = volvo2chChargerInfo.chg_sw_version.replaceAll("\\p{Z}", "");

        //서버에서 받은 버전과 충전기 버전이 다르다면 업데이트 요청(m1)
        if (!chargerVersion.equals(serverVersion)) volvoCommManager.sendFirmwareDownReq();
    }

    @Override
    public void onFirmwareDownCompleted(byte[] fwDownData, int ch) {
        String updatePath = Environment.getExternalStorageDirectory()+TypeDefine.UPDATE_PATH;

        //기존 업데이트 파일 삭제
        try {
            File file = new File(updatePath,"update.zip");
            file.delete();
        }catch (Exception e) {}

        try {
            File file = new File(updatePath,"update.apk");
            file.delete();
        }catch (Exception e) {}

        FileUtil.bufferToFile(updatePath, "update.zip", fwDownData, false);

        //unzip
        try {
            ZipUtils.unzip(updatePath+"/update.zip", updatePath, false);
            // 성공시 처리
            LogWrapper.v(TAG, "Firmware Unzip Successed");

            // Update 완료 Flag 시작(충전시가 아닐때 업데이트시작)
            isFWUpdateReady = true;
        } catch(Exception e) {
            LogWrapper.v(TAG, "Firmware Unzip Failed");
        }
    }

    /**
     * 업데이트 체크한다. isFWUpdateReady 가 true이고 UI가 초기화면인 경우에 업데이트 및 리셋 실시
     */
    void checkUpdateReady() {
        if ( isFWUpdateReady == true ) {
            if(meterConfig.lcdType.equals("None")) {
                if (getUIFlowState() == UIFlowState.UI_READY) {
                    doInstallFirmware();
                }
            }
            else{
                if (getUIFlowState() == UIFlowState.UI_CARD_TAG) {
                    doInstallFirmware();
                }
            }
        }
    }
    public void doInstallFirmware() {
        String updatePath = Environment.getExternalStorageDirectory()+TypeDefine.UPDATE_PATH;
        RemoteUpdater updater = new RemoteUpdater(mainActivity, updatePath, "update.apk");
        updater.doUpdateFromApk("com.joas.smartcharger");
    }


    @Override
    public boolean onReqInfoDownByHttp(byte dest1, byte dest2, String url, int ch) {
        if(!url.equals("")) {
            //http download start
            volvo2chChargerInfo.destFlag1 = dest1;
            volvo2chChargerInfo.destFlag2 = dest2;
            volvo2chChargerInfo.remoteURL = url;

            startHttpDownload();
            return true;
        }
        else return false;
    }
    //http download test method
    String fileName = "1.zip";
    String fileURL = "http://joascharger.co.kr:8888/data/1048576/1";            //웹서버 쪽 파일이 있는 경로

    HttpDownloadThread dThread;
    public void startHttpDownload() {
        String savePath = "";       //파일 저장경로
        if (volvo2chChargerInfo.destFlag2 == 0x01) {
            //fw download일 경우 경로 설정
            savePath = Environment.getExternalStorageDirectory() + TypeDefine.UPDATE_PATH;
        }
        else if(volvo2chChargerInfo.destFlag2 == 0x05 || volvo2chChargerInfo.destFlag2 == 0x06 || volvo2chChargerInfo.destFlag2 == 0x07){
            if(volvo2chChargerInfo.infoch == 0){
                savePath = Environment.getExternalStorageDirectory() + TypeDefine.INFO_DOWN_CH0_PATH;
            }
            else savePath = Environment.getExternalStorageDirectory() + TypeDefine.INFO_DOWN_CH1_PATH;

        }
        File dir = new File(savePath);
        //해당경로의 폴더가 존재하지 않을경우 생성
        if (!dir.exists()) {
            dir.mkdir();
        }

        dThread = new HttpDownloadThread(volvo2chChargerInfo.remoteURL, savePath, volvo2chChargerInfo.destFlag2, this);
        dThread.start();
    }
    public void completeHttpDownload() {
        LogWrapper.d("HTTP_FOTA", " HTTP download complete!!");
        dThread.interrupt();
        LogWrapper.d("HTTP_FOTA", " HTTP download thread stopped..");

        String filePath = "";
        String filename = "";
        //서버로 다운로드 완료 알람 전송
        volvoCommManager.SendAlarm(AlarmCode.DOWNLOAD_COMPLETE, AlarmCode.STATE_OCCUR);

        if (volvo2chChargerInfo.destFlag2 == 0x01) {
            filePath = Environment.getExternalStorageDirectory() + TypeDefine.UPDATE_PATH;
            onhttpFirmwarDownloadComplete(filePath);
        } else if (volvo2chChargerInfo.destFlag2 == 0x05 || volvo2chChargerInfo.destFlag2 == 0x06 || volvo2chChargerInfo.destFlag2 == 0x07) {
            //먼저 기존 멤버 정보 모두 지우기
            if(volvo2chChargerInfo.destFlag2 == 0x05) volvoCommManager.doRemoveAllMember();

            //추가할 멤버파일 읽기 및 db작업
            doMemberFileRead(volvo2chChargerInfo.destFlag2);
        }
    }
    public void doMemberFileRead(byte destflag) {
        int listcnt = 0;
        String fileContent = "";
        String filePath = "";
        if(this.channel == 0){
            filePath = Environment.getExternalStorageDirectory() + TypeDefine.INFO_DOWN_CH0_PATH;
        }
        else filePath = Environment.getExternalStorageDirectory() + TypeDefine.INFO_DOWN_CH1_PATH;

        try {
            StringBuffer sb = new StringBuffer();
            InputStream is = null;
            if (destflag == 0x05) is = new FileInputStream(filePath + "/psmember.txt");
            else if (destflag == 0x06) is = new FileInputStream(filePath + "/psmemadd.txt");
            else if (destflag == 0x07) is = new FileInputStream(filePath + "/psmemdel.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line = "";
            while ((line = br.readLine()) != null) {
                if (line.length() >= 32) line = line.substring(0, 32);
                sb.append(line + "\n");
            }
            br.close();
            is.close();

            fileContent = sb.toString();
            String[] cardList = fileContent.split("\n");
            listcnt = cardList.length;

            if (destflag == 0x05 || destflag == 0x06) memberAddThread(cardList, listcnt, destflag);
            else if (destflag == 0x07) volvoCommManager.doDelMember(cardList, listcnt);

        } catch (Exception e) {
            LogWrapper.e("doMemberFileRead", "MemberFileReadError : " + e.getMessage());
        }
    }

    AddMemThread addMemThread;
    public void memberAddThread(String[] cardlist, int cnt, byte destflag) {

        addMemThread = new AddMemThread(this, cnt, cardlist);
        addMemThread.start();
        if (destflag == 0x05) LogWrapper.d("memberAddThread", " TOTAL MEM ADD Thread Start..");
        else LogWrapper.d("memberAddThread", " ADD MEM Thread Start..");
    }
    public void completeMemAdd(){
        addMemThread.interrupt();
        LogWrapper.d("memberAddThread", " Add Member Complete!..Thread stopped.");
    }

    public void onhttpFirmwarDownloadComplete(String path) {
        //unzip
        try {
            ZipUtils.unzip(path + "/update.zip", path, false);
            // 성공시 처리
            LogWrapper.v(TAG, "Firmware Unzip Successed");

            // Update 완료 Flag 시작(충전시가 아닐때 업데이트시작)
            isFWUpdateReady = true;
        } catch (Exception e) {
            LogWrapper.v(TAG, "Firmware Unzip Failed");
        }
    }


    @Override
    public void onChangeChargerMode(int mode, int ch) {
        volvo2chChargerInfo.cpMode[ch] = mode;
        if(volvo2chChargerInfo.cpMode[ch]!=volvo2chChargerInfo.pre_cpMode[ch]){
            if(volvo2chChargerInfo.cpMode[ch] == 17 || volvo2chChargerInfo.cpMode[ch] == 2) {
                //점검중, 혹은 운영중지를 수신받은 시점이 충전중이라면 충전 종료후 모드전환
                if (getUIFlowState() == UIFlowState.UI_CHARGING) {
                    onChargingStop();
                }
                //운영중지, 점검중 창 띄우기
                pageManager.showUnavailableConView();

                setUIFlowState(UIFlowState.UI_SERVICE_STOP);
                LogWrapper.v(TAG, "ModeChanged : Service Stopped");
            }
            else if(volvo2chChargerInfo.cpMode[ch] == 5){
                pageManager.hideUnavailableConView();
                onPageStartEvent();
                LogWrapper.v(TAG, "ModeChanged : Normal");
            }
            volvo2chChargerInfo.pre_cpMode[ch] = volvo2chChargerInfo.cpMode[ch];
        }
    }

    /***
     * 서버로부터 수신받은 단가정보 저장(G1)
     * @param pinfo : 단가정보
     */
    @Override
    public void onRecvCostInfo(Volvo2chChargerInfo pinfo, int ch) {
        volvo2chChargerInfo.saveCostInfo(mainActivity.getBaseContext(), Environment.getExternalStorageDirectory()+TypeDefine.CP_CONFIG_PATH, TypeDefine.COST_INFO_FILENAME);
    }

    @Override
    public void onRecvCellAuthResp(String authNum, byte authResult, String prepayauthnum, String prepayDatetime, int prepayPrice, int usePayReal) {

    }

    @Override
    public void onRecvMissingPaymentCompleteResp(byte authResult, String prepayApprovalnum, String prepayDatetime, int prepayPrice, int usePayReal) {

    }

    @Override
    public void onRecvModemMDNInfo(String pnum) {
        String recv_data = pnum;

        try {
            //전화번호 부분 분리
            String[] parseList = recv_data.split("\\+");
            String mdn = "0" + parseList[2].substring(2, 12);
            volvo2chChargerInfo.mtoPhoneNumber = mdn;

            LogWrapper.v("MDN", volvo2chChargerInfo.mtoPhoneNumber);
        }catch (Exception e)
        {
            LogWrapper.v(TAG, e.toString());
        }

        if(volvo2chChargerInfo.h1_mdnNumIsSpace) {
            volvoCommManager.SendInstallationInfo();
        }
    }

    @Override
    public void onRecvModemRSSiInfo(String rssi) {
        try {
            String raw_data = rssi;
            String[] parseList = raw_data.split(",");
            String[] rssi_list = parseList[22].split(":");
            String tmp_modem_rssi = rssi_list[1].substring(1);
            String[] splitval = tmp_modem_rssi.split("\\.");
            String modem_rssi = splitval[0];
            volvo2chChargerInfo.mtomRssi = modem_rssi + "00";

            LogWrapper.v("RSSI", volvo2chChargerInfo.mtomRssi);

        }catch (Exception e)
        {
            LogWrapper.v(TAG, e.toString());
        }
    }


    public enum UIFlowState {
        UI_READY,
        UI_CARD_TAG,
        UI_AUTH_WAIT,
        UI_SELECT_TYPE_AND_TIME,        //차지비 충전커넥터 타입 및 시간 설정 상태
        UI_CONNECTOR_WAIT,
        UI_RUN_CHECK,
        UI_CHARGING,
        UI_FINISH_CHARGING,
        UI_UNPLUG,
        UI_SERVICE_STOP,     //add by si. 운영중지 상태
        UI_EIM_COMM_ERR     //add by si. 내부통신 장애 상태

    }

    MultiChannelUIManager multiChannelUIManager;
    PageManager pageManager;
    DSPControl2 dspControl;

    VolvoTouch2chUIActivity mainActivity;

    UIFlowState flowState = UIFlowState.UI_READY;
    ChargeData chargeData;
    CPConfig cpConfig;
    MeterConfig meterConfig;
    LocalMember localMem;

    Volvo2chChargerInfo volvo2chChargerInfo;
    VolvoCommManager volvoCommManager;
    PoscoModemCommManager poscoModemCommManager;


    boolean isDspTestMode = false;          //si.add 200729  - chargev testmode flag
    boolean backupDspTestModeFlag = false;  //si.add 200729  - chargev testmode backup flag

    boolean isDspReady = false;
    boolean isDspAvalCharge = false;
    boolean isDspDoor = false;
    boolean isDspPlug = false;
    boolean isDspChargeRun = false;
    boolean isDspChargeFinish = false;

    boolean isDspFault = false;
    boolean isPreDspFault = false;
    boolean isEmergencyPressed = false;

    long lastMeterValue = -1;

    String lastCardNum = "";
    String remote_lastCardnum = "";

    int meterTimerCnt = 0;
    long lastClockedMeterValue = -1;

    boolean isRemoteStarted = false;
    boolean isConnectorOperative = true;

    int unplugTimerCnt = 0;

    FaultManagerV2 faultManager;
    Vector<FaultInfo> faultList = new Vector<FaultInfo>();

    double powerLimit = -1;

    int finishChargingViewCnt = 0;

    int dspVersion = 0;
    boolean isFWUpdateReady = false;
    // ReserveNow
    ReserveInfo reserveInfo = new ReserveInfo();
    int channel;
    //si.add 201026 - 충전량 변화 종료관련 변수
    long initTime;
    long endTime;
    long distanceTime;
    int backupMeterval;
    boolean nonChangeMeterStopFlag = false;

    public UIFlowManager(int chan, VolvoTouch2chUIActivity activity, MultiChannelUIManager uiManager, ChargeData data, CPConfig config, MeterConfig mconfig, DSPControl2 control, PageManager page) {
        channel = chan;
        mainActivity = activity;
        multiChannelUIManager = uiManager;
        chargeData = data;
        cpConfig = config;
        meterConfig = mconfig;
        dspControl = control;
        pageManager = page;
//        localMem = new LocalMember();

        //단가정보 로드
        volvo2chChargerInfo = new Volvo2chChargerInfo(channel);
        volvo2chChargerInfo.loadCostInfo(activity.getBaseContext(), Environment.getExternalStorageDirectory() + TypeDefine.CP_CONFIG_PATH + "/" + TypeDefine.COST_INFO_FILENAME);
        setMemberCostUnit();

        //si. 로드된 충전기 설치정보를 volvo2chChargerInfo객체로 전달
        volvo2chChargerInfo.loadCpConfigInfo(activity.getBaseContext(), Environment.getExternalStorageDirectory() + TypeDefine.CP_CONFIG_PATH + "/" + CPConfig.CP_CONFIG_FILE_NAME);

        //모델명 및 UI 버전설정
        setUIversionAndModelName();

        //UI sequnece 초기화
        flowState = UIFlowState.UI_READY;

        //모뎀통신 생성 TOdo
        poscoModemCommManager = new PoscoModemCommManager(activity.getBaseContext(), "192.168.1.1", 7788, this);
        poscoModemCommManager.start();

        //통신프로그램 시작
        String basePath = Environment.getExternalStorageDirectory()+TypeDefine.REPOSITORY_BASE_PATH;
        switch (channel) {
            case 0:
                volvoCommManager = new VolvoCommManager(activity.getBaseContext(), cpConfig.serverIP, cpConfig.serverPort, cpConfig.stationID, cpConfig.chargerID,
                        chargeData.chargerType, volvo2chChargerInfo, basePath, 30 * 1000, channel);
                break;
            case 1:
                volvoCommManager = new VolvoCommManager(activity.getBaseContext(), cpConfig.serverIP, cpConfig.serverPort, cpConfig.stationID2, cpConfig.chargerID2,
                        chargeData.chargerType, volvo2chChargerInfo, basePath, 30 * 1000, channel);
                break;
        }
        volvoCommManager.setVolvoCommManagerListener(this);

        // FaultManager를 생성한다.
        faultManager = new FaultManagerV2(dspControl, mainActivity, chargeData.dspChannel);

        //사용자 구분
        chargeData.authType = TypeDefine.AuthType.MEMBER;

//        initDspState();
        initStartState();
    }

    void setUIversionAndModelName(){
        TypeDefine.SW_VER = "220118("+cpConfig.chargerKind+")";
        volvo2chChargerInfo.chg_sw_version = "UI:1.3 220118(" + cpConfig.chargerKind + ")";
        TypeDefine.SW_RELEASE_DATE = "2022-01-18";
//        if(cpConfig.chargerKind.equals("OP") || cpConfig.chargerKind.equals("CV")) {
//            TypeDefine.MODEL_NAME = "JC-9111KE-TP-BC";
//        }
//        else if(cpConfig.chargerKind.equals("CL")){
//            TypeDefine.MODEL_NAME = "JC-92C1-7-0P";
//        }
    }

    public void setMemberCostUnit() {
        if (chargeData.authType == TypeDefine.AuthType.MEMBER)
            chargeData.chargingUnitCost = ((double) volvo2chChargerInfo.memberCostUnit) / 100.0;
        else chargeData.chargingUnitCost = ((double) volvo2chChargerInfo.nonMemberCostUnit) / 100.0;
        LogWrapper.d(TAG, "setMemberCostUnit:" + volvo2chChargerInfo.curChargingCostUnit);
    }

    public void setPageManager(PageManager manager) {
        pageManager = manager;
    }
    public DSPControl2 getDspControl() { return dspControl; }
    public LocalMember getLocalmember(){return localMem;}
    public CPConfig getCpConfig() { return cpConfig; }
    public ChargeData getChargeData() { return chargeData; }
    public MultiChannelUIManager getMultiChannelUIManager() { return multiChannelUIManager; }
    public Volvo2chChargerInfo getVolvoChargerInfo() { return volvo2chChargerInfo; }

    public UIFlowState getUIFlowState() { return flowState; }

    public int getUIFlowChannel(){return this.channel;}
    public Volvo2chChargerInfo getChargevReservInfo(){return volvo2chChargerInfo;}

    void setUIFlowState(UIFlowState state) {
        flowState = state;

        processChangeState(flowState );
    }

    public int getDspVersion() { return dspVersion; }
    public EvCommManager getCommManager() { return volvoCommManager; }

    /**
     *  UI 상태값이 바뀔 때 수행되어야 할 부분을 구현
     *  DSP의 UI 상태값 변경, 도어, 변경 충전시작 관련
     *  DSP 이외에 다른 동작은 되도록 추가하지 말것(타 이벤트에서 처리. 이 함수는 DSP에서 처리하는 것을 모아서 하나로 보려고함)
     * @param state 바뀐 UI 상태 값
     */
    void processChangeState(UIFlowState state) {
        switch ( state ) {
            case UI_READY:
                initStartState();
                break;

            case UI_CONNECTOR_WAIT:
                dspControl.setUIState(chargeData.dspChannel, DSPTxData2.DSP_UI_STATE.UI_CONNECT);
                // Ctype인 경우에는 도어를 오픈할 필요가 없음
                if ( chargeData.connectorType != TypeDefine.ConnectorType.CTYPE ) {
                    dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, true);
                }
                break;

            case UI_RUN_CHECK:
                dspControl.setUIState(chargeData.dspChannel, DSPTxData2.DSP_UI_STATE.UI_START_CHARGE);
                dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.START_CHARGE, true);
                break;

            case UI_FINISH_CHARGING:
                dspControl.setUIState(chargeData.dspChannel, DSPTxData2.DSP_UI_STATE.UI_FINISH_CHARGE);
                unplugTimerCnt = 0;
                break;
        }
    }

    void initDspState()
    {
        dspControl.setUIState(chargeData.dspChannel, DSPTxData2.DSP_UI_STATE.UI_READY);
        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.READY, true);
        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.START_CHARGE, false);
        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.FINISH_CHARGE, false);
        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, false);

        chargeData.connectorType = TypeDefine.ConnectorType.CTYPE;
        dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData2.CHARGER_SELECT_SLOW_CTYPE); // CC타입으로 처음부터 계속 주는것으로 하드웨어팀과 협의 20201217
    }
    void initStartState() {
        initDspState();

        if ( isConnectorOperative == false ) pageManager.showUnavailableConView();

        // 변수 초기화
        chargeData.measureWh = 0;
        chargeData.chargingTime = 0;
        chargeData.chargingCost = 0;
        powerLimit = -1.0d;
        unplugTimerCnt = 0;

//        multiChannelUIManager.rfidReaderRelease(channel);

//        mainActivity.setRemoteStartedVisible(View.INVISIBLE);
        isRemoteStarted = false;

        volvo2chChargerInfo.isRemoteCharge = false;
    }

    public void onPageStartEvent() {
        setUIFlowState(UIFlowState.UI_READY);
        pageManager.changePage(PageID.PAGE_READY);
//        multiChannelUIManager.rfidReaderRelease(channel);
    }

    /**
     * pageaskstopcharging 화면에서 예,아니오 버튼을 눌렀을 때 이벤트 처리
     * @param event
     */
    public void onChargingStopAskNoticePageEvent(PageEvent event){
        // Fault인경우에 초기화면으로 돌아감
        if ( isDspFault ) {
            fillFaultMessage();
            pageManager.showFaultBox();
            return;
        }

        switch (event){
            case SELECT_KEEP_CHARGING:
                //충전중 화면으로 다시 돌아감
                pageManager.changePage(PageID.CHARGING);
                break;
            case SELECT_STOP_CHARGING:
                volvo2chChargerInfo.isChgStopAsk = true;
                if(cpConfig.isAuthSkip){
                    //자동인증일 경우 바로 종료
                    onChargingStop();
                }
                else{
                    //카드태깅 안내 화면 표시
                    multiChannelUIManager.rfidReaderRequest(channel);
                    pageManager.changePage(PageID.CARD_TAG);
                }
                break;
        }
    }

    /**
     * Select화면에서 선택되었을때 이벤트 발생
     * @param event
     */

    public void onPageSelectEvent(PageEvent event) {
        // Fault인경우에 초기화면으로 돌아감
        if ( isDspFault ) {
            fillFaultMessage();
            pageManager.showFaultBox();
            return;
        }

        switch ( event ) {
            case SELECT_BTYPE_CLICK:
                chargeData.connectorType = TypeDefine.ConnectorType.BTYPE;
                dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData2.CHARGER_SELECT_SLOW_BTYPE);
                break;
            case SELECT_CTYPE_CLICK:
                chargeData.connectorType = TypeDefine.ConnectorType.CTYPE;
                dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData2.CHARGER_SELECT_SLOW_CTYPE);
                break;
            case SELECT_AC3_CLICK:
                chargeData.connectorType = TypeDefine.ConnectorType.AC3;
                dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData2.CHARGER_SELECT_FAST_AC3);
                break;
            case SELECT_CHADEMO_CLICK:
                chargeData.connectorType = TypeDefine.ConnectorType.CHADEMO;
                dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData2.CHARGER_SELECT_FAST_DCCHADEMO);
                break;
            case SELECT_DCCOMBO_CLICK:
                chargeData.connectorType = TypeDefine.ConnectorType.DCCOMBO;
                dspControl.setConnectorSelect(chargeData.dspChannel, DSPTxData2.CHARGER_SELECT_FAST_DCCOMBO);
                break;

        }

        dspControl.setState216(chargeData.dspChannel, DSPTxData2.STATUS216.MANAGE_STOP, false);     //무료모드

        // Session Start

        // Next Flow. Card Tag
        if(cpConfig.isAuthSkip)
        {
            dspControl.setState216(chargeData.dspChannel, DSPTxData2.STATUS216.FREE_MODE, true);     //무료모드

            setUIFlowState(UIFlowState.UI_CONNECTOR_WAIT);
            pageManager.changePage(PageID.CONNECTOR_WAIT);
//            doAuthComplete();
        }else {
            dspControl.setState216(chargeData.dspChannel, DSPTxData2.STATUS216.FREE_MODE, false);     //무료모드

            //회원 충전일 경우 커넥터 연결 대기 화면으로 이동 - edit by si.210413
            setUIFlowState(UIFlowState.UI_CONNECTOR_WAIT);
            pageManager.changePage(PageID.CONNECTOR_WAIT);
        }
    }


    public void onPageCommonEvent(PageEvent event) {
        switch ( event ) {
            case GO_HOME:
                onPageStartEvent();
                break;
            case SELECT_KEEP_CHARGING:                      //충전중 충전종료위해 나타난 태깅화면에서 처음으로 버튼 눌렀을 경우 충전중으로 이동
                pageManager.changePage(PageID.CHARGING);
                break;
            case SELECT_MEMBER_CLICK:
                //rfid read request
                multiChannelUIManager.rfidReaderRequest(channel);

                //멤버 관련 정보 초기화
                chargeData.authType = TypeDefine.AuthType.MEMBER;
                volvo2chChargerInfo.authType = Volvo2chChargerInfo.CommAuthType.MEMBER;
                volvo2chChargerInfo.paymentCompany = 0x00;
                volvo2chChargerInfo.payMethod = 0x00;
                onSelectChargevMemberEvent();
                break;
        }
    }

    public void onSelectChargevMemberEvent(){
        if(cpConfig.isAuthSkip){
//            dspControl.setState216(chargeData.dspChannel, DSPTxData2.STATUS216.FREE_MODE, true);     //무료모드
            doAuthComplete();
        }
        else{
            setUIFlowState(UIFlowState.UI_CARD_TAG);
            pageManager.changePage(PageID.CARD_TAG);
        }

    }

    public boolean isLocalAuth = false;

    public void onCardTagEvent(String tagNum, boolean isSuccess ) {
        if ( isSuccess ) {
            if ( flowState == UIFlowState.UI_CARD_TAG ) {
                volvo2chChargerInfo.cardNum = tagNum;
                volvo2chChargerInfo.payMethod = Volvo2chChargerInfo.PAY_METHOD_BY_SERVER;
                lastCardNum = tagNum;

                setUIFlowState(UIFlowState.UI_AUTH_WAIT);
                pageManager.changePage(PageID.AUTH_WAIT);

                //로컬인증 진행
                if(volvoCommManager.isConneted() == false){
                    isLocalAuth = true;
                    if(volvoCommManager.localAuthentication(tagNum)){
                        onAuthResultEvent(true,0,0,this.channel);
                    }
                    else{
                        onAuthResultEvent(false,11,0,this.channel);
                    }
                }
                else{
                    isLocalAuth = false;
                    volvoCommManager.sendAuthReq();
                }
            }
            else if ( flowState == UIFlowState.UI_CHARGING ) {
                if(volvo2chChargerInfo.isRemoteCharge)
                {
                    //원격 충전으로 시작해도 현장 카드태깅 종료 가능하도록 모니터링
                    if(tagNum.equals(remote_lastCardnum))
                    {
                        LogWrapper.v(TAG, "Stop by User Card Tag");
                        onChargingStop();
//                        dispMeteringString(new String[] {"Stoping...", "Wait a second"});
                    }
//                    else dispTempMeteringString(new String[] {"Card Not Match"}, 4000);
                    else{
                        chargeData.messageBoxTitle = "[인증 실패]";
                        chargeData.messageBoxContent = "최초 사용한 카드번호와 맞지 않습니다.";
                        pageManager.showMessageBox();
                    }
                }
                else {
                    if ( tagNum.equals(lastCardNum) == true ) {
                        LogWrapper.v(TAG, "Stop by User Card Tag");
                        onChargingStop();
//                        dispMeteringString(new String[] {"Stoping...", "Wait a second"});
                    }
                    else {
                        chargeData.messageBoxTitle = "[인증 실패]";
                        chargeData.messageBoxContent = "최초 사용한 카드번호와 맞지 않습니다.";
                        pageManager.showMessageBox();
//                        dispTempMeteringString(new String[] {"Card Not Match"}, 4000);
                    }

                    //2채널 충전중 종료위해 카드 인증 완료 후 다시 충전중 화면으로 돌아가기 위해 처리
                    if(volvo2chChargerInfo.isChgStopAsk){
                        pageManager.changePage(PageID.CHARGING);
                    }
                }
            }
        }
    }

    public void goHomeProcessDelayed(int delayMs) {
        final int timeout = delayMs;
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        onPageStartEvent();
                    }
                }, timeout);
            }
        });
    }
    /*
     * Check reservation 함수
     * add by si. 210202
     */
    public boolean checkReservation()
    {
        boolean ret = false;

        if(volvo2chChargerInfo.rsv_flag == 0) ret = true;      //예약이 없을경우 바로 true 리턴
        else if(volvo2chChargerInfo.rsv_flag == 1)
        {
            //비예약자만 사용가능(예약시작시간 30분 전까지)
            if(chargeData.authType == TypeDefine.AuthType.NOMEMBER) ret = true;
            else if(!volvo2chChargerInfo.rsv_uid.equals(volvo2chChargerInfo.cardNum)) ret = true;
            else ret = false;
        }
        else if(volvo2chChargerInfo.rsv_flag == 2) ret = false;        //예약자,비예약자 둘다 불가
        else if(volvo2chChargerInfo.rsv_flag == 3)
        {
            //예약자만 사용가능(예약시작시간 10분 전후)
            if(volvo2chChargerInfo.rsv_uid.equals(volvo2chChargerInfo.cardNum)) ret = true;
            else ret = false;
        }

        return ret;
    }

    public void onAuthResultEvent(boolean isSuccess, int reason) {
        if ( flowState == UIFlowState.UI_AUTH_WAIT ) {
            if (isSuccess) {
                //add by si.210202 - 차지비 예약상태 체크
                if(checkReservation())
                {
                    doAuthComplete();
                }
                else{
                    //예약건으로 인한 인증실패 문구 띄워주기
                    chargeData.messageBoxTitle = "인증 실패";
                    chargeData.messageBoxContent = "예약이 존재합니다.";
                    pageManager.getAuthWaitView().stopTimer();
                    pageManager.showMessageBox();
                    goHomeProcessDelayed(chargeData.messageBoxTimeout * 1000);
                }
            } else {
                // 메시지 박스 내용 채움
                chargeData.messageBoxTitle = mainActivity.getResources().getString(R.string.str_auth_fail_title);
                chargeData.messageBoxContent = mainActivity.getResources().getString(R.string.str_auth_fail_content);
                pageManager.getAuthWaitView().stopTimer();
                pageManager.showMessageBox();
                goHomeProcessDelayed(chargeData.messageBoxTimeout * 1000);
                //dispTempMeteringString(new String[]{"Card Failed", "Check Card Info"}, 6000);
            }
        }
    }

    void doAuthComplete() {
        if(volvo2chChargerInfo.isRemoteCharge) {
            //원격 충전시도일 경우
            LogWrapper.v(TAG, "Remote Auth Complete");

//            dispMeteringString(new String[] {"Remote Check OK.", "Connect Cable"});
            setUIFlowState(UIFlowState.UI_CONNECTOR_WAIT);
            pageManager.changePage(PageID.CONNECTOR_WAIT);
        }
        else {
            if(cpConfig.isAuthSkip) LogWrapper.v(TAG, "Auth skip mode.");
            else LogWrapper.v(TAG, "Auth Complete");
//            dispMeteringString(new String[]{"Card Check Done.", "Connect Cable"});
            //캐릭터 LCD일경우 CONNECTOR_WAIT 상태로 전환, 아닐경우 충전방식 및 시간설정 화면으로 이동
            if(meterConfig.lcdType.equals("None")){
                //시간설정 및 타입 선택 화면으로 이동
                setUIFlowState(UIFlowState.UI_SELECT_TYPE_AND_TIME);
                pageManager.changePage(PageID.SELECT_SLOW);
            }
            else {
                setUIFlowState(UIFlowState.UI_CONNECTOR_WAIT);
                pageManager.changePage(PageID.CONNECTOR_WAIT);
            }
        }

//        DSPRxData2 rxData = dspControl.getDspRxData2(chargeData.dspChannel);
//        if (rxData.get400Reg(DSPRxData2.STATUS400.STATE_PLUG) == true) {
//            // 이미 Connect 된 상태이라면
//            onConnectedCableEvent(true);
//        } else {
//            // changePage가 하나의 함수에서 2번이상 불려지면. UI Thread로 인한 문제가 발생할 소지가 있음
//            pageManger.changePage(PageID.CONNECTOR_WAIT);
//            // Ctype인 경우에는 도어를 오픈할 필요가 없음
//            dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, true);
//
//        }
    }

    /**
     * 충전 시작시에 초기화가 필요한 변수를 세팅한다.
     */
    public void initChargingStartValue() {
        volvo2chChargerInfo.isChgStopAsk = false;
        chargeData.measureWh = 0;
        chargeData.chargeStartTime = new Date();        //충전 시작시간 설정
        volvo2chChargerInfo.chargingStartTime = chargeData.chargeStartTime;

        //add by si. 20200528 - 차지비 충전 종료시간 설정
        int usersettingTime = 0;

        //원격충전일 경우 수신된 충전시간(분) 만큼 설정하고, 원격이아닐 경우 max 시간만큼 충전수행
        if (volvo2chChargerInfo.isRemoteCharge) {
            usersettingTime = volvo2chChargerInfo.remoteStartChargingTimeLimit;
        } else if (volvo2chChargerInfo.rsv_flag == 0) {
            if (meterConfig.lcdType.equals("None")) {
                //사용자가 설정한 시간으로 설정
                usersettingTime = volvo2chChargerInfo.userSetTime;
            } else {
                //캐릭터 LCD일경우 최대시간으로 설정
//                usersettingTime = Integer.parseInt(poscoChargerInfo.maxsetTime);
            }
            usersettingTime = usersettingTime * 60;       //초단위로 변경
        } else if (volvo2chChargerInfo.rsv_flag == 1) {
            //비예약자만이 충전가능, 예약시작시간 30분전까지 충전가능
            if (!volvo2chChargerInfo.rsv_uid.equals(volvo2chChargerInfo.cardNum)) {
                if(meterConfig.lcdType.equals("None")){
                    usersettingTime = volvo2chChargerInfo.userSetTime;
                }
                else{
//                    usersettingTime = (int) poscoChargerInfo.rsv_leftMin - 30;
                }
                usersettingTime = usersettingTime * 60;
            }
        }
        else if (volvo2chChargerInfo.rsv_flag == 3) {
            //예약자만 충전가능
            if (volvo2chChargerInfo.rsv_uid.equals(volvo2chChargerInfo.cardNum)) {
                usersettingTime = volvo2chChargerInfo.rsv_chargingTimeMin;
                usersettingTime = usersettingTime * 60;
            }
        }
        //종료시간 설정
        chargeData.chargeEndTime = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(chargeData.chargeEndTime);
        cal.add(Calendar.SECOND, usersettingTime);
        chargeData.chargeEndTime = cal.getTime();

        chargeData.chargingTime = 0;
        chargeData.chargingCost = 0;
        meterTimerCnt = 0;

        // 통신 충전 메시지 초기화
        volvo2chChargerInfo.reqAmoundSel = 0x01; // Full
        volvo2chChargerInfo.remainTime = usersettingTime;        //d1 - 충전 남은 시간(시작시이므로 설정된 시간값을 넘김.

        //poscoChargerInfo.payMethod = 0x02; // 전기요금 합산

        // 단가 설정
        setMemberCostUnit();

//        if(!meterConfig.lcdType.equals("None")) {
//            // 바로 Char 디스플레이 카운트 초기화
//            dispChargingMeterStrCnt = TypeDefine.DISP_CHARGING_CHARLCD_PERIOD;
//            dispChargingMeterBacklightCnt = 0; // 백라이트 카운터 초기화
//        }

        //미터량 변화에 따른 충전종료 관련 변수 초기화 - add by si. 201026
        initTime = System.currentTimeMillis();
        endTime = System.currentTimeMillis();
        backupMeterval = 0;
        nonChangeMeterStopFlag = false;
    }

    /**
     * 실제 충전 시작일때 이벤트(DSP에서 받은)
     * 충전 시작시 필요한 사항을 기술한다.
     */
    public void onDspChargingStartEvent() {
        if ( getUIFlowState() == UIFlowState.UI_RUN_CHECK ) {
            //lastMeterValue = dspControl.getLastMeterValue(chargeData.dspChannel);

            LogWrapper.v(TAG, "Start Charging");

            // 충전 관련 변수를 초기화 한다.
            initChargingStartValue();

            setUIFlowState(UIFlowState.UI_CHARGING);
            pageManager.changePage(PageID.CHARGING);

            if(isDspTestMode || cpConfig.isAuthSkip) {}
            else {
                if (volvo2chChargerInfo.cpMode[this.channel] == 5) {
                    volvo2chChargerInfo.cpMode[this.channel] = 9;             //충전중 모드 변경 - add by si.200901
                    volvo2chChargerInfo.pre_cpMode[this.channel] = volvo2chChargerInfo.cpMode[this.channel];
                    volvoCommManager.sendStartCharging();       //d1 전문 전송(자동인증이나 테스트모드가 아닐 경우에만 충전시작전문 전송)
                }
            }

            DSPRxData2 rxData = dspControl.getDspRxData2(chargeData.dspChannel);
        }
    }

    public void onConnectedCableEvent(boolean isConnected) {
        if ( isConnected ) {
            // 급속에서 사용자가 충전시작을 하게끔 한다. 수정.. 커넥터 체크 자동으로 할 때는 아래코드를 이용함
            if ( getUIFlowState() == UIFlowState.UI_CONNECTOR_WAIT ) {
                setUIFlowState(UIFlowState.UI_RUN_CHECK);

                // 이미 Run이 된 상태이라면
                if ( isDspChargeRun ) {
                    onDspChargingStartEvent();
                }
                else {
                    // changePage가 하나의 함수에서 2번이상 불려지면. UI Thread로 인한 문제가 발생할 소지가 있음
                    pageManager.changePage(PageID.CONNECT_CAR_WAIT);
                }
            }
            else if (  getUIFlowState() == UIFlowState.UI_CARD_TAG  ) {
                // add by scchoi 자동인증 추가, 20200610
                if ( cpConfig.isAuthSkip == true || isDspTestMode) {
                    setUIFlowState(UIFlowState.UI_RUN_CHECK);

                    // 이미 Run이 된 상태이라면
                    if ( isDspChargeRun ) {
                        onDspChargingStartEvent();
                    }
                    else {
                        // changePage가 하나의 함수에서 2번이상 불려지면. UI Thread로 인한 문제가 발생할 소지가 있음
                        pageManager.changePage(PageID.CONNECT_CAR_WAIT);
                    }
                }
            }
        }
        else {

        }
    }

    public void onFinishChargingEvent() {
        if ( getUIFlowState() == UIFlowState.UI_CHARGING ) {
            if(isDspTestMode || cpConfig.isAuthSkip){}
            else {
                if (volvo2chChargerInfo.cpMode[this.channel] == 9) {
                    volvo2chChargerInfo.cpMode[this.channel] = 5;
                    volvo2chChargerInfo.pre_cpMode[this.channel] = volvo2chChargerInfo.cpMode[this.channel];
//                    poscoCommManager.sendFinishCharging();
                }
            }

            //회원일 경우 언플러그 화면으로 이동 - edit by si. 210421
            if(chargeData.authType == TypeDefine.AuthType.MEMBER){
                volvoCommManager.sendFinishCharging();      //충전완료 f1 전송
                setUIFlowState(UIFlowState.UI_UNPLUG);
                pageManager.changePage(PageID.UNPLUG);
            }


            //DSP 종료신호 해제
            dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.FINISH_CHARGE, false);

            //Fault가 아닐 시, 정상 충전 완료
            if (!isDspFault) {
//                dispMeteringString(new String[]{"Finished.",
//                        "Unplug Cable",
//                        String.format("Usage: %.2fkWh", ((double) poscoChargerInfo.curChargingKwh) / 100.0),
//                        String.format("Cost: %dWon", poscoChargerInfo.curChargingCost)});

                LogWrapper.v(TAG, "Finish Charging:" + String.format("Usage: %.2fkWh", ((double) volvo2chChargerInfo.curChargingKwh) / 100.0) +
                        ", " + String.format("Cost: %dWon", volvo2chChargerInfo.curChargingCost));
            }
        }
        mainActivity.setRemoteStartedVisible(View.INVISIBLE);
    }

    public void onChargingStop() {
        //DSP에 STOP 신호를 보낸다.
        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.FINISH_CHARGE, true);
    }

    /**
     * add by si. 2채널 충전 중 충전종료 안내 상태 페이지 설정
     */
    public void setStateChargingStopOrNot(){
        pageManager.changePage(PageID.PAGE_ASK_STOP_CHG);
    }

    /**
     * add by si. 20200604 차지비 예약정보 체크(타이머)
     */
    public void reserveUpdateInfo()
    {
        volvoCommManager.getReservInfo();

        //String val1 = poscoChargerInfo.rsv_orderNum;

    }

    /**
     * 1초에 한번씩 실행되면서 필요한 일을 수행한다.
     */
    public void timerProcessSec() {
        //Fault 함수 수행
        onFaultEventProcess();
        //Reserve Check
        if ( reserveInfo.expiryCheck() == true ) mainActivity.setReservedVisible(View.INVISIBLE);

        //Update 검사
        checkUpdateReady();

        //add by ksi. 200604 예약정보 모니터링
        reserveUpdateInfo();

        //전력량계 값 가져오기
        // meterVal = 전력량계
        if ( mainActivity.getMeterService() != null ) {
            try {
                long meterVal = mainActivity.getMeterService().readMeterCh(channel);
                double meterVolt = mainActivity.getMeterService().readMeterVoltageCh(channel);
                double meterCurrent = mainActivity.getMeterService().readMeterCurrentCh(channel);
                int meterseqnum = mainActivity.getMeterService().readSeqNumber();
                String meter_version = mainActivity.getMeterService().readMeterVersion();

                if(chargeData.meter_version.equals("")) {
                    chargeData.meter_version = meter_version;
                    volvo2chChargerInfo.chg_sw_version += " M:"+chargeData.meter_version;
                    volvoCommManager.SendInstallationInfo();
                }

                try {
                    volvo2chChargerInfo.meterVal[channel] = (int)meterVal;
                }
                catch (Exception e1)
                {}

                Log.d(TAG, "Meter:" + meterVal + ", Volt:" + meterVolt + ", current:" + meterCurrent + ", m_seqnum:" + meterseqnum+", version:"+chargeData.meter_version);

                //add by si - 20.12.09 - MeterReadError상태 감지추가
                MeterStatusMonitoring(meterVal);

                //add by si - 211130 - meter view program seqnum 증가상태 감지(없을시 UI재부팅)
                MeterviewSeqnumMonitor(meterseqnum);

                float fMeterVolt = (float) meterVolt;
                float fMeterCurrent = (float) meterCurrent;
                float fMeterVal = (float) meterVal;

                //DSP Write (전력량 관련)
                dspControl.setOutputVoltageAC(chargeData.dspChannel, fMeterVolt);       //AC 전압
                dspControl.setOutputAmpareAC(chargeData.dspChannel, fMeterCurrent);     //AC 전류

//                LogWrapper.d(TAG, "CH:"+ chargeData.dspChannel +", Meter:"+meterVal+", Volt:"+meterVolt+", current:"+meterCurrent);
                if (meterVal >= 0) {
                    dspControl.setMeterAC(chargeData.dspChannel, fMeterVal);
                    if (getUIFlowState() == UIFlowState.UI_CHARGING) {
                        if (lastMeterValue > 0) {
                            int gapMeter = (int) (meterVal - lastMeterValue);

                            if (gapMeter > 0) {
                                //gapMeter 최대 증가폭 1초당 0.5kw를 넘지못함. - add by si. 200831
                                if (gapMeter > 500) {
                                    gapMeter = 500;
                                }
                                chargeData.measureWh += gapMeter;
                            }
                        }
                    }
                    lastMeterValue = meterVal;
                } else {
                    // Meter Error !!!
                    if (lastMeterValue < 0) lastMeterValue = 0;
                    dspControl.setMeterAC(chargeData.dspChannel, lastMeterValue);
                    volvo2chChargerInfo.meterVal[channel] = (int) lastMeterValue;
                }
            }catch(Exception e){
                LogWrapper.d(TAG, "Meter Err:"+e.toString());
            }
        }



        DSPRxData2 rxData = dspControl.getDspRxData2(chargeData.dspChannel);
        dspVersion = rxData.version; // DSP 버전 정보 저장

        //test mode(200/5bit) 상태 체크
        if(cpConfig.useTestMode) dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.TEST_FLAG, true);
        else dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.TEST_FLAG, false);

        // 충전중일때 충전 시간을 계산한다.
        if ( getUIFlowState() == UIFlowState.UI_CHARGING ) {
            //충전 시간
            chargeData.chargingTime = (new Date()).getTime() - chargeData.chargeStartTime.getTime();
            int chargingTimeSec = (int)(chargeData.chargingTime/1000);

            //충전 남은 시간
            chargeData.chargingRemainTime = chargeData.chargeEndTime.getTime() - (new Date()).getTime();
            int remianTimeSec = (int)(chargeData.chargingRemainTime/1000);

            //시간 및 충전관련 전달정보 저장
            volvo2chChargerInfo.remainTime = remianTimeSec;
            volvo2chChargerInfo.chargingTime = chargingTimeSec;
            volvo2chChargerInfo.curChargingKwh = (int)(chargeData.measureWh/10);
            //poscoChargerInfo.curChargingCost = (int)chargeData.chargingCost;      //delete by si. 20200528, 차지비는 충전종료 후 1f의 요금정보로 표시, 상시체크 X



            //차지비 충전종료 모니터링
            if(remianTimeSec < 1) onChargingStop();     //add by si. 20200529 : cv, 충전설정 잔여시간이 1초 미만일 경우 충전 종료 요청
            //충전량 변화 모니터링 종료시퀀스 - add by si. 20201026
            nonChangeMeterStopFlag = getMeterChargingStopFlag();
            if(nonChangeMeterStopFlag) onChargingStop();     //add by si. 201026 - 충전량 변화가 3분동안 없을경우 충전종료 요청

            // 충전중 상태 정보 주기에 따라 값을 보낸다.
            if (chargingTimeSec != 0 && (chargingTimeSec % TypeDefine.COMM_CHARGE_STATUS_PERIOD) == 0) {
                if(isDspTestMode || cpConfig.isAuthSkip){}
                else volvoCommManager.sendChargingStatus();
            }

            if ( rxData.get400Reg(DSPRxData2.STATUS400.FINISH_CHARGE) == true ) {
                onFinishChargingEvent();
            }
        }
        // Event에서 poll로 바꿈.
        if ( rxData.get400Reg(DSPRxData2.STATUS400.STATE_DOOR) == false ) {
            //도어 명령이 성공적으로 수행이 되면 Door Open을 더 이상 수행하지 않는다.
            dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, false);
        }

        // connect 체크 polling
        // Event에서 poll로 바꿈.
        if ( getUIFlowState() == UIFlowState.UI_CONNECTOR_WAIT && rxData.get400Reg(DSPRxData2.STATUS400.STATE_PLUG) == true ) {
            onConnectedCableEvent(true);
        }

        // Finish화면에서 일정시간 이상 지났을때 Unplug가 되면 초기화면
//        if ( getUIFlowState() == UIFlowState.UI_FINISH_CHARGING ) {
        if ( getUIFlowState() == UIFlowState.UI_UNPLUG ) {
            // 5초이상 Gap을 준다.(MC 융착을 피하기 위함)
            if (unplugTimerCnt++ > 5 && rxData.get400Reg(DSPRxData2.STATUS400.STATE_PLUG) == false) {
                if(isDspTestMode || cpConfig.isAuthSkip){}
                else volvoCommManager.sendUnplug();

                //충전 완료로 이동
                setUIFlowState(UIFlowState.UI_FINISH_CHARGING);
                pageManager.changePage(PageID.FINISH_CHARGING);

            } else if(finishChargingViewCnt++ == 5) {
//                //Fault가 아닐 시, 정상 충전 완료
//                dispMeteringString(new String[]{"Finished.", "Unplug Cable",
//                        String.format("Usage: %.2fkWh", ((double) poscoChargerInfo.curChargingKwh) / 100.0),
//                        String.format("Cost: %dWon", poscoChargerInfo.curChargingCost)});
            }
        }

        //add by si.200729 - 407번지 1bit Testmode 모니터링
        isDspTestMode = rxData.get407Reg(DSPRxData2.STATUS407.TEST_MODE);

        if(isDspTestMode!=backupDspTestModeFlag) {

            if (isDspTestMode)
                LogWrapper.d(TAG, "ChargerMODE : DSP Test Mode!");
            else
                LogWrapper.d(TAG, "ChargerMode : Normal Mode!");

            backupDspTestModeFlag = isDspTestMode;
        }
    }

    //add by si.201026 - 충전중 충전량 변화에 따른 충전종료 플래그 리턴 함수
    public boolean getMeterChargingStopFlag(){
        boolean retval = false;
        try {
            if (volvo2chChargerInfo.curChargingKwh != backupMeterval) {
                backupMeterval = volvo2chChargerInfo.curChargingKwh;
                initTime = System.currentTimeMillis();
                endTime = System.currentTimeMillis();
                distanceTime = (long) ((endTime - initTime) / 1000.0);       //초
            }
            else if(volvo2chChargerInfo.curChargingKwh == backupMeterval){
                endTime = System.currentTimeMillis();
                distanceTime = (long) ((endTime - initTime) / 1000.0);       //초

                if(distanceTime == TypeDefine.METERING_CHANGE_TIMEOUT){
                    retval = true;
                }
                else retval = false;
            }
        }catch (Exception e ) {

        }

        return retval;
    }
    /**
     * 계량기 프로그램 동작여부 판단
     * seqnum 변화없을 경우 충전중이 아닐때 UI 재부팅
     * @param seqnum 계량기프로그램에서 0~255까지 증가되는 값을 모니터링
     */
    int meter_seqnum_backup = 0;
    int meter_seqnum = 0;
    int meter_comm_errcnt = 0;

    public void MeterviewSeqnumMonitor(int seqnum){
        //1초마다 실행됨
        meter_seqnum = seqnum;
        if((meter_seqnum == meter_seqnum_backup) || (meter_seqnum == 0)){
            meter_comm_errcnt++;
        }
        else {
            meter_seqnum_backup = meter_seqnum;
            meter_comm_errcnt = 0;
        }

        //err count 감지
        if(meter_comm_errcnt >= TypeDefine.METER_COMM_ERR_TIMEOUT){
            //충전중이 아닐경우에 리셋 진행
            if(getUIFlowState() != UIFlowState.UI_CHARGING) {
                if(getUIFlowState() == UIFlowState.UI_READY || getUIFlowState() == UIFlowState.UI_CARD_TAG){
                    //UI 리셋
                    meter_comm_errcnt = 0;
                    multiChannelUIManager.onResetRequest(true);
                }
            }
        }

    }


    /***
     * 계량기 통신 오류시 200-7 bit로 계량기 통신 오류 폴트 알림
     */
    //add by si.201209 - 전력량계 오류상태 모니터링함수
    boolean isMeterCommErr = false;
    boolean isMeterCommErr_backup = false;
    public void MeterStatusMonitoring(long m_meterVal) {
        try {
            if (m_meterVal == -1) isMeterCommErr = true;
            else isMeterCommErr = false;

            if (isMeterCommErr != isMeterCommErr_backup) {
                if (isMeterCommErr) {
                    //충전중 발생했을 경우 충전 중지.
                    if(getUIFlowState() == UIFlowState.UI_CHARGING) {
                        onChargingStop();
                    }
                    else if ( getUIFlowState() != UIFlowState.UI_FINISH_CHARGING &&
                            getUIFlowState() != UIFlowState.UI_READY)  {
                        onPageStartEvent();
                    }
                    //Meter Read error일 경우
                    //dsp로 에러신호 전송
                    dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.UI_FAULT, true);

                    //fault messagebox 띄우기
                    chargeData.faultBoxContent = "";
                    chargeData.faultBoxContent += "[20007] 전력량계 통신오류 발생(" + String.valueOf(chargeData.dspChannel)+")";
                    chargeData.faultBoxContent += "\r\n";
                    pageManager.showFaultBox();

                } else if (!isMeterCommErr) {
                    //미터기 상태 정상일 경우
                    //dsp 미터에러신호 복구 및 기타변수 초기화
                    dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.UI_FAULT, false);

                    pageManager.hideFaultBox();
                }
                isMeterCommErr_backup = isMeterCommErr;
            }
        } catch (Exception e) {

        }
    }

    /**
     *  DSP에서 오는 이벤트를 처리한다.
     * @param channel 해당 채널값
     * @param idx 상태값 Index
     * @param val
     */
    @Override
    public void onDspStatusChange(int channel, DSPRxData2.STATUS400 idx, boolean val) {
        if ( channel == chargeData.dspChannel ) {

            LogWrapper.v(TAG, "DSP Status Change:"+ idx.name()+" is "+val);

            switch (idx) {
                case READY:
                    isDspReady = val;
                    break;

                case AVAL_CHARGE:
                    isDspAvalCharge = val;
                    break;

                case STATE_PLUG:
                    isDspPlug = val;
                    onConnectedCableEvent(val);
                    break;

                case STATE_DOOR:
                    isDspDoor = val;
                    if ( isDspDoor == false ) { // 도어 오픈
                        //도어 명령이 성공적으로 수행이 되면 Door Open을 더 이상 수행하지 않는다.
                        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.DOOR_OPEN, false);
                    }
                    break;

                case CHARGE_RUN:
                    isDspChargeRun = val;
                    if ( val == true ) {
                        dspControl.setState200(chargeData.dspChannel, DSPTxData2.STATUS200.START_CHARGE, false);
                        onDspChargingStartEvent();
                    }
                    break;

                case CG_STARTSTOPBT:
                    if(getUIFlowState() == UIFlowState.UI_CHARGING) {
//                        dispTempMeteringString(new String[]{"Stopping...", "Wait a second.."}, 6000);
                        //충전중지
                    }
                    break;

                case FINISH_CHARGE:
                    isDspChargeFinish = val;
                    if ( val == true ) onFinishChargingEvent();
                    break;

                case FAULT:
                    isDspFault = val;
                    onFaultEventProcess();
                    break;

                case STATE_RESET:
                    break;

                case CONNECTOR_LOCK_A:
                    break;
            }
        }
    }

    public void fillFaultMessage() {
        // 메시지 박스 내용 채움
        chargeData.faultBoxContent = "";
        for (FaultInfo fInfo: faultList) {
            if ( fInfo.isRepair == false ) {
                // 비상정지버튼은 별도 창을 띄운다
                if (!faultManager.isFaultEmergency(chargeData.dspChannel)) {
                    chargeData.faultBoxContent += "[" + fInfo.errorCode + "] " + fInfo.errorMsg;
                    chargeData.faultBoxContent += "\r\n";
                }
            }
        }
    }

    public synchronized void onFaultEventProcess() {
        Vector<FaultInfo> fList = faultManager.scanFaultV2(chargeData.dspChannel);
        boolean isEmergency = faultManager.isFaultEmergency(chargeData.dspChannel);

        if ( fList.size() > 0 ) {
            boolean isContain = false;
            for (FaultInfo fInfo : fList) {
                for (FaultInfo fInfoCur: faultList) {
                    if ( fInfoCur.id == fInfo.id ) {
                        isContain = true;
                        fInfoCur.isRepair = fInfo.isRepair;
                    }
                }
                // 새로운 이벤트인경우
                if ( isContain == false ) {
                    FaultInfo newInfo = new FaultInfo(fInfo.id, fInfo.errorCode, fInfo.errorMsg, fInfo.isRepair);
                    faultList.add(newInfo);
                }
            }
        }

        if ( isPreDspFault != isDspFault ) {
            if ( isDspFault == true ) {
                // 충전충이라면 충전을 중지한다.
                if ( getUIFlowState() == UIFlowState.UI_CHARGING ) {
                    onChargingStop();
                }
                else if ( getUIFlowState() != UIFlowState.UI_FINISH_CHARGING &&
                            getUIFlowState() != UIFlowState.UI_READY)  {
                    onPageStartEvent();
                }

                if (faultManager.isFaultEmergency(chargeData.dspChannel)) {     //20201215
                    pageManager.showEmergencyBox();
                }
                else {              //20201215
                    fillFaultMessage();
                    pageManager.showFaultBox();
                }
            }
            else {
                if (!isEmergency)       //20201215
                    pageManager.hideEmergencyBox();

                pageManager.hideFaultBox();
            }
            isPreDspFault = isDspFault;
        }



        // 긴급버턴 이벤트 발생
//        if ( isEmergencyPressed != isEmergency ) {
//            if (isEmergency == true) {
//                pageManager.showEmergencyBox();
//            } else { // 긴급 버턴 해제
//                pageManager.hideEmergencyBox();
//            }
//            isEmergencyPressed = isEmergency;
//        }
    }

    @Override
    public void onDspMeterChange(int channel, long meterVal) {
        // 계량기 값의 차이를 계속 더한다.
        // 추후 시간별 과금을 위해서.(사용량 x 시간별 단가로 계산)
        //if ( getUIFlowState() == UIFlowState.UI_CHARGING ) {
        //    if ( lastMeterValue < 0) lastMeterValue = meterVal;
        //    int gapMeter = (int)(meterVal - lastMeterValue);
        //    if ( gapMeter < 0) gapMeter = 0;
        //    chargeData.measureWh += gapMeter;
        //    chargeData.chargingCost += ((double)gapMeter/1000.0)*(double)chargeData.chargingUnitCost;
        //}
        //lastMeterValue = meterVal;
        //LogWrapper.v(TAG, "MeterVal : "+meterVal+", measure:"+chargeData.measureWh );
    }

    @Override
    public void onDspCommErrorStstus(boolean isError) {
        if (isError == true) {
            pageManager.showDspCommErrView();
            if (getUIFlowState() == UIFlowState.UI_CHARGING) {
                onChargingStop();
            }
            else if ( getUIFlowState() != UIFlowState.UI_FINISH_CHARGING &&
                    (getUIFlowState() != UIFlowState.UI_CARD_TAG || getUIFlowState()!=UIFlowState.UI_READY))  {
                onPageStartEvent();
            }
//            setUIFlowState(UIFlowState.UI_EIM_COMM_ERR);
            volvoCommManager.SendFaultStat(AlarmCode.ERR_CODE_11, AlarmCode.STATE_OCCUR);        //add by si. 200605 - 차지비 폴트알람 b1 전송(발생)
            LogWrapper.e(TAG, "DSP-UI Comm Error!!");
        } else {
            pageManager.hideDspCommErrView();
            volvoCommManager.SendFaultStat(AlarmCode.ERR_CODE_11, AlarmCode.STATE_RESTORE);        //add by si. 200605 - 차지비 폴트알람 b1 전송(복구)
            if ( getUIFlowState() == UIFlowState.UI_CHARGING ) {
                onChargingStop();
            }
            else if ( getUIFlowState() != UIFlowState.UI_FINISH_CHARGING &&
                    getUIFlowState() != UIFlowState.UI_READY)  {
                onPageStartEvent();
            }

//            onPageStartEvent();
            LogWrapper.e(TAG, "DSP-UI Comm Recovery.");
        }
    }

    //================================================
    // RFID 이벤트 수신
    //================================================
    @Override
    public void onRfidDataReceive(String rfid, boolean success) {
        int ch = channel; //test
//        localMem.readCardNum = rfid;
        if (flowState == UIFlowState.UI_CARD_TAG || flowState == UIFlowState.UI_CHARGING ) {
            onCardTagEvent(rfid, true);
        }

        multiChannelUIManager.rfidReaderRelease(channel);
    }
}
