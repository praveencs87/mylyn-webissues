package net.sf.webissues.api;

import java.util.Calendar;
import java.util.List;

/**
 * Represents a single comment in an {@link IssueDetails} object.
 */
public class Comment {

    private int id;
    private String text;
    private Calendar createdDate;
    private User createdUser;
    private final IssueDetails issueDetails;

    /**
     * Constructor.
     * 
     * @param issue issue this comment is attached to
     * @param text text of comment
     * @param user user that created comment
     */
    public Comment(IssueDetails issue, String text, User user) {
        this.issueDetails = issue;
        this.text = text;
        this.createdUser = user;
        this.createdDate = Calendar.getInstance();
    }

    /**
     * Get the comment ID.
     * 
     * @return ID
     */
    public int getId() {
        return id;
    }

    /**
     * Get the text of the comment
     * 
     * @return text
     */
    public String getText() {
        return text;
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

    @Override
    public String toString() {
        return "Comment [createdDate=" + Util.formatDateTime(createdDate) + ", createdUser=" + createdUser + ", id=" + id
                        + ", text=" + text + "]";
    }

    public IssueDetails getIssueDetails() {
        return issueDetails;
    }

    /*
     * Internal constructor.
     */
    Comment(IssueDetails issue, int id, String text, Calendar createdDate, User createdUser) {
        super();
        this.issueDetails = issue;
        this.id = id;
        this.text = text;
        this.createdDate = createdDate;
        this.createdUser = createdUser;
    }

    static Comment createFromResponse(IssueDetails issueDetails, List<String> response, Users users) {
        return new Comment(issueDetails, Integer.parseInt(response.get(1)), response.get(5), Util.parseTimestampInSeconds(response
                        .get(3)), users.get(Integer.parseInt(response.get(4))));
    }

    protected void setId(int id) {
        this.id = id;
    }
}
