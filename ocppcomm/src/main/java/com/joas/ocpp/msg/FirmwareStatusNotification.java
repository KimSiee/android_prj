
package com.joas.ocpp.msg;

import java.util.HashMap;
import java.util.Map;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * FirmwareStatusNotificationRequest
 * <p>
 * 
 * 
 */
public class FirmwareStatusNotification {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("status")
    @Expose
    private FirmwareStatusNotification.Status status;

    /**
     * 
     * (Required)
     * 
     */
    public FirmwareStatusNotification.Status getStatus() {
        return status;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setStatus(FirmwareStatusNotification.Status status) {
        this.status = status;
    }

    public enum Status {

        @SerializedName("Downloaded")
        DOWNLOADED("Downloaded"),
        @SerializedName("DownloadFailed")
        DOWNLOAD_FAILED("DownloadFailed"),
        @SerializedName("Downloading")
        DOWNLOADING("Downloading"),
        @SerializedName("Idle")
        IDLE("Idle"),
        @SerializedName("InstallationFailed")
        INSTALLATION_FAILED("InstallationFailed"),
        @SerializedName("Installing")
        INSTALLING("Installing"),
        @SerializedName("Installed")
        INSTALLED("Installed");
        private final String value;
        private final static Map<String, FirmwareStatusNotification.Status> CONSTANTS = new HashMap<String, FirmwareStatusNotification.Status>();

        static {
            for (FirmwareStatusNotification.Status c: values()) {
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

        public static FirmwareStatusNotification.Status fromValue(String value) {
            FirmwareStatusNotification.Status constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
