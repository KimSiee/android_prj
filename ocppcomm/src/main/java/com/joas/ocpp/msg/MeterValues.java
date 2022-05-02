
package com.joas.ocpp.msg;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * MeterValuesRequest
 * <p>
 * 
 * 
 */
public class MeterValues {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("connectorId")
    @Expose
    private Integer connectorId;
    @SerializedName("transactionId")
    @Expose
    private Integer transactionId;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("meterValue")
    @Expose
    private List<MeterValue> meterValue = new ArrayList<MeterValue>();

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
    public List<MeterValue> getMeterValue() {
        return meterValue;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setMeterValue(List<MeterValue> meterValue) {
        this.meterValue = meterValue;
    }

}
