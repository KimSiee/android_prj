
package com.joas.ocpp.msg;

import java.util.HashMap;
import java.util.Map;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * GetCompositeScheduleRequest
 * <p>
 * 
 * 
 */
public class GetCompositeSchedule {

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
    @SerializedName("duration")
    @Expose
    private Integer duration;
    @SerializedName("chargingRateUnit")
    @Expose
    private GetCompositeSchedule.ChargingRateUnit chargingRateUnit;

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
    public Integer getDuration() {
        return duration;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public GetCompositeSchedule.ChargingRateUnit getChargingRateUnit() {
        return chargingRateUnit;
    }

    public void setChargingRateUnit(GetCompositeSchedule.ChargingRateUnit chargingRateUnit) {
        this.chargingRateUnit = chargingRateUnit;
    }

    public enum ChargingRateUnit {

        @SerializedName("A")
        A("A"),
        @SerializedName("W")
        W("W");
        private final String value;
        private final static Map<String, GetCompositeSchedule.ChargingRateUnit> CONSTANTS = new HashMap<String, GetCompositeSchedule.ChargingRateUnit>();

        static {
            for (GetCompositeSchedule.ChargingRateUnit c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private ChargingRateUnit(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public String value() {
            return this.value;
        }

        public static GetCompositeSchedule.ChargingRateUnit fromValue(String value) {
            GetCompositeSchedule.ChargingRateUnit constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
