
package com.joas.ocpp.msg;

import java.util.HashMap;
import java.util.Map;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * DiagnosticsStatusNotificationRequest
 * <p>
 * 
 * 
 */
public class DiagnosticsStatusNotification {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("status")
    @Expose
    private DiagnosticsStatusNotification.Status status;

    /**
     * 
     * (Required)
     * 
     */
    public DiagnosticsStatusNotification.Status getStatus() {
        return status;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setStatus(DiagnosticsStatusNotification.Status status) {
        this.status = status;
    }

    public enum Status {

        @SerializedName("Idle")
        IDLE("Idle"),
        @SerializedName("Uploaded")
        UPLOADED("Uploaded"),
        @SerializedName("UploadFailed")
        UPLOAD_FAILED("UploadFailed"),
        @SerializedName("Uploading")
        UPLOADING("Uploading");
        private final String value;
        private final static Map<String, DiagnosticsStatusNotification.Status> CONSTANTS = new HashMap<String, DiagnosticsStatusNotification.Status>();

        static {
            for (DiagnosticsStatusNotification.Status c: values()) {
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

        public static DiagnosticsStatusNotification.Status fromValue(String value) {
            DiagnosticsStatusNotification.Status constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
