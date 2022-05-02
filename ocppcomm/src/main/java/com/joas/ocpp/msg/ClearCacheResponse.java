
package com.joas.ocpp.msg;

import java.util.HashMap;
import java.util.Map;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * ClearCacheResponse
 * <p>
 * 
 * 
 */
public class ClearCacheResponse {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("status")
    @Expose
    private ClearCacheResponse.Status status;

    /**
     * 
     * (Required)
     * 
     */
    public ClearCacheResponse.Status getStatus() {
        return status;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setStatus(ClearCacheResponse.Status status) {
        this.status = status;
    }

    public enum Status {

        @SerializedName("Accepted")
        ACCEPTED("Accepted"),
        @SerializedName("Rejected")
        REJECTED("Rejected");
        private final String value;
        private final static Map<String, ClearCacheResponse.Status> CONSTANTS = new HashMap<String, ClearCacheResponse.Status>();

        static {
            for (ClearCacheResponse.Status c: values()) {
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

        public static ClearCacheResponse.Status fromValue(String value) {
            ClearCacheResponse.Status constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
