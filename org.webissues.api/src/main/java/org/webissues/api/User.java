package org.webissues.api;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;

/**
 * Map of {@link User}s.
 */
public class User extends HashMap<Integer, ProjectMembership> implements NamedEntity, Serializable {

    private static final long serialVersionUID = -7910305270280658583L;

    private int id;
    private String login;
    private Access access;
    private String name;
    private Map<String, String> preferences = new HashMap<String, String>();
    private Map<String, String> currentPreferences = new HashMap<String, String>();
    private IEnvironment environment;

    protected User(IEnvironment environment, int id, String login, String name, Access access) {
        super();
        this.environment = environment;
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
    public User(IEnvironment environment, String login, String name, Access access) {
        this(environment, -1, login, name, access);
    }

    public Map<String, String> getPreferences() {
        return preferences;
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

    /**
     * Reload all user preferences from the server.
     * 
     * @param operation operation call-back
     * @throws HttpException on HTTP error
     * @throws IOException on any other IO error
     * @throws ProtocolException on error return by server or protocol problem
     */
    public void reload(final Operation operation) throws HttpException, IOException, ProtocolException {
        environment.getClient().doCall(new Call<Object>() {
            public Object call() throws HttpException, IOException, ProtocolException {
                operation.beginJob("Reloading user", 1);
                try {
                    doReload(operation);
                } finally {
                    operation.done();
                }
                return null;
            }
        }, operation);
    }

    /**
     * Save any changes preferences.
     * 
     * @param operation operation call-back
     * @throws HttpException on HTTP error
     * @throws IOException on any other IO error
     * @throws ProtocolException on error return by server or protocol problem
     */
    public void save(final Operation operation) throws HttpException, IOException, ProtocolException {
        environment.getClient().doCall(new Call<Object>() {
            public Object call() throws HttpException, IOException, ProtocolException {
                operation.beginJob("Saving user preferences", 1);
                try {
                    for (String k : preferences.keySet()) {
                        String v = preferences.get(k);
                        if (!v.equals(currentPreferences.get(k))) {
                            Client client = environment.getClient();
                            HttpMethod method = client.doCommand("SET PREFERENCE " + getId() + " '" + Util.escape(k) + "' '"
                                            + Util.escape(v) + "'");
                            try {
                                for (List<String> response : client.readResponse(method.getResponseBodyAsStream())) {
                                    if (!response.get(0).equals("OK")) {
                                        throw new IOException("Unexpected response.");
                                    } else {
                                        Client.LOG.warn("Unexpected response \"" + response + "\"");
                                    }
                                }
                            } finally {
                                method.releaseConnection();
                            }

                        }
                    }
                } finally {
                    operation.done();
                }
                return null;
            }
        }, operation);
    }

    @Override
    public String toString() {
        return "User [id=" + id + ", login=" + login + ", access=" + access + ", name=" + name + ", preferences=" + preferences
                        + "]";
    }

    private void doReload(Operation operation) throws HttpException, IOException, ProtocolException {
        Client client = environment.getClient();
        HttpMethod method = client.doCommand("LIST PREFERENCES " + getId());
        currentPreferences.clear();
        preferences.clear();
        try {
            for (List<String> response : client.readResponse(method.getResponseBodyAsStream())) {
                if (response.get(0).equals("P")) {
                    preferences.put(response.get(1), response.get(2));
                    currentPreferences.put(response.get(1), response.get(2));
                } else {
                    Client.LOG.warn("Unexpected response \"" + response + "\"");
                }
            }
        } finally {
            method.releaseConnection();
        }
    }

    public IEnvironment getEnvironment() {
        return environment;
    }
    
    /**
     * Rename this user.
     * 
     * @param operation operation
     * @param newName new name
     * 
     * @throws IOException on any error
     * @throws HttpException on HTTP error
     * @throws ProtocolException 
     */
    public void rename(Operation operation, final String newName) throws HttpException, IOException, ProtocolException {
        final Client client = environment.getClient();
        client.doCall(new Call<Boolean>() {
            public Boolean call() throws HttpException, IOException, ProtocolException {
                client.doCommand("RENAME USER " + id + " '" + Util.escape(newName) + "'");
                User.this.name = newName;
                return true;
            }
        }, operation);
    }
    
    /**
     * Grant this user an access level. To disable the user, set {@link Access} to {@link Access#NONE}.
     * 
     * @param operation operation
     * @param access access
     * 
     * @throws IOException on any error
     * @throws HttpException on HTTP error
     * @throws ProtocolException 
     */
    public void grant(Operation operation, final Access access) throws HttpException, IOException, ProtocolException {
        final Client client = environment.getClient();
        client.doCall(new Call<Boolean>() {
            public Boolean call() throws HttpException, IOException, ProtocolException {
                client.doCommand("GRANT USER " + id + " " + access);
                User.this.access = access;
                return true;
            }
        }, operation);
    }
    
    
    /**
     * Grant this user membership to a project. To revoke membership, set {@link Access} to {@link Access#NONE}.
     * 
     * @param operation operation
     * @param project project
     * @param access access
     * 
     * @throws IOException on any error
     * @throws HttpException on HTTP error
     * @throws ProtocolException 
     */
    public void grantMembership(Operation operation, final Project project, final Access access) throws HttpException, IOException, ProtocolException {
        final Client client = environment.getClient();
        client.doCall(new Call<Boolean>() {
            public Boolean call() throws HttpException, IOException, ProtocolException {
                client.doCommand("GRANT MEMBER " + id + " " + project.getId() + " " + access);
                ProjectMembership pmb = new ProjectMembership(User.this, project, access);
                User.this.put(project.getId(), pmb);
                return true;
            }
        }, operation);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        User other = (User) obj;
        if (id != other.id)
            return false;
        return true;
    }
    
    

}
