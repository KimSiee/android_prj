
package com.joas.ocpp.msg;

import java.util.HashMap;
import java.util.Map;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * DataTransferResponse
 * <p>
 * 
 * 
 */
public class DataTransferResponse {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("status")
    @Expose
    private DataTransferResponse.Status status;
    @SerializedName("data")
    @Expose
    private String data;

    /**
     * 
     * (Required)
     * 
     */
    public DataTransferResponse.Status getStatus() {
        return status;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setStatus(DataTransferResponse.Status status) {
        this.status = status;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public enum Status {

        @SerializedName("Accepted")
        ACCEPTED("Accepted"),
        @SerializedName("Rejected")
        REJECTED("Rejected"),
        @SerializedName("UnknownMessageId")
        UNKNOWN_MESSAGE_ID("UnknownMessageId"),
        @SerializedName("UnknownVendorId")
        UNKNOWN_VENDOR_ID("UnknownVendorId");
        private final String value;
        private final static Map<String, DataTransferResponse.Status> CONSTANTS = new HashMap<String, DataTransferResponse.Status>();

        static {
            for (DataTransferResponse.Status c: values()) {
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

        public static DataTransferResponse.Status fromValue(String value) {
            DataTransferResponse.Status constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
