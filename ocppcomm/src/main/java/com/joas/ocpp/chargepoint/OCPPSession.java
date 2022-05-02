/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 6. 29 오전 10:42
 */

package com.joas.ocpp.chargepoint;

import android.util.Log;

import com.joas.ocpp.msg.AuthorizeResponse;
import com.joas.ocpp.msg.ChargingProfile;
import com.joas.ocpp.msg.IdTagInfo;
import com.joas.ocpp.msg.SampledValue;
import com.joas.ocpp.msg.StopTransaction;
import com.joas.utils.LogWrapper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class OCPPSession {
    private static final String TAG = "OCPPSession";
    public static final int RESERVE_ID_NONE = -1;
    public enum SessionState {
        NONE,
        AUTH_START,
        AUTH_WAIT,
        READY,
        CHARGING,
        FINISHED
    };

    private int connectorId = -1;
    private int curTransactionId = -1;
    private boolean isAuthed = false;
    private String authTag = "";
    private String parentTag = "";
    private String lastParentTag = "";
    private String lastAuthTag = "";
    private SessionState sessionState = SessionState.NONE;

    private OCPPSessionManager manager = null;
    private Calendar chargingStartTime = null;
    private Object lockTransactionStartComm = new Object();

    public OCPPSession(int _connectorId, OCPPSessionManager _manager) {
        this.connectorId = _connectorId;
        this.manager = _manager;
    }

    private void setState(SessionState newState) {
        sessionState = newState;

        if ( manager.getListener() != null ) manager.getListener().onChangeState(connectorId, sessionState);
        Log.v(TAG, "setSessionState("+connectorId+"):"+newState.name());
    }

    public SessionState getState() {
        return sessionState;
    }

    public Calendar getChargingStartTime() { return chargingStartTime; }
    public int getCurTransactionId() { return curTransactionId; }
    public String getParentTag(){return parentTag;}

    private void initAuth() {
        isAuthed = false;
        authTag = "";
    }

    public void authorizeRequest(String idTag) {
        if(sessionState == SessionState.CHARGING){

        }
        else{
            if ( sessionState != OCPPSession.SessionState.AUTH_START ) {
                LogWrapper.e(TAG, "closeSession:Wrong State("+sessionState.name()+")!!");
                return;
            }
            setState(SessionState.AUTH_WAIT);
        }


        authTag = idTag;

        manager.authorizeRequest(idTag);
    }

    public void onAuthorizeResponse(AuthorizeResponse response) {
        if ( response.getIdTagInfo().getStatus() == IdTagInfo.Status.ACCEPTED ) {
            if(sessionState == SessionState.CHARGING){
                parentTag = response.getIdTagInfo().getParentIdTag();
                if(parentTag!=null){
                    if(parentTag.equals(lastParentTag)){
                        if ( manager.getListener() != null ) manager.getListener().onAuthSuccess(connectorId);
                    }
                    else{
                        if ( manager.getListener() != null ) manager.getListener().onAuthFailed(connectorId);
                    }
                }
                else{
                    if(authTag.equals(lastAuthTag)){
                        if ( manager.getListener() != null ) manager.getListener().onAuthSuccess(connectorId);
                    }
                    else {
                        if (manager.getListener() != null)
                            manager.getListener().onAuthFailed(connectorId);
                    }
                }
            }
            else{
                parentTag = response.getIdTagInfo().getParentIdTag();
                lastParentTag = parentTag;
                lastAuthTag = authTag;
                isAuthed = true;
                setState(OCPPSession.SessionState.READY);
                if ( manager.getListener() != null ) manager.getListener().onAuthSuccess(connectorId);
            }
        } else {
            authTag = "";
            if ( manager.getListener() != null ) manager.getListener().onAuthFailed(connectorId);
        }
    }


    public void startSession() {
        setState(SessionState.AUTH_START);
        initAuth();
    }

    public void closeSession() {
        initAuth();
        setState(SessionState.NONE);
    }

    public void startCharging(int meterStart) {
        startCharging(meterStart, RESERVE_ID_NONE);
    }

    public void startCharging(int meterStart, int reservedId) {
        if ( sessionState == SessionState.CHARGING || sessionState == SessionState.FINISHED) {
            LogWrapper.e(TAG, "startCharging:Wrong State("+sessionState.name()+")!!");
            return;
        }

        curTransactionId = -1;

        chargingStartTime = Calendar.getInstance();

        // TxProfile을 초기화 한다.
        manager.getSmartChargerManager().clearTxProfile(connectorId);

        manager.getOcppStack().sendStartTransactionRequest(connectorId, authTag, meterStart , reservedId, chargingStartTime);

        setState(SessionState.CHARGING);
    }

    public void stopCharging(int meterStop, StopTransaction.Reason reason) {
        if ( sessionState != SessionState.CHARGING ) {
            LogWrapper.e(TAG, "stopCharging:Wrong State("+sessionState.name()+")!!");
            return;
        }

        // TxProfile을 초기화 한다.
        manager.getSmartChargerManager().clearTxProfile(connectorId);

        manager.getOcppStack().sendStoptTransactionRequest(connectorId, authTag, meterStop, reason, chargingStartTime, curTransactionId);

        setState(SessionState.FINISHED);
        curTransactionId = -1;
    }

    public void sendMeterValueRequest(int connectorId, long meterVal, int meterValInterval, int current, int curPower, int curOffered, int powerOffered, int soc, SampledValue.Context context, String measurand, boolean isInTransaction) {
        synchronized (lockTransactionStartComm) {
            manager.getOcppStack().sendMeterValueRequest(connectorId, meterVal, meterValInterval, current, curPower, curOffered, powerOffered, soc, context, measurand, isInTransaction, isInTransaction ? chargingStartTime : null, curTransactionId);

        }
    }

    public boolean onRemoteStartTransaction(int connectorId, String idTag, ChargingProfile chargingProfile) {
        boolean ret = false;
        if ( sessionState == SessionState.NONE || sessionState == SessionState.AUTH_START ) {
            isAuthed = true;
            authTag = idTag;
            setState(OCPPSession.SessionState.READY);
            if (manager.getListener() != null)
                ret =  manager.getListener().onRemoteStartTransaction(connectorId, idTag, chargingProfile);
        }
        return ret;
    }

    public void onStartTransactionResult(int connectorId, IdTagInfo tagInfo, String startTime, int transactionId) {
        synchronized (lockTransactionStartComm) {
            SimpleDateFormat formatter =
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

            String strStartTime = formatter.format(chargingStartTime.getTime());

            // 상태와 시간이 틀린경우에는 UI로 넘기지 않음
            if (sessionState != SessionState.CHARGING || strStartTime.equals(startTime) == false)
                return;

            // 시간이 같으현 현재 세션정보임 transactionId를 저장함
            curTransactionId = transactionId;

            lastParentTag = tagInfo.getParentIdTag();

            if (manager.getListener() != null)
                manager.getListener().onStartTransactionResult(connectorId, tagInfo);
        }
    }

    public boolean checkAuthTag(String tag) {
        return authTag.equals(tag);
    }
    public void setAuthTag(String tag){this.authTag = tag;}
}
