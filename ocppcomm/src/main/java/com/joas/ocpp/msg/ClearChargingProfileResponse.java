
package com.joas.ocpp.msg;

import java.util.HashMap;
import java.util.Map;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * ClearChargingProfileResponse
 * <p>
 * 
 * 
 */
public class ClearChargingProfileResponse {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("status")
    @Expose
    private ClearChargingProfileResponse.Status status;

    /**
     * 
     * (Required)
     * 
     */
    public ClearChargingProfileResponse.Status getStatus() {
        return status;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setStatus(ClearChargingProfileResponse.Status status) {
        this.status = status;
    }

    public enum Status {

        @SerializedName("Accepted")
        ACCEPTED("Accepted"),
        @SerializedName("Unknown")
        UNKNOWN("Unknown");
        private final String value;
        private final static Map<String, ClearChargingProfileResponse.Status> CONSTANTS = new HashMap<String, ClearChargingProfileResponse.Status>();

        static {
            for (ClearChargingProfileResponse.Status c: values()) {
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

        public static ClearChargingProfileResponse.Status fromValue(String value) {
            ClearChargingProfileResponse.Status constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
