/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 6. 29 오전 10:42
 */

package com.joas.ocpp.chargepoint;

import com.joas.ocpp.msg.CancelReservationResponse;
import com.joas.ocpp.msg.ChangeAvailability;
import com.joas.ocpp.msg.ChargingProfile;
import com.joas.ocpp.msg.IdTagInfo;
import com.joas.ocpp.msg.ReserveNowResponse;
import com.joas.ocpp.msg.TriggerMessage;

import java.net.URI;
import java.util.Calendar;
import java.util.Date;

public interface OCPPSessionManagerListener {

    public void onAuthSuccess(int connectorId);
    public void onAuthFailed(int connectorId);

    public void onChangeState(int connectorId, OCPPSession.SessionState state);
    public CancelReservationResponse.Status onCancelReservation(int reservationId);

    public void onBootNotificationResponse(boolean success);
    public void onRemoteStopTransaction(int connectorId);
    public boolean onRemoteStartTransaction(int connectorId, String idTag, ChargingProfile chargingProfile);
    public void onStartTransactionResult(int connectorId, IdTagInfo tagInfo);
    public ReserveNowResponse.Status onReserveNow(int connectorId, Calendar expiryDate, String idTag, String parentIdTag, int reservationId);
    public void onTriggerMessage(TriggerMessage message);
    public void onChangeAvailability(int connectorId, ChangeAvailability.Type type);
    public void onResetRequest(boolean isHard);
    public void onUpdateFirmwareRequest(URI location, int retry, Calendar retrieveDate, int retryInterval);

    public void onTimeUpdate(Calendar syncTime);
}
