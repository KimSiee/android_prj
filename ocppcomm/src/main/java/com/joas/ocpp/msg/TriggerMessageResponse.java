
package com.joas.ocpp.msg;

import java.util.HashMap;
import java.util.Map;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * TriggerMessageResponse
 * <p>
 * 
 * 
 */
public class TriggerMessageResponse {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("status")
    @Expose
    private TriggerMessageResponse.Status status;

    /**
     * 
     * (Required)
     * 
     */
    public TriggerMessageResponse.Status getStatus() {
        return status;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setStatus(TriggerMessageResponse.Status status) {
        this.status = status;
    }

    public enum Status {

        @SerializedName("Accepted")
        ACCEPTED("Accepted"),
        @SerializedName("Rejected")
        REJECTED("Rejected"),
        @SerializedName("NotImplemented")
        NOT_IMPLEMENTED("NotImplemented");
        private final String value;
        private final static Map<String, TriggerMessageResponse.Status> CONSTANTS = new HashMap<String, TriggerMessageResponse.Status>();

        static {
            for (TriggerMessageResponse.Status c: values()) {
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

        public static TriggerMessageResponse.Status fromValue(String value) {
            TriggerMessageResponse.Status constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
