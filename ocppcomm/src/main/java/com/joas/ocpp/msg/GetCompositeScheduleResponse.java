
package com.joas.ocpp.msg;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * GetCompositeScheduleResponse
 * <p>
 * 
 * 
 */
public class GetCompositeScheduleResponse {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("status")
    @Expose
    private GetCompositeScheduleResponse.Status status;
    @SerializedName("connectorId")
    @Expose
    private Integer connectorId;
    @SerializedName("scheduleStart")
    @Expose
    private Calendar scheduleStart;
    @SerializedName("chargingSchedule")
    @Expose
    private ChargingSchedule chargingSchedule;

    /**
     * 
     * (Required)
     * 
     */
    public GetCompositeScheduleResponse.Status getStatus() {
        return status;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setStatus(GetCompositeScheduleResponse.Status status) {
        this.status = status;
    }

    public Integer getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(Integer connectorId) {
        this.connectorId = connectorId;
    }

    public Calendar getScheduleStart() {
        return scheduleStart;
    }

    public void setScheduleStart(Calendar scheduleStart) {
        this.scheduleStart = scheduleStart;
    }

    public ChargingSchedule getChargingSchedule() {
        return chargingSchedule;
    }

    public void setChargingSchedule(ChargingSchedule chargingSchedule) {
        this.chargingSchedule = chargingSchedule;
    }

    public enum Status {

        @SerializedName("Accepted")
        ACCEPTED("Accepted"),
        @SerializedName("Rejected")
        REJECTED("Rejected");
        private final String value;
        private final static Map<String, GetCompositeScheduleResponse.Status> CONSTANTS = new HashMap<String, GetCompositeScheduleResponse.Status>();

        static {
            for (GetCompositeScheduleResponse.Status c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private Status(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public String value() {
            return this.value;
        }

        public static GetCompositeScheduleResponse.Status fromValue(String value) {
            GetCompositeScheduleResponse.Status constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
