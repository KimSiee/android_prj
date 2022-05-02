
package com.joas.ocpp.msg;

import java.net.URI;
import java.util.Calendar;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * GetDiagnosticsRequest
 * <p>
 * 
 * 
 */
public class GetDiagnostics {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("location")
    @Expose
    private URI location;
    @SerializedName("retries")
    @Expose
    private Integer retries;
    @SerializedName("retryInterval")
    @Expose
    private Integer retryInterval;
    @SerializedName("startTime")
    @Expose
    private Calendar startTime;
    @SerializedName("stopTime")
    @Expose
    private Calendar stopTime;

    /**
     * 
     * (Required)
     * 
     */
    public URI getLocation() {
        return location;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setLocation(URI location) {
        this.location = location;
    }

    public Integer getRetries() {
        return retries;
    }

    public void setRetries(Integer retries) {
        this.retries = retries;
    }

    public Integer getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(Integer retryInterval) {
        this.retryInterval = retryInterval;
    }

    public Calendar getStartTime() {
        return startTime;
    }

    public void setStartTime(Calendar startTime) {
        this.startTime = startTime;
    }

    public Calendar getStopTime() {
        return stopTime;
    }

    public void setStopTime(Calendar stopTime) {
        this.stopTime = stopTime;
    }

}
