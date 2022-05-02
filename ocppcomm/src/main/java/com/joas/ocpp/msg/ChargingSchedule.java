
package com.joas.ocpp.msg;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ChargingSchedule {

    @SerializedName("duration")
    @Expose
    private Integer duration;
    @SerializedName("startSchedule")
    @Expose
    private Calendar startSchedule;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("chargingRateUnit")
    @Expose
    private ChargingSchedule.ChargingRateUnit chargingRateUnit;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("chargingSchedulePeriod")
    @Expose
    private List<ChargingSchedulePeriod> chargingSchedulePeriod = new ArrayList<ChargingSchedulePeriod>();
    @SerializedName("minChargingRate")
    @Expose
    private Double minChargingRate;

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Calendar getStartSchedule() {
        return startSchedule;
    }

    public void setStartSchedule(Calendar startSchedule) {
        this.startSchedule = startSchedule;
    }

    /**
     * 
     * (Required)
     * 
     */
    public ChargingSchedule.ChargingRateUnit getChargingRateUnit() {
        return chargingRateUnit;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setChargingRateUnit(ChargingSchedule.ChargingRateUnit chargingRateUnit) {
        this.chargingRateUnit = chargingRateUnit;
    }

    /**
     * 
     * (Required)
     * 
     */
    public List<ChargingSchedulePeriod> getChargingSchedulePeriod() {
        return chargingSchedulePeriod;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setChargingSchedulePeriod(List<ChargingSchedulePeriod> chargingSchedulePeriod) {
        this.chargingSchedulePeriod = chargingSchedulePeriod;
    }

    public Double getMinChargingRate() {
        return minChargingRate;
    }

    public void setMinChargingRate(Double minChargingRate) {
        this.minChargingRate = minChargingRate;
    }

    public enum ChargingRateUnit {

        @SerializedName("A")
        A("A"),
        @SerializedName("W")
        W("W");
        private final String value;
        private final static Map<String, ChargingSchedule.ChargingRateUnit> CONSTANTS = new HashMap<String, ChargingSchedule.ChargingRateUnit>();

        static {
            for (ChargingSchedule.ChargingRateUnit c: values()) {
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

        public static ChargingSchedule.ChargingRateUnit fromValue(String value) {
            ChargingSchedule.ChargingRateUnit constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
