
package com.joas.ocpp.msg;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * StatusNotificationRequest
 * <p>
 * 
 * 
 */
public class StatusNotification {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("connectorId")
    @Expose
    private Integer connectorId;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("errorCode")
    @Expose
    private StatusNotification.ErrorCode errorCode;
    @SerializedName("info")
    @Expose
    private String info;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("status")
    @Expose
    private StatusNotification.Status status;
    @SerializedName("timestamp")
    @Expose
    private Calendar timestamp;
    @SerializedName("vendorId")
    @Expose
    private String vendorId;
    @SerializedName("vendorErrorCode")
    @Expose
    private String vendorErrorCode;

    /**
     * 
     * (Required)
     * 
     */
    public Integer getConnectorId() {
        return connectorId;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setConnectorId(Integer connectorId) {
        this.connectorId = connectorId;
    }

    /**
     * 
     * (Required)
     * 
     */
    public StatusNotification.ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setErrorCode(StatusNotification.ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    /**
     * 
     * (Required)
     * 
     */
    public StatusNotification.Status getStatus() {
        return status;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setStatus(StatusNotification.Status status) {
        this.status = status;
    }

    public Calendar getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Calendar timestamp) {
        this.timestamp = timestamp;
    }

    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    public String getVendorErrorCode() {
        return vendorErrorCode;
    }

    public void setVendorErrorCode(String vendorErrorCode) {
        this.vendorErrorCode = vendorErrorCode;
    }

    public enum ErrorCode {

        @SerializedName("ConnectorLockFailure")
        CONNECTOR_LOCK_FAILURE("ConnectorLockFailure"),
        @SerializedName("EVCommunicationError")
        EV_COMMUNICATION_ERROR("EVCommunicationError"),
        @SerializedName("GroundFailure")
        GROUND_FAILURE("GroundFailure"),
        @SerializedName("HighTemperature")
        HIGH_TEMPERATURE("HighTemperature"),
        @SerializedName("InternalError")
        INTERNAL_ERROR("InternalError"),
        @SerializedName("LocalListConflict")
        LOCAL_LIST_CONFLICT("LocalListConflict"),
        @SerializedName("NoError")
        NO_ERROR("NoError"),
        @SerializedName("OtherError")
        OTHER_ERROR("OtherError"),
        @SerializedName("OverCurrentFailure")
        OVER_CURRENT_FAILURE("OverCurrentFailure"),
        @SerializedName("PowerMeterFailure")
        POWER_METER_FAILURE("PowerMeterFailure"),
        @SerializedName("PowerSwitchFailure")
        POWER_SWITCH_FAILURE("PowerSwitchFailure"),
        @SerializedName("ReaderFailure")
        READER_FAILURE("ReaderFailure"),
        @SerializedName("ResetFailure")
        RESET_FAILURE("ResetFailure"),
        @SerializedName("UnderVoltage")
        UNDER_VOLTAGE("UnderVoltage"),
        @SerializedName("OverVoltage")
        OVER_VOLTAGE("OverVoltage"),
        @SerializedName("WeakSignal")
        WEAK_SIGNAL("WeakSignal");
        private final String value;
        private final static Map<String, StatusNotification.ErrorCode> CONSTANTS = new HashMap<String, StatusNotification.ErrorCode>();

        static {
            for (StatusNotification.ErrorCode c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private ErrorCode(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public String value() {
            return this.value;
        }

        public static StatusNotification.ErrorCode fromValue(String value) {
            StatusNotification.ErrorCode constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

    public enum Status {

        @SerializedName("Available")
        AVAILABLE("Available"),
        @SerializedName("Preparing")
        PREPARING("Preparing"),
        @SerializedName("Charging")
        CHARGING("Charging"),
        @SerializedName("SuspendedEVSE")
        SUSPENDED_EVSE("SuspendedEVSE"),
        @SerializedName("SuspendedEV")
        SUSPENDED_EV("SuspendedEV"),
        @SerializedName("Finishing")
        FINISHING("Finishing"),
        @SerializedName("Reserved")
        RESERVED("Reserved"),
        @SerializedName("Unavailable")
        UNAVAILABLE("Unavailable"),
        @SerializedName("Faulted")
        FAULTED("Faulted");
        private final String value;
        private final static Map<String, StatusNotification.Status> CONSTANTS = new HashMap<String, StatusNotification.Status>();

        static {
            for (StatusNotification.Status c: values()) {
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

        public static StatusNotification.Status fromValue(String value) {
            StatusNotification.Status constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
