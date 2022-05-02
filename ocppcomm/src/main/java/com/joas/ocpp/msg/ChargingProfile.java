
package com.joas.ocpp.msg;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ChargingProfile {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("chargingProfileId")
    @Expose
    private Integer chargingProfileId;
    @SerializedName("transactionId")
    @Expose
    private Integer transactionId;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("stackLevel")
    @Expose
    private Integer stackLevel;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("chargingProfilePurpose")
    @Expose
    private ChargingProfile.ChargingProfilePurpose chargingProfilePurpose;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("chargingProfileKind")
    @Expose
    private ChargingProfile.ChargingProfileKind chargingProfileKind;
    @SerializedName("recurrencyKind")
    @Expose
    private ChargingProfile.RecurrencyKind recurrencyKind;
    @SerializedName("validFrom")
    @Expose
    private Calendar validFrom;
    @SerializedName("validTo")
    @Expose
    private Calendar validTo;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("chargingSchedule")
    @Expose
    private ChargingSchedule chargingSchedule;

    /**
     * 
     * (Required)
     * 
     */
    public Integer getChargingProfileId() {
        return chargingProfileId;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setChargingProfileId(Integer chargingProfileId) {
        this.chargingProfileId = chargingProfileId;
    }

    public Integer getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Integer transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * 
     * (Required)
     * 
     */
    public Integer getStackLevel() {
        return stackLevel;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setStackLevel(Integer stackLevel) {
        this.stackLevel = stackLevel;
    }

    /**
     * 
     * (Required)
     * 
     */
    public ChargingProfile.ChargingProfilePurpose getChargingProfilePurpose() {
        return chargingProfilePurpose;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setChargingProfilePurpose(ChargingProfile.ChargingProfilePurpose chargingProfilePurpose) {
        this.chargingProfilePurpose = chargingProfilePurpose;
    }

    /**
     * 
     * (Required)
     * 
     */
    public ChargingProfile.ChargingProfileKind getChargingProfileKind() {
        return chargingProfileKind;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setChargingProfileKind(ChargingProfile.ChargingProfileKind chargingProfileKind) {
        this.chargingProfileKind = chargingProfileKind;
    }

    public ChargingProfile.RecurrencyKind getRecurrencyKind() {
        return recurrencyKind;
    }

    public void setRecurrencyKind(ChargingProfile.RecurrencyKind recurrencyKind) {
        this.recurrencyKind = recurrencyKind;
    }

    public Calendar getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Calendar validFrom) {
        this.validFrom = validFrom;
    }

    public Calendar getValidTo() {
        return validTo;
    }

    public void setValidTo(Calendar validTo) {
        this.validTo = validTo;
    }

    /**
     * 
     * (Required)
     * 
     */
    public ChargingSchedule getChargingSchedule() {
        return chargingSchedule;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setChargingSchedule(ChargingSchedule chargingSchedule) {
        this.chargingSchedule = chargingSchedule;
    }

    public enum ChargingProfileKind {

        @SerializedName("Absolute")
        ABSOLUTE("Absolute"),
        @SerializedName("Recurring")
        RECURRING("Recurring"),
        @SerializedName("Relative")
        RELATIVE("Relative");
        private final String value;
        private final static Map<String, ChargingProfile.ChargingProfileKind> CONSTANTS = new HashMap<String, ChargingProfile.ChargingProfileKind>();

        static {
            for (ChargingProfile.ChargingProfileKind c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private ChargingProfileKind(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public String value() {
            return this.value;
        }

        public static ChargingProfile.ChargingProfileKind fromValue(String value) {
            ChargingProfile.ChargingProfileKind constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

    public enum ChargingProfilePurpose {

        @SerializedName("ChargePointMaxProfile")
        CHARGE_POINT_MAX_PROFILE("ChargePointMaxProfile"),
        @SerializedName("TxDefaultProfile")
        TX_DEFAULT_PROFILE("TxDefaultProfile"),
        @SerializedName("TxProfile")
        TX_PROFILE("TxProfile");
        private final String value;
        private final static Map<String, ChargingProfile.ChargingProfilePurpose> CONSTANTS = new HashMap<String, ChargingProfile.ChargingProfilePurpose>();

        static {
            for (ChargingProfile.ChargingProfilePurpose c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private ChargingProfilePurpose(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public String value() {
            return this.value;
        }

        public static ChargingProfile.ChargingProfilePurpose fromValue(String value) {
            ChargingProfile.ChargingProfilePurpose constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

    public enum RecurrencyKind {

        @SerializedName("Daily")
        DAILY("Daily"),
        @SerializedName("Weekly")
        WEEKLY("Weekly");
        private final String value;
        private final static Map<String, ChargingProfile.RecurrencyKind> CONSTANTS = new HashMap<String, ChargingProfile.RecurrencyKind>();

        static {
            for (ChargingProfile.RecurrencyKind c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private RecurrencyKind(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public String value() {
            return this.value;
        }

        public static ChargingProfile.RecurrencyKind fromValue(String value) {
            ChargingProfile.RecurrencyKind constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
