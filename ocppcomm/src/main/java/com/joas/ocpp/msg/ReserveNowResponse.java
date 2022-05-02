
package com.joas.ocpp.msg;

import java.util.HashMap;
import java.util.Map;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * ReserveNowResponse
 * <p>
 * 
 * 
 */
public class ReserveNowResponse {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("status")
    @Expose
    private ReserveNowResponse.Status status;

    /**
     * 
     * (Required)
     * 
     */
    public ReserveNowResponse.Status getStatus() {
        return status;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setStatus(ReserveNowResponse.Status status) {
        this.status = status;
    }

    public enum Status {

        @SerializedName("Accepted")
        ACCEPTED("Accepted"),
        @SerializedName("Faulted")
        FAULTED("Faulted"),
        @SerializedName("Occupied")
        OCCUPIED("Occupied"),
        @SerializedName("Rejected")
        REJECTED("Rejected"),
        @SerializedName("Unavailable")
        UNAVAILABLE("Unavailable");
        private final String value;
        private final static Map<String, ReserveNowResponse.Status> CONSTANTS = new HashMap<String, ReserveNowResponse.Status>();

        static {
            for (ReserveNowResponse.Status c: values()) {
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

        public static ReserveNowResponse.Status fromValue(String value) {
            ReserveNowResponse.Status constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
