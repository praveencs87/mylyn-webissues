package org.webissues.api;

import java.io.Serializable;

/**
 * A {@link User} may be a member of many project {@link Project}s. For each
 * project, a user will have an {@link Access} level determining if they may
 * perform administration commands on the project.
 */
public class ProjectMembership implements Entity, Serializable {

    private static final long serialVersionUID = 332909825661268570L;

    private Project project;
    private Access access;
    private User user;

    protected ProjectMembership(User user, Project project, Access access) {
        super();
        this.user = user;
        this.project = project;
        this.access = access;
    }

    /**
     * Get the project.
     * 
     * @return project
     */
    public Project getProject() {
        return project;
    }

    /**
     * Get the user.
     * 
     * @return user
     */
    public User getUser() {
        return user;
    }

    /**
     * Get the access allowed to this project.
     * 
     * @return access
     */
    public Access getAccess() {
        return access;
    }

    @Override
    public String toString() {
        return "ProjectMembership [access=" + access + ", project=" + project + "]";
    }

    public int getId() {
        return project.getId();
    }
}
