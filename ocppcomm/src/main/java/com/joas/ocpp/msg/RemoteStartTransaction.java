
package com.joas.ocpp.msg;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * RemoteStartTransactionRequest
 * <p>
 * 
 * 
 */
public class RemoteStartTransaction {

    @SerializedName("connectorId")
    @Expose
    private Integer connectorId;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("idTag")
    @Expose
    private String idTag;
    @SerializedName("chargingProfile")
    @Expose
    private ChargingProfile chargingProfile;

    public Integer getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(Integer connectorId) {
        this.connectorId = connectorId;
    }

    /**
     * 
     * (Required)
     * 
     */
    public String getIdTag() {
        return idTag;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setIdTag(String idTag) {
        this.idTag = idTag;
    }

    public ChargingProfile getChargingProfile() {
        return chargingProfile;
    }

    public void setChargingProfile(ChargingProfile chargingProfile) {
        this.chargingProfile = chargingProfile;
    }

}
