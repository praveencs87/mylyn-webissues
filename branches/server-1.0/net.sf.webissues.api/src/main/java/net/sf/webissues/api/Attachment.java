package net.sf.webissues.api;

import java.io.Serializable;
import java.util.Calendar;
import java.util.List;

/**
 * Represents a file attached to an issues.
 */
public class Attachment implements Serializable, Entity {

    private static final long serialVersionUID = 3247541221241784316L;

    // Private instance variables
    private int id;
    private String name;
    private Calendar createdDate;
    private User createdUser;
    private long size;
    private String description;

    /**
     * Constructor.
     * 
     * @param createdUser user that is creating this attachment
     * @param name the filename of the attachment
     * @param description a description of the attachment
     * @param size the size in bytes of the attachment
     */
    public Attachment(User createdUser, String name, String description, long size) {
        this.createdUser = createdUser;
        this.description = description;
        this.name = name;
        this.size = size;
    }

    /*
     * Constructor used internally.
     */
    protected Attachment(int id, String name, Calendar createdDate, User createdUser, long size, String description) {
        super();
        this.id = id;
        this.name = name;
        this.createdDate = createdDate;
        this.createdUser = createdUser;
        this.size = size;
        this.description = description;
    }

    /**
     * Create an attachment from a response of the <code>GET DETAILS</code>
     * command. The response for an attachment is in the format :-
     * 
     * <pre>
     *      A attachmentId issueId 'name' createdDate createdUser size 'description'
     * </pre>
     * 
     * @param response
     * @param environment
     * @return attachment
     * @throws IllegalArgumentException if response is incorrect size or not an
     *         attachment response
     */
    protected static Attachment createFromResponse(List<String> response, Environment environment) {
        if (response.size() != 8 || !response.get(0).equals("A")) {
            throw new IllegalArgumentException(
                            "Incorrect response. Expected 'A attachmentId issueId 'name' createdDate createdUser size 'description'");
        }
        return new Attachment(Integer.parseInt(response.get(1)), response.get(3), Util.parseTimestampInSeconds(response.get(4)),
                        environment.getUsers().get(Integer.parseInt(response.get(5))), Long.parseLong(response.get(6)), response
                                        .get(7));
    }

    public int getId() {
        return id;
    }

    /**
     * Get file filename of the attachment.
     * 
     * @return filename
     */
    public String getName() {
        return name;
    }

    /**
     * Get the date the attachment was created.
     * 
     * @return created date
     */
    public Calendar getCreatedDate() {
        return createdDate;
    }

    /**
     * Get the user that created the attachment.
     * 
     * @return user that created attachment
     */
    public User getCreatedUser() {
        return createdUser;
    }

    /**
     * Get the size in bytes of the attachment.
     * 
     * @return size in bytes of attachment
     */
    public long getSize() {
        return size;
    }

    /**
     * Get a description of the attachment.
     * 
     * @return description
     */
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "Attachment [createdDate=" + Util.formatDateTime(createdDate) + ", createdUser=" + createdUser + ", description="
                        + description + ", id=" + id + ", name=" + name + ", size=" + size + "]";
    }

    protected void setId(int id) {
        this.id = id;
    }
}
