
package com.joas.ocpp.msg;

import java.net.URI;
import java.util.Calendar;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * UpdateFirmwareRequest
 * <p>
 * 
 * 
 */
public class UpdateFirmware {

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
    private Double retries;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("retrieveDate")
    @Expose
    private Calendar retrieveDate;
    @SerializedName("retryInterval")
    @Expose
    private Double retryInterval;

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

    public Double getRetries() {
        return retries;
    }

    public void setRetries(Double retries) {
        this.retries = retries;
    }

    /**
     * 
     * (Required)
     * 
     */
    public Calendar getRetrieveDate() {
        return retrieveDate;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setRetrieveDate(Calendar retrieveDate) {
        this.retrieveDate = retrieveDate;
    }

    public Double getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(Double retryInterval) {
        this.retryInterval = retryInterval;
    }

}
