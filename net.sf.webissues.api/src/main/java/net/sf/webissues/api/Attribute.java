package net.sf.webissues.api;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.httpclient.HttpException;

/**
 * Represents an Attribute. The actual properties available will depend on the
 * {@link AttributeType} of data the attribute will hold.
 * <p>
 * Some properties are common to all types, such as {@link #isRequired()} and
 * {@link #getDefaultValue()}. Others wil depend on the {@link AttributeType} (see the
 * document for the type constants for details).
 */
public class Attribute implements Entity, NamedEntity, Serializable, Comparable<Attribute> {

    private static final long serialVersionUID = 2062539745488858480L;

    // Private instance variables
    private int id;
    private String name;
    private String definition;
    private IssueType type;
    private long minValue = Long.MIN_VALUE;
    private long maxValue = Long.MAX_VALUE;
    private long maxLength = Long.MAX_VALUE;
    private int decimalPlaces = 0;
    private boolean required;
    private String defaultValue;
    private boolean dateOnly;
    private boolean membersOnly;
    private Collection<String> options;
    private AttributeType attributeType;
    private Client client;
    private boolean builtIn;
    private boolean forViewFilter = true;

    private boolean fixedPosition;

    /**
     * The type of data this attribute will hold.
     */
    public enum AttributeType {
        /**
         * Enumeration of values. The values available can be retrieved using
         * {@link Attribute#getOptions()}.
         */
        ENUM,
        /**
         * Numeric value. Can option has minimum or maximum values which may be
         * retried using {@link Attribute#getMinValue()} and
         * {@link Attribute#getMaxValue()}. If either of these are ommited they
         * will default to {@link Long#MIN_VALUE} and
         * {@link Long#MAX_VALUE} respectively.
         */
        NUMERIC,
        /**
         * A list of available {@link User}s. The list may optionally be
         * restricted to project member if {@link Attribute#isMembersOnly()} is
         * <code>true</code>
         */
        USER,
        /**
         * A text value. May optionally have a maximum length as returned by
         * {@link Attribute#getMaxLength()}. If no length is specified, the text
         * has no practical size limit.
         */
        TEXT,
        /**
         * A date only, or date and time depending on whether
         * {@link Attribute#isDateOnly()} is set.
         */
        DATETIME;

        /**
         * Parse the response from the server for this type and configure the
         * attribute properties accordingly.
         * 
         * @param args response arguments
         * @param attribute attribute to configure
         */
        public void parse(List<String> args, Attribute attribute) {
            attribute.required = getIntAttribute(args, "required", 0) == 1;
            attribute.defaultValue = getStringAttribute(args, "default", null);
            switch (this) {
                case ENUM:
                    attribute.options = getEnumAttribute(args, "items", null);
                    break;
                case TEXT:
                    attribute.maxLength = getIntAttribute(args, "max-length", Integer.MAX_VALUE);
                    break;
                case USER:
                    attribute.membersOnly = getIntAttribute(args, "members", 0) == 1;
                    break;
                case DATETIME:
                    attribute.dateOnly = getIntAttribute(args, "time", 0) == 0;
                    break;
                case NUMERIC:
                    attribute.minValue = getIntAttribute(args, "min-value", Integer.MIN_VALUE);
                    attribute.maxValue = getIntAttribute(args, "max-value", Integer.MIN_VALUE);
                    attribute.decimalPlaces = getIntAttribute(args, "decimal", 0);
                    break;
            }
        }

        private String getStringAttribute(List<String> args, String name, String defaultValue) {
            for (String arg : args) {
                if (arg.startsWith(name + "=\"")) {
                    return arg.substring(name.length() + 2, arg.length() - 1);
                }
            }
            return defaultValue;
        }

        private Collection<String> getEnumAttribute(List<String> args, String name, String defaultValue) {
            for (String arg : args) {
                if (arg.startsWith(name + "={")) {
                    String line = arg.substring(name.length() + 2, arg.length() - 1);
                    return Util.parseLine(line, ',', '"');
                }
            }
            return Collections.emptyList();
        }

        private int getIntAttribute(List<String> args, String name, int defaultValue) {
            for (String arg : args) {
                if (arg.startsWith(name + "=")) {
                    String substring = arg.substring(name.length() + 1);
                    if (substring.startsWith("\"")) {
                        substring = substring.substring(1, substring.length() - 1);
                    }
                    return Integer.parseInt(substring);
                }
            }
            return defaultValue;
        }
    }

    /*
     * Internal constructor.
     */
    protected Attribute(IssueType type, int id, String name, String definition, boolean builtIn) {
        super();
        this.builtIn = builtIn;
        this.type = type;
        this.id = id;
        this.name = name;
        this.definition = definition;
        parseDefinition();
    }
    
    protected Attribute(IssueType type, int id, String name, AttributeType attributeType, boolean builtIn) {
        super();
        this.builtIn = builtIn;
        this.type = type;
        this.attributeType = attributeType;
        this.id = id;
        this.name = name;
    }

    /**
     * Get if this attribute is built in (i.e. not definied on the server. The ID and name
     * attributes are examples of built in attributes. They are only really here to provide
     * definitions that may be used in {@link View} definitions.
     * 
     * @return built in
     */
    public boolean isBuiltIn() {
        return builtIn;
    }
    
    /**
     * Delete this attribute.
     * 
     * @param operation operation
     * 
     * @throws IOException on any error
     * @throws ProtocolException 
     */
    public void delete(Operation operation) throws IOException, ProtocolException {
        client.doCall(new Call<Boolean>() {
            public Boolean call() throws HttpException, IOException, ProtocolException {
                client.doCommand("DELETE ATTRIBUTE " + getId());
                type.remove(getId());
                return true;
            }
        }, operation);
    }
    
    /**
     * Rename this attribute.
     * 
     * @param operation operation
     * @param newName new name
     * 
     * @throws IOException on any error
     * @throws ProtocolException 
     */
    public void rename(Operation operation, final String newName) throws IOException, ProtocolException {
        client.doCall(new Call<Boolean>() {
            public Boolean call() throws HttpException, IOException, ProtocolException {
                client.doCommand("RENAME ATTRIBUTE " + getId() + " '" + Util.escape(newName) + "'");
                Attribute.this.name = newName;
                return true;
            }
        }, operation);
    }
    
    /**
     * Modify the attribute definition.
     * 
     * @param operation operation
     * @param newDefinition new definition
     * 
     * @throws IOException on any error
     * @throws ProtocolException 
     */
    public void modify(Operation operation, final String newDefinition) throws IOException, ProtocolException {
        client.doCall(new Call<Boolean>() {
            public Boolean call() throws HttpException, IOException, ProtocolException {
                client.doCommand("MODIFY ATTRIBUTE " + getId() + " '" + Util.escape(newDefinition) + "'");
                Attribute.this.definition = newDefinition;
                return true;
            }
        }, operation);
    }
    
    /**
     * Get whether this attribute is supported in view filters
     * 
     * @return for view filter
     */
    public final boolean isForViewFilter() {
        return forViewFilter;
    }

    /**
     * Get whether this attribute is fixed (i.e. cannot be moved or
     * removed from a view filter).
     * 
     * @return fixed position attribute
     */
    public boolean isFixedPosition() {
        return fixedPosition;
    }

    /**
     * Get all of the options available for this attribute. Will be
     * <code>null</code> unless the type is {@link AttributeType#ENUM}.
     * 
     * @return options
     */
    public final Collection<String> getOptions() {
        return options;
    }

    /**
     * Get the default value.
     * 
     * @return default value
     */
    public final String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Get if the user list should contain only members. Only relevant if the
     * type is {@link AttributeType#USER}.
     * 
     * @return user list should contain only members
     */
    public final boolean isMembersOnly() {
        return membersOnly;
    }

    /**
     * Get the maximum length of the string. Only relevant if the type is
     * {@link AttributeType#TEXT}.
     * 
     * @return maximum length
     */
    public long getMaxLength() {
        return maxLength;
    }

    /**
     * Get if the value should contain only a date (i.e. no time). Only relevant
     * is the type is {@link AttributeType#DATETIME}.
     * 
     * @return date only
     */
    public boolean isDateOnly() {
        return dateOnly;
    }

    public int getId() {
        return id;
    }

    /**
     * Get the name of this attribute.
     * 
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Get if this attribute requires a value. Applies to all types.
     * 
     * @return attribute requires a value.
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * Get the type of attribute. See {@link AttributeType} for more details.
     * 
     * @return attribute type
     */
    public AttributeType getAttributeType() {
        return attributeType;
    }

    /**
     * Get the issue type the attribute is attached too.
     * 
     * @return type
     */
    public IssueType getType() {
        return type;
    }

    /**
     * Get the maximum number of decimal places allowed. Only applicable if the type is
     * {@link AttributeType#NUMERIC}. If there is no minimum value, the default is 0 (i.e an
     * integer).
     * 
     * @return decimal places
     */
    public int getDecimalPlaces() {
        return decimalPlaces;
    }

    /**
     * Get the minimum value of the attribute. Only applicable if the type is
     * {@link AttributeType#NUMERIC}. If there is no minimum value, the default is
     * {@link Long#MIN_VALUE}.
     * 
     * @return minimum value
     */
    public long getMinValue() {
        return minValue;
    }

    /**
     * Get the maximum value of the attribute. Only applicable if the type is
     * {@link AttributeType#NUMERIC}. If there is no maximum value, the default is
     * {@link Long#MAX_VALUE}.
     * 
     * @return maximum value
     */
    public long getMaxValue() {
        return maxValue;
    }

    public void setDateOnly(boolean dateOnly) {
        this.dateOnly = dateOnly;
    }

    @Override
    public String toString() {
        return "Attribute [id=" + id + ", name=" + name + "]";
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof Attribute && id == ((Attribute)obj).id; 
    }

    @Override
    public int hashCode() {
        return getId();
    }

    public int compareTo(Attribute o) {
        return name.compareTo(o.name);
    }

    protected void setName(String name) {
        this.name = name;
    }
    
    protected  void setFixedPosition(boolean fixedPosition) {
        this.fixedPosition = fixedPosition;
    }

    protected void setDefinition(String definition) {
        this.definition = definition;
    }

    protected void parseDefinition() {
        List<String> args = Util.parseLine(definition, ' ', '\'');
        attributeType = AttributeType.valueOf(args.get(0));
        args.remove(0);
        attributeType.parse(args, this);
    }
    
    protected void setForViewFilter(boolean forViewFilter) {
        this.forViewFilter = forViewFilter;
    }
}
