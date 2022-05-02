
package com.joas.ocpp.msg;

import java.util.HashMap;
import java.util.Map;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * SendLocalListResponse
 * <p>
 * 
 * 
 */
public class SendLocalListResponse {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("status")
    @Expose
    private SendLocalListResponse.Status status;

    /**
     * 
     * (Required)
     * 
     */
    public SendLocalListResponse.Status getStatus() {
        return status;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setStatus(SendLocalListResponse.Status status) {
        this.status = status;
    }

    public enum Status {

        @SerializedName("Accepted")
        ACCEPTED("Accepted"),
        @SerializedName("Failed")
        FAILED("Failed"),
        @SerializedName("NotSupported")
        NOT_SUPPORTED("NotSupported"),
        @SerializedName("VersionMismatch")
        VERSION_MISMATCH("VersionMismatch");
        private final String value;
        private final static Map<String, SendLocalListResponse.Status> CONSTANTS = new HashMap<String, SendLocalListResponse.Status>();

        static {
            for (SendLocalListResponse.Status c: values()) {
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

        public static SendLocalListResponse.Status fromValue(String value) {
            SendLocalListResponse.Status constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
