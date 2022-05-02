
package com.joas.ocpp.msg;

import java.util.HashMap;
import java.util.Map;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * SetChargingProfileResponse
 * <p>
 * 
 * 
 */
public class SetChargingProfileResponse {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("status")
    @Expose
    private SetChargingProfileResponse.Status status;

    /**
     * 
     * (Required)
     * 
     */
    public SetChargingProfileResponse.Status getStatus() {
        return status;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setStatus(SetChargingProfileResponse.Status status) {
        this.status = status;
    }

    public enum Status {

        @SerializedName("Accepted")
        ACCEPTED("Accepted"),
        @SerializedName("Rejected")
        REJECTED("Rejected"),
        @SerializedName("NotSupported")
        NOT_SUPPORTED("NotSupported");
        private final String value;
        private final static Map<String, SetChargingProfileResponse.Status> CONSTANTS = new HashMap<String, SetChargingProfileResponse.Status>();

        static {
            for (SetChargingProfileResponse.Status c: values()) {
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

        public static SetChargingProfileResponse.Status fromValue(String value) {
            SetChargingProfileResponse.Status constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
