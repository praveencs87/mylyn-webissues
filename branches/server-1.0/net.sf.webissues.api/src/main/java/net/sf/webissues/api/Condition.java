package net.sf.webissues.api;

import java.io.Serializable;
import java.util.List;

public class Condition implements Serializable {
    
    private static final long serialVersionUID = 3333094590709062871L;
    private ConditionType type;
    private Attribute attribute;
    private String value;
    
    public Condition(ConditionType type, Attribute attribute, String value) {
        this.type = type;
        this.attribute = attribute;
        this.value = value;
    }
    
    public Condition(String string, Type issueType) {
        List<String> args = Util.parseLine(string, ',','"');
        type = ConditionType.valueOf(args.get(0));
        attribute = issueType.get(Integer.parseInt(args.get(1)));
        if(attribute == null) {
            throw new IllegalArgumentException("Unknwon attribute ID " + args.get(1));
        }
        value = args.get(2);
    }
    
    public String toParameter() {
        StringBuilder bui = new StringBuilder();
        bui.append(type.name());
        bui.append(",");
        bui.append(attribute.getId());
        bui.append(",\"");
        bui.append(value == null ? "" : value);
        bui.append("\"");
        return bui.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attribute == null) ? 0 : attribute.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Condition other = (Condition) obj;
        if (attribute == null) {
            if (other.attribute != null)
                return false;
        } else if (!attribute.equals(other.attribute))
            return false;
        if (type != other.type)
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public void setAttribute(Attribute attribute) {
        this.attribute = attribute;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setType(ConditionType type) {
        this.type = type;
    }

    public ConditionType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Condition [type=" + type + ", attributeId=" + attribute.getId() + ", value=" + value + "]";
    }
    
    
}