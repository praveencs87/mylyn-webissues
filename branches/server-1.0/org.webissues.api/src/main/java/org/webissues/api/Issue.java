package org.webissues.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.httpclient.HttpException;

/**
 * Represents the main body of an issue. This is made up of a small number of
 * static details such as the title of the issue and the creating / modifying
 * user and date, and a map of {@link Attribute}s.
 * 
 * For more issue details, such as changes and attachments, see
 * {@link IssueDetails}.
 */
public class Issue extends HashMap<Attribute, String> implements Entity, NamedEntity {

    private static final long serialVersionUID = -7659698729892890054L;

    private int id;
    private String name;
    private Calendar createdDate;
    private User createdUser;
    private Calendar modifiedDate;
    private User modifiedUser;
    private Folder folder;

    private long stamp;

    private boolean read;

    /**
     * Constructor.
     * 
     * @param name name (or summary) of the issue
     * @param createdUser the user creating the issue
     * @param folder folder the issue belongs to
     */
    public Issue(String name, User createdUser, Folder folder) {
        super();
        this.name = name;
        this.createdUser = createdUser;
        this.modifiedUser = createdUser;
        this.folder = folder;
    }

    protected Issue(int id, long stamp, String name, Calendar createdDate, User createdUser, Calendar modifiedDate,
                    User modifiedUser, Folder folder) {
        super();
        this.id = id;
        this.stamp = stamp;
        this.name = name;
        this.createdDate = createdDate;
        this.createdUser = createdUser;
        this.modifiedDate = modifiedDate;
        this.modifiedUser = modifiedUser;
        this.folder = folder;
    }

    protected static Issue createFromResponse(List<String> response, IEnvironment environment) {
        int issueId = Integer.parseInt(response.get(1));
        int folderId = Integer.parseInt(response.get(2));
        String issueName = response.get(3);
        long stamp = Long.parseLong(response.get(4));
        Calendar created = Util.parseTimestampInSeconds(response.get(5));
        User createdBy = environment.getUsers().get(Integer.parseInt(response.get(6)));
        Calendar modified = Util.parseTimestampInSeconds(response.get(7));
        User modifiedBy = environment.getUsers().get(Integer.parseInt(response.get(8)));
        return new Issue(issueId, stamp, issueName, created, createdBy, modified, modifiedBy, environment.getProjects().getFolder(
            folderId));
    }

    /**
     * Get if this issue is <strong>new</strong>, i.e. it has had no
     * modifications.
     * 
     * @return new
     */
    public boolean isNew() {
        return stamp == 0;
    }

    /**
     * Delete this issue.
     * 
     * @param operation operation
     * @throws ProtocolException
     * @throws IOException
     */
    public void delete(Operation operation) throws IOException, ProtocolException {
        getFolder().getProject().getProjects().getEnvironment().getClient().deleteIssue(getId(), operation);
    }

    /**
     * Rename this issue.
     * 
     * @param operation operation
     * @throws ProtocolException
     * @throws IOException
     */
    public void rename(String newName, Operation operation) throws IOException, ProtocolException {
        getFolder().getProject().getProjects().getEnvironment().getClient().renameIssue(getId(), newName, operation);
        this.name = newName;
    }

    /**
     * Move this issue to another folder
     * 
     * @param operation operation
     * @throws ProtocolException
     * @throws IOException
     */
    public void moveTo(Folder newFolder, Operation operation) throws IOException, ProtocolException {
        getFolder().getProject().getProjects().getEnvironment().getClient().moveIssue(getId(), operation, newFolder.getId());
        this.folder = newFolder;
    }

    /**
     * Get the folder the issue belongs to.
     * 
     * @return folder
     */
    public Folder getFolder() {
        return folder;
    }

    /**
     * Get the issue ID
     * 
     * @return issue ID
     */
    public int getId() {
        return id;
    }

    /**
     * Get the name of the issue.
     * 
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the date the issue was created.
     * 
     * @return date issue created
     */
    public Calendar getCreatedDate() {
        return createdDate;
    }

    /**
     * Get the user the created the issue.
     * 
     * @return creating user
     */
    public User getCreatedUser() {
        return createdUser;
    }

    /**
     * Get the date the issue was modified.
     * 
     * @return date issue modified
     */
    public Calendar getModifiedDate() {
        return modifiedDate;
    }

    /**
     * Get the user that last modified the issue
     * 
     * @return last modifying user
     */
    public User getModifiedUser() {
        return modifiedUser;
    }

    @Override
    public String toString() {
        return "Issue [id=" + id + ", name=" + name + ", stamp=" + stamp + ", folder=" + folder + ", createdDate=" + createdDate
                        + ", createdUser=" + createdUser + ", modifiedDate=" + modifiedDate + ", modifiedUser=" + modifiedUser
                        + ", read=" + read + "]";
    }

    /**
     * Get an {@link Attribute} given its name.
     * 
     * @param name name
     * @return attribute or <code>null</code> if no such attribute exists
     */
    public String getAttributeValueByName(String name) {
        for (Attribute attr : keySet()) {
            if (attr.getName().equals(name)) {
                return get(attr);
            }
        }
        return null;
    }

    /**
     * Get the stamp. This is increase every time a modification is made
     * 
     * @return stamp
     */
    public long getStamp() {
        return stamp;
    }

    /**
     * Set the 'read' status of the usses
     * 
     * @param operation operation
     * @param read read
     * 
     * @throws IOException on any error
     * @throws ProtocolException
     */
    public void setRead(Operation operation, boolean read) throws IOException, ProtocolException {
        getFolder().getProject().getProjects().getEnvironment().getClient().setFolderRead(getId(), read, operation);
        this.read = read;
    }

    /**
     * Get the issue details.
     * 
     * @param op operations
     * @return details
     * @throws ProtocolException
     * @throws IOException
     * @throws HttpException
     */
    public IssueDetails getIssueDetails(Operation op) throws HttpException, IOException, ProtocolException {
        return getFolder().getProject().getProjects().getEnvironment().getClient().getIssueDetails(getId(), op);
    }

    protected void setRead(boolean read) {
        this.read = read;
    }
}
