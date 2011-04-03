package net.sf.webissues.core;

import java.util.List;

import net.sf.webissues.api.Environment;
import net.sf.webissues.api.Util;

public class WebIssuesFilterCondition {

    public enum Type {
        IS_EQUAL_TO("eq", true), IS_ME("me", false), IS_EMPTY("mt", false), IS_IN("in", true);

        String mnemonic;
        boolean hasArgument;

        Type(String mnemonic, boolean hasArgument) {
            this.mnemonic = mnemonic;
            this.hasArgument = hasArgument;
        }

        public boolean hasArgument() {
            return hasArgument;
        }

        public String getMnemonic() {
            return mnemonic;
        }

        public static Type fromMnemonic(String mnemonic) {
            for (Type type : values()) {
                if (type.mnemonic.equals(mnemonic)) {
                    return type;
                }
            }
            return null;
        }
    }

    private Type type;
    private boolean negate;
    private String attrName;
    private String attrValue;
    
    public static final String FOLDER = "Folder";
    public static final String PROJECT = "Project";
    public static final String NAME = "Name";
    public static final String DATE_CREATED = "Date Created";
    public static final String USER_CREATED = "User Created";
    public static final String DATE_MODIFIED = "Date Modified";
    public static final String USER_MODIFIED = "User Modified";

    public WebIssuesFilterCondition(Type type, boolean negate, String attrName, String attrValue) {
        super();
        this.type = type;
        this.negate = negate;
        this.attrName = attrName;
        this.attrValue = attrValue;
    }

    public WebIssuesFilterCondition() {
    }

    public String getExpression() {
        return "'" + Util.escape(attrName) + "' " + (negate ? "!" : "") + type.getMnemonic() + " '" + Util.escape(attrValue) + "'";
    }

    public static WebIssuesFilterCondition fromParameterValue(String value, Environment environment) {
        List<String> condition = Util.parseLine(value);
        String attrId = condition.get(0);
        String mnemonic = condition.get(1);
        boolean negate = false;
        if (mnemonic.startsWith("!")) {
            negate = true;
            mnemonic = mnemonic.substring(1);
        }
        Type type = Type.fromMnemonic(mnemonic);
        String attrValue = condition.get(2);
        return new WebIssuesFilterCondition(type, negate, attrId, attrValue);
    }

    public String getName() {
        return attrName;
    }

    public String getValue() {
        return attrValue;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public boolean isNegate() {
        return negate;
    }

    public void setNegate(boolean negate) {
        this.negate = negate;
    }

    public void setValue(String attrValue) {
        this.attrValue = attrValue;
    }

    public void setName(String attrName) {
        this.attrName = attrName;
    }
}
