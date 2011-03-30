package net.sf.webissues.api;

import java.io.Serializable;
import java.util.HashMap;


/**
 * Map of {@link User}s.
 */
public class User extends HashMap<Integer, ProjectMembership> implements NamedEntity, Serializable {

    private static final long serialVersionUID = -7910305270280658583L;
    
    private int id;
    private String login;
    private Access access;
    private String name;

    protected User(int id, String login, String name, Access access) {
        super();
        this.id = id;
        this.name = name;
        this.login = login;
        this.access = access;
    }

    /**
     * Constructor.
     *
     * @param login login
     * @param name name
     * @param access access
     */
    public User(String login, String name, Access access) {
        this(-1, login, name, access);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * Get the login name.
     * 
     * @return login
     */
    public String getLogin() {
        return login;
    }

    /**
     * Get the access type this user has.
     * 
     * @return access
     */
    public Access getAccess() {
        return access;
    }

    @Override
    public String toString() {
        return "User [access=" + access + ", id=" + id + ", login=" + login + ", name=" + name + ",projectMembership=" + super.toString() + "]";
    }

}
