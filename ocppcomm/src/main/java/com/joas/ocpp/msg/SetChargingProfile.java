
package com.joas.ocpp.msg;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * SetChargingProfileRequest
 * <p>
 * 
 * 
 */
public class SetChargingProfile {

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
    @SerializedName("csChargingProfiles")
    @Expose
    private CsChargingProfiles csChargingProfiles;

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
    public CsChargingProfiles getCsChargingProfiles() {
        return csChargingProfiles;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setCsChargingProfiles(CsChargingProfiles csChargingProfiles) {
        this.csChargingProfiles = csChargingProfiles;
    }

}
