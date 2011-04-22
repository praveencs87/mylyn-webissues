package org.webissues.api;

import java.util.Arrays;

import org.webissues.api.Attribute.AttributeType;


public enum ConditionType {
    
    EQ("Equals"), GT("Greater than"), GTE("Greater than or equal"), BEG("Starts with"), END("Ends with"), NEQ("Not equals"), LT("Less than"), LTE("Less than or equal"), CON("Contains"), IN("In");
    
    private String label;
    
    private ConditionType(String label) {
        this.label = label;
    }
    
    public String getLabel() {
        return label;
    }
    
    public static ConditionType fromLabel(String label) {
        for(ConditionType c : values()) {
            if(c.getLabel().equals(label)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Unknown condition");
    }

    public boolean isFor(Attribute attribute) {
        if (attribute.getAttributeType().equals(AttributeType.USER)) {
            return Arrays.asList(new ConditionType[] { IN, NEQ, EQ, BEG, END, CON }).contains(this);
        } else if (attribute.getAttributeType().equals(AttributeType.DATETIME)) {
            return Arrays.asList(new ConditionType[] { IN, NEQ, EQ, GT, GTE, LT, LTE }).contains(this);
        } else if (attribute.getAttributeType().equals(AttributeType.NUMERIC)) {
            return Arrays.asList(new ConditionType[] { IN, NEQ, EQ, GT, GTE, LT, LTE }).contains(this);
        } else if (attribute.getAttributeType().equals(AttributeType.ENUM)) {
            return Arrays.asList(new ConditionType[] { IN, NEQ, EQ, BEG, END, CON }).contains(this);
        } else if (attribute.getAttributeType().equals(AttributeType.TEXT)) {
            return Arrays.asList(new ConditionType[] { IN, NEQ, EQ, BEG, END, CON }).contains(this);
        }
        return true;
    }
}