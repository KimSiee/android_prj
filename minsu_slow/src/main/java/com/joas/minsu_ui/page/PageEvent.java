/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 20. 2. 26 오후 2:29
 *
 */

package com.joas.minsu_ui.page;

public enum PageEvent {
    GO_HOME,
    GO_PREV_CONNECTOR_WAIT_TO_TAG_CARD,         //전시회용
    GO_PREV_CHARGING_TO_CONNECTOR_WAIT,         //전시회용
    GO_PREV_CHG_COMPLTETE_TO_CHARGING,          //전시회용
    SELECT_AC3_CLICK,
    SELECT_CHADEMO_CLICK,
    SELECT_DCCOMBO_CLICK,
    SELECT_BTYPE_CLICK,
    SELECT_CTYPE_CLICK,
    GO_UNPLUG
}
