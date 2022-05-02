
package com.joas.ocpp.msg;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class TransactionDatum {

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
    @SerializedName("sampledValue")
    @Expose
    private List<SampledValue> sampledValue = new ArrayList<SampledValue>();

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
    public List<SampledValue> getSampledValue() {
        return sampledValue;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setSampledValue(List<SampledValue> sampledValue) {
        this.sampledValue = sampledValue;
    }

}
