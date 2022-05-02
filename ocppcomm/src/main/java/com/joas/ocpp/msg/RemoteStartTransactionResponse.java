
package com.joas.ocpp.msg;

import java.util.HashMap;
import java.util.Map;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * RemoteStartTransactionResponse
 * <p>
 * 
 * 
 */
public class RemoteStartTransactionResponse {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("status")
    @Expose
    private RemoteStartTransactionResponse.Status status;

    /**
     * 
     * (Required)
     * 
     */
    public RemoteStartTransactionResponse.Status getStatus() {
        return status;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setStatus(RemoteStartTransactionResponse.Status status) {
        this.status = status;
    }

    public enum Status {

        @SerializedName("Accepted")
        ACCEPTED("Accepted"),
        @SerializedName("Rejected")
        REJECTED("Rejected");
        private final String value;
        private final static Map<String, RemoteStartTransactionResponse.Status> CONSTANTS = new HashMap<String, RemoteStartTransactionResponse.Status>();

        static {
            for (RemoteStartTransactionResponse.Status c: values()) {
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

        public static RemoteStartTransactionResponse.Status fromValue(String value) {
            RemoteStartTransactionResponse.Status constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
