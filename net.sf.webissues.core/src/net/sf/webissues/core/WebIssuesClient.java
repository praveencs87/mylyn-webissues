package net.sf.webissues.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;


import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScheme;
import org.apache.commons.httpclient.auth.CredentialsNotAvailableException;
import org.apache.commons.httpclient.auth.CredentialsProvider;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.commons.net.AuthenticationCredentials;
import org.eclipse.mylyn.commons.net.AuthenticationType;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.webissues.api.Attachment;
import org.webissues.api.Attribute;
import org.webissues.api.Authenticator;
import org.webissues.api.Client;
import org.webissues.api.Comment;
import org.webissues.api.Folder;
import org.webissues.api.IEnvironment;
import org.webissues.api.Issue;
import org.webissues.api.IssueDetails;
import org.webissues.api.ProtocolException;

public class WebIssuesClient implements CredentialsProvider, Serializable, Authenticator {

    private static final long serialVersionUID = 2277223233454809290L;

    private transient final static Map<String, Integer> authAttempts = new WeakHashMap<String, Integer>();

    protected Client client;
    protected transient String repositoryUrl;
    protected transient AbstractWebLocation location;

    private Exception error;
    private List<String> completedStatusList = new ArrayList<String>();
    private String statusAttributeName = "Status";
    private String dueDateAttributeName = "Due Date";
    private String estimateAttributeName = "Work Hours";

    public WebIssuesClient(TaskRepository taskRepository, HttpClient httpClient, AbstractWebLocation location)
        throws MalformedURLException {
        client = new Client();
        configure(taskRepository, httpClient, location);
    }

    void configure(TaskRepository taskRepository, HttpClient httpClient, AbstractWebLocation location) throws MalformedURLException {
        this.location = location;
        this.repositoryUrl = location.getUrl();
        if (!this.repositoryUrl.endsWith("/")) {
            this.repositoryUrl += "/";
        }
        URL urlObj = new URL(repositoryUrl);
        client.setUrl(urlObj);
        client.setHttpClient(httpClient);
        client.setAuthenticator(this);
        client.setCredentialsProvider(this);
        String statusListString = taskRepository.getProperty("completedStatusList");
        if (statusListString != null) {
            completedStatusList = Arrays.asList(statusListString.toLowerCase().split(","));
        }
    }

    public String getStatusAttributeName() {
        return statusAttributeName;
    }

    public void setStatusAttributeName(String statusAttributeName) {
        this.statusAttributeName = statusAttributeName;
    }

    public String getDueDateAttributeName() {
        return dueDateAttributeName;
    }

    public void setDueDateAttributeName(String dueDateAttributeName) {
        this.dueDateAttributeName = dueDateAttributeName;
    }

    public String getEstimateAttributeName() {
        return estimateAttributeName;
    }

    public void setEstimateAttributeName(String estimateAttributeName) {
        this.estimateAttributeName = estimateAttributeName;
    }

    public List<String> getCompletedStatusList() {
        return completedStatusList;
    }

    public boolean isCompletedStatus(String statusName) {
        return completedStatusList.contains(statusName.toLowerCase());
    }

    public boolean isConfigured() {
        return location != null;
    }

    protected boolean credentialsValid(AuthenticationCredentials credentials) {
        return credentials != null && credentials.getUserName().length() > 0;
    }

    public void logout() {
    }

    public void connect(IProgressMonitor monitor) throws ProtocolException, IOException {
        try {
            client.connect(new MonitorOperationAdapter(monitor));
        } catch (HttpException e) {
            error = e;
            throw e;
        } catch (IOException e) {
            error = e;
            throw e;
        } catch (ProtocolException e) {
            error = e;
            throw e;
        } finally {
            finishOp();
        }
    }

    public IEnvironment getEnvironment() {
        return client.getEnvironment();
    }

    public boolean isOnline() {
        return client.isConnected() && client.getEnvironment().isOnline();
    }

    public Throwable getException() {
        return error;
    }

    public boolean goOnline(IProgressMonitor monitor) {
        try {
            if (!client.isConnected()) {
                connect(monitor);
            } else if (!client.getEnvironment().isOnline()) {
                client.getEnvironment().goOnline(client, new MonitorOperationAdapter(monitor));
                error = null;
            }
        } catch (Exception e) {
            error = e;
            return false;
        }
        return true;
    }

    public void moveIssue(int issueId, int newFolderId, IProgressMonitor monitor) throws HttpException, IOException, ProtocolException {
        try {
            MonitorOperationAdapter operation = new MonitorOperationAdapter(monitor);
            client.moveIssue(issueId, operation, newFolderId);
        } finally {
            finishOp();
        }
    }

    public void updateIssue(int issueId, String newName, Map<Attribute, String> attributes, IProgressMonitor monitor)
                    throws HttpException, IOException, ProtocolException {
        try {
            MonitorOperationAdapter operation = new MonitorOperationAdapter(monitor);
            if (newName != null) {
                client.renameIssue(issueId, newName, operation);
            }
            if (attributes != null) {
                client.setIssueAttributeValues(issueId, attributes, operation);
            }
        } finally {
            finishOp();
        }
    }

    public void updateAttributes(IProgressMonitor monitor, boolean force) throws HttpException, IOException, ProtocolException {
        try {
            if (force) {
                doUpdate(monitor);
            } else {
                try {
                    doUpdate(monitor);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } finally {
            finishOp();
        }
    }

    private void doUpdate(IProgressMonitor monitor) throws HttpException, IOException, ProtocolException {
        if (!client.getEnvironment().isOnline()) {
            client.getEnvironment().goOnline(client, new MonitorOperationAdapter(monitor));
        } else {
            client.getEnvironment().reload(client, new MonitorOperationAdapter(monitor));
        }
    }

    public void putAttachmentData(int issueId, Attachment attachment, InputStream inputStream, long length, String contentType,
                                  IProgressMonitor monitor) throws HttpException, IOException, ProtocolException {
        try {
            client.putAttachmentData(issueId, attachment.getName(), attachment.getDescription(), inputStream, length, contentType,
                new MonitorOperationAdapter(monitor));
        } finally {
            finishOp();
        }
    }

    public int createIssue(Issue issue, IProgressMonitor monitor) throws IOException, ProtocolException {
        try {
            return client.createIssue(issue, new MonitorOperationAdapter(monitor));
        } finally {
            finishOp();
        }
    }

    public InputStream getAttachmentData(int attachmentId, IProgressMonitor monitor) throws HttpException, IOException,
                    ProtocolException {
        try {
            return client.getAttachmentData(attachmentId, new MonitorOperationAdapter(monitor));
        } finally {
            finishOp();
        }
    }

    public Collection<? extends Issue> getFolderIssues(Folder folder, long stamp, IProgressMonitor monitor) throws HttpException,
                    IOException, ProtocolException {
        if (folder == null) {
            throw new IllegalArgumentException("Folder may not be null");
        }
        try {
            return folder.getIssues(new MonitorOperationAdapter(monitor), stamp);
        } finally {
            finishOp();
        }
    }

    public Collection<Issue> findIssues(Map<Folder, Long> stamps, IProgressMonitor monitor) throws HttpException, ProtocolException, IOException {
        try {
            return client.findIssues(stamps, new MonitorOperationAdapter(monitor));
        } finally {
            finishOp();
        }
    }

    public IssueDetails getIssueDetails(int issueId, IProgressMonitor monitor) throws HttpException, IOException,
                    NumberFormatException, ProtocolException, Error {
        try {
            return client.getIssueDetails(issueId, new MonitorOperationAdapter(monitor));
        } finally {
            finishOp();
        }
    }

    public void addComment(Comment comment, IProgressMonitor monitor) throws IOException, ProtocolException {
        try {
            comment.getIssue().addComment(comment, new MonitorOperationAdapter(monitor));
        } finally {
            finishOp();
        }

    }

    @Override
    public org.webissues.api.Authenticator.Credentials getCredentials(URL url) {
        try {
            final UsernamePasswordCredentials httpCredentials = getCredentials(null, url.getHost(), url.getPort(), false);
            return new Credentials() {

                @Override
                public String getUsername() {
                    return httpCredentials.getUserName();
                }

                @Override
                public char[] getPassword() {
                    return httpCredentials.getPassword().toCharArray();
                }
            };
        } catch (CredentialsNotAvailableException e) {
            e.printStackTrace();
        }
        return null;
    }

    public UsernamePasswordCredentials getCredentials(AuthScheme scheme, String host, int port, boolean proxy)
                    throws CredentialsNotAvailableException {
        synchronized (authAttempts) {
            String authCacheKey = getAuthCacheKey(scheme, host, port, proxy);
            Integer attempts = authAttempts.get(authCacheKey);
            if (attempts == null) {
                attempts = Integer.valueOf(0);
            } else {
                attempts = Integer.valueOf(attempts.intValue() + 1);
            }
            if (attempts.intValue() == 3) {
                authAttempts.remove(authCacheKey);
                throw new CredentialsNotAvailableException("Too many authentication attempts.");
            }
            authAttempts.put(authCacheKey, attempts);
            AuthenticationCredentials authCreds = getMylynCredentials(proxy);
            return new UsernamePasswordCredentials(authCreds.getUserName(), authCreds.getPassword());
        }
    }

    private String getAuthCacheKey(AuthScheme scheme, String host, int port, boolean proxy) {
        return host + ":" + port + ":" + proxy + ":" + (scheme == null ? "" : scheme.getRealm());
    }

    private AuthenticationCredentials getMylynCredentials(boolean proxy) {
        if (proxy) {
            return location.getCredentials(AuthenticationType.PROXY);
        } else {
            return location.getCredentials(AuthenticationType.REPOSITORY);
        }
    }

    private void finishOp() {
        authAttempts.clear();
    }

    public void deleteTask(ITask task, IProgressMonitor monitor) throws NumberFormatException, IOException, ProtocolException {
        try {
            client.deleteIssue(Integer.parseInt(task.getTaskId()), new MonitorOperationAdapter(monitor));
        } finally {
            finishOp();
        }
    }

}
