package net.sf.webissues.api;

import java.util.Calendar;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Represents a single change made to an issue.
 */
public class Change implements Entity {

    private final static Log LOG = LogFactory.getLog(Change.class);

    // Private instance variables
    private int id;
    private Calendar modifiedDate;
    private User modifiedUser;
    private Attribute attribute;
    private String oldValue;
    private String newValue;

    /*
     * Internal constructor.
     */
    protected Change(int id, Calendar modifiedDate, User modifiedUser, Attribute attribute, String oldValue, String newValue) {
        super();
        this.id = id;
        this.modifiedDate = modifiedDate;
        this.modifiedUser = modifiedUser;
        this.attribute = attribute;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    /**
     * Create an attachment from a response of the <code>GET DETAILS</code>
     * command. The response for an attachment is in the format :-
     * 
     * <pre>
     *      H changeId issueId modifiedDate modifiedUser attributeId 'oldValue' 'newValue'
     * </pre>
     * 
     * @param response
     * @param environment
     * @return attachment
     * @throws IllegalArgumentException if response is incorrect size or not an
     *         attachment response
     */
    static Change createFromResponse(List<String> response, Users users, Environment environment) {
        if (response.size() != 8 || !response.get(0).equals("H")) {
            throw new IllegalArgumentException(
                            "Incorrect response. Expected 'A attachmentId issueId 'name' createdDate createdUser size 'description'");
        }
        int attributeId = Integer.parseInt(response.get(5));
        Attribute attributeObject = environment.getTypes().getAttribute(attributeId);
        if(attributeObject == null) {
            LOG.debug("Invalid change attribute ID " + attributeId + ", will assume this is the title?");
        }
        return new Change(Integer.parseInt(response.get(1)), Util.parseTimestampInSeconds(response.get(3)), users.get(Integer
                        .parseInt(response.get(4))), attributeObject,
                        response.get(6), response.get(7));
    }

    public int getId() {
        return id;
    }

    /**
     * Get the date the change was made.
     * 
     * @return date
     */
    public Calendar getModifiedDate() {
        return modifiedDate;
    }

    /**
     * Get the user that made the change
     * 
     * @return user
     */
    public User getModifiedUser() {
        return modifiedUser;
    }

    /**
     * Get the attribute that was change.
     * 
     * @return attribute
     */
    public Attribute getAttribute() {
        return attribute;
    }

    /**
     * Get the old value of the attribute.
     * 
     * @return old value
     */
    public String getOldValue() {
        return oldValue;
    }

    /**
     * Get the new value of the attribute.
     * 
     * @return new value
     */
    public String getNewValue() {
        return newValue;
    }

    @Override
    public String toString() {
        return "Change [attribute=" + attribute + ", id=" + id + ", modifiedDate=" + Util.formatDateTime(modifiedDate)
                        + ", modifiedUser=" + modifiedUser + ", newValue=" + newValue + ", oldValue=" + oldValue + "]";
    }

}
