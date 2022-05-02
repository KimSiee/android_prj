
package com.joas.ocpp.msg;

import java.util.Calendar;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * StartTransactionRequest
 * <p>
 * 
 * 
 */
public class StartTransaction {

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
    @SerializedName("idTag")
    @Expose
    private String idTag;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("meterStart")
    @Expose
    private Integer meterStart;
    @SerializedName("reservationId")
    @Expose
    private Integer reservationId;
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

    /**
     * 
     * (Required)
     * 
     */
    public Integer getMeterStart() {
        return meterStart;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setMeterStart(Integer meterStart) {
        this.meterStart = meterStart;
    }

    public Integer getReservationId() {
        return reservationId;
    }

    public void setReservationId(Integer reservationId) {
        this.reservationId = reservationId;
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

}
