package org.webissues.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Contains all details about an issue including the {@link Issue} itself and
 * all of the {@link Comment}, {@link Change} and {@link Attachment} entities
 * that are associated with it.
 */
public class IssueDetails {

    private List<Comment> comments = new ArrayList<Comment>();
    private List<Change> changes = new ArrayList<Change>();
    private List<Attachment> attachments = new ArrayList<Attachment>();
    private Issue issue;
    private Client client;

    IssueDetails(Client client, Issue issue) {
        this.issue = issue;
        this.client = client;
    }

    /**
     * Get the issue.
     * 
     * @return issue
     */
    public Issue getIssue() {
        return issue;
    }

    /**
     * Get all comments.
     * 
     * @return comments
     */
    public Collection<Comment> getComments() {
        return comments;
    }

    /**
     * Get all changes.
     * 
     * @return changes
     */
    public Collection<Change> getChanges() {
        return changes;
    }

    /**
     * Get all attachments.
     * 
     * @return attachments
     */
    public Collection<Attachment> getAttachments() {
        return attachments;
    }

    @Override
    public String toString() {
        return "IssueDetails [attachments=" + attachments + ", changes=" + changes + ", comments=" + comments + ", issue=" + issue
                        + "]";
    }
}
