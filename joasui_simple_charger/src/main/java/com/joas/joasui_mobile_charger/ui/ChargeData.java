/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 6. 21 오후 3:35
 *
 */

package com.joas.joasui_mobile_charger.ui;

import java.util.Date;

public class ChargeData {
    /**
     * 커넥터 종류, UI에서 사용자 선택에 의해서 바뀜
     */
    public TypeDefine.ConnectorType connectorType = TypeDefine.ConnectorType.BTYPE;

    public int chargerType = TypeDefine.CP_TYPE_SLOW_AC;
    public int dspChannel = TypeDefine.DEFAULT_CHANNEL;

    /**
     * UI에서 인증 타임아웃 시간, 기본 60초
     */
    public int authTimeout = TypeDefine.DEFAULT_AUTH_TIMEOUT;
    public int connectCarTimeout = TypeDefine.DEFAULT_CONNECT_CAR_TIMEOUT ;

    // 충전진행시 필요로 하는 공유 변수들(충전량, 시간등)

    public long measureWh  = 0;
    public Date chargeStartTime = new Date();
    public Date chargeEndTime = new Date();
    public long chargingTime = 0;
    public double chargingCost = 0;
    public int chargingUnitCost = TypeDefine.DEFAULT_UNIT_COST;
    public int soc = 0; // 충전량 %
    public int remainTime = 0; // 남은시간

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
    public int messageBoxTimeout = TypeDefine.MESSAGEBOX_TIMEOUT_SHORT;

    public String faultBoxContent = "";
}
