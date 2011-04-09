package net.sf.webissues.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpException;

/**
 * Represents a file attached to an issues.
 */
public class Attachment extends AbstractChange implements Serializable, Entity {

    private static final long serialVersionUID = 3247541221241784316L;

    // Private instance variables
    private String name;
    private long size;
    private String description;
    private IssueDetails issueDetails;

    /**
     * Constructor.
     * 
     * @param createdUser user that is creating this attachment
     * @param name the filename of the attachment
     * @param description a description of the attachment
     * @param size the size in bytes of the attachment
     */
    public Attachment(IssueDetails issueDetails, User createdUser, String name, String description, long size) {
        super(issueDetails, Type.FILE_ADDED, 0, Calendar.getInstance(), createdUser, Calendar.getInstance(), createdUser, null, null, null);
        this.description = description;
        this.name = name;
        this.size = size;
    }

    /*
     * Constructor used internally.
     */
    protected Attachment(IssueDetails issueDetails, int id, String name, Calendar createdDate, User createdUser, Calendar modifiedDate, User modifiedUser,
                         long size, String description) {
        super(issueDetails, Type.FILE_ADDED, id, createdDate, createdUser, modifiedDate, modifiedUser, null, null, null);
        this.name = name;
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
    protected static Attachment createFromResponse(IssueDetails issue, List<String> response, Environment environment) {
        if (response.size() != 8 || !response.get(0).equals("A")) {
            throw new IllegalArgumentException(
                            "Incorrect response. Expected 'A attachmentId issueId 'name' createdDate createdUser size 'description'");
        }
        Calendar date = Util.parseTimestampInSeconds(response.get(4));
        User user = environment.getUsers().get(Integer.parseInt(response.get(5)));
        return new Attachment(issue, Integer.parseInt(response.get(1)), response.get(3), date, user, date, user, Long.parseLong(response
                        .get(6)), response.get(7));
    }
    
    /**
     * Delete this attachment.
     * 
     * @throws IOException on any error
     * @throws ProtocolException 
     */
    public void delete(Operation operation) throws IOException, ProtocolException {
        final Client client = getIssueDetails().getIssue().getFolder().getProject().getProjects().getEnvironment().getClient();
        client.doCall(new Call<Boolean>() {
            public Boolean call() throws HttpException, IOException, ProtocolException {
                client.doCommand("DELETE ATTACHMENT " + getId());
                return true;
            }
        }, operation);
    }

    /**
     * Get content of an attachment.
     * 
     * @param attachmentId attachment ID
     * @param operation operation call-back
     * @return stream of attachment data
     * @throws HttpException on HTTP error
     * @throws IOException on any other IO error
     * @throws ProtocolException on error return by server or protocol problem
     */
    public InputStream getAttachmentData(Operation operation) throws HttpException, IOException,
                    ProtocolException {
        final Client client = getIssueDetails().getIssue().getFolder().getProject().getProjects().getEnvironment().getClient();
        return client.getAttachmentData(getId(), operation);

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
        return "Attachment [name=" + name + ", size=" + size + ", description=" + description + ", toString()=" + super.toString()
                        + "]";
    }

    public static Attachment createFromResponse(IssueDetails issueDetails, List<String> response, Environment environment,
                                                Map<Integer, Change> changeMap) {

        if (response.size() != 5 || !response.get(0).equals("A")) {
            throw new IllegalArgumentException(
                            "Incorrect response. Expected 'A fileId 'filename' fileSize fileData fileDescription fileStorage");
        }
        int id = Integer.parseInt(response.get(1));
        Change change = changeMap.get(id);
        if (change == null) {
            throw new Error("No change for ID " + id);
        }
        return new Attachment(issueDetails, id, response.get(2), change.getCreatedDate(), change.getCreatedUser(), change.getModifiedDate(),
                        change.getModifiedUser(), Long.parseLong(response.get(3)), response.get(4));
    }
}
