
package com.joas.ocpp.msg;

import java.util.HashMap;
import java.util.Map;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
 * ResetRequest
 * <p>
 * 
 * 
 */
public class Reset {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("type")
    @Expose
    private Reset.Type type;

    /**
     * 
     * (Required)
     * 
     */
    public Reset.Type getType() {
        return type;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setType(Reset.Type type) {
        this.type = type;
    }

    public enum Type {

        @SerializedName("Hard")
        HARD("Hard"),
        @SerializedName("Soft")
        SOFT("Soft");
        private final String value;
        private final static Map<String, Reset.Type> CONSTANTS = new HashMap<String, Reset.Type>();

        static {
            for (Reset.Type c: values()) {
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

        public static Reset.Type fromValue(String value) {
            Reset.Type constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
