package org.webissues.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.auth.CredentialsProvider;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.PartSource;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.webissues.api.AbstractChange.Type;

/**
 * <p>
 * Manages all interaction with the WebIssues server. Users of this API should
 * create an instance of this class, set the {@link Authenticator} and
 * {@link HttpClient} and call {@link #connect(Operation)}.
 * </p>
 * <p>
 * Once the client is connected, the {@link IEnvironment} is loaded and may be
 * retrieved used {@link #getEnvironment()}.
 * </p>
 * <p>
 * This object and the environment are {@link Serializable}. If the client is
 * persisted, when it is loaded back in, it will be <i>Connected</i> but
 * <i>Offline</i>. When connected, all objects that are stored in memory (i.e.
 * everything in {@link IEnvironment} will still be available.
 * </p>
 * <p>
 * Any operations that require a connection to the server will automatically try
 * to go back on-line, provided that {@link #setAuthenticator(Authenticator)}
 * and {@link #setHttpClient(HttpClient)} have been called to set up new
 * instances of these objects.
 * </p>
 */
public class Client implements Serializable {

    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm";
    public static final String DATEONLY_FORMAT = "yyyy-MM-dd";

    public static boolean DEBUG = false;

    /**
     * Protocol versions
     */
    public final static String MIN_PROTOCOL_VERSION = "0.7";
    public final static String MAX_PROTOCOL_VERSION = "1.0";

    static {
        DEBUG = System.getProperty("wi.debug", String.valueOf(DEBUG)).equals("true");
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire.header", DEBUG ? "debug" : "warn");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", DEBUG ? "debug" : "warn");
        System.setProperty("org.apache.commons.logging.simplelog.log.net.sf.webissues", DEBUG ? "debug" : "warn");
    }

    private static final long serialVersionUID = -3644715142860489561L;

    static final Log LOG = LogFactory.getLog(Client.class);

    // Private instance variables
    private URL url;
    private IEnvironment environment;
    private CredentialsProvider credentialsProvider;
    private int majorProtocolVersion = -1;

    // Private transient variables
    private transient HttpClient httpClient;
    private transient Authenticator authenticator;
    private transient PasswordChangeCallback passwordChangeCallback;

    public Client(HttpClient client) {
        this.httpClient = client;
    }

    public Client() {
        newHttpClient();
    }

    /**
     * Set the major protocol version number to use for this connection. A
     * version of -1 indicates automatic detection (the default). You should not
     * really need to use anything other than automatic detection, although if
     * know for certain what version you are using you can. It *may* save an
     * additional HTTP request, but that would be the only advantage.
     * 
     * @param majorProtocolVersion major protocol version.
     */
    public final void setServerMajorProtocolVersion(int majorProtocolVersion) {
        this.majorProtocolVersion = majorProtocolVersion;
    }

    /**
     * Set the HTTP client to use
     * 
     * @param httpClient HTTP client
     */
    public final void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Set the URL of the webissues server
     * 
     * @param url webissues server URL
     */
    public final void setUrl(URL url) {
        this.url = url;
    }

    /**
     * Set the authenticator to use for web-issues server authentication. Note,
     * this field is transient, so if you serialize the client object, you must
     * set the authenticator again before using it.
     * 
     * @param authenticator authenticator
     */
    public final void setAuthenticator(Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    /**
     * Set the callback to use when a password change is requested on login.
     * Note, this field is transient, so if you serialize the client object, you
     * must set the callback again before using it.
     * 
     * @param callback password change callback
     */
    public final void setPasswordChangeCallback(PasswordChangeCallback passwordChangeCallback) {
        this.passwordChangeCallback = passwordChangeCallback;
    }

    /**
     * Get the callback to use when a password change is requested on login.
     * 
     * @return password change callback
     * @see #setP
     */
    public final PasswordChangeCallback getPasswordChangeCallback() {
        return passwordChangeCallback;
    }

    /**
     * Get the authenticator to use for web-issues server authentication.
     * 
     * @param authenticator authenticator
     * @see #setAuthenticator(Authenticator)
     */
    protected final Authenticator getAuthenticator() {
        return authenticator;
    }

    /**
     * Set the credentials provided used for PROXY authentication. Note this is
     * NOT used for authentication with the webissues server. Note, this field
     * is transient, so if you serialize the client object, you must set the
     * provider again before using it.
     * 
     * @param credentialsProvider credential provider for PROXY authentication
     */
    public final void setCredentialsProvider(CredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    /**
     * Get if the client is currently connected. Note that a client may be
     * connected but not 'online' if the Client was serialised while it was
     * connected. If an operation that requires communication with the server is
     * invoked, then an attempt will be made to go online again.
     * 
     * @return connected
     */
    public boolean isConnected() {
        return environment != null;
    }

    /**
     * Get the current environment. This wil be <code>null</code> until the
     * client has been connected.
     * 
     * @return environment
     */
    public IEnvironment getEnvironment() {
        return environment;
    }

    /**
     * Get the URL of the server.
     * 
     * @return URL
     */
    public final URL getUrl() {
        return url;
    }

    /**
     * Get the HTTP client
     * 
     * @return http client
     */
    public HttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * Connect to the server
     * 
     * @param operation operation call-back
     * @throws HttpException on HTTP error
     * @throws IOException on any other IO error
     * @throws ProtocolException on error return by server or protocol problem
     * @throws IllegalStateException if already connected or connection is not
     *         possible for some other state related reason
     */
    public void connect(Operation operation) throws HttpException, IOException, ProtocolException {
        if (isConnected()) {
            throw new IllegalStateException("Already connected");
        }
        environment = new Environment(this);
        checkConnectedAndOnline(operation);
    }

    /**
     * Set attribute values for an issue.
     * 
     * @param issueId issue ID
     * @param attributes attributes
     * @param operation operation call-back
     * @return collection of change ID
     * @throws HttpException on HTTP error
     * @throws IOException on any other IO error
     * @throws ProtocolException on error return by server or protocol problem
     */
    public Collection<Integer> setIssueAttributeValues(final int issueId, final Map<Attribute, String> attributes,
                                                       Operation operation) throws HttpException, IOException, ProtocolException {

        final SimpleDateFormat dateFormat = new SimpleDateFormat(DATEONLY_FORMAT);
        final SimpleDateFormat dateTimeFormat = new SimpleDateFormat(DATETIME_FORMAT);
        return doCall(new Call<Collection<Integer>>() {
            public Collection<Integer> call() throws HttpException, IOException, ProtocolException {
                List<Integer> changes = new ArrayList<Integer>();
                for (Attribute attribute : attributes.keySet()) {
                    String value = attributes.get(attribute);
                    if (!Util.isNullOrBlank(value) && attribute.getType().equals(Attribute.AttributeType.DATETIME)) {
                        if (attribute.isDateOnly()) {
                            value = dateFormat.format(Util.parseTimestamp(value).getTime());
                        } else {
                            value = dateTimeFormat.format(Util.parseTimestamp(value).getTime());
                        }
                    }
                    HttpMethod method = doCommand("SET VALUE " + issueId + " " + attribute.getId() + " '" + Util.escape(value)
                                    + "'");
                    try {
                        for (List<String> response : readResponse(method.getResponseBodyAsStream())) {
                            if (response.get(0).equals("ID")) {
                                changes.add(Integer.parseInt(response.get(1)));
                            } else {
                                LOG.warn("Unexpected SET VALUE response \"" + response + "\"");
                            }
                        }
                    } catch (ProtocolException pe) {
                        if (pe.getCode() == ProtocolException.INVALID_STRING) {
                            throw new ProtocolException(ProtocolException.INVALID_STRING, "Invalid attribute value for '"
                                            + attribute.getName() + "'. " + pe.getMessage());
                        } else {
                            throw pe;
                        }
                    } finally {
                        method.releaseConnection();
                    }
                }
                return changes;
            }
        }, operation);
    }

    /**
     * Move this issue to another folder.
     * 
     * @throws IOException on any error
     * @throws ProtocolException
     */
    public void moveIssue(final int issueId, Operation operation, final int folderId) throws IOException, ProtocolException {
        doCall(new Call<Boolean>() {
            public Boolean call() throws HttpException, IOException, ProtocolException {
                doCommand("MOVE ISSUE " + issueId + " " + folderId);
                return true;
            }
        }, operation);
    }

    /**
     * Delete an issue.
     * 
     * @param issueId issue ID
     * @param operation operation
     * 
     * @throws IOException on any error
     * @throws ProtocolException
     */
    public void deleteIssue(final int issueId, Operation operation) throws IOException, ProtocolException {
        doCall(new Call<Boolean>() {
            public Boolean call() throws HttpException, IOException, ProtocolException {
                doCommand("DELETE ISSUE " + issueId);
                return true;
            }
        }, operation);
    }

    /**
     * Rename an issue
     * 
     * @param issueId issue ID
     * @param newName name issue name
     * @return change ID or -1 if the change fails
     * @throws HttpException on HTTP error
     * @throws IOException on any other IO error
     * @throws ProtocolException on error return by server or protocol problem
     */
    public int renameIssue(final int issueId, final String newName, Operation operation) throws HttpException, IOException,
                    ProtocolException {
        return doCall(new Call<Integer>() {
            public Integer call() throws HttpException, IOException, ProtocolException {
                HttpMethod method = doCommand("RENAME ISSUE " + issueId + " '" + Util.escape(newName) + "'");
                try {
                    for (List<String> response : readResponse(method.getResponseBodyAsStream())) {
                        if (response.get(0).equals("ID")) {
                            return Integer.parseInt(response.get(1));
                        } else {
                            LOG.warn("Unexpected RENAME ISSUE response \"" + response + "\"");
                        }
                    }
                } catch (ProtocolException pe) {
                    if (pe.getCode() == ProtocolException.INVALID_STRING) {
                        throw new ProtocolException(ProtocolException.INVALID_STRING, "Invalid issue name. Maximum length of 80");
                    } else {
                        throw pe;
                    }
                } finally {
                    method.releaseConnection();
                }
                return -1;
            }
        }, operation);
    }

    /**
     * Mark an issue as read.
     * 
     * @param issueId issue ID
     * @param read read
     * @return change ID or -1 if the change fails
     * @throws HttpException on HTTP error
     * @throws IOException on any other IO error
     * @throws ProtocolException on error return by server or protocol problem
     */
    public void setIssueRead(final int issueId, final boolean read, Operation operation) throws HttpException, IOException,
                    ProtocolException {
        doCall(new Call<Integer>() {
            public Integer call() throws HttpException, IOException, ProtocolException {
                doCommand("SET ISSUE READ " + issueId + " " + (read ? "1" : "0"));
                return -1;
            }
        }, operation);
    }

    /**
     * Mark a folder as read.
     * 
     * @param folderId folder ID
     * @param read read
     * @return change ID or -1 if the change fails
     * @throws HttpException on HTTP error
     * @throws IOException on any other IO error
     * @throws ProtocolException on error return by server or protocol problem
     */
    public void setFolderRead(final int folderId, final boolean read, Operation operation) throws HttpException, IOException,
                    ProtocolException {
        doCall(new Call<Integer>() {
            public Integer call() throws HttpException, IOException, ProtocolException {
                doCommand("SET FOLDER READ " + folderId + " " + (read ? "1" : "0"));
                return -1;
            }
        }, operation);
    }

    /**
     * Get the full issue details.
     * 
     * @param issueId issue ID
     * @param operation operation call-back
     * @return issue details
     * @throws HttpException on HTTP error
     * @throws IOException on any other IO error
     * @throws ProtocolException on error return by server or protocol problem
     */
    public IssueDetails getIssueDetails(final int issueId, Operation operation) throws HttpException, IOException,
                    ProtocolException {

        return doCall(new Call<IssueDetails>() {
            public IssueDetails call() throws HttpException, IOException, ProtocolException {
                String command = "GET DETAILS " + issueId + " 0";
                if(Util.compareVersions(getEnvironment().getVersion(), "1.0") >= 0) {
                    // Mark read only
                    command += " 0";
                }
                HttpMethod method = doCommand(command);
                IssueDetails issueDetails = null;
                Issue issue = null;
                Map<Integer, Change> changeMap = null;
                try {
                    for (List<String> response : readResponse(method.getResponseBodyAsStream())) {
                        if (response.get(0).equals("V")) {
                            int attributeId = Integer.parseInt(response.get(1));
                            if (issueDetails == null) {
                                throw new Error("Expected issue before attribute");
                            }
                            issueDetails.getIssue().put(environment.getTypes().getAttribute(attributeId), response.get(3));
                        } else if (response.get(0).equals("I")) {
                            if (issueDetails != null) {
                                throw new Error("Received two issues");
                            }
                            issue = Issue.createFromResponse(response, environment);
                            issueDetails = new IssueDetails(Client.this, issue);
                        } else if (response.get(0).equals("C")) {
                            if (issueDetails == null) {
                                throw new Error("Expected issue before comment");
                            }
                            if (!environment.getVersion().startsWith("0.")) {
                                // From Version 1.0, Comments are also changes
                                if (changeMap == null) {
                                    throw new Error("Expected changes before comment");
                                }
                                issueDetails.getComments().add(Comment.createFromResponse(issue, response, environment, changeMap));
                            } else {
                                issueDetails.getComments().add(Comment.createFromResponse(issue, response, environment));
                            }
                        } else if (response.get(0).equals("A")) {
                            if (issueDetails == null) {
                                throw new Error("Expected issue before attachment");
                            }
                            if (!environment.getVersion().startsWith("0.")) {
                                // From Version 1.0, Attachments are also
                                // changes
                                if (changeMap == null) {
                                    throw new Error("Expected changes before comment");
                                }
                                issueDetails.getAttachments().add(
                                    Attachment.createFromResponse(issue, response, environment, changeMap));
                            } else {
                                issueDetails.getAttachments().add(Attachment.createFromResponse(issue, response, environment));
                            }
                        } else if (response.get(0).equals("H")) {
                            if (issueDetails == null) {
                                throw new Error("Expected issue before change");
                            }
                            if (changeMap == null) {
                                changeMap = new HashMap<Integer, Change>();
                            }
                            Change change = Change.createFromResponse(issue, response, environment.getUsers(), environment);
                            changeMap.put(change.getId(), change);
                            if (change.getType().equals(Type.VALUE_CHANGED) || change.getType().equals(Type.ISSUE_MOVED)
                                            || change.getType().equals(Type.ISSUE_RENAMED)) {
                                issueDetails.getChanges().add(change);
                            }
                        } else {
                            LOG.warn("Unexpected GET DETAILS response \"" + response + "\"");
                        }
                    }
                } finally {
                    method.releaseConnection();
                }
                return issueDetails;
            }
        }, operation);
    }

    /**
     * Create a new issue on the server.
     * 
     * @param issue issue to create
     * @param operation operation call-back
     * @return new issue ID
     * @throws IOException on any other IO error
     * @throws ProtocolException on error return by server or protocol problem
     */
    public int createIssue(final Issue issue, final Operation operation) throws IOException, ProtocolException {
        return doCall(new Call<Integer>() {
            public Integer call() throws IOException, HttpException, ProtocolException {
                HttpMethod method = doCommand("ADD ISSUE " + issue.getFolder().getId() + " '" + Util.escape(issue.getName()) + "'");
                try {
                    List<String> response = readResponse(method.getResponseBodyAsStream()).iterator().next();
                    int id = Integer.parseInt(response.get(1));
                    issue.setId(id);
                    return id;
                } finally {
                    method.releaseConnection();
                }
            }
        }, operation);
    }

    /**
     * Store the content of an attachment.
     * 
     * @param issueId issue to attach to
     * @param attachment attachment
     * @param inputStream attachment data
     * @param operation operation call-back
     * @throws HttpException on HTTP error
     * @throws IOException on any other IO error
     * @throws ProtocolException on error return by server or protocol problem
     */
    public int putAttachmentData(final int issueId, final String name, final String description, final InputStream inputStream,
                                 final long length, final String contentType, Operation operation) throws HttpException,
                    IOException, ProtocolException {
        return doCall(new Call<Integer>() {
            public Integer call() throws HttpException, IOException, ProtocolException {
                // TODO the value of 40 was determined through trial and error.
                // check
                // the exact restriction. If this is too long "invalid string"
                // error is
                // returned
                String nname = name;
                if (nname.length() > 40) {
                    nname = nname.substring(0, 40);
                }
                nname = Util.escape(nname);
                HttpMethod method = doCommand(new Part[] {
                                new StringPart("command", "ADD ATTACHMENT " + issueId + " '" + Util.escape(nname) + "' '"
                                                + Util.escape(description) + "'"),
                                new FilePart("file", new AttachmentPartSource(inputStream, length)) {
                                } });
                try {
                    List<String> response = readResponse(method.getResponseBodyAsStream()).iterator().next();
                    return Integer.parseInt(response.get(1));
                } finally {
                    method.releaseConnection();
                }
            }
        }, operation);
    }

    /**
     * Get content of an attachment.
     * 
     * @param attachmentId attachment ID
     * @param operation operation call-back
     * @return stream of attachment data
     * @throws HttpException on HTTP error
     * @throws IOException on any other IO error
     * @throws ProtocolException on error return by server or protocol problem
     */
    public InputStream getAttachmentData(final int attachmentId, Operation operation) throws HttpException, IOException,
                    ProtocolException {
        return doCall(new Call<InputStream>() {
            public InputStream call() throws HttpException, IOException, ProtocolException {
                final HttpMethod method = doCommand("GET ATTACHMENT " + attachmentId);
                return new java.io.FilterInputStream(method.getResponseBodyAsStream()) {
                    @Override
                    public void close() throws IOException {
                        try {
                            super.close();
                        } finally {
                            method.releaseConnection();
                        }
                    }

                };
            }

        }, operation);

    }

    /**
     * Get all issues across all projects and folders changed since the provide
     * time.
     * 
     * @param stamp only retrieve uses with the supplied stamp or higher. Use
     *        null to retrieve all issues for all folders
     * @param operation operation callback
     * @return list of issues
     * @throws HttpException on HTTP error
     * @throws IOException on any other IO error
     * @throws ProtocolException on error return by server or protocol problem
     */
    public Collection<Issue> findIssues(Map<Folder, Long> stamps, Operation operation) throws ProtocolException, HttpException,
                    IOException {
        checkConnectedAndOnline(operation);
        List<Issue> issues = new ArrayList<Issue>();
        Projects projects = environment.getProjects();
        int folders = 0;
        for (Project project : projects.values()) {
            folders += project.size();
        }
        operation.beginJob("Finding issues", folders);
        try {
            for (Project project : projects.values()) {
                for (Folder folder : project.values()) {
                    if (operation.isCanceled()) {
                        throw new ProtocolException(ProtocolException.CANCELLED);
                    }
                    operation.setName("Looking in " + folder.getName());
                    issues.addAll(folder.getIssues(operation,
                        stamps.size() == 0 ? 0 : (stamps.containsKey(folder) ? stamps.get(folder) : 0)));
                    stamps.put(folder, Long.valueOf(folder.getStamp()));
                    operation.progressed(1);
                }
            }
        } finally {
            operation.done();
        }
        return issues;
    }

    private void checkConnectedAndOnline(Operation operation) throws HttpException, IOException, ProtocolException {
        if (environment == null) {
            throw new IOException("Not connected");
        }
        if (!environment.isOnline()) {
            if (authenticator == null) {
                throw new IllegalStateException("No authenticator set and the client needs to go online");
            }
            if (httpClient == null) {
                throw new IllegalStateException("No HTTP client set and the client needs to go online");
            }
            environment.goOnline(this, operation);
        }
    }

    protected static List<List<String>> readResponse(InputStream in) throws IOException, ProtocolException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        List<List<String>> responses = new ArrayList<List<String>>();
        String response = null;
        while ((response = reader.readLine()) != null) {
            LOG.debug(">" + response);
            List<String> parsed = Util.parseLine(response);
            if (responses.size() == 0 && parsed.size() > 0 && parsed.get(0).equals("ERROR")) {
                throw new ProtocolException(Integer.parseInt(parsed.get(1)), parsed.get(2));
            }
            if (responses.size() != 0 || parsed.size() != 1 || !parsed.get(0).equals("NULL")) {
                responses.add(parsed);
            }
        }
        return responses;
    }

    protected <T> T doCall(Call<T> call, Operation operation) throws HttpException, IOException, ProtocolException {
        for (int i = 0; i < 2; i++) {
            if (operation.isCanceled()) {
                throw new ProtocolException(ProtocolException.CANCELLED);
            }
            checkConnectedAndOnline(operation);
            try {
                return call.call();
            } catch (ProtocolException pe) {
                if (pe.getCode() != ProtocolException.LOGIN_REQUIRED) {
                    throw pe;
                } else {
                    // Force a login
                    environment.goOffline();
                }
            }
        }
        return null;
    }

    protected HttpMethod doCommand(String command) throws IOException, HttpException {
        LOG.debug(command);
        return doCommand(new StringPart("command", command));
    }

    protected HttpMethod doCommand(Part... parts) throws IOException, HttpException {
        String urlText = url.toExternalForm();

        // Version 1
        if (majorProtocolVersion == 0) {
            // Version 0
            if (!urlText.endsWith("/")) {
                urlText += "/";
            }
        } else {
            while (urlText.endsWith("/")) {
                urlText = urlText.substring(0, urlText.length() - 1);
            }
            urlText += "/server/webissues/handler.php";
        }

        PostMethod authpost = new PostMethod(urlText);
        authpost.getParams().setParameter(CredentialsProvider.PROVIDER, credentialsProvider);
        authpost.setRequestEntity(new MultipartRequestEntity(parts, authpost.getParams()));
        try {
            int status = httpClient.executeMethod(authpost);
            Header responseHeader = authpost.getResponseHeader("X-WebIssues-Version");
            if (responseHeader == null) {
                /*
                 * If current major protocol version is automatic, then retry
                 * the command using protocol version 0.x. An attempt is made to
                 * switch back to the original version if there is an error
                 */
                if (majorProtocolVersion == -1) {
                    int oldProtocolVersion = majorProtocolVersion;
                    majorProtocolVersion = 0;
                    try {
                        return doCommand(parts);
                    } catch (IOException ioe) {
                        majorProtocolVersion = oldProtocolVersion;
                        throw ioe;
                    }
                }
                throw new IOException("The URL " + urlText + " does not appear to be a WebIssues server. Is the URL correct?");
            }
            if (status != 200) {
                throw new HttpException("HTTP error " + status);
            }
        } catch (IOException ioe) {
            if (environment.isOnline()) {
                environment.goOffline();
            }
            authpost.releaseConnection();
            throw ioe;
        }
        return authpost;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        newHttpClient();
    }

    private void newHttpClient() {
        httpClient = new HttpClient();
        httpClient.getParams().setCookiePolicy(CookiePolicy.RFC_2109);
    }

    /*
     * Main method. The Jar's Main-Class points to this class, so the library
     * and protocol version can be printed by executing the Jar
     */

    public static void main(String[] args) {
        LOG.info("WebIssues Protocol Library (Version " + getVersion() + "). Supports protocol version " + MIN_PROTOCOL_VERSION + " to " + MAX_PROTOCOL_VERSION);
    }

    public static String getVersion() {
        Class<?> clazz = Client.class;
        try {
            String className = clazz.getSimpleName();
            String classFileName = className + ".class";
            String pathToThisClass = clazz.getResource(classFileName).toString();
            int mark = pathToThisClass.indexOf("!");
            String pathToManifest = pathToThisClass.toString().substring(0, mark + 1);
            pathToManifest = pathToManifest + "/META-INF/MANIFEST.MF";
            Manifest manifest = new Manifest(new URL(pathToManifest).openStream());
            String version = manifest.getMainAttributes().getValue("Bundle-Version");
            return version == null || version.length() == 0 ? "999.999.999" : version;
        } catch (IOException ioe) {
            return "999.999.999";
        }
    }

    private final class AttachmentPartSource implements PartSource {

        private final InputStream in;
        private long length;

        AttachmentPartSource(InputStream in, long length) {
            this.in = in;
            this.length = length;
        }

        public InputStream createInputStream() throws IOException {
            return in;
        }

        public String getFileName() {
            return "file";
        }

        public long getLength() {
            return length;
        }

    }
}
