/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 11. 12 오전 11:29
 *
 */

package com.joas.ocppui_kevcs.webservice;

import android.support.v4.util.CircularArray;
import android.util.ArrayMap;

import com.joas.ocpp.msg.ConfigurationKey;
import com.joas.ocpp.msg.GetConfigurationResponse;
import com.joas.ocpp.msg.IdTagInfo;
import com.joas.ocppui_kevcs.CPConfig;
import com.joas.ocppui_kevcs.OCPPUIKevcsActivity;
import com.joas.ocppui_kevcs.TypeDefine;
import com.joas.ocppui_kevcs.UIFlowManager;
import com.joas.ocppui_kevcs.page.JoasCommMonitorView;
import com.joas.utils.LogWrapperMsg;
import com.joas.utils.NetUtil;
import com.joas.utils.TimeUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

public class WebChargerInfo {

    public static JSONObject getVersions() {
        UIFlowManager flowManager = OCPPUIKevcsActivity.getMainActivity().getFlowManager();

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
        UIFlowManager flowManager = OCPPUIKevcsActivity.getMainActivity().getFlowManager();
        CPConfig cpConfig = flowManager.getCpConfig();

        JSONObject json = new JSONObject();
        try {
            json.put("svraddr", cpConfig.serverURI);
            json.put("cid", cpConfig.chargerID);
            json.put("comm", OCPPUIKevcsActivity.getMainActivity().getIsCommConnected() ? "Connected" : "Disconnected");
            json.put("connector", flowManager.getChargeData().ocppStatus.value());

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
        UIFlowManager flowManager = OCPPUIKevcsActivity.getMainActivity().getFlowManager();
        CPConfig cpConfig = flowManager.getCpConfig();

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
            json.put("is_fastcharger",""+cpConfig.isFastCharger);
        }
        catch (Exception e) {}
        return json;
    }

    public static JSONArray getConfigKeyList() {
        UIFlowManager flowManager = OCPPUIKevcsActivity.getMainActivity().getFlowManager();
        GetConfigurationResponse response = flowManager.getOcppSessionManager().getOcppConfiguration().getConfigurationsAll();

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
        UIFlowManager flowManager = OCPPUIKevcsActivity.getMainActivity().getFlowManager();
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
            flowManager.onResetRequest(true);
        }
        catch (Exception e) {}
    }

    public static void setConfigKey(String key, String val) {
        UIFlowManager flowManager = OCPPUIKevcsActivity.getMainActivity().getFlowManager();
        flowManager.getOcppSessionManager().getOcppStack().getLocalConfig().saveOcppConfiguration(key, val);
        flowManager.getOcppSessionManager().getOcppConfiguration().setConfigration(key, val);
    }

    public static void setChargerSetting(JSONObject json) {
        UIFlowManager flowManager = OCPPUIKevcsActivity.getMainActivity().getFlowManager();
        CPConfig cpConfig = flowManager.getCpConfig();

        try {
            cpConfig.chargerID = json.getString("charger_id");
            cpConfig.serverURI = json.getString("server_uri");
            cpConfig.httpBasicAuthID = json.getString("http_auth_id");
            cpConfig.httpBasicAuthPassword = json.getString("http_auth_pwd");
            cpConfig.settingPassword = json.getString("admin_pwd");
            cpConfig.useHttpBasicAuth = json.getBoolean("use_http_auth");
            cpConfig.useWatchDogTimer = json.getBoolean("use_watchdog");
            cpConfig.useTrustCA = json.getBoolean("use_trustca");
            cpConfig.isFastCharger = json.getBoolean("is_fastcharger");
            cpConfig.saveConfig(OCPPUIKevcsActivity.getMainActivity().getApplicationContext());
            flowManager.stopWatdogTimer();
            flowManager.onSettingChanged();
            flowManager.runSoftReset(3);
        }
        catch (Exception e) {}
    }

    public static JSONArray getRecentSysLog() {
        CircularArray<LogWrapperMsg> queue = OCPPUIKevcsActivity.getMainActivity().getPageManger().getJoasDebugMsgView().getPacketQueue();
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
        CircularArray<JoasCommMonitorView.OCPPMonitorMsg> queue = OCPPUIKevcsActivity.getMainActivity().getPageManger().getCommMonitorView().getPacketQueue();

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

    public static JSONArray getAuthCacheList() {
        Map<String, IdTagInfo> list = OCPPUIKevcsActivity.getMainActivity().getFlowManager().getOcppSessionManager().getOcppStack().getAuthorizeCache().getAllCacheIdTag();
        JSONArray array = new JSONArray();
        try {
            String k;
            IdTagInfo v;
            for(Map.Entry<String, IdTagInfo> entry : list.entrySet()) {
                String key = entry.getKey();
                IdTagInfo idTagInfo = entry.getValue();
                JSONObject json = new JSONObject();
                json.put("idtag", key);
                json.put("parentid", idTagInfo.getParentIdTag());
                json.put("status", idTagInfo.getStatus());
                json.put("expired", TimeUtil.getDateAsString("dd/MM/yyyy HH:mm:ss", idTagInfo.getExpiryDate().getTime()));
                array.put(json);
            }
        }
        catch (Exception e) {}
        return array;
    }
    public static JSONArray getLocalAuthList() {
        Map<String, IdTagInfo> list = OCPPUIKevcsActivity.getMainActivity().getFlowManager().getOcppSessionManager().getOcppStack().getLocalAuthList().getAllLocalAuthList();
        JSONArray array = new JSONArray();
        try {
            String k;
            IdTagInfo v;
            for(Map.Entry<String, IdTagInfo> entry : list.entrySet()) {
                String key = entry.getKey();
                IdTagInfo idTagInfo = entry.getValue();
                JSONObject json = new JSONObject();
                json.put("idtag", key);
                json.put("parentid", idTagInfo.getParentIdTag());
                json.put("status", idTagInfo.getStatus());
                json.put("expired", TimeUtil.getDateAsString("dd/MM/yyyy HH:mm:ss", idTagInfo.getExpiryDate().getTime()));
                array.put(json);
            }
        }
        catch (Exception e) {}
        return array;
    }

}
