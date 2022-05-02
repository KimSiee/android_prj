
package com.joas.ocpp.msg;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ChargingSchedulePeriod {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("startPeriod")
    @Expose
    private Integer startPeriod;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("limit")
    @Expose
    private Double limit;
    @SerializedName("numberPhases")
    @Expose
    private Integer numberPhases;

    /**
     * 
     * (Required)
     * 
     */
    public Integer getStartPeriod() {
        return startPeriod;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setStartPeriod(Integer startPeriod) {
        this.startPeriod = startPeriod;
    }

    /**
     * 
     * (Required)
     * 
     */
    public Double getLimit() {
        return limit;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setLimit(Double limit) {
        this.limit = limit;
    }

    public Integer getNumberPhases() {
        return numberPhases;
    }

    public void setNumberPhases(Integer numberPhases) {
        this.numberPhases = numberPhases;
    }

}
