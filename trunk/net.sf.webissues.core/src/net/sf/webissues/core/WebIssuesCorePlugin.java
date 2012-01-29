package net.sf.webissues.core;

import java.io.IOException;
import java.net.MalformedURLException;


import org.apache.commons.httpclient.HttpException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.mylyn.tasks.core.RepositoryStatus;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.osgi.framework.BundleContext;
import org.webissues.api.ProtocolException;

public class WebIssuesCorePlugin extends Plugin {

    public static final String ID_PLUGIN = "net.sf.webissues.core";
    public static final String ENCODING_UTF_8 = "UTF-8";
    public static final String CONNECTOR_KIND = "webissues";

    private static WebIssuesCorePlugin plugin;

    private WebIssuesRepositoryConnector connector;

    public static WebIssuesCorePlugin getDefault() {
        return plugin;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (connector != null) {
            connector.stop();
            connector = null;
        }
        plugin = null;
        super.stop(context);
    }

    public static WebIssuesClient getClient(TaskRepository taskRepository) throws HttpException, ProtocolException, IOException {
        return getClient(taskRepository, new NullProgressMonitor());
    }
    
    public static WebIssuesClient getClient(TaskRepository taskRepository, IProgressMonitor monitor) throws HttpException, ProtocolException, IOException {
        return getDefault().getConnector().getClientManager().getClient(taskRepository, monitor);
    }

    public WebIssuesRepositoryConnector getConnector() {
        return connector;
    }

    void setConnector(WebIssuesRepositoryConnector connector) {
        this.connector = connector;
    }

    protected IPath getCachePath() {
        return Platform.getStateLocation(getBundle()).append("repositoryCache");
    }

    public static IStatus toStatus(Throwable e, TaskRepository repository) {
        if (e instanceof ProtocolException) {
            ProtocolException pe = (ProtocolException) e;
            if (pe.getCode() == ProtocolException.INCORRECT_LOGIN) {
                return RepositoryStatus.createLoginError(repository.getRepositoryUrl(), ID_PLUGIN);
            } else if (pe.getCode() == ProtocolException.LOGIN_REQUIRED) {
                return new RepositoryStatus(repository.getRepositoryUrl(), IStatus.ERROR, ID_PLUGIN,
                                RepositoryStatus.REPOSITORY_LOGGED_OUT, "Login required.", e);
            }
        } else if (e instanceof IOException) {
            return new RepositoryStatus(repository.getRepositoryUrl(), IStatus.ERROR, ID_PLUGIN, RepositoryStatus.ERROR_IO,
                            "I/O Error.", e);
        } else if (e instanceof HttpException) {
            return new RepositoryStatus(repository.getRepositoryUrl(), IStatus.ERROR, ID_PLUGIN, RepositoryStatus.ERROR_NETWORK,
                            "HTTP Error.", e);
        } else if (e instanceof MalformedURLException) {
            return new RepositoryStatus(IStatus.ERROR, ID_PLUGIN, RepositoryStatus.ERROR_IO, "Repository URL is invalid", e);
        }
        return new RepositoryStatus(IStatus.ERROR, ID_PLUGIN, RepositoryStatus.ERROR_INTERNAL, "Unexpected error", e);
    }

}
