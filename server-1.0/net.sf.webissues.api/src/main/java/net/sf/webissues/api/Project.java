package net.sf.webissues.api;

import java.io.Serializable;

/**
 * Represents a single Project. A project consists of many {@link Folder}s.
 */
public class Project extends EntityMap<Folder> implements Serializable, NamedEntity, Comparable<Project> {

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
        return obj != null && obj instanceof Project && id == ((Project) obj).id;
    }

    @Override
    public int hashCode() {
        return getId();
    }

    @Override
    public String toString() {
        return "Project [folders=" + super.toString() + ", id=" + id + ", name=" + name + "]";
    }

    public int compareTo(Project o) {
        return name.compareTo(o.name);
    }

}
