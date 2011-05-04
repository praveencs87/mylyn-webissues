package org.webissues.api;

import java.io.IOException;
import java.io.Serializable;

import org.apache.commons.httpclient.HttpException;

/**
 * Represents a single Project. A project consists of many {@link Folder}s.
 */
public class Project extends NamedEntityMap<Folder> implements Serializable, NamedEntity, Comparable<Project> {

    private static final long serialVersionUID = -5170541898454267445L;

    private int id;
    private String name;
    private Projects projects;

    /**
     * Constructor.
     * 
     * @param projects projects list
     * @param id id of project
     * @param name name of project
     */
    public Project(Projects projects, int id, String name) {
        super();
        this.id = id;
        this.name = name;
        this.projects = projects;
    }
    
    /**
     * Rename this project.
     * 
     * @throws IOException on any error
     * @throws ProtocolException 
     */
    public void rename(Operation operation, final String newName) throws IOException, ProtocolException {
        final Client client = getProjects().getEnvironment().getClient();
        client.doCall(new Call<Boolean>() {
            public Boolean call() throws HttpException, IOException, ProtocolException {
                client.doCommand("RENAME PROJECT " + id + " '" + Util.escape(newName) + "'");
                Project.this.name = newName;
                return true;
            }
        }, operation);
    }
    
    /**
     * Delete this project.
     * 
     * @throws IOException on any error
     * @throws ProtocolException 
     */
    public void delete(Operation operation) throws IOException, ProtocolException {
        final Client client = getProjects().getEnvironment().getClient();
        client.doCall(new Call<Boolean>() {
            public Boolean call() throws HttpException, IOException, ProtocolException {
                client.doCommand("DELETE PROJECT " + id);
                getProjects().remove(Project.this);
                return true;
            }
        }, operation);
    }

    /**
     * Get the parent projects list.
     * 
     * @return projects
     */
    public Projects getProjects() {
        return projects;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Project other = (Project) obj;
        if (id != other.id)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    @Override
    public String toString() {
        return "Project [folders=" + super.toString() + ", id=" + id + ", name=" + name + "]";
    }

    public int compareTo(Project o) {
        return name.compareTo(o.name);
    }

}
