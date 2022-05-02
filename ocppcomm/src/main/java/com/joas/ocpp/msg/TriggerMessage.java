
package com.joas.ocpp.msg;

import java.util.HashMap;
import java.util.Map;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * TriggerMessageRequest
 * <p>
 * 
 * 
 */
public class TriggerMessage {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("requestedMessage")
    @Expose
    private TriggerMessage.RequestedMessage requestedMessage;
    @SerializedName("connectorId")
    @Expose
    private Integer connectorId;

    /**
     * 
     * (Required)
     * 
     */
    public TriggerMessage.RequestedMessage getRequestedMessage() {
        return requestedMessage;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setRequestedMessage(TriggerMessage.RequestedMessage requestedMessage) {
        this.requestedMessage = requestedMessage;
    }

    public Integer getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(Integer connectorId) {
        this.connectorId = connectorId;
    }

    public enum RequestedMessage {

        @SerializedName("BootNotification")
        BOOT_NOTIFICATION("BootNotification"),
        @SerializedName("DiagnosticsStatusNotification")
        DIAGNOSTICS_STATUS_NOTIFICATION("DiagnosticsStatusNotification"),
        @SerializedName("FirmwareStatusNotification")
        FIRMWARE_STATUS_NOTIFICATION("FirmwareStatusNotification"),
        @SerializedName("Heartbeat")
        HEARTBEAT("Heartbeat"),
        @SerializedName("MeterValues")
        METER_VALUES("MeterValues"),
        @SerializedName("StatusNotification")
        STATUS_NOTIFICATION("StatusNotification");
        private final String value;
        private final static Map<String, TriggerMessage.RequestedMessage> CONSTANTS = new HashMap<String, TriggerMessage.RequestedMessage>();

        static {
            for (TriggerMessage.RequestedMessage c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private RequestedMessage(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public String value() {
            return this.value;
        }

        public static TriggerMessage.RequestedMessage fromValue(String value) {
            TriggerMessage.RequestedMessage constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
