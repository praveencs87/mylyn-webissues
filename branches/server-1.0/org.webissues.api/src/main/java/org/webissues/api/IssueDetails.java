package org.webissues.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;

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
     * Add a new comment to the issue.
     * 
     * @param comment comment text
     * @param operation
     * @throws IOException
     * @throws ProtocolException
     */
    public void addComment(final Comment comment, Operation operation) throws IOException, ProtocolException {
        client.doCall(new Call<Object>() {
            public Object call() throws HttpException, IOException, ProtocolException {
                HttpMethod method = client.doCommand("ADD COMMENT " + issue.getId() + " '" + Util.escape(comment.getText()) + "'");
                try {
                    List<String> response = client.readResponse(method.getResponseBodyAsStream()).iterator().next();
                    comment.setId(Integer.parseInt(response.get(1)));
                    comments.add(comment);
                } finally {
                    method.releaseConnection();
                }
                return null;
            }
        }, operation);
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

    /**
     * Store the content of an attachment.
     * 
     * @param attachment attachment
     * @param inputStream attachment data
     * @param operation operation call-back
     * @throws HttpException on HTTP error
     * @throws IOException on any other IO error
     * @throws ProtocolException on error return by server or protocol problem
     */
    public void attachmentData(final Attachment attachment, final InputStream inputStream, final long length,
                                  final String contentType, Operation operation) throws HttpException, IOException,
                    ProtocolException {
        attachment.setId(getIssue().getFolder().getProject().getProjects().getEnvironment().getClient().putAttachmentData(getIssue().getId(), attachment.getName(), attachment.getDescription(), inputStream, length, contentType, operation));
    }

    @Override
    public String toString() {
        return "IssueDetails [attachments=" + attachments + ", changes=" + changes + ", comments=" + comments + ", issue=" + issue
                        + "]";
    }
}
