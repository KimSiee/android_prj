/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 19. 3. 4 오전 10:03
 *
 */

package com.joas.ocpp.chargepoint;

import com.google.gson.Gson;
import com.joas.ocpp.msg.ChargingSchedulePeriod;
import com.joas.ocpp.msg.ClearChargingProfile;
import com.joas.ocpp.msg.CsChargingProfiles;
import com.joas.utils.FileUtil;
import com.joas.utils.LogWrapper;
import com.joas.utils.TimeoutHandler;
import com.joas.utils.TimeoutTimer;

import java.io.File;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

/**
 * Created by user on 2019-03-04.
 */

public class SmartChargerManager {
    private static final String TAG = "SmartChargerManager";
    private static final String PROFILE_PATH = "/ChargingProfile/";

    TimeoutTimer checkScheduleTimer;

    OCPPSession[] ocppSessions;

    CsChargingProfiles[] chargePointMaxProfile = null;
    CsChargingProfiles[][] txDefaultProfiles;
    CsChargingProfiles[][] txProfiles;

    double[] powerLimit;
    String profilePath;

    int maxConnector = 0;
    int maxStackLevel = 0;

    Object lockPowerLimit = new Object();

    public SmartChargerManager(int maxConnector, int maxStackLevel, OCPPSession[] sessions, String baseDir) {
        this.maxConnector= maxConnector;
        this.maxStackLevel = maxStackLevel;
        this.ocppSessions = sessions;
        this.txDefaultProfiles = new CsChargingProfiles[maxConnector+1][maxStackLevel+1];
        this.txProfiles = new CsChargingProfiles[maxConnector+1][maxStackLevel+1];
        this.powerLimit = new double[maxConnector+1];
        this.chargePointMaxProfile = new CsChargingProfiles[maxStackLevel+1];

        for (int i=0; i<maxConnector+1; i++ ) {
            this.txDefaultProfiles[i]= new CsChargingProfiles[maxStackLevel+1];
            this.txProfiles[i]= new CsChargingProfiles[maxStackLevel+1];
            this.powerLimit[i] = -1.0d;
        }
        profilePath = baseDir + PROFILE_PATH;

        loadProfile();

        checkScheduleTimer = new TimeoutTimer(5000, new TimeoutHandler() {
            public void run() {
                checkSmartChargerSchedule();
            }
        });
        checkScheduleTimer.beginPeriod();
    }

    public void closeManager() {
        checkScheduleTimer.end();
    }

    public double getPowerLimit(int connectorId) {
        return powerLimit[connectorId];
    }

    /**
     * 해당 커넥터의 모든 Tx Profile을 삭제한다.
     * @param connectorId
     */
    public void clearTxProfile(int connectorId) {
        synchronized (lockPowerLimit) {
            try {
                for (int i = 0; i < maxStackLevel + 1; i++) {
                    if (txProfiles[connectorId][i] != null) {
                            txProfiles[connectorId][i] = null;
                    }
                }
            }
            catch(Exception e) {

            }
        }
    }

    public boolean clearChargingProfile(ClearChargingProfile profile) {
        boolean ret = false;

        synchronized (lockPowerLimit) {
            for (int connector = 0; connector < maxConnector + 1; connector++) {
                for (int i = 0; i < maxStackLevel + 1; i++) {
                    if ( profile.getId() != null ) {
                        if (connector == 0) {
                            // ID를 검사하여 삭제
                            if ( chargePointMaxProfile[i] != null ) {
                                if (chargePointMaxProfile[i].getChargingProfileId() == profile.getId()) {
                                    removeProfile(connector, chargePointMaxProfile[i]);
                                    chargePointMaxProfile[i] = null;
                                    ret = true;
                                }
                            }
                        }

                        // ID를 검사하여 삭제
                        if ( txDefaultProfiles[connector][i] != null ) {
                            if (txDefaultProfiles[connector][i].getChargingProfileId() == profile.getId()) {
                                removeProfile(connector, txDefaultProfiles[connector][i]);
                                txDefaultProfiles[connector][i] = null;
                                ret = true;
                            }
                        }
                    }
                    else {
                        // 필터링
                        boolean isRemoveNeed = true;
                        boolean isMaxRemoveNeed = true;

                        // Connector ID 필터링
                        if ( profile.getConnectorId() != null ) {
                            if ( profile.getConnectorId() != connector ) isRemoveNeed = false;
                        }

                        //Purpose 필터링
                        if ( profile.getChargingProfilePurpose() != null ) {
                            if (connector == 0) {
                                if ( chargePointMaxProfile[i] != null) {
                                    if (chargePointMaxProfile[i].getChargingProfilePurpose().value() != profile.getChargingProfilePurpose().value()) {
                                        isMaxRemoveNeed = false;
                                    }
                                }
                            }
                            if ( txDefaultProfiles[connector][i] != null ) {
                                if (txDefaultProfiles[connector][i].getChargingProfilePurpose().value() != profile.getChargingProfilePurpose().value()) {
                                    isRemoveNeed = false;
                                }
                            }
                        }

                        //StackLevel 필터링
                        if (profile.getStackLevel() != null) {
                            if ( profile.getStackLevel() != i ) {
                                isMaxRemoveNeed = false;
                                isRemoveNeed = false;
                            }
                        }

                        if ( connector == 0 ) {
                            if (isMaxRemoveNeed) {
                                if (chargePointMaxProfile[i] != null) {
                                    removeProfile(connector, chargePointMaxProfile[i]);
                                    chargePointMaxProfile[i] = null;
                                    ret = true;
                                }
                            }
                        }

                        if ( isRemoveNeed ) {
                            if ( txDefaultProfiles[connector][i] != null ) {
                                removeProfile(connector, txDefaultProfiles[connector][i]);
                                txDefaultProfiles[connector][i] = null;
                                ret = true;
                            }
                        }
                    }
                }
            }
        }
        return ret;
    }

    public boolean setProfile(int connectorid, CsChargingProfiles profile) {
        return setProfile(connectorid, profile, true);
    }

    /**
     * 충전 Profile을 지정한다.
     * @param connectorid
     * @param profile
     * @param isSave
     * @return
     */
    public boolean setProfile(int connectorid, CsChargingProfiles profile, boolean isSave) {
        //DB에 저장
        if ( profile.getChargingProfilePurpose() == CsChargingProfiles.ChargingProfilePurpose.TX_DEFAULT_PROFILE) {
            if ( profile.getStackLevel() >= txDefaultProfiles[connectorid].length ) return false;
            if ( connectorid == 0 ) {
                for ( int i=0; i<maxConnector+1; i++) {
                    txDefaultProfiles[i][profile.getStackLevel()] = profile;
                }
            }
            else {
                txDefaultProfiles[connectorid][profile.getStackLevel()] = profile;
            }
        }
        else if ( profile.getChargingProfilePurpose() == CsChargingProfiles.ChargingProfilePurpose.TX_PROFILE) {
            //  커넥터아이디가 0보다 크고 충전중이라면 세팅함
            if ( connectorid > 0 && ocppSessions[connectorid].getState() == OCPPSession.SessionState.CHARGING) {
                txProfiles[connectorid][profile.getStackLevel()] = profile;
                return true;
            }
            else return false;
        }
        else if (profile.getChargingProfilePurpose() == CsChargingProfiles.ChargingProfilePurpose.CHARGE_POINT_MAX_PROFILE) {
            //Max Profile인 경우에는 ConnectorId가 0이므로 저장하고 바로 종료함
            chargePointMaxProfile[profile.getStackLevel()] = profile;
            if ( isSave ) saveProfile(connectorid, profile);
            return true;
        }
        else {
            return false;
        }

        if ( isSave ) {
            saveProfile(connectorid, profile);
        }
        return true;
    }

    /**
     *  해당되는 connectorId의 Profile을 Stack별로 저장한다.
      * @param connectorid
     * @param profile
     */
    void saveProfile(int connectorid, CsChargingProfiles profile) {
        Gson gson = new Gson();
        String str = gson.toJson(profile, CsChargingProfiles.class);

        String filename = profile.getChargingProfilePurpose().value()+"_"+profile.getStackLevel()+".txt";
        FileUtil.stringToFile(profilePath+connectorid+"/", filename, str, false);
    }

    /**
     * 모든 Connector에 대해서 Profile을 읽어온다.
     */
    void loadProfile() {
        Gson gson = new Gson();
        String filename;
        for (int connector = 0; connector< maxConnector + 1; connector++) {
            for (int i = 0; i < maxStackLevel+1; i++) {
                if ( connector == 0 ) {
                    filename = profilePath + connector + "/" + CsChargingProfiles.ChargingProfilePurpose.CHARGE_POINT_MAX_PROFILE + "_" + i + ".txt";
                    fileStrToProfile(connector, gson, filename);
                }

                filename = profilePath+connector+"/"+ CsChargingProfiles.ChargingProfilePurpose.TX_DEFAULT_PROFILE + "_" + i+".txt";
                fileStrToProfile(connector, gson, filename);
            }
        }
    }

    /**
     * Profile을 삭제한다.
     */
    void removeProfile(int connectorId, CsChargingProfiles profile) {
        try {
            String filename = profilePath + connectorId + "/" + profile.getChargingProfilePurpose().value() + "_" + profile.getStackLevel() + ".txt";
            File file = new File(filename);
            file.delete();
        }catch(Exception e) {
            LogWrapper.e(TAG, "removeProfile:"+e.toString());
        }
    }

    /**
     * 해당 파일을 읽어서 프로파일을 세팅한다.
     * @param connectorId
     * @param gson
     * @param filename
     */
    void fileStrToProfile(int connectorId, Gson gson, String filename) {
        try {
            String str = FileUtil.getStringFromFile(filename);
            CsChargingProfiles profiles = gson.fromJson(str, CsChargingProfiles.class);
            setProfile(connectorId, profiles, false);
        }catch(Exception ex) {
            // Not Found
        }
    }

    public double getCurPowerLimit(int connectorId) {
        double limit = 0;
        synchronized (lockPowerLimit) {
            limit = powerLimit[connectorId];
        }
        return limit;
    }

    /**
     * 모든 커넥터에 대해서 충전기 스케줄을 체크하고 전력제한을 검사한다.
     */
    public void checkSmartChargerSchedule() {
        synchronized (lockPowerLimit) {
            try {
                //개별 커넥터에 대한 스케줄 검사를 실시한다.
                for (int i = 1; i < maxConnector + 1; i++) {
                    checkProfileList(i);
                }

                // 총 전력량 제한(모든 커넥터합)을 검사한다.
                // maxProfile에 적용되는지 검사한다.
                if (chargePointMaxProfile == null) return;
                // 3. 높은 순서대로 Stack을 검사하여 있는경우 해당 스케줄 정보를 검사한다.
                for (int i = maxStackLevel; i >= 0; i--) {
                    if (chargePointMaxProfile[i] != null) {
                        if (checkPropfile(0, chargePointMaxProfile[i])) break;
                    }
                }

                // 총 전력량 제한값이 있다면 현재 충전 세션의 모든 커넥터의 전력 제한을 재계산한다.
                double sumPowerLimit = 0;
                for (int i = 1; i < maxConnector + 1; i++) {
                    if (ocppSessions[i].getState() == OCPPSession.SessionState.CHARGING) {
                        sumPowerLimit += powerLimit[i];
                    }
                }

                // 만약 전력제어의 합이 총합보다 높다면 각 충전량을 비율만큼 내린다.
                if (powerLimit[0] > 0 && sumPowerLimit > powerLimit[0]) {
                    double ratio = powerLimit[0] / sumPowerLimit;
                    for (int i = 1; i < maxConnector + 1; i++) {
                        if (ocppSessions[i].getState() == OCPPSession.SessionState.CHARGING) {
                            powerLimit[i] = powerLimit[i] * ratio;
                        }
                    }
                }
            }
            catch(Exception e) {
                LogWrapper.e(TAG, "checkSmartChargerSchedule:"+e.toString()+","+e.getStackTrace().toString());
            }
        }
    }

    /**
     * 해당 커넥터에 대해서 충전 스케줄을 검사한다.
     * @param connectorId
     */
    public void checkProfileList(int connectorId) {
        // 1. 충전중이 아니면 검사하지 않음
        if ( ocppSessions[connectorId].getState() != OCPPSession.SessionState.CHARGING ) return;

        // 2. 높은 순서대로 TxProfile Stack을 검사하여 있는경우 해당 스케줄 정보를 검사한다.
        for (int i=maxStackLevel; i>=0; i--) {
            if ( txProfiles[connectorId][i] != null ) {
                if ( checkPropfile(connectorId, txProfiles[connectorId][i]) ) return;
            }
        }

        // 3. 높은 순서대로 Stack을 검사하여 있는경우 해당 스케줄 정보를 검사한다.
        for (int i=maxStackLevel; i>=0; i--) {
            if ( txDefaultProfiles[connectorId][i] != null ) {
                if ( checkPropfile(connectorId, txDefaultProfiles[connectorId][i]) ) return;
            }
        }
    }

    /**
     * 프로파일을 분석하여 충전 스케줄 제어
     * @param connectorId
     * @param profile
     * @return
     */
    private boolean checkPropfile(int connectorId, CsChargingProfiles profile) {
        boolean ret = false;

        Calendar curTime = Calendar.getInstance();

        // 1. Optional Packet ValidFrom/To Check
        // ValidFrom / To 를 검사하여 지정된 날짜가 아니면 false를 리턴한다.
        Calendar dateFrom = profile.getValidFrom();
        if ( dateFrom != null ) {
            if ( dateFrom.compareTo(curTime) > 0 ) return false;
        }

        Calendar dateTo = profile.getValidTo();
        if ( dateTo  != null ) {
            if ( dateTo.compareTo(curTime) < 0 ) return  false;
        }

        //2. Profile kind Check
        switch (profile.getChargingProfileKind()) {
            case ABSOLUTE:
                ret = checkAbsoluteSchedule(connectorId, profile, curTime);
                break;
            case RECURRING:
                ret = checkRecurringSchdule(connectorId, profile, curTime);
                break;
            case RELATIVE:
                ret = checkRelativeSchedule(connectorId, profile, curTime);
                break;
        }

        // TODO minChargingRate 의미 및 구현??

        return ret;
    }

    /**
     * 정해진 충전 스케줄에 의한 충전 제어
     * @param connectorId
     * @param profile
     * @param curTime
     * @return
     */
    private boolean checkAbsoluteSchedule(int connectorId, CsChargingProfiles profile, Calendar curTime) {
        boolean ret = false;

        try {
            // Absolute인데 만약 startSchedule가 없으면 false를 리턴한다.
            Calendar startSchedule = profile.getChargingSchedule().getStartSchedule();
            if (startSchedule == null) return false;

            // 시작 스케줄이 현재 시각 이전이면 false를 리턴한다.
            if (startSchedule.compareTo(curTime) > 0) return false;

            // Duration 체크
            if (profile.getChargingSchedule().getDuration() != null) {
                Calendar endSchedule = (Calendar) startSchedule.clone();
                endSchedule.add(Calendar.SECOND, profile.getChargingSchedule().getDuration());

                // 끝 스케줄이 현재 시각 이후이면 false를 리턴한다.
                if (endSchedule.compareTo(curTime) < 0) return false;
            }

            //Schedule 체크
            double limit = checkChargingSchedulePeriod(startSchedule, curTime, profile.getChargingSchedule().getChargingSchedulePeriod());

            if ( limit > 0 ) {
                powerLimit[connectorId] = limit;
                ret = true;
            }

        }
        catch (Exception e) {
            LogWrapper.e(TAG, "checkAbsoluteSchedule:"+e.toString());
        }

        return ret;
    }

    /**
     * 매일 혹은 매주 반복되는 충전 스케줄 제어
     * @param connectorId
     * @param profile
     * @param curTime
     * @return
     */
    private boolean checkRecurringSchdule(int connectorId, CsChargingProfiles profile, Calendar curTime) {
        boolean ret = false;
        try {
            // 만약 startSchedule가 없으면 충전 시작 시간이 시작 스케줄이다.
            Calendar startSchedule = profile.getChargingSchedule().getStartSchedule();
            if (startSchedule == null) {
                startSchedule = ocppSessions[connectorId].getChargingStartTime();
            }
            else {
                // 시작 스케줄이 현재 시각 이전이면 false를 리턴한다.
                if (startSchedule.compareTo(curTime) > 0) return false;
            }


            // Recurring 체크
            // 만약 Daily이면 시작시간을 해당 날로 재조정한다.
            if ( profile.getRecurrencyKind() == CsChargingProfiles.RecurrencyKind.DAILY) {
                startSchedule.set(Calendar.DAY_OF_MONTH, curTime.get(Calendar.DAY_OF_MONTH));
            }
            // 만약 Week이면 시작날짜와 비교하여 주단위 날짜로 변환한다.
            else if (profile.getRecurrencyKind() == CsChargingProfiles.RecurrencyKind.WEEKLY) {

                long diff = startSchedule.getTimeInMillis() - curTime.getTimeInMillis();
                // 일주일 이상 차이가 난다면 7일의 배수만큼 더해야함
                if ( diff > (1000*60*60*24*7) ) {
                    int mod7 = (int)(diff/(1000*60*60*24*7));
                    startSchedule.add(Calendar.DATE, mod7*7);
                }
            }
            else {
                return false;
            }

            // 재조정된 시작 스케줄이 현재 시각 이전이면 false를 리턴한다.
            if (startSchedule.compareTo(curTime) > 0) return false;

            // Duration 체크
            if (profile.getChargingSchedule().getDuration() != null) {
                Calendar endSchedule = (Calendar) startSchedule.clone();
                endSchedule.add(Calendar.SECOND, profile.getChargingSchedule().getDuration());

                // 끝 스케줄이 현재 시각 이후이면 false를 리턴한다.
                if (endSchedule.compareTo(curTime) < 0) return false;
            }

            //Schedule 체크
            double limit = checkChargingSchedulePeriod(startSchedule, curTime, profile.getChargingSchedule().getChargingSchedulePeriod());

            if ( limit > 0 ) {
                powerLimit[connectorId] = limit;
                ret = true;
            }

        }
        catch (Exception e) {
            LogWrapper.e(TAG, "checkAbsoluteSchedule:"+e.toString());
        }

        return ret;
    }

    /**
     * 차량 충전 시작 시간과 연개한 충전 스케줄 제어
     * @param connectorId
     * @param profile
     * @param curTime
     * @return
     */
    private boolean checkRelativeSchedule(int connectorId, CsChargingProfiles profile, Calendar curTime) {
        boolean ret = false;
        try {
            // 만약 startSchedule가 없으면 충전 시작 시간이 시작 스케줄이다.
            Calendar startSchedule = profile.getChargingSchedule().getStartSchedule();
            if (startSchedule == null) {
                startSchedule = ocppSessions[connectorId].getChargingStartTime();
            }
            else {
                // 시작 스케줄이 현재 시각 이전이면 false를 리턴한다.
                if (startSchedule.compareTo(curTime) > 0) return false;
            }

            // Duration 체크
            if (profile.getChargingSchedule().getDuration() != null) {
                Calendar endSchedule = (Calendar) startSchedule.clone();
                endSchedule.add(Calendar.SECOND, profile.getChargingSchedule().getDuration());

                // 끝 스케줄이 현재 시각 이후이면 false를 리턴한다.
                if (endSchedule.compareTo(curTime) < 0) return false;
            }

            //Schedule 체크
            double limit = checkChargingSchedulePeriod(startSchedule, curTime, profile.getChargingSchedule().getChargingSchedulePeriod());

            if ( limit > 0 ) {
                powerLimit[connectorId] = limit;
                ret = true;
            }

        }
        catch (Exception e) {
            LogWrapper.e(TAG, "checkAbsoluteSchedule:"+e.toString());
        }

        return ret;
    }

    /**
     * 지정된 시간으로 부터 스케줄 정보를 얻어온다.
     * @param startTime 기준 시작 시간
     * @param curTime 현재 시간
     * @param periods 주기저보들(OCPP)
     * @return 최종 전력 제한값,만약 -1이라면 적용안함
     */
    private double checkChargingSchedulePeriod(Calendar startTime, Calendar curTime, List<ChargingSchedulePeriod> periods) {
        double ret = -1;

        try {
            Iterator<ChargingSchedulePeriod> iterator = periods.iterator();
            while (iterator.hasNext()) {
                ChargingSchedulePeriod period = iterator.next();
                int startOffset = period.getStartPeriod();
                Calendar offsetTime = (Calendar)startTime.clone();
                offsetTime.add(Calendar.SECOND, startOffset);
                if ( offsetTime.compareTo(curTime) < 0 ) {
                    ret = period.getLimit();
                }
            }
        }
        catch (Exception e) {
            LogWrapper.e(TAG, "checkChargingSchedulePeriod:"+e.toString());
        }
        return ret;
    }
}
