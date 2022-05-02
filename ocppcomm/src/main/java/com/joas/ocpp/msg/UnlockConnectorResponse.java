
package com.joas.ocpp.msg;

import java.util.HashMap;
import java.util.Map;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * UnlockConnectorResponse
 * <p>
 * 
 * 
 */
public class UnlockConnectorResponse {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("status")
    @Expose
    private UnlockConnectorResponse.Status status;

    /**
     * 
     * (Required)
     * 
     */
    public UnlockConnectorResponse.Status getStatus() {
        return status;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setStatus(UnlockConnectorResponse.Status status) {
        this.status = status;
    }

    public enum Status {

        @SerializedName("Unlocked")
        UNLOCKED("Unlocked"),
        @SerializedName("UnlockFailed")
        UNLOCK_FAILED("UnlockFailed"),
        @SerializedName("NotSupported")
        NOT_SUPPORTED("NotSupported");
        private final String value;
        private final static Map<String, UnlockConnectorResponse.Status> CONSTANTS = new HashMap<String, UnlockConnectorResponse.Status>();

        static {
            for (UnlockConnectorResponse.Status c: values()) {
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

        public static UnlockConnectorResponse.Status fromValue(String value) {
            UnlockConnectorResponse.Status constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
