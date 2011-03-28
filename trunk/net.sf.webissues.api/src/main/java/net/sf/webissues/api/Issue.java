package net.sf.webissues.api;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

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
    private final Folder folder;

    private long stamp;

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

    protected static Issue createFromResponse(List<String> response, Environment environment) {
        int folderId = Integer.parseInt(response.get(2));
        return new Issue(Integer.parseInt(response.get(1)), Long.parseLong(response.get(4)), response.get(3), Util
                        .parseTimestampInSeconds(response.get(5)), environment.getUsers().get(Integer.parseInt(response.get(6))),
                        Util.parseTimestampInSeconds(response.get(7)), environment.getUsers()
                                        .get(Integer.parseInt(response.get(8))), environment.getProjects().getFolder(folderId));
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
                        + "]";
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
}
