/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 5. 12 오후 4:02
 *
 */

package com.joas.kevcs_charger_charlcd.webservice;


import android.os.Environment;
import android.util.Log;

import com.joas.kevcs_charger_charlcd.CPConfig;
import com.joas.kevcs_charger_charlcd.KevcsCharLCDUIActivity;
import com.joas.kevcs_charger_charlcd.TypeDefine;
import com.joas.kevcs_charger_charlcd.UIFlowManager;
import com.joas.utils.FileUtil;
import com.joas.utils.RemoteUpdater;
import com.joas.utils.ZipUtils;
import com.yanzhenjie.andserver.annotation.GetMapping;
import com.yanzhenjie.andserver.annotation.PathVariable;
import com.yanzhenjie.andserver.annotation.PostMapping;
import com.yanzhenjie.andserver.annotation.RequestMapping;
import com.yanzhenjie.andserver.annotation.RequestParam;
import com.yanzhenjie.andserver.annotation.RestController;
import com.yanzhenjie.andserver.http.HttpRequest;
import com.yanzhenjie.andserver.http.HttpResponse;
import com.yanzhenjie.andserver.http.multipart.MultipartFile;
import com.yanzhenjie.andserver.util.MediaType;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping(path = "/api")
class WebController {

    @GetMapping(path = "/login/{userId}/{password}")
    String login(@PathVariable(name = "userId") String userId, @PathVariable(name = "password") String password) {
        JSONObject result = new JSONObject();

        try {
            UIFlowManager flowManager = KevcsCharLCDUIActivity.getMainActivity().getFlowManager();
            CPConfig cpConfig = flowManager.getCpConfig();

            if (userId.equals("admin") && password.equals(cpConfig.settingPassword)) {
                result.put("token", WebAuthManager.getAuthToken(userId));
                result.put("result", "success");
            } else {
                result.put("token", "");
                result.put("result", "fail");
            }
        }
        catch(Exception e) {}

        return result.toString();
    }

    @GetMapping(path = "/verify_token/{token}")
    String verify_token(@PathVariable(name = "token") String token) {
        boolean ret = false;

        ret = WebAuthManager.verifyAuthToken(token);

        JSONObject result = new JSONObject();

        try {
            if ( ret ) result.put("result", "success");
            else result.put("result", "fail");
        }
        catch(Exception e) {}

        return result.toString();
    }

    @GetMapping(path = "/getstatus/{token}")
    String get_status(@PathVariable(name = "token") String token) {
        boolean ret = false;

        ret = WebAuthManager.verifyAuthToken(token);

        JSONObject result = new JSONObject();

        try {
            if ( ret == false ) {
                result.put("result", "fail");
                return result.toString();
            }

            result.put("result", "success");
            result.put("version", WebChargerInfo.getVersions());
            result.put("network", WebChargerInfo.getNetworkInfo());
            result.put("charger", WebChargerInfo.getChargerStat());

        }
        catch(Exception e) {}

        return result.toString();
    }

    @GetMapping(path = "/getnetwork/{token}")
    String get_network(@PathVariable(name = "token") String token) {
        boolean ret = false;

        ret = WebAuthManager.verifyAuthToken(token);

        JSONObject result = new JSONObject();

        try {
            if ( ret == false ) {
                result.put("result", "fail");
                return result.toString();
            }

            result.put("result", "success");
            result.put("network", WebChargerInfo.getNetworkInfo());

        }
        catch(Exception e) {}

        return result.toString();
    }

    @GetMapping(path = "/getsystem/{token}")
    String get_system(@PathVariable(name = "token") String token) {
        boolean ret = false;

        ret = WebAuthManager.verifyAuthToken(token);

        JSONObject result = new JSONObject();

        try {
            if ( ret == false ) {
                result.put("result", "fail");
                return result.toString();
            }

            result.put("result", "success");
            result.put("system", WebChargerInfo.getSystemConfig());

        }
        catch(Exception e) {}

        return result.toString();
    }


    @PostMapping(path = "/setnetwork", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    String set_network(HttpRequest request, HttpResponse response) {
        String body = "";
        JSONObject result = new JSONObject();

        try {
            body = request.getBody().string();

            JSONObject json = new JSONObject(body);

            boolean ret = WebAuthManager.verifyAuthToken(json.getString("token"));

            if ( ret == false ) {
                result.put("result", "fail");
                return result.toString();
            }

            result.put("result", "success");
            WebChargerInfo.setNetwork(json);

        } catch(Exception e){}

        return result.toString();
    }

    @PostMapping(path = "/setsystem", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    String set_system(HttpRequest request, HttpResponse response) {
        String body = "";
        JSONObject result = new JSONObject();

        try {
            body = request.getBody().string();

            JSONObject json = new JSONObject(body);

            boolean ret = WebAuthManager.verifyAuthToken(json.getString("token"));

            if ( ret == false ) {
                result.put("result", "fail");
                return result.toString();
            }

            result.put("result", "success");
            WebChargerInfo.setChargerSetting(json);

        } catch(Exception e){}

        return result.toString();
    }


    @PostMapping(path = "/swupdate", produces = MediaType.APPLICATION_JSON_UTF8_VALUE )
    String sw_update(@RequestParam(name = "file") MultipartFile file, @RequestParam(name = "token") String token) throws IOException {
        JSONObject result = new JSONObject();

        try {
            boolean ret = WebAuthManager.verifyAuthToken(token);

            if (ret == false) {
                result.put("result", "fail");
                return result.toString();
            }

            String updatePath = Environment.getExternalStorageDirectory() + TypeDefine.REPOSITORY_BASE_PATH + "/Update";

            File parent = new File(updatePath);

            if (!parent.exists()) {
                parent.mkdirs();
            }
            String updateFile = updatePath + "/update.zip";

            File localFile = new File(updateFile);
            file.transferTo(localFile);

            result.put("result", "success");

            ZipUtils.unzip(updateFile, updatePath, false);
            RemoteUpdater updater = new RemoteUpdater(KevcsCharLCDUIActivity.getMainActivity(), updatePath, "update.apk");
            updater.doUpdateFromApk("com.joas.smartcharger");

        } catch (Exception e) {
            Log.e("sw_update", "err:"+e.toString());
        }

        return result.toString();
    }


    @GetMapping(path = "/factoryreset/{token}")
    String factoryreset(@PathVariable(name = "token") String token) {
        boolean ret = false;

        ret = WebAuthManager.verifyAuthToken(token);

        JSONObject result = new JSONObject();

        try {
            if ( ret == false ) {
                result.put("result", "fail");
                return result.toString();
            }

            result.put("result", "success");
            KevcsCharLCDUIActivity.getMainActivity().getFlowManager().doFactoryReset();
        }
        catch(Exception e) {}

        return result.toString();
    }


    @GetMapping(path = "/getrecentsyslog/{token}")
    String get_recentsyslog(@PathVariable(name = "token") String token) {
        boolean ret = false;

        ret = WebAuthManager.verifyAuthToken(token);

        JSONObject result = new JSONObject();

        try {
            if ( ret == false ) {
                result.put("result", "fail");
                return result.toString();
            }

            result.put("result", "success");
            result.put("log", WebChargerInfo.getRecentSysLog());
        }
        catch(Exception e) {}

        return result.toString();
    }

    @GetMapping(path = "/getrecentcommlog/{token}")
    String get_recentcommlog(@PathVariable(name = "token") String token) {
        boolean ret = false;

        ret = WebAuthManager.verifyAuthToken(token);

        JSONObject result = new JSONObject();

        try {
            if ( ret == false ) {
                result.put("result", "fail");
                return result.toString();
            }

            result.put("result", "success");
            result.put("log", WebChargerInfo.getRecentCommLog());
        }
        catch(Exception e) {}

        return result.toString();
    }

    @GetMapping(path = "/getcommlogfiles/{token}")
    String get_commlogfiles(@PathVariable(name = "token") String token) {
        boolean ret = false;

        ret = WebAuthManager.verifyAuthToken(token);

        JSONObject result = new JSONObject();

        try {
            if ( ret == false ) {
                result.put("result", "fail");
                return result.toString();
            }

            result.put("result", "success");
            result.put("files", WebChargerInfo.getCommLogFiles());
        }
        catch(Exception e) {}

        return result.toString();
    }

    @GetMapping(path = "/getcharginglogfiles/{token}")
    String get_charginglogfiles(@PathVariable(name = "token") String token) {
        boolean ret = false;

        ret = WebAuthManager.verifyAuthToken(token);

        JSONObject result = new JSONObject();

        try {
            if ( ret == false ) {
                result.put("result", "fail");
                return result.toString();
            }

            result.put("result", "success");
            result.put("files", WebChargerInfo.getChargingLogFiles());
        }
        catch(Exception e) {}

        return result.toString();
    }

    @GetMapping(path = "/getcommlogfiledown/{token}/{filename}", produces = MediaType.TEXT_PLAIN_VALUE)
    String get_commlogfiledown(@PathVariable(name = "token") String token, @PathVariable(name = "filename") String filename) {
        String content = "Not Found File";
        try {
            content = FileUtil.getStringFromFile(Environment.getExternalStorageDirectory() + TypeDefine.KEVCS_COMM_PATH+"/Log/" + filename);
        }
        catch (Exception e) {
            content = "Exception :"+e.toString();
        }
        return content;
    }

    @GetMapping(path = "/getcharginglogfiledown/{token}/{filename}", produces = MediaType.TEXT_PLAIN_VALUE)
    String get_charginglogfiledown(@PathVariable(name = "token") String token, @PathVariable(name = "filename") String filename) {
        String content = "Not Found File";
        try {
            content = FileUtil.getStringFromFile(Environment.getExternalStorageDirectory() + TypeDefine.SYSLOG_PATH+"/" + filename);
        }
        catch (Exception e) {
            content = "Exception :"+e.toString();
        }
        return content;
    }


    // For Test
    @PostMapping(path = "/post", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    String post(HttpRequest request, HttpResponse response) {
        String ret = "";
        try {
            ret = request.getBody().string();
        }catch(Exception e){}
        return ret;
    }

    // For Test
    @GetMapping(path = "/get", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    String get(HttpRequest request, HttpResponse response) {
        String ret = "";
        UIFlowManager flowManager = KevcsCharLCDUIActivity.getMainActivity().getFlowManager();

        return ret;
    }
}