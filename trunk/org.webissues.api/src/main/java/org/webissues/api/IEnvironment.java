package org.webissues.api;

import java.io.IOException;
import java.util.Collection;

import org.apache.commons.httpclient.HttpException;

public interface IEnvironment {

    /**
     * Get if the client is <i>Online</i>. If this object is reload after
     * serialization, it will be <i>offline</i> until
     * {@link #goOnline(Client, Operation)} is called. Normally methods that
     * require access to the server will do this automatically.
     * 
     * @return online
     */
    boolean isOnline();

    /**
     * Get the list of projects available on this server.
     * 
     * @return projects
     */
    Projects getProjects();

    /**
     * Get the list of users available on this server.
     * 
     * @return users
     */
    Users getUsers();

    /**
     * Get the list of issues types available on this user.
     * 
     * @return issue types
     */
    IssueTypes getTypes();

    /**
     * Get the user object for the currently logged on user.
     * 
     * @return logged on user
     */
    User getOwnerUser();

    /**
     * Get the version of the protocol in use by the server.
     * 
     * @return protocol version
     */
    String getVersion();

    /**
     * Get the list of feature names supported by this server.
     * 
     * @return feature names
     */
    Collection<String> getFeatures();

    /**
     * Get the server
     * 
     * @return server
     */
    String getServer();

    /**
     * Get the name of this WebIssues installation.
     * 
     * @return name
     */
    String getName();

    /**
     * Get the unique ID of the session.
     * 
     * @return unique session ID
     */
    String getUUID();

    /**
     * Get the current user ID.
     * 
     * @return user ID
     */
    int getUserId();

    /**
     * Get the current access level
     * 
     * @return access level
     */
    Access getAccess();

    String toString();

    /**
     * Reload all objects (projects, types, users and server data)
     * 
     * @param client client
     * @param operation operation
     * @throws HttpException on any HTTP error
     * @throws IOException on any I/O error
     * @throws ProtocolException on any protocol error
     */
    void reload(Client client, Operation operation) throws HttpException, IOException, ProtocolException;

    /**
     * Disconnect from the server. All projects, types and users will be cleared
     * as well as the server details.
     */
    void disconnect();

    /**
     * Go offline. This is not a full disconnect as all of the environment is
     * still available.
     */
    void goOffline();

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
    void goOnline(Client client, Operation operation) throws HttpException, IOException, ProtocolException;

    /**
     * Get a list of users that are a member of the provided project.
     * 
     * @param project
     * @return member users
     */
    Collection<User> getMembersOf(Project project);

    /**
     * Get the client this environment is attached to.
     * 
     * @return client
     */
    Client getClient();

}