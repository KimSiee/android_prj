/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 6. 29 오전 10:45
 */

package com.joas.ocpp.msg;

import java.util.HashMap;
import java.util.Map;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * CancelReservationResponse
 * <p>
 * 
 * 
 */
public class CancelReservationResponse {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("status")
    @Expose
    private CancelReservationResponse.Status status;

    /**
     * 
     * (Required)
     * 
     */
    public CancelReservationResponse.Status getStatus() {
        return status;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setStatus(CancelReservationResponse.Status status) {
        this.status = status;
    }

    public enum Status {

        @SerializedName("Accepted")
        ACCEPTED("Accepted"),
        @SerializedName("Rejected")
        REJECTED("Rejected");
        private final String value;
        private final static Map<String, CancelReservationResponse.Status> CONSTANTS = new HashMap<String, CancelReservationResponse.Status>();

        static {
            for (CancelReservationResponse.Status c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private Status(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public String value() {
            return this.value;
        }

        public static CancelReservationResponse.Status fromValue(String value) {
            CancelReservationResponse.Status constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
