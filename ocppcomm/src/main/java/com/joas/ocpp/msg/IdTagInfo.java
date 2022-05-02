
package com.joas.ocpp.msg;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class IdTagInfo {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("status")
    @Expose
    private IdTagInfo.Status status;
    @SerializedName("expiryDate")
    @Expose
    private Calendar expiryDate;
    @SerializedName("parentIdTag")
    @Expose
    private String parentIdTag;

    /**
     * 
     * (Required)
     * 
     */
    public IdTagInfo.Status getStatus() {
        return status;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setStatus(IdTagInfo.Status status) {
        this.status = status;
    }

    public Calendar getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Calendar expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getParentIdTag() {
        return parentIdTag;
    }

    public void setParentIdTag(String parentIdTag) {
        this.parentIdTag = parentIdTag;
    }

    public enum Status {

        @SerializedName("Accepted")
        ACCEPTED("Accepted"),
        @SerializedName("Blocked")
        BLOCKED("Blocked"),
        @SerializedName("Expired")
        EXPIRED("Expired"),
        @SerializedName("Invalid")
        INVALID("Invalid"),
        @SerializedName("ConcurrentTx")
        CONCURRENT_TX("ConcurrentTx");
        private final String value;
        private final static Map<String, IdTagInfo.Status> CONSTANTS = new HashMap<String, IdTagInfo.Status>();

        static {
            for (IdTagInfo.Status c: values()) {
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

        public static IdTagInfo.Status fromValue(String value) {
            IdTagInfo.Status constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
