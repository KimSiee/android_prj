
package com.joas.ocpp.msg;

import java.util.HashMap;
import java.util.Map;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * ChangeConfigurationResponse
 * <p>
 * 
 * 
 */
public class ChangeConfigurationResponse {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("status")
    @Expose
    private ChangeConfigurationResponse.Status status;

    /**
     * 
     * (Required)
     * 
     */
    public ChangeConfigurationResponse.Status getStatus() {
        return status;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setStatus(ChangeConfigurationResponse.Status status) {
        this.status = status;
    }

    public enum Status {

        @SerializedName("Accepted")
        ACCEPTED("Accepted"),
        @SerializedName("Rejected")
        REJECTED("Rejected"),
        @SerializedName("RebootRequired")
        REBOOT_REQUIRED("RebootRequired"),
        @SerializedName("NotSupported")
        NOT_SUPPORTED("NotSupported");
        private final String value;
        private final static Map<String, ChangeConfigurationResponse.Status> CONSTANTS = new HashMap<String, ChangeConfigurationResponse.Status>();

        static {
            for (ChangeConfigurationResponse.Status c: values()) {
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

        public static ChangeConfigurationResponse.Status fromValue(String value) {
            ChangeConfigurationResponse.Status constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
