/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 5. 11 오후 1:23
 *
 */

package com.joas.minsu_ui_charlcd;

import com.joas.ocpp.msg.StatusNotification;

import java.util.Date;

public class ChargeData {
    public int dspChannel = com.joas.minsu_ui_charlcd.TypeDefine.DEFAULT_CHANNEL;

    /**
     * 동시에 사용 가능한 총 커넥처(채널) 수
     */
    public int ocppConnectorCnt = com.joas.minsu_ui_charlcd.TypeDefine.OCPP_CONNECTOR_CNT_1CH_3MODE;

    /**
     * 커넥터 종류, UI에서 사용자 선택에 의해서 바뀜
     */
    public com.joas.minsu_ui_charlcd.TypeDefine.ConnectorType connectorType = com.joas.minsu_ui_charlcd.TypeDefine.ConnectorType.AC3;

    /**
     * 현재 커넥터 번호(현재 채널), 1개인 경우에는 항상 1이 됨
     */
    public int curConnectorId = com.joas.minsu_ui_charlcd.TypeDefine.OCPP_DEFAULT_CONNECTOR_ID ;

    /**
     * UI에서 인증 타임아웃 시간, 기본 60초
     */
    public int authTimeout = com.joas.minsu_ui_charlcd.TypeDefine.DEFAULT_AUTH_TIMEOUT;
    public int connectCarTimeout = com.joas.minsu_ui_charlcd.TypeDefine.DEFAULT_CONNECT_CAR_TIMEOUT ;

    // 충전진행시 필요로 하는 공유 변수들(충전량, 시간등)

    public long measureWh  = 0;
    public Date chargeStartTime = new Date();
    public Date chargeEndTime = new Date();
    public long chargingTime = 0;
    public double chargingCost = 0;
    public int chargingUnitCost = com.joas.minsu_ui_charlcd.TypeDefine.DEFAULT_UNIT_COST;
    public int soc = 0;
    public int remainTime = 0; // 남은시간
    //==============================
    // OCPP 통신에 필요한 값들
    //=============================

    // 현재 커넥터에 대한 상태값
    StatusNotification.Status ocppStatus = StatusNotification.Status.AVAILABLE;
    StatusNotification.ErrorCode ocppStatusError = StatusNotification.ErrorCode.NO_ERROR;

    /**
     * 메시지 박스 타이틀
     */
    public String messageBoxTitle = "";

    /**
     * 메시지 박스 내용(멀티라인)
     */
    public String messageBoxContent = "";

    /**
     * 메시지 박스 시간(초)
     */
    public int messageBoxTimeout = com.joas.minsu_ui_charlcd.TypeDefine.MESSAGEBOX_TIMEOUT_SHORT;

    public String faultBoxContent = "";
}
