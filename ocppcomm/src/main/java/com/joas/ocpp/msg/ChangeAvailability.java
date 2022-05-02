
package com.joas.ocpp.msg;

import java.util.HashMap;
import java.util.Map;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * ChangeAvailabilityRequest
 * <p>
 * 
 * 
 */
public class ChangeAvailability {

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
    @SerializedName("type")
    @Expose
    private ChangeAvailability.Type type;

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
    public ChangeAvailability.Type getType() {
        return type;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setType(ChangeAvailability.Type type) {
        this.type = type;
    }

    public enum Type {

        @SerializedName("Inoperative")
        INOPERATIVE("Inoperative"),
        @SerializedName("Operative")
        OPERATIVE("Operative");
        private final String value;
        private final static Map<String, ChangeAvailability.Type> CONSTANTS = new HashMap<String, ChangeAvailability.Type>();

        static {
            for (ChangeAvailability.Type c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private Type(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public String value() {
            return this.value;
        }

        public static ChangeAvailability.Type fromValue(String value) {
            ChangeAvailability.Type constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
