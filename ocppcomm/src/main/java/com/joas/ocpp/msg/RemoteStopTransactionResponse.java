
package com.joas.ocpp.msg;

import java.util.HashMap;
import java.util.Map;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * RemoteStopTransactionResponse
 * <p>
 * 
 * 
 */
public class RemoteStopTransactionResponse {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("status")
    @Expose
    private RemoteStopTransactionResponse.Status status;

    /**
     * 
     * (Required)
     * 
     */
    public RemoteStopTransactionResponse.Status getStatus() {
        return status;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setStatus(RemoteStopTransactionResponse.Status status) {
        this.status = status;
    }

    public enum Status {

        @SerializedName("Accepted")
        ACCEPTED("Accepted"),
        @SerializedName("Rejected")
        REJECTED("Rejected");
        private final String value;
        private final static Map<String, RemoteStopTransactionResponse.Status> CONSTANTS = new HashMap<String, RemoteStopTransactionResponse.Status>();

        static {
            for (RemoteStopTransactionResponse.Status c: values()) {
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

        public static RemoteStopTransactionResponse.Status fromValue(String value) {
            RemoteStopTransactionResponse.Status constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
