package net.sf.webissues.api;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * Represents a single comment in an {@link IssueDetails} object.
 */
public class Comment extends AbstractChange {

    private final IssueDetails issueDetails;
    private String text;

    /**
     * Constructor.
     * 
     * @param issue issue this comment is attached to
     * @param text text of comment
     * @param user user that created comment
     */
    public Comment(IssueDetails issue, String text, User user) {
        super(Type.COMMENT_ADDED, 0, Calendar.getInstance(), user, Calendar.getInstance(), user, null, null, null);
        this.issueDetails = issue;
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

    public IssueDetails getIssueDetails() {
        return issueDetails;
    }

    /*
     * Internal constructor.
     */
    Comment(IssueDetails issue, Change change, String text) {
        this(issue, change.getId(), text, change.getCreatedDate(), change.getCreatedUser(), change.getModifiedDate(), change
                        .getModifiedUser());
    }

    Comment(IssueDetails issue, int id, String text, Calendar createdDate, User createdUser, Calendar modifiedDate, User modfiedUser) {
        super(Type.COMMENT_ADDED, id, createdDate, createdUser, modifiedDate, modfiedUser, null, null, null);
        this.issueDetails = issue;
        this.text = text;
    }

    static Comment createFromResponse(IssueDetails issueDetails, List<String> response, Environment environment) {
        Calendar date = Util.parseTimestampInSeconds(response.get(3));
        User user = environment.getUsers().get(Integer.parseInt(response.get(4)));
        return new Comment(issueDetails, Integer.parseInt(response.get(1)), response.get(5), date, user, date, user);
    }

    static Comment createFromResponse(IssueDetails issueDetails, List<String> response, Environment environment,
                                      Map<Integer, Change> changeMap) {
        if (response.size() != 3 || !response.get(0).equals("C")) {
            throw new IllegalArgumentException("Incorrect response. Expected 'C commentId 'comment'");
        }
        int id = Integer.parseInt(response.get(1));
        Change change = changeMap.get(id);
        if(change == null) {
            throw new Error("No change for ID " + id);
        }
        return new Comment(issueDetails, change, response.get(2));
    }
}
