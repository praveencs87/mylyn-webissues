package org.webissues.api;

import java.io.IOException;
import java.io.Serializable;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpException;

/**
 * Represents a single comment in an {@link IssueDetails} object.
 */
public class Comment extends AbstractChange implements Serializable {

    private static final long serialVersionUID = 1L;
    private String text;

    /**
     * Constructor for comment that is not yet attached to an issue.
     * 
     * @param issue issue this comment is attached to
     * @param text text of comment
     * @param user user that created comment
     */
    public Comment(String text, User user) {
        this(null, text, user);
    }

    /**
     * Constructor.
     * 
     * @param issue issue this comment is attached to
     * @param text text of comment
     * @param user user that created comment
     */
    public Comment(Issue issue, String text, User user) {
        super(issue, Type.COMMENT_ADDED, 0, Calendar.getInstance(), user, Calendar.getInstance(), user, null, null, null);
        this.text = text;
    }

    /**
     * Get the text of the comment.
     * 
     * @return text
     */
    public String getText() {
        return text;
    }

    /*
     * Internal constructor.
     */
    Comment(Issue issue, Change change, String text) {
        this(issue, change.getId(), text, change.getCreatedDate(), change.getCreatedUser(), change.getModifiedDate(), change
                        .getModifiedUser());
    }

    Comment(Issue issue, int id, String text, Calendar createdDate, User createdUser, Calendar modifiedDate, User modfiedUser) {
        super(issue, Type.COMMENT_ADDED, id, createdDate, createdUser, modifiedDate, modfiedUser, null, null, null);
        this.text = text;
    }
    
    /**
     * Delete this comment.
     * 
     * @throws IOException on any error
     * @throws ProtocolException 
     */
    public void delete(Operation operation) throws IOException, ProtocolException {
        final Client client = getClient();
        client.doCall(new Call<Boolean>() {
            public Boolean call() throws HttpException, IOException, ProtocolException {
                client.doCommand("DELETE COMMENT " + getId());
                return true;
            }
        }, operation);
    }
    
    /**
     * Edit this comment text.
     * 
     * @param operation operation
     * @param newText new comment text
     * @throws IOException on any error
     * @throws ProtocolException 
     */
    public void edit(Operation operation, final String newText) throws IOException, ProtocolException {
        final Client client = getClient();
        client.doCall(new Call<Boolean>() {
            public Boolean call() throws HttpException, IOException, ProtocolException {
                client.doCommand("EDIT COMMENT " + getId() + " '" + Util.escape(newText) + "'");
                return true;
            }
        }, operation);
    }

    private Client getClient() {
        final Client client = getIssue().getFolder().getProject().getProjects().getEnvironment().getClient();
        return client;
    }

    static Comment createFromResponse(Issue issue, List<String> response, IEnvironment environment) {
        Calendar date = Util.parseTimestampInSeconds(response.get(3));
        User user = environment.getUsers().get(Integer.parseInt(response.get(4)));
        return new Comment(issue, Integer.parseInt(response.get(1)), response.get(5), date, user, date, user);
    }

    static Comment createFromResponse(Issue issue, List<String> response, IEnvironment environment,
                                      Map<Integer, Change> changeMap) {
        if (response.size() != 3 || !response.get(0).equals("C")) {
            throw new IllegalArgumentException("Incorrect response. Expected 'C commentId 'comment'");
        }
        int id = Integer.parseInt(response.get(1));
        Change change = changeMap.get(id);
        if(change == null) {
            throw new Error("No change for ID " + id);
        }
        return new Comment(issue, change, response.get(2));
    }
}
