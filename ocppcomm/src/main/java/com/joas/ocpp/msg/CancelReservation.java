/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 6. 29 오전 10:45
 */

package com.joas.ocpp.msg;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * CancelReservationRequest
 * <p>
 * 
 * 
 */
public class CancelReservation {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("reservationId")
    @Expose
    private Integer reservationId;

    /**
     * 
     * (Required)
     * 
     */
    public Integer getReservationId() {
        return reservationId;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setReservationId(Integer reservationId) {
        this.reservationId = reservationId;
    }

}
