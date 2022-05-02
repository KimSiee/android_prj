
package com.joas.ocpp.msg;

import java.util.HashMap;
import java.util.Map;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * ClearChargingProfileRequest
 * <p>
 * 
 * 
 */
public class ClearChargingProfile {

    @SerializedName("id")
    @Expose
    private Integer id;
    @SerializedName("connectorId")
    @Expose
    private Integer connectorId;
    @SerializedName("chargingProfilePurpose")
    @Expose
    private ClearChargingProfile.ChargingProfilePurpose chargingProfilePurpose;
    @SerializedName("stackLevel")
    @Expose
    private Integer stackLevel;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(Integer connectorId) {
        this.connectorId = connectorId;
    }

    public ClearChargingProfile.ChargingProfilePurpose getChargingProfilePurpose() {
        return chargingProfilePurpose;
    }

    public void setChargingProfilePurpose(ClearChargingProfile.ChargingProfilePurpose chargingProfilePurpose) {
        this.chargingProfilePurpose = chargingProfilePurpose;
    }

    public Integer getStackLevel() {
        return stackLevel;
    }

    public void setStackLevel(Integer stackLevel) {
        this.stackLevel = stackLevel;
    }

    public enum ChargingProfilePurpose {

        @SerializedName("ChargePointMaxProfile")
        CHARGE_POINT_MAX_PROFILE("ChargePointMaxProfile"),
        @SerializedName("TxDefaultProfile")
        TX_DEFAULT_PROFILE("TxDefaultProfile"),
        @SerializedName("TxProfile")
        TX_PROFILE("TxProfile");
        private final String value;
        private final static Map<String, ClearChargingProfile.ChargingProfilePurpose> CONSTANTS = new HashMap<String, ClearChargingProfile.ChargingProfilePurpose>();

        static {
            for (ClearChargingProfile.ChargingProfilePurpose c: values()) {
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

        public static ClearChargingProfile.ChargingProfilePurpose fromValue(String value) {
            ClearChargingProfile.ChargingProfilePurpose constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
