/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 11. 19 오후 2:40
 *
 */

package com.joas.ocppui_dubai_2ch.webservice;

import android.support.v4.util.CircularArray;
import android.util.ArrayMap;

import com.joas.ocpp.msg.ConfigurationKey;
import com.joas.ocpp.msg.GetConfigurationResponse;
import com.joas.ocppui_dubai_2ch.CPConfig;
import com.joas.ocppui_dubai_2ch.MultiChannelUIManager;
import com.joas.ocppui_dubai_2ch.OCPPUIActivity;
import com.joas.ocppui_dubai_2ch.TypeDefine;
import com.joas.ocppui_dubai_2ch.UIFlowManager;
import com.joas.ocppui_dubai_2ch.page.JoasCommMonitorView;
import com.joas.utils.LogWrapperMsg;
import com.joas.utils.NetUtil;

import org.json.JSONArray;
import org.json.JSONObject;

public class WebChargerInfo {

    public static JSONObject getVersions() {
        UIFlowManager flowManager = OCPPUIActivity.getMainActivity().getMultiChannelUIManager().getUIFlowManager(0);

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
        MultiChannelUIManager manager = OCPPUIActivity.getMainActivity().getMultiChannelUIManager();
        CPConfig cpConfig = manager.getCpConfig();

        JSONObject json = new JSONObject();
        try {
            json.put("svraddr", cpConfig.serverURI);
            json.put("cid", cpConfig.chargerID);
            json.put("comm", OCPPUIActivity.getMainActivity().getIsCommConnected() ? "Connected" : "Disconnected");
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
        MultiChannelUIManager manager = OCPPUIActivity.getMainActivity().getMultiChannelUIManager();
        CPConfig cpConfig = manager.getCpConfig();

        JSONObject json = new JSONObject();
        try {
            json.put("charger_id", cpConfig.chargerID);
            json.put("server_uri", cpConfig.serverURI);
            json.put("http_auth_id", cpConfig.httpBasicAuthID);
            json.put("http_auth_pwd", cpConfig.httpBasicAuthPassword);
            json.put("admin_pwd", cpConfig.settingPassword);
            json.put("use_http_auth", ""+cpConfig.useHttpBasicAuth);
            json.put("use_watchdog", ""+cpConfig.useWatchDogTimer);
            json.put("use_trustca", ""+cpConfig.useTrustCA);
        }
        catch (Exception e) {}
        return json;
    }

    public static JSONArray getConfigKeyList() {
        MultiChannelUIManager manager = OCPPUIActivity.getMainActivity().getMultiChannelUIManager();
        GetConfigurationResponse response = manager.getOcppSessionManager().getOcppConfiguration().getConfigurationsAll();

        JSONArray array = new JSONArray();
        try {
            for (ConfigurationKey key: response.getConfigurationKey()) {
                JSONObject json = new JSONObject();
                json.put("key", key.getKey());
                json.put("readonly", key.getReadonly());
                json.put("value", key.getValue());
                array.put(json);
            }
        }
        catch (Exception e) {}
        return array;
    }

    public static void setNetwork(JSONObject json) {
        MultiChannelUIManager manager = OCPPUIActivity.getMainActivity().getMultiChannelUIManager();
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
        MultiChannelUIManager manager = OCPPUIActivity.getMainActivity().getMultiChannelUIManager();
        manager.getOcppSessionManager().getOcppStack().getLocalConfig().saveOcppConfiguration(key, val);
        manager.getOcppSessionManager().getOcppConfiguration().setConfigration(key, val);
    }

    public static void setChargerSetting(JSONObject json) {
        MultiChannelUIManager manager = OCPPUIActivity.getMainActivity().getMultiChannelUIManager();
        CPConfig cpConfig = manager.getCpConfig();

        try {
            cpConfig.chargerID = json.getString("charger_id");
            cpConfig.serverURI = json.getString("server_uri");
            cpConfig.httpBasicAuthID = json.getString("http_auth_id");
            cpConfig.httpBasicAuthPassword = json.getString("http_auth_pwd");
            cpConfig.settingPassword = json.getString("admin_pwd");
            cpConfig.useHttpBasicAuth = json.getBoolean("use_http_auth");
            cpConfig.useWatchDogTimer = json.getBoolean("use_watchdog");
            cpConfig.useTrustCA = json.getBoolean("use_trustca");
            cpConfig.saveConfig(OCPPUIActivity.getMainActivity().getApplicationContext());
            manager.stopWatdogTimer();
            manager.onSettingChanged();
            manager.runSoftReset(3);
        }
        catch (Exception e) {}
    }

    public static JSONArray getRecentSysLog() {
        CircularArray<LogWrapperMsg> queue = OCPPUIActivity.getMainActivity().getMultiChannelUIManager().getJoasDebugMsgView().getPacketQueue();
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
        CircularArray<JoasCommMonitorView.OCPPMonitorMsg> queue = OCPPUIActivity.getMainActivity().getMultiChannelUIManager().getCommMonitorView().getPacketQueue();
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
