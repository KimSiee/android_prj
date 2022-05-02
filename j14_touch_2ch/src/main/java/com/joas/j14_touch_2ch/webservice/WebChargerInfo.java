/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 11. 3 오전 9:59
 *
 */

package com.joas.j14_touch_2ch.webservice;

import android.support.v4.util.CircularArray;
import android.util.ArrayMap;

import com.joas.j14_touch_2ch.J14Touch2chUIActivity;
import com.joas.j14_touch_2ch.CPConfig;
import com.joas.j14_touch_2ch.MultiChannelUIManager;
import com.joas.j14_touch_2ch.TypeDefine;
import com.joas.j14_touch_2ch.UIFlowManager;
import com.joas.j14_touch_2ch.page.JoasCommMonitorView;
import com.joas.utils.LogWrapperMsg;
import com.joas.utils.NetUtil;

import org.json.JSONArray;
import org.json.JSONObject;

public class WebChargerInfo {

    public static JSONObject getVersions() {
        UIFlowManager flowManager = J14Touch2chUIActivity.getMainActivity().getMultiChannelUIManager().getUIFlowManager(0);

        JSONObject json = new JSONObject();
        try {
            json.put("model_name", TypeDefine.MODEL_NAME);
            json.put("sw_ver", TypeDefine.SW_VER + " " + TypeDefine.SW_RELEASE_DATE);
            json.put("dsp_ver", String.format("0x%X", flowManager.getDspVersion()));

        }
        catch (Exception e) {}
        return json;
    }

    public static JSONObject getChargerStat() {
        MultiChannelUIManager manager = J14Touch2chUIActivity.getMainActivity().getMultiChannelUIManager();
        CPConfig cpConfig = manager.getCpConfig();

        JSONObject json = new JSONObject();
        try {
            json.put("svraddr", cpConfig.serverIP);
            json.put("svrport", cpConfig.serverPort);
            json.put("sid", cpConfig.stationID);
            json.put("cid", cpConfig.chargerID);
            json.put("comm", J14Touch2chUIActivity.getMainActivity().getIsCommConnected() ? "Connected" : "Disconnected");
            String strConnector = "1:"+manager.getUIFlowManager(0).getChargeData().ocppStatus.value() + ", " +
                                    "2:"+manager.getUIFlowManager(1).getChargeData().ocppStatus.value();
            json.put("connector", strConnector);

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
        MultiChannelUIManager manager = J14Touch2chUIActivity.getMainActivity().getMultiChannelUIManager();
        CPConfig cpConfig = manager.getCpConfig();

        JSONObject json = new JSONObject();
        try {
            json.put("server_addr", cpConfig.serverIP);
            json.put("server_port", cpConfig.serverPort);
            json.put("station_id", cpConfig.stationID);
            json.put("charger_id", cpConfig.chargerID);
            json.put("admin_pwd", cpConfig.settingPassword);
            json.put("use_watchdog", ""+cpConfig.useWatchDogTimer);
            json.put("auth_skip", ""+cpConfig.isAuthSkip);
        }
        catch (Exception e) {}
        return json;
    }

    public static JSONArray getConfigKeyList() {
        MultiChannelUIManager manager = J14Touch2chUIActivity.getMainActivity().getMultiChannelUIManager();
//        GetConfigurationResponse response = manager.getOcppSessionManager().getOcppConfiguration().getConfigurationsAll();
//
//        JSONArray array = new JSONArray();
//        try {
//            for (ConfigurationKey key: response.getConfigurationKey()) {
//                JSONObject json = new JSONObject();
//                json.put("key", key.getKey());
//                json.put("readonly", key.getReadonly());
//                json.put("value", key.getValue());
//                array.put(json);
//            }
//        }
//        catch (Exception e) {}
//        return array;
        return null;
    }

    public static void setNetwork(JSONObject json) {
        MultiChannelUIManager manager = J14Touch2chUIActivity.getMainActivity().getMultiChannelUIManager();
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
            manager.onResetRequest(true);
        }
        catch (Exception e) {}
    }

    public static void setConfigKey(String key, String val) {
        MultiChannelUIManager manager = J14Touch2chUIActivity.getMainActivity().getMultiChannelUIManager();
//        manager.getOcppSessionManager().getOcppStack().getLocalConfig().saveOcppConfiguration(key, val);
//        manager.getOcppSessionManager().getOcppConfiguration().setConfigration(key, val);
    }

    public static void setChargerSetting(JSONObject json) {
        MultiChannelUIManager manager = J14Touch2chUIActivity.getMainActivity().getMultiChannelUIManager();
        CPConfig cpConfig = manager.getCpConfig();

        try {
            cpConfig.stationID  = json.getString("station_id");
            cpConfig.chargerID = json.getString("charger_id");
            cpConfig.serverIP = json.getString("server_addr");
            cpConfig.serverPort = json.getInt("server_port");
            cpConfig.settingPassword = json.getString("admin_pwd");
            cpConfig.useWatchDogTimer = json.getBoolean("use_watchdog");
            cpConfig.isAuthSkip = json.getBoolean("auth_skip");
            cpConfig.saveConfig(J14Touch2chUIActivity.getMainActivity().getApplicationContext());
            manager.stopWatdogTimer();
            manager.onSettingChanged();
            manager.runSoftReset(3);
        }
        catch (Exception e) {}
    }

    public static JSONArray getRecentSysLog() {
        CircularArray<LogWrapperMsg> queue = J14Touch2chUIActivity.getMainActivity().getMultiChannelUIManager().getJoasDebugMsgView().getPacketQueue();
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
        CircularArray<JoasCommMonitorView.OCPPMonitorMsg> queue = J14Touch2chUIActivity.getMainActivity().getMultiChannelUIManager().getCommMonitorView().getPacketQueue();
        int cnt = queue.size();
        JSONArray array = new JSONArray();
        try {
            synchronized (queue) {
                for (int i = 0; i < cnt; i++) {
                    JSONObject json = new JSONObject();
                    JoasCommMonitorView.OCPPMonitorMsg item = queue.get(i);
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
}
