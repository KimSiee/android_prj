
package com.joas.ocpp.msg;

import java.util.Calendar;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * HeartbeatResponse
 * <p>
 * 
 * 
 */
public class HeartbeatResponse {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("currentTime")
    @Expose
    private Calendar currentTime;

    /**
     * 
     * (Required)
     * 
     */
    public Calendar getCurrentTime() {
        return currentTime;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setCurrentTime(Calendar currentTime) {
        this.currentTime = currentTime;
    }

}
