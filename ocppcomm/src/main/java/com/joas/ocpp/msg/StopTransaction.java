
package com.joas.ocpp.msg;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * StopTransactionRequest
 * <p>
 * 
 * 
 */
public class StopTransaction {

    @SerializedName("idTag")
    @Expose
    private String idTag;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("meterStop")
    @Expose
    private Integer meterStop;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("timestamp")
    @Expose
    private Calendar timestamp;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("transactionId")
    @Expose
    private Integer transactionId;
    @SerializedName("reason")
    @Expose
    private StopTransaction.Reason reason;
    @SerializedName("transactionData")
    @Expose
    private List<TransactionDatum> transactionData = new ArrayList<TransactionDatum>();

    public String getIdTag() {
        return idTag;
    }

    public void setIdTag(String idTag) {
        this.idTag = idTag;
    }

    /**
     * 
     * (Required)
     * 
     */
    public Integer getMeterStop() {
        return meterStop;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setMeterStop(Integer meterStop) {
        this.meterStop = meterStop;
    }

    /**
     * 
     * (Required)
     * 
     */
    public Calendar getTimestamp() {
        return timestamp;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setTimestamp(Calendar timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * 
     * (Required)
     * 
     */
    public Integer getTransactionId() {
        return transactionId;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setTransactionId(Integer transactionId) {
        this.transactionId = transactionId;
    }

    public StopTransaction.Reason getReason() {
        return reason;
    }

    public void setReason(StopTransaction.Reason reason) {
        this.reason = reason;
    }

    public List<TransactionDatum> getTransactionData() {
        return transactionData;
    }

    public void setTransactionData(List<TransactionDatum> transactionData) {
        this.transactionData = transactionData;
    }

    public enum Reason {

        @SerializedName("EmergencyStop")
        EMERGENCY_STOP("EmergencyStop"),
        @SerializedName("EVDisconnected")
        EV_DISCONNECTED("EVDisconnected"),
        @SerializedName("HardReset")
        HARD_RESET("HardReset"),
        @SerializedName("Local")
        LOCAL("Local"),
        @SerializedName("Other")
        OTHER("Other"),
        @SerializedName("PowerLoss")
        POWER_LOSS("PowerLoss"),
        @SerializedName("Reboot")
        REBOOT("Reboot"),
        @SerializedName("Remote")
        REMOTE("Remote"),
        @SerializedName("SoftReset")
        SOFT_RESET("SoftReset"),
        @SerializedName("UnlockCommand")
        UNLOCK_COMMAND("UnlockCommand"),
        @SerializedName("DeAuthorized")
        DE_AUTHORIZED("DeAuthorized");
        private final String value;
        private final static Map<String, StopTransaction.Reason> CONSTANTS = new HashMap<String, StopTransaction.Reason>();

        static {
            for (StopTransaction.Reason c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private Reason(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public String value() {
            return this.value;
        }

        public static StopTransaction.Reason fromValue(String value) {
            StopTransaction.Reason constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
