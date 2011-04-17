/**
 * 
 */
package net.sf.webissues.api;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;

/**
 * Contains all (mostly) top level static data that is retrieved upon
 * connection. This includes {@link Users}, {@link Projects} and {@link IssueTypes}.
 * Also maintains the online / offline state of the client (see {@link Client}
 * description for details.
 */
public class Environment implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private String name;
    private String version;
    private String server;
    private int userId;
    private Access access;
    private String uuid;
    private List<String> features = new ArrayList<String>();
    private IssueTypes types;
    private Projects projects;
    private Users users;
    private transient boolean online;
    private Client client;

    /**
     * Constructor.
     */
    protected Environment(Client client) {
        this.client = client;

        types = new IssueTypes(this);
        projects = new Projects(this);
        users = new Users(this);
    }

    /**
     * Get if the client is <i>Online</i>. If this object is reload after
     * serialization, it will be <i>offline</i> until
     * {@link #goOnline(Client, Operation)} is called. Normally methods that
     * require access to the server will do this automatically.
     * 
     * @return online
     */
    public boolean isOnline() {
        return online;
    }

    /**
     * Get the list of projects available on this server.
     * 
     * @return projects
     */
    public Projects getProjects() {
        return projects;
    }

    /**
     * Get the list of users available on this server.
     * 
     * @return users
     */
    public Users getUsers() {
        return users;
    }

    /**
     * Get the list of issues types available on this user.
     * 
     * @return issue types
     */
    public IssueTypes getTypes() {
        return types;
    }

    /**
     * Get the user object for the currently logged on user.
     * 
     * @return logged on user
     */
    public User getOwnerUser() {
        return users.get(userId);
    }

    /**
     * Get the version of the protocol in use by the server.
     * 
     * @return protocol version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Get the list of feature names supported by this server.
     * 
     * @return feature names
     */
    public Collection<String> getFeatures() {
        return features;
    }

    /**
     * Get the server
     * 
     * @return server
     */
    public String getServer() {
        return server;
    }

    /**
     * Get the name of this WebIssues installation.
     * 
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the unique ID of the session.
     * 
     * @return unique session ID
     */
    public String getUUID() {
        return uuid;
    }

    /**
     * Get the current user ID.
     * 
     * @return user ID
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Get the current access level
     * 
     * @return access level
     */
    public Access getAccess() {
        return access;
    }

    @Override
    public String toString() {
        return "ConnectionDetails[version=" + version + ",server=" + server + ",name=" + name + ",uuid=" + uuid + ",access="
                        + access + ",features=" + features + ",types=" + types + "]";
    }

    /**
     * Reload all objects (projects, types, users and server data)
     * 
     * @param client client
     * @param operation operation
     * @throws HttpException on any HTTP error
     * @throws IOException on any I/O error
     * @throws ProtocolException on any protocol error
     */
    public void reload(Client client, Operation operation) throws HttpException, IOException, ProtocolException {
        if (!online) {
            throw new IllegalStateException("Not online");
        }
        doReload(operation);
    }

    /**
     * Disconnect from the server. All projects, types and users will be cleared
     * as well as the server details.
     */
    public synchronized void disconnect() {
        if (!online) {
            throw new IllegalStateException("Not online");
        }
        features.clear();
        version = null;
        uuid = null;
        name = null;
        server = null;
        userId = 0;
        access = Access.NONE;
        types.clear();
        projects.clear();
        users.clear();
        online = false;
    }

    /**
     * Go offline. This is not a full disconnect as all of the environment is
     * still available.
     */
    public void goOffline() {
        if (!online) {
            throw new IllegalStateException("Not online");
        }
        online = false;
    }

    /**
     * Go online. Make a connection to the server and load all of the
     * environment.
     * 
     * @param client client
     * @param operation operation callback
     * @throws HttpException on any HTTP error
     * @throws IOException on any I/O error
     * @throws ProtocolException on any protocol error
     */
    public void goOnline(Client client, Operation operation) throws HttpException, IOException, ProtocolException {
        if (online) {
            throw new IllegalStateException("Already online");
        }
        operation.beginJob("Connection", 4);
        features.clear();
        try {
            // Hello
            HttpMethod method = client.doCommand("HELLO");
            try {
                Header responseHeader = method.getResponseHeader("X-WebIssues-Version");
                if(responseHeader == null) {
                    throw new IOException();
                }
                version = responseHeader.getValue();

                if (version.startsWith("0.")) {
                    // Version 0.X+
                    server = method.getResponseHeader("X-WebIssues-Server").getValue();
                } else {
                    // Version 1.0+
                    server = method.getResponseHeader("Server").getValue();
                }
                List<String> row = client.readResponse(method.getResponseBodyAsStream()).iterator().next();
                name = row.get(1);
                uuid = row.get(2);
            } finally {
                operation.progressed(1);
                method.releaseConnection();
            }

            if (version.compareTo(Client.PROTOCOL_VERSION) > 0) {
                synchronized (Client.LOG) {
                    Client.LOG.warn("******************************************");
                    Client.LOG.warn("*   WARNING - Protocol Version Mismatch  *");
                    Client.LOG.warn("******************************************");
                    Client.LOG.warn("This server is using protocol version " + version);
                    Client.LOG.warn("where as this library only supports <= " + Client.PROTOCOL_VERSION);
                    Client.LOG.warn("You may experience problems, and it is not");
                    Client.LOG.warn("recommended you continue");
                }
            }

            // Login
            operation.setName("Authenticating");
            Authenticator.Credentials credentials = client.getAuthenticator().getCredentials(client.getUrl());
            if (credentials == null) {
                throw new ProtocolException(ProtocolException.AUTHENTICATION_CANCELLED);
            }
            String command = "LOGIN '" + Util.escape(credentials.getUsername()) + "' '"
                            + Util.escape(new String(credentials.getPassword())) + "'";

            while (true) {
                try {
                    method = client.doCommand(command);
                    try {
                        List<String> row = client.readResponse(method.getResponseBodyAsStream()).iterator().next();
                        userId = Integer.parseInt(row.get(1));
                        if (version.startsWith("0.")) {
                            // Version 0.X+
                            access = Access.fromValue(Integer.parseInt(row.get(2)));
                        } else {
                            // Version 1.0+
                            access = Access.fromValue(Integer.parseInt(row.get(3)));
                        }
                    } finally {
                        operation.progressed(1);
                        method.releaseConnection();
                    }
                    break;
                } catch (ProtocolException pe) {
                    if (pe.getCode() == ProtocolException.MUST_CHANGE_PASSWORD) {
                        /* Version 1.0+ supports change password on logon. To support this the
                         * caller must have set a PasswordChangeCallback
                         */
                        if (client.getPasswordChangeCallback() == null) {
                            throw pe;
                        }
                        char[] newPassword = client.getPasswordChangeCallback().getNewPassword();
                        if (newPassword == null) {
                            throw pe;
                        }
                        command = "LOGIN NEW '" + Util.escape(credentials.getUsername()) + "' '"
                                        + Util.escape(new String(credentials.getPassword())) + "' '"
                                        + Util.escape(new String(newPassword)) + "'";
                    } else {
                        throw pe;
                    }
                }
            }

            // List featureset(1));

            if (version.startsWith("0.")) {
                // Version 0.X+
                operation.setName("Retrieving features");
                method = client.doCommand("LIST FEATURES");
                try {
                    for (List<String> response : client.readResponse(method.getResponseBodyAsStream())) {
                        if (response.get(0).equals("F")) {
                            features.add(response.get(1));
                        } else {
                            Client.LOG.warn("Unexpected response \"" + response + "\"");
                        }
                    }
                } finally {
                    operation.progressed(1);
                    method.releaseConnection();
                }
            }
            else {
                // TODO does 1.0 have any kind of feature list?
            }

            operation.setName("Getting types");
            try {
                doReload(operation);
            } finally {
                operation.progressed(1);
            }
            online = true;
        } finally {
            operation.done();
        }
    }

    /**
     * Get a list of users that are a member of the provided project.
     * 
     * @param project
     * @return member users
     */
    public Collection<User> getMembersOf(Project project) {
        List<User> users = new ArrayList<User>();
        for (User user : getUsers().values()) {
            if (user.containsKey(project.getId())) {
                users.add(user);
            }
        }
        return users;
    }

    protected Client getClient() {
        return client;
    }

    private void doReload(Operation operation) throws HttpException, IOException, ProtocolException {
        types.doReload(operation);
        projects.doReload(operation);
        users.doReload(operation);
    }

}