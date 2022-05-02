
package com.joas.ocpp.msg;

import java.util.HashMap;
import java.util.Map;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class SampledValue {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("value")
    @Expose
    private String value;
    @SerializedName("context")
    @Expose
    private SampledValue.Context context;
    @SerializedName("format")
    @Expose
    private SampledValue.Format format;
    @SerializedName("measurand")
    @Expose
    private SampledValue.Measurand measurand;
    @SerializedName("phase")
    @Expose
    private SampledValue.Phase phase;
    @SerializedName("location")
    @Expose
    private SampledValue.Location location;
    @SerializedName("unit")
    @Expose
    private SampledValue.Unit unit;

    /**
     * 
     * (Required)
     * 
     */
    public String getValue() {
        return value;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setValue(String value) {
        this.value = value;
    }

    public SampledValue.Context getContext() {
        return context;
    }

    public void setContext(SampledValue.Context context) {
        this.context = context;
    }

    public SampledValue.Format getFormat() {
        return format;
    }

    public void setFormat(SampledValue.Format format) {
        this.format = format;
    }

    public SampledValue.Measurand getMeasurand() {
        return measurand;
    }

    public void setMeasurand(SampledValue.Measurand measurand) {
        this.measurand = measurand;
    }

    public SampledValue.Phase getPhase() {
        return phase;
    }

    public void setPhase(SampledValue.Phase phase) {
        this.phase = phase;
    }

    public SampledValue.Location getLocation() {
        return location;
    }

    public void setLocation(SampledValue.Location location) {
        this.location = location;
    }

    public SampledValue.Unit getUnit() {
        return unit;
    }

    public void setUnit(SampledValue.Unit unit) {
        this.unit = unit;
    }

    public enum Context {

        @SerializedName("Interruption.Begin")
        INTERRUPTION_BEGIN("Interruption.Begin"),
        @SerializedName("Interruption.End")
        INTERRUPTION_END("Interruption.End"),
        @SerializedName("Sample.Clock")
        SAMPLE_CLOCK("Sample.Clock"),
        @SerializedName("Sample.Periodic")
        SAMPLE_PERIODIC("Sample.Periodic"),
        @SerializedName("Transaction.Begin")
        TRANSACTION_BEGIN("Transaction.Begin"),
        @SerializedName("Transaction.End")
        TRANSACTION_END("Transaction.End"),
        @SerializedName("Trigger")
        TRIGGER("Trigger"),
        @SerializedName("Other")
        OTHER("Other");
        private final String value;
        private final static Map<String, SampledValue.Context> CONSTANTS = new HashMap<String, SampledValue.Context>();

        static {
            for (SampledValue.Context c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private Context(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public String value() {
            return this.value;
        }

        public static SampledValue.Context fromValue(String value) {
            SampledValue.Context constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

    public enum Format {

        @SerializedName("Raw")
        RAW("Raw"),
        @SerializedName("SignedData")
        SIGNED_DATA("SignedData");
        private final String value;
        private final static Map<String, SampledValue.Format> CONSTANTS = new HashMap<String, SampledValue.Format>();

        static {
            for (SampledValue.Format c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private Format(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public String value() {
            return this.value;
        }

        public static SampledValue.Format fromValue(String value) {
            SampledValue.Format constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

    public enum Location {

        @SerializedName("Cable")
        CABLE("Cable"),
        @SerializedName("EV")
        EV("EV"),
        @SerializedName("Inlet")
        INLET("Inlet"),
        @SerializedName("Outlet")
        OUTLET("Outlet"),
        @SerializedName("Body")
        BODY("Body");
        private final String value;
        private final static Map<String, SampledValue.Location> CONSTANTS = new HashMap<String, SampledValue.Location>();

        static {
            for (SampledValue.Location c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private Location(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public String value() {
            return this.value;
        }

        public static SampledValue.Location fromValue(String value) {
            SampledValue.Location constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

    public enum Measurand {

        @SerializedName("Energy.Active.Export.Register")
        ENERGY_ACTIVE_EXPORT_REGISTER("Energy.Active.Export.Register"),
        @SerializedName("Energy.Active.Import.Register")
        ENERGY_ACTIVE_IMPORT_REGISTER("Energy.Active.Import.Register"),
        @SerializedName("Energy.Reactive.Export.Register")
        ENERGY_REACTIVE_EXPORT_REGISTER("Energy.Reactive.Export.Register"),
        @SerializedName("Energy.Reactive.Import.Register")
        ENERGY_REACTIVE_IMPORT_REGISTER("Energy.Reactive.Import.Register"),
        @SerializedName("Energy.Active.Export.Interval")
        ENERGY_ACTIVE_EXPORT_INTERVAL("Energy.Active.Export.Interval"),
        @SerializedName("Energy.Active.Import.Interval")
        ENERGY_ACTIVE_IMPORT_INTERVAL("Energy.Active.Import.Interval"),
        @SerializedName("Energy.Reactive.Export.Interval")
        ENERGY_REACTIVE_EXPORT_INTERVAL("Energy.Reactive.Export.Interval"),
        @SerializedName("Energy.Reactive.Import.Interval")
        ENERGY_REACTIVE_IMPORT_INTERVAL("Energy.Reactive.Import.Interval"),
        @SerializedName("Power.Active.Export")
        POWER_ACTIVE_EXPORT("Power.Active.Export"),
        @SerializedName("Power.Active.Import")
        POWER_ACTIVE_IMPORT("Power.Active.Import"),
        @SerializedName("Power.Offered")
        POWER_OFFERED("Power.Offered"),
        @SerializedName("Power.Reactive.Export")
        POWER_REACTIVE_EXPORT("Power.Reactive.Export"),
        @SerializedName("Power.Reactive.Import")
        POWER_REACTIVE_IMPORT("Power.Reactive.Import"),
        @SerializedName("Power.Factor")
        POWER_FACTOR("Power.Factor"),
        @SerializedName("Current.Import")
        CURRENT_IMPORT("Current.Import"),
        @SerializedName("Current.Export")
        CURRENT_EXPORT("Current.Export"),
        @SerializedName("Current.Offered")
        CURRENT_OFFERED("Current.Offered"),
        @SerializedName("Voltage")
        VOLTAGE("Voltage"),
        @SerializedName("Frequency")
        FREQUENCY("Frequency"),
        @SerializedName("Temperature")
        TEMPERATURE("Temperature"),
        @SerializedName("SoC")
        SO_C("SoC"),
        @SerializedName("RPM")
        RPM("RPM");
        private final String value;
        private final static Map<String, SampledValue.Measurand> CONSTANTS = new HashMap<String, SampledValue.Measurand>();

        static {
            for (SampledValue.Measurand c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private Measurand(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public String value() {
            return this.value;
        }

        public static SampledValue.Measurand fromValue(String value) {
            SampledValue.Measurand constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

    public enum Phase {

        @SerializedName("L1")
        L_1("L1"),
        @SerializedName("L2")
        L_2("L2"),
        @SerializedName("L3")
        L_3("L3"),
        @SerializedName("N")
        N("N"),
        @SerializedName("L1-N")
        L_1_N("L1-N"),
        @SerializedName("L2-N")
        L_2_N("L2-N"),
        @SerializedName("L3-N")
        L_3_N("L3-N"),
        @SerializedName("L1-L2")
        L_1_L_2("L1-L2"),
        @SerializedName("L2-L3")
        L_2_L_3("L2-L3"),
        @SerializedName("L3-L1")
        L_3_L_1("L3-L1");
        private final String value;
        private final static Map<String, SampledValue.Phase> CONSTANTS = new HashMap<String, SampledValue.Phase>();

        static {
            for (SampledValue.Phase c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private Phase(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public String value() {
            return this.value;
        }

        public static SampledValue.Phase fromValue(String value) {
            SampledValue.Phase constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

    public enum Unit {

        @SerializedName("Wh")
        WH("Wh"),
        @SerializedName("kWh")
        K_WH("kWh"),
        @SerializedName("varh")
        VARH("varh"),
        @SerializedName("kvarh")
        KVARH("kvarh"),
        @SerializedName("W")
        W("W"),
        @SerializedName("kW")
        K_W("kW"),
        @SerializedName("VA")
        VA("VA"),
        @SerializedName("kVA")
        K_VA("kVA"),
        @SerializedName("var")
        VAR("var"),
        @SerializedName("kvar")
        KVAR("kvar"),
        @SerializedName("A")
        A("A"),
        @SerializedName("V")
        V("V"),
        @SerializedName("K")
        K("K"),
        @SerializedName("Celcius")
        CELCIUS("Celcius"),
        @SerializedName("Fahrenheit")
        FAHRENHEIT("Fahrenheit"),
        @SerializedName("Percent")
        PERCENT("Percent");
        private final String value;
        private final static Map<String, SampledValue.Unit> CONSTANTS = new HashMap<String, SampledValue.Unit>();

        static {
            for (SampledValue.Unit c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private Unit(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public String value() {
            return this.value;
        }

        public static SampledValue.Unit fromValue(String value) {
            SampledValue.Unit constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
