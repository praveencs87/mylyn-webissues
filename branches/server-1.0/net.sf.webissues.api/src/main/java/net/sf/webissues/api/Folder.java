package net.sf.webissues.api;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;

/**
 * Each {@link Project} may contain many {@link Folder}s and is of a single
 * {@link Type}. All issues must belong to a folder, so this class provides this
 * {@link #getIssues(Operation, long)} method retrieve all issues in the folder.
 */
public class Folder implements Serializable, Entity, NamedEntity {

    private static final long serialVersionUID = -8269970767878718415L;

    private int id;
    private String name;
    private Type type;
    private int stamp;
    private final Client client;
    private final Project project;

    protected Folder(Client client, Project project, int id, String name, Type type, int stamp) {
        super();
        this.client = client;
        this.id = id;
        this.name = name;
        this.type = type;
        this.stamp = stamp;
        this.project = project;
    }

    /**
     * Delete this folder.
     * 
     * @throws IOException on any error
     * @throws ProtocolException
     */
    public void delete(Operation operation) throws IOException, ProtocolException {
        client.doCall(new Call<Boolean>() {
            public Boolean call() throws HttpException, IOException, ProtocolException {
                client.doCommand("DELETE FOLDER " + id);
                project.remove(Folder.this);
                return true;
            }
        }, operation);
    }

    /**
     * Rename this folder.
     * 
     * @throws IOException on any error
     * @throws ProtocolException
     */
    public void rename(Operation operation, final String newName) throws IOException, ProtocolException {
        client.doCall(new Call<Boolean>() {
            public Boolean call() throws HttpException, IOException, ProtocolException {
                client.doCommand("RENAME FOLDER " + id + " '" + Util.escape(newName) + "'");
                Folder.this.name = newName;
                return true;
            }
        }, operation);
    }

    /**
     * Set the 'read' status of the folder
     * 
     * @param operation operation
     * @param read read
     * @throws IOException on any error
     * @throws ProtocolException
     */
    public void setRead(Operation operation, boolean read) throws IOException, ProtocolException {
        client.setFolderRead(getId(), read, operation);
    }

    /**
     * Get the project this folder belongs to.
     * 
     * @return project
     */
    public Project getProject() {
        return project;
    }

    /**
     * Get a list of all issues contained in this folder, optionally after a
     * specified date / time.
     * 
     * @param operation
     * @param stamp only collects issues that were last changed on or after the
     *        supplied stamp.
     * @return list of issues
     * @throws HttpException on any HTTP error
     * @throws IOException on any I/O error
     * @throws ProtocolException on any protocol error
     */
    public Collection<Issue> getIssues(Operation operation, final long stamp) throws HttpException, IOException, ProtocolException {
        return client.doCall(new Call<Collection<Issue>>() {
            public Collection<Issue> call() throws HttpException, IOException, ProtocolException {
                Map<Integer, Issue> issues = new HashMap<Integer, Issue>();
                HttpMethod method = client.doCommand("LIST ISSUES " + id + " " + stamp);
                try {
                    for (List<String> response : client.readResponse(method.getResponseBodyAsStream())) {
                        if (response.get(0).equals("V")) {
                            int attributeId = Integer.parseInt(response.get(1));
                            int issueId = Integer.parseInt(response.get(2));
                            Issue issue = issues.get(issueId);
                            if (issue == null) {
                                throw new Error("Expected issue before attribute");
                            }
                            issue.put(client.getEnvironment().getTypes().getAttribute(attributeId), response.get(3));
                        } else if (response.get(0).equals("I")) {
                            Issue issue = Issue.createFromResponse(response, client.getEnvironment());
                            int folderId = Integer.parseInt(response.get(2));
                            if (folderId != id) {
                                throw new Error("Unexpected folderId");
                            }
                            issues.put(issue.getId(), issue);
                        } else if (response.get(0).equals("F")) {
                            // We already have this
                        } else {
                            Client.LOG.warn("Unexpected issues response \"" + response + "\"");
                        }
                    }
                } finally {
                    method.releaseConnection();
                }

                // States
                method = client.doCommand("LIST STATES " + stamp);
                try {
                    for (List<String> response : client.readResponse(method.getResponseBodyAsStream())) {
                        if (response.get(0).equals("S")) {
                            int stateId = Integer.parseInt(response.get(1));
                            int issueId = Integer.parseInt(response.get(2));
                            int readId = Integer.parseInt(response.get(3));
                            Issue issue  = issues.get(issueId);
                            if (issue == null) {
                                throw new Error("Expected issue for " + issueId + " before state");
                            }
                            issue.setRead(readId == 1);
                        } else {
                            Client.LOG.warn("Unexpected states response \"" + response + "\"");
                        }
                    }
                } finally {
                    method.releaseConnection();
                }
                return issues.values();
            }
        }, operation);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * Get the type of issue this folder contains.
     * 
     * @return folder issue type
     */
    public Type getType() {
        return type;
    }

    /**
     * The stamp value increases for each modifcation made.
     * 
     * @return stamp
     */
    public int getStamp() {
        return stamp;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof Folder && id == ((Folder) obj).id;
    }

    @Override
    public int hashCode() {
        return getId();
    }

    @Override
    public String toString() {
        return "Folder [id=" + id + ", name=" + name + ", stamp=" + stamp + ", typeId=" + type + "]";
    }
}
