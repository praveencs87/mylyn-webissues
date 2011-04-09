package net.sf.webissues.api;

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
public class Users extends EntityMap<User> implements Serializable {

    private static final long serialVersionUID = -45193382221486291L;
    private Environment environment;

    protected Users(Environment environment) {
        this.environment = environment;
    }

    /**
     * Get the environment this map belongs to.
     * 
     * @return environment
     */
    public Environment getEnvironment() {
        return environment;
    }

    /**
     * Create a new user
     * 
     * @param loginId login
     * @param name name
     * @param password password
     * @return user
     * @throws IOException
     * @throws HttpException
     * @throws ProtocolException 
     */
    public User createUser(String loginId, String name, char[] password) throws HttpException, IOException, ProtocolException {
        Client client = environment.getClient();
        HttpMethod method = client.doCommand("ADD USER '" + Util.escape(loginId) + "' '" + Util.escape(name) + "' '"
                        + Util.escape(new String(password)));
        try {
            List<List<String>> response = client.readResponse(method.getResponseBodyAsStream());
            User u  = new User(client.getEnvironment(), Integer.parseInt(response.get(0).get(1)), loginId, name, Access.NORMAL);
            add(u);
            return u;
        } finally {
            method.releaseConnection();
        }
    }

    /**
     * Reload all users from the server.
     * 
     * @param operation operation call-back
     * @throws HttpException on HTTP error
     * @throws IOException on any other IO error
     * @throws ProtocolException on error return by server or protocol problem
     */
    public void reload(final Operation operation) throws HttpException, IOException, ProtocolException {
        environment.getClient().doCall(new Call<Object>() {
            public Object call() throws HttpException, IOException, ProtocolException {
                operation.beginJob("Reloading users", 1);
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
     * Get a user given it's login name
     * 
     * @param login user's login name
     * @return user or <code>null</code> if
     */
    public User getUserByLogin(String login) {
        for (User user : values()) {
            if (user.getLogin().equals(login)) {
                return user;
            }
        }
        return null;
    }

    /**
     * Get a user given it's name
     * 
     * @param name user's name
     * @return user or <code>null</code> if
     */
    public User getUserByName(String name) {
        for (User user : values()) {
            if (user.getName().equals(name)) {
                return user;
            }
        }
        return null;
    }

    protected void doReload(Operation operation) throws HttpException, IOException, ProtocolException {
        Client client = environment.getClient();
        HttpMethod method = client.doCommand("LIST USERS");
        try {
            Map<Integer, User> users = new HashMap<Integer, User>();
            for (List<String> response : client.readResponse(method.getResponseBodyAsStream())) {
                if (response.get(0).equals("M")) {
                    int userId = Integer.parseInt(response.get(1));
                    int projectId = Integer.parseInt(response.get(2));
                    Project project = client.getEnvironment().getProjects().get(projectId);
                    if (project == null) {
                        throw new Error("Project " + projectId + " for member " + userId + " is not known");
                    }
                    User user = users.get(userId);
                    if (user == null) {
                        throw new Error("Expected project before folder");
                    }
                    user.put(project.getId(),
                        new ProjectMembership(user, project, Access.fromValue(Integer.parseInt(response.get(3)))));
                } else if (response.get(0).equals("U")) {
                    int userId = Integer.parseInt(response.get(1));
                    users.put(
                        userId,
                        new User(environment, userId, response.get(2), response.get(3), Access.fromValue(Integer.parseInt(response
                                        .get(4)))));
                } else {
                    Client.LOG.warn("Unexpected response \"" + response + "\"");
                }
            }
            clear();
            for (User user : users.values()) {
                add(user);
            }
        } finally {
            method.releaseConnection();
        }
    }
}
