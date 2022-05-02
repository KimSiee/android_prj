/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 1. 13 오후 5:19
 *
 */

package com.joas.posco_slow_charlcd.webservice;

import android.os.Environment;
import android.support.v4.util.CircularArray;
import android.util.ArrayMap;

import com.joas.posco_comm.PoscoChargerInfo;
import com.joas.posco_slow_charlcd.CPConfig;
import com.joas.posco_slow_charlcd.PoscoSlowCharLCDUIActivity;
import com.joas.posco_slow_charlcd.UIFlowManager;
import com.joas.posco_slow_charlcd.TypeDefine;

import com.joas.posco_slow_charlcd.page.JoasCommMonitorView;
import com.joas.posco_slow_charlcd.page.OcppCommMonitorView;
import com.joas.utils.LogWrapperMsg;
import com.joas.utils.NetUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.Arrays;

public class WebChargerInfo {

    public static JSONObject getVersions() {
        UIFlowManager flowManager = PoscoSlowCharLCDUIActivity.getMainActivity().getFlowManager();

        JSONObject json = new JSONObject();
        try {
            json.put("model_name", TypeDefine.MODEL_NAME);
            json.put("sw_ver", TypeDefine.SW_VERSION + " " + TypeDefine.SW_RELEASE_DATE);
            json.put("dsp_ver", String.format("0x%X", flowManager.getDspVersion()));

        }
        catch (Exception e) {}
        return json;
    }

    public static JSONObject getChargerStat() {
        UIFlowManager flowManager = PoscoSlowCharLCDUIActivity.getMainActivity().getFlowManager();
        CPConfig cpConfig = flowManager.getCpConfig();
        PoscoChargerInfo poscoChargerInfo = flowManager.getPoscoChargerInfo();

        JSONObject json = new JSONObject();
        try {
            json.put("svraddr", cpConfig.serverIP);
            json.put("svrport", cpConfig.serverPort);
            json.put("sid", cpConfig.stationID);
            json.put("cid", cpConfig.chargerID);
            json.put("comm", PoscoSlowCharLCDUIActivity.getMainActivity().getIsCommConnected() ? "Connected" : "Disconnected");
            json.put("mdn_num", poscoChargerInfo.mtoPhoneNumber);
            json.put("mdn_rssi", poscoChargerInfo.mtomRssi);
        }
        catch (Exception e) {}
        return json;
    }

    public static JSONObject getNetworkInfo() {
        ArrayMap<String, String> map = NetUtil.readIpConfigurations();
        String ipaddr = map.get("ipaddress");
        String netmask  = map.get("netmask");
        String gateway= map.get("gateway");
        String dns = map.get("dns");

        if ( map.size() == 0 ) map.put("type", "DHCP");

        if ( map.get("type").equals("DHCP") ) {
            ipaddr = NetUtil.getDHCPIpAddr();
            netmask = NetUtil.getDHCPNetmask();
            gateway = NetUtil.getDHCPGateway();
            dns = NetUtil.getDHCPDNS();
        }

        JSONObject json = new JSONObject();
        try {
            json.put("type", map.get("type"));
            json.put("ip", ipaddr);
            json.put("netmask", netmask);
            json.put("gateway", gateway);
            json.put("dns", dns);
        }
        catch (Exception e) {}
        return json;
    }

    public static JSONObject getSystemConfig() {
        UIFlowManager flowManager = PoscoSlowCharLCDUIActivity.getMainActivity().getFlowManager();
        CPConfig cpConfig = flowManager.getCpConfig();

        JSONObject json = new JSONObject();
        try {
            json.put("server_addr", cpConfig.serverIP);
            json.put("server_port", cpConfig.serverPort);
            json.put("station_id", cpConfig.stationID);
            json.put("charger_id", cpConfig.chargerID);
            json.put("admin_pwd", cpConfig.settingPassword);
            json.put("chargerkind",cpConfig.chargerKind);
            json.put("kakao_qr_cost",""+cpConfig.kakaoQRCost);
            json.put("kakao_credit_cost",""+cpConfig.kakaoCreditCost);
            json.put("use_watchdog", ""+cpConfig.useWatchDogTimer);
            json.put("auth_skip", ""+cpConfig.isAuthSkip);
            json.put("use_kakaocost",""+cpConfig.useKakaoCost);
            json.put("use_kakaonavi",""+cpConfig.useKakaoNavi);
            json.put("use_tl3500bs",""+cpConfig.useTL3500BS);
            json.put("use_sehanrf",""+cpConfig.useSehanRF);
            json.put("use_acmrf",""+cpConfig.useACMRF);
        }
        catch (Exception e) {}
        return json;
    }

    //ocpp setting get - add by si-220121
    public static JSONObject getOcppSystemConfig(){
        UIFlowManager flowManager = PoscoSlowCharLCDUIActivity.getMainActivity().getFlowManager();
        CPConfig cpConfig = flowManager.getCpConfig();

        JSONObject json = new JSONObject();
        try{
            json.put("ocpp_server_url", cpConfig.ocpp_serverURI);
            json.put("ocpp_cpid", cpConfig.ocpp_chargerID);
            json.put("auth_id",cpConfig.httpBasicAuthID);
            json.put("auth_password",cpConfig.httpBasicAuthPassword);
            json.put("charger_serial_num",cpConfig.ocpp_chargePointSerialNumber);
            json.put("use_ocpp",""+cpConfig.useOcpp);
            json.put("use_ssl",""+cpConfig.useSSL);
            json.put("use_basic_auth",""+cpConfig.useBasicAuth);

        }catch (Exception e){}
        return json;
    }

    public static void setNetwork(JSONObject json) {
        UIFlowManager flowManager = PoscoSlowCharLCDUIActivity.getMainActivity().getFlowManager();
        try {
            String type = json.getString("type");
            String ipaddr  = json.getString("ipaddr");
            String netmask = json.getString("netmask");
            String gateway = json.getString("gateway");
            String dns = json.getString("dns");

            if ( type.equals("dhcp") ) {
                NetUtil.configurationDHCP();
            }
            else {
                NetUtil.configurationStaticIP(ipaddr, netmask, gateway, dns);
            }
            flowManager.resetRequest(true);
        }
        catch (Exception e) {}
    }

    public static void setChargerSetting(JSONObject json) {
        UIFlowManager flowManager = PoscoSlowCharLCDUIActivity.getMainActivity().getFlowManager();
        CPConfig cpConfig = flowManager.getCpConfig();

        try {
            cpConfig.stationID  = json.getString("station_id");
            cpConfig.chargerID = json.getString("charger_id");
            cpConfig.serverIP = json.getString("server_addr");
            cpConfig.serverPort = json.getInt("server_port");
            cpConfig.settingPassword = json.getString("admin_pwd");
            cpConfig.chargerKind = json.getString("chargerkind");
            cpConfig.kakaoQRCost = json.getInt("kakao_qr_cost");
            cpConfig.kakaoCreditCost = json.getInt("kakao_credit_cost");
            cpConfig.useWatchDogTimer = json.getBoolean("use_watchdog");
            cpConfig.isAuthSkip = json.getBoolean("auth_skip");
            cpConfig.useKakaoCost = json.getBoolean("use_kakaocost");
            cpConfig.useKakaoNavi = json.getBoolean("use_kakaonavi");
            cpConfig.useTL3500BS = json.getBoolean("use_tl3500bs");
            cpConfig.useSehanRF = json.getBoolean("use_sehanrf");
            cpConfig.useACMRF = json.getBoolean("use_acmrf");

            cpConfig.saveConfig(PoscoSlowCharLCDUIActivity.getMainActivity().getApplicationContext());

            flowManager.stopWatdogTimer();
            flowManager.onSettingChanged();
            flowManager.runSoftReset(3);
        }
        catch (Exception e) {}
    }

    public static void setOcppSetting(JSONObject json){
        UIFlowManager flowManager = PoscoSlowCharLCDUIActivity.getMainActivity().getFlowManager();
        CPConfig cpConfig = flowManager.getCpConfig();

        try{
            cpConfig.ocpp_serverURI = json.getString("ocpp_server_url");
            cpConfig.ocpp_chargerID = json.getString("ocpp_cpid");
            cpConfig.httpBasicAuthID = json.getString("auth_id");
            cpConfig.httpBasicAuthPassword = json.getString("auth_password");
            cpConfig.ocpp_chargePointSerialNumber = json.getString("charger_serial_num");
            cpConfig.useOcpp = json.getBoolean("use_ocpp");
            cpConfig.useSSL = json.getBoolean("use_ssl");
            cpConfig.useBasicAuth = json.getBoolean("use_basic_auth");

            cpConfig.saveConfig(PoscoSlowCharLCDUIActivity.getMainActivity().getApplicationContext());

            flowManager.stopWatdogTimer();
            flowManager.onSettingChanged();
            flowManager.runSoftReset(3);
        }catch (Exception e){}
    }

    public static JSONArray getRecentSysLog() {
        CircularArray<LogWrapperMsg> queue = PoscoSlowCharLCDUIActivity.getMainActivity().getPageManger().getJoasDebugMsgView().getPacketQueue();
        int cnt = queue.size();
        JSONArray array = new JSONArray();
        try {
            synchronized (queue) {
                for (int i = 0; i < cnt; i++) {
                    JSONObject json = new JSONObject();
                    LogWrapperMsg item = queue.get(i);
                    json.put("time", item.time);
                    json.put("level", LogWrapperMsg.getLevelString(item.level));
                    json.put("tag", item.TAG);
                    json.put("msg", item.msg);
                    array.put(json);
                }
            }
        }
        catch (Exception e) {}
        return array;
    }

    public static JSONArray getRecentCommLog() {
        CircularArray<JoasCommMonitorView.CommMonitorMsg> queue = PoscoSlowCharLCDUIActivity.getMainActivity().getPageManger().getCommMonitorView().getPacketQueue();
        int cnt = queue.size();
        JSONArray array = new JSONArray();
        try {
            synchronized (queue) {
                for (int i = 0; i < cnt; i++) {
                    JSONObject json = new JSONObject();
                    JoasCommMonitorView.CommMonitorMsg item = queue.get(i);
                    json.put("time", item.time);
                    json.put("trx", item.trx);
                    json.put("msg", item.data);
                    array.put(json);
                }
            }
        }
        catch (Exception e) {}
        return array;
    }

    //add by si - ocppcommlog get
    public static JSONArray getRecentOcppCommLog(){
        CircularArray<OcppCommMonitorView.OCPPMonitorMsg> queue = PoscoSlowCharLCDUIActivity.getMainActivity().getPageManger().getOcppCommMonitorView().getPacketQueue();

        int cnt = queue.size();
        JSONArray array = new JSONArray();
        try {
            synchronized (queue) {
                for (int i = 0; i < cnt; i++) {
                    JSONObject json = new JSONObject();
                    OcppCommMonitorView.OCPPMonitorMsg item = queue.get(i);
                    json.put("time", item.time);
                    json.put("trx", item.trx);
                    json.put("msg", item.data);
                    array.put(json);
                }
            }
        }
        catch (Exception e) {}
        return array;
    }

    public static JSONArray getCommLogFiles() {
        JSONArray array = new JSONArray();
        try {
            File file = new File(Environment.getExternalStorageDirectory(), TypeDefine.REPOSITORY_BASE_PATH+"/Log");
            File[] files = file.listFiles();
            Arrays.sort(files, (a, b) -> a.getName().compareTo(b.getName()));

            for (int i = 0; i < files.length; i++) {
                array.put(files[i].getName());
            }
        }
        catch (Exception e) {}
        return array;
    }
}
