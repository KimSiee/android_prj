/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 17. 6. 29 오전 10:45
 */

package com.joas.ocpp.msg;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * BootNotificationResponse
 * <p>
 * 
 * 
 */
public class BootNotificationResponse {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("status")
    @Expose
    private BootNotificationResponse.Status status;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("currentTime")
    @Expose
    private Calendar currentTime;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("interval")
    @Expose
    private Double interval;

    /**
     * 
     * (Required)
     * 
     */
    public BootNotificationResponse.Status getStatus() {
        return status;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setStatus(BootNotificationResponse.Status status) {
        this.status = status;
    }

    /**
     * 
     * (Required)
     * 
     */
    public Calendar getCurrentTime() {
        return currentTime;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setCurrentTime(Calendar currentTime) {
        this.currentTime = currentTime;
    }

    /**
     * 
     * (Required)
     * 
     */
    public Double getInterval() {
        return interval;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setInterval(Double interval) {
        this.interval = interval;
    }

    public enum Status {

        @SerializedName("Accepted")
        ACCEPTED("Accepted"),
        @SerializedName("Pending")
        PENDING("Pending"),
        @SerializedName("Rejected")
        REJECTED("Rejected");
        private final String value;
        private final static Map<String, BootNotificationResponse.Status> CONSTANTS = new HashMap<String, BootNotificationResponse.Status>();

        static {
            for (BootNotificationResponse.Status c: values()) {
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

        public static BootNotificationResponse.Status fromValue(String value) {
            BootNotificationResponse.Status constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
