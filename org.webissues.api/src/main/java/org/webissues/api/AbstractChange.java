package org.webissues.api;

import java.util.Calendar;

/**
 * Abstract representation of a single change made to an issue. Prior to
 * 1.0-alpha, the only changes recorded were attribute changes (and so other
 * classes that extend this one are not included in the issues history). Since
 * 1.0-alpha, more operation are considered "Changes", including comments,
 * attachments, title changes and others.
 */
public abstract class AbstractChange implements Entity {

    public enum Type {
        ISSUE_CREATED("Issue Created"), ISSUE_RENAMED("Issue Renamed"), VALUE_CHANGED("Value changed"), COMMENT_ADDED("Comment"), FILE_ADDED(
                        "File attached"), ISSUE_MOVED("Issue moved");

        private String label;

        private Type(String label) {
            this.label = label;
        }

        public final static Type fromCode(int code) {
            switch (code) {
                case 0:
                    return ISSUE_CREATED;
                case 1:
                    return ISSUE_RENAMED;
                case 2:
                    return VALUE_CHANGED;
                case 3:
                    return FILE_ADDED;
                case 4:
                    return COMMENT_ADDED;
                case 5:
                    return ISSUE_MOVED;

            }
            throw new IllegalArgumentException();
        }

        public final static int toCode(Type type) {
            switch (type) {
                case ISSUE_CREATED:
                    return 0;
                case ISSUE_RENAMED:
                    return 0;
                case VALUE_CHANGED:
                    return 0;
                case FILE_ADDED:
                    return 0;
                case COMMENT_ADDED:
                    return 0;
                case ISSUE_MOVED:
                    return 0;
            }
            throw new IllegalArgumentException();
        }

        public String getLabel() {
            return label;
        }
    }

    // Private instance variables
    private int id;
    private Calendar modifiedDate;
    private User modifiedUser;
    private Calendar createdDate;
    private User createdUser;
    private Attribute attribute;
    private String oldValue;
    private String newValue;
    private Type type;
    private IssueDetails issueDetails;

    /*
     * Internal constructor.
     */
    protected AbstractChange(IssueDetails issueDetails, Type type, int id, Calendar createdDate, User createdUser,
                             Calendar modifiedDate, User modifiedUser, Attribute attribute, String oldValue, String newValue) {
        super();
        this.issueDetails = issueDetails;
        this.type = type;
        this.id = id;
        this.modifiedDate = modifiedDate;
        this.modifiedUser = modifiedUser;
        this.createdDate = createdDate;
        this.createdUser = createdUser;
        this.attribute = attribute;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public int getId() {
        return id;
    }

    /**
     * Get the issue this change is attached to.
     * 
     * @return issue
     */
    public IssueDetails getIssueDetails() {
        return issueDetails;
    }

    /**
     * Get the date the comment was created.
     * 
     * @return date
     */
    public Calendar getCreatedDate() {
        return createdDate;
    }

    /**
     * Get the user that created this comment.
     * 
     * @return user
     */
    public User getCreatedUser() {
        return createdUser;
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

    /**
     * Get the change type.
     * 
     * @return change type
     */
    public Type getType() {
        return type;
    }

    /**
     * Set the change type.
     * 
     * @param type type
     */
    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "AbstractChange [id=" + id + ", modifiedDate=" + modifiedDate + ", modifiedUser=" + modifiedUser + ", attribute="
                        + attribute + ", oldValue=" + oldValue + ", newValue=" + newValue + ", type=" + type + "]";
    }

    protected void setId(int id) {
        this.id = id;
    }

}
