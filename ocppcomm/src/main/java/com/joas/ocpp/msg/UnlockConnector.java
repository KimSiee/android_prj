
package com.joas.ocpp.msg;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * UnlockConnectorRequest
 * <p>
 * 
 * 
 */
public class UnlockConnector {

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

}
