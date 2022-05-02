
package com.joas.ocpp.msg;

import java.util.Calendar;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * ReserveNowRequest
 * <p>
 * 
 * 
 */
public class ReserveNow {

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
    @SerializedName("expiryDate")
    @Expose
    private Calendar expiryDate;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("idTag")
    @Expose
    private String idTag;
    @SerializedName("parentIdTag")
    @Expose
    private String parentIdTag;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("reservationId")
    @Expose
    private Integer reservationId;

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
    public Calendar getExpiryDate() {
        return expiryDate;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setExpiryDate(Calendar expiryDate) {
        this.expiryDate = expiryDate;
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

    public String getParentIdTag() {
        return parentIdTag;
    }

    public void setParentIdTag(String parentIdTag) {
        this.parentIdTag = parentIdTag;
    }

    /**
     * 
     * (Required)
     * 
     */
    public Integer getReservationId() {
        return reservationId;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setReservationId(Integer reservationId) {
        this.reservationId = reservationId;
    }

}
