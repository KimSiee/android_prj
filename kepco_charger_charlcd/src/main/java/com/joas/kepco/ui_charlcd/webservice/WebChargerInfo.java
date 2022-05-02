/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 5. 12 오후 4:02
 *
 */

package com.joas.kepco.ui_charlcd.webservice;

import android.os.Environment;
import android.support.v4.util.CircularArray;
import android.util.ArrayMap;

import com.joas.kepco.ui_charlcd.CPConfig;
import com.joas.kepco.ui_charlcd.KepcoCharLCDUIActivity;
import com.joas.kepco.ui_charlcd.TypeDefine;
import com.joas.kepco.ui_charlcd.UIFlowManager;
import com.joas.utils.LogWrapper;
import com.joas.utils.LogWrapperMsg;
import com.joas.utils.NetUtil;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.Arrays;

public class WebChargerInfo {

    public static JSONObject getVersions() {
        UIFlowManager flowManager = KepcoCharLCDUIActivity.getMainActivity().getFlowManager();

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
        UIFlowManager flowManager = KepcoCharLCDUIActivity.getMainActivity().getFlowManager();
        CPConfig cpConfig = flowManager.getCpConfig();

        JSONObject json = new JSONObject();
        try {
            json.put("svraddr", cpConfig.serverIP);
            json.put("svrport", cpConfig.serverPort);
            json.put("authkey", cpConfig.serverAuthKey);
            json.put("cid", cpConfig.chargerID);
            json.put("comm", KepcoCharLCDUIActivity.getMainActivity().getFlowManager().getKepcoComm().getConnectStatus() ? "Connected" : "Disconnected");
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
        UIFlowManager flowManager = KepcoCharLCDUIActivity.getMainActivity().getFlowManager();
        CPConfig cpConfig = flowManager.getCpConfig();

        JSONObject json = new JSONObject();
        try {
            json.put("server_addr", cpConfig.serverIP);
            json.put("server_port", cpConfig.serverPort);
            json.put("auth_key", cpConfig.serverAuthKey);
            json.put("charger_id", cpConfig.chargerID);
            json.put("admin_pwd", cpConfig.settingPassword);
            json.put("use_payterminal", ""+cpConfig.usePayTerminal);
            json.put("use_watchdog", ""+cpConfig.useWatchDogTimer);
            json.put("auth_skip", ""+cpConfig.isAuthSkip);
        }
        catch (Exception e) {}
        return json;
    }

    public static void setNetwork(JSONObject json) {
        UIFlowManager flowManager = KepcoCharLCDUIActivity.getMainActivity().getFlowManager();
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

            TimeoutTimer timer = new TimeoutTimer(3000, new TimeoutHandler() {
                @Override
                public void run() {
                    flowManager.rebootSystem();
                }
            });
            timer.startOnce();

        }
        catch (Exception e) {}
    }

    public static void setChargerSetting(JSONObject json) {
        UIFlowManager flowManager = KepcoCharLCDUIActivity.getMainActivity().getFlowManager();
        CPConfig cpConfig = flowManager.getCpConfig();

        try {
            cpConfig.serverAuthKey  = json.getString("auth_key");
            cpConfig.chargerID = json.getString("charger_id");
            cpConfig.serverIP = json.getString("server_addr");
            cpConfig.serverPort = json.getInt("server_port");
            cpConfig.settingPassword = json.getString("admin_pwd");
            cpConfig.usePayTerminal = json.getBoolean("use_payterminal");
            cpConfig.useWatchDogTimer = json.getBoolean("use_watchdog");
            cpConfig.isAuthSkip = json.getBoolean("auth_skip");

            cpConfig.saveConfig(KepcoCharLCDUIActivity.getMainActivity().getApplicationContext());

            flowManager.stopWatdogTimer();
            flowManager.onSettingChanged();
            flowManager.runSoftReset(3);
        }
        catch (Exception e) {
            LogWrapper.e("WebChargerInfo", "setChargerSetting:"+e.toString());
        }
    }

    public static JSONArray getRecentSysLog() {
        CircularArray<LogWrapperMsg> queue = KepcoCharLCDUIActivity.getMainActivity().getPageManger().getJoasDebugMsgView().getPacketQueue();
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
        CircularArray<UIFlowManager.CommMonitorMsg> queue = KepcoCharLCDUIActivity.getMainActivity().getFlowManager().getCommLogPacketQueue();
        int cnt = queue.size();
        JSONArray array = new JSONArray();
        try {
            synchronized (queue) {
                for (int i = 0; i < cnt; i++) {
                    JSONObject json = new JSONObject();
                    UIFlowManager.CommMonitorMsg item = queue.get(i);
                    json.put("time", item.time);
                    json.put("tag", item.tag);
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
            File file = new File(Environment.getExternalStorageDirectory(), TypeDefine.KEPCO_COMM_PATH+"/Log");
            File[] files = file.listFiles();
            Arrays.sort(files, (a, b) -> a.getName().compareTo(b.getName()));

            for (int i = 0; i < files.length; i++) {
                array.put(files[i].getName());
            }
        }
        catch (Exception e) {}
        return array;
    }

    public static JSONArray getChargingLogFiles() {
        JSONArray array = new JSONArray();
        try {
            File file = new File(Environment.getExternalStorageDirectory(), TypeDefine.SYSLOG_PATH);
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
