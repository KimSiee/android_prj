/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 21. 7. 21. 오후 4:36
 *
 */

package com.joas.volvo_touch_2ch;

import android.app.Activity;
import android.os.Environment;

import com.joas.utils.FileUtil;
import com.joas.utils.GetURIFile;
import com.joas.utils.GetURIFileListener;
import com.joas.utils.LogWrapper;
import com.joas.utils.RemoteUpdater;
import com.joas.utils.ZipUtils;

import java.io.File;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

interface UpdateManagerListener {
    public void onUpdateStatus(UpdateManager.UpdateState state);
}

public class UpdateManager implements GetURIFileListener {
    public static final String TAG = "UpdateManager";
    public static final String OLDVERSION_FILENAME = "OldVersion.txt";

    public enum UpdateState {
        None,
        Waiting,
        Retrying,
        Started,
        Finished,
        Error
    };

    String updatePath = Environment.getExternalStorageDirectory()+"/Update";
    String updateFile;

    Timer timerUpdate = null;
    Activity mainActivity;
    URI targetURI;
    int maxRetry = 1;
    int retryPeriod = 60; // sec
    Calendar startTime;
    int curRetry = 0;

    GetURIFile getURIFile;
    UpdateManagerListener listener;

    UpdateState updateState = UpdateState.None;
    public boolean newFirmwareUpdateed = false;

    public UpdateManager(Activity act, UpdateManagerListener updateManagerListener, String version) {
        mainActivity = act;
        listener = updateManagerListener;

        File parent = new File(updatePath);

        if (!parent.exists()) {
            parent.mkdirs();
        }
        updateFile = updatePath + "update.zip";

        try {
            File existFile = new File(updateFile);
            if (existFile.exists()) {
                existFile.delete();
            }
        } catch (Exception e) {
            LogWrapper.e(TAG, "UpdateManager() updateFile delete");
        }

        // Check New Firmware Updated
        try {
            File fileVersion = new File(updatePath + OLDVERSION_FILENAME);
            if (fileVersion.exists()) {
                String str = FileUtil.getStringFromFile(updatePath + OLDVERSION_FILENAME);
                if (str.compareTo(version) != 0) {
                    newFirmwareUpdateed = true;
                }
                else {
                    fileVersion.delete();
                }
            }
        } catch (Exception e) {
            LogWrapper.e(TAG, "UpdateManager() version file load");
        }
    }

    /**
     * 업데이트 완료 메시지를 보낸다음 처리(파일을 지운다.)
     */
    public void onUpdateCompleteMsgSent() {
        newFirmwareUpdateed = false;
        try {
            File fileVersion = new File(updatePath + OLDVERSION_FILENAME);
            fileVersion.delete();
        }
        catch(Exception e) {
            LogWrapper.e(TAG, "UpdateManager() version file delete");
        }
    }

    public void closeManager() {
        try {
            if (getURIFile != null) getURIFile.cancel(true);
        } catch (Exception e) {}

        try {
            if ( timerUpdate != null ) timerUpdate.cancel();
        } catch (Exception e) {}
    }

    void startTimer(Date targetTime) {
        timerUpdate = new Timer();
        timerUpdate.schedule(new TimerTask() {
            @Override
            public void run() {
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateProcess();
                    }
                });
            }
        }, targetTime); // 해당시간에 수행
    }


    void updateProcess() {
        switch ( updateState ) {
            case Waiting:
            case Retrying:
                setState(UpdateState.Started);
                doUpdate();
                break;
        }
    }

    public void setUpdateInfo(URI location, int retry, Calendar retrieveDate, int retryInterval) {
        targetURI = location;
        maxRetry = retry;
        startTime = retrieveDate;
        retryPeriod = retryInterval;

        curRetry = 0;

        setState(UpdateState.Waiting);
        if ( timerUpdate != null ) {
            timerUpdate.cancel();
        }

        startTimer(startTime.getTime());
    }

    public void doUpdate() {
        try {
            if (getURIFile != null) getURIFile.cancel(true);
        } catch (Exception e) {}

        getURIFile = new GetURIFile(updateFile , this);
        getURIFile.execute(targetURI.toString());
    }

    void setState(UpdateState state) {
        updateState = state;
        if ( listener != null) listener.onUpdateStatus(updateState);
    }

    public UpdateState getStatus() {
        return updateState;
    }

    void errorRetry() {
        curRetry++;
        if (curRetry > maxRetry) {
            setState(UpdateState.None);
        }
        else {
            startTime.add(Calendar.SECOND, retryPeriod); // 재시도 시간만큼 더한다.(초)
            startTimer(startTime.getTime());
            setState(UpdateState.Retrying);
        }
    }

    public void doInstallFirmware(String oldVersion) {
        if ( updateState == UpdateState.Finished ) {
            FileUtil.stringToFile(updatePath, OLDVERSION_FILENAME, oldVersion, false);

            RemoteUpdater updater = new RemoteUpdater(mainActivity, updatePath, "update.apk");
            updater.doUpdateFromApk("com.joas.smartcharger");
        }
    }

    @Override
    public void onGetURIFileError(String err) {
        LogWrapper.v(TAG, "Firmware Downloaded Fail");
        setState(UpdateState.Error);
        errorRetry();
    }

    @Override
    public void onGetURIFileFinished() {
        LogWrapper.v(TAG, "onGetURIFileFinished");
        try {
            setState(UpdateState.Finished);

            ZipUtils.unzip(updateFile, updatePath, false);
            // 성공시 처리
            LogWrapper.v(TAG, "Firmware Unzip Successed");

        } catch(Exception e) {
            LogWrapper.v(TAG, "Firmware Unzip Failed");

            setState(UpdateState.Error);
            errorRetry();
        }
    }
}
