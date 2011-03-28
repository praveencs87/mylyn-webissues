package net.sf.webissues.ui.wizard;

import java.net.MalformedURLException;
import java.net.URL;

import net.sf.webissues.core.WebIssuesClientManager;
import net.sf.webissues.core.WebIssuesCorePlugin;
import net.sf.webissues.ui.WebIssuesUiPlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositorySettingsPage;
import org.eclipse.swt.widgets.Composite;

public class WebIssuesRepositorySettingsPage extends AbstractRepositorySettingsPage {

    private static final String TITLE = "WebIssues Repository Settings";
    private static final String DESCRIPTION = "http://myserver/webissues/";

    public WebIssuesRepositorySettingsPage(TaskRepository taskRepository) {
        super(TITLE, DESCRIPTION, taskRepository);
        setNeedsProxy(true);
        setNeedsHttpAuth(false);
        setNeedsTimeZone(true);
    }

    @Override
    protected void createAdditionalControls(Composite parent) {
    }

    @Override
    public void applyTo(TaskRepository repository) {
        WebIssuesClientManager clientManager = WebIssuesCorePlugin.getDefault().getConnector().getClientManager();
        clientManager.removeClient(repository);
        super.applyTo(repository);
    }

    @Override
    protected boolean isValidUrl(String name) {
        boolean isValidUrl = false;
        if (name.startsWith(URL_PREFIX_HTTPS) || name.startsWith(URL_PREFIX_HTTP)) {
            try {
                new URL(name);
                isValidUrl = true;
            } catch (MalformedURLException e) {
            }
        }

        return isValidUrl;
    }

    @Override
    protected Validator getValidator(TaskRepository repository) {
        return new WebIssuesValidator(repository);
    }

    class WebIssuesValidator extends Validator {
        final TaskRepository repository;

        public WebIssuesValidator(TaskRepository repository) {
            this.repository = repository;
        }

        @Override
        public void run(IProgressMonitor monitor) throws CoreException {
            try {
                WebIssuesClientManager clientManager = WebIssuesCorePlugin.getDefault().getConnector().getClientManager();
                clientManager.validate(repository, monitor);
                setStatus(new Status(IStatus.OK, WebIssuesUiPlugin.ID_PLUGIN, IStatus.OK, "Valid settings found", null));
            } catch (Exception e) {
                e.printStackTrace();
                setStatus(WebIssuesCorePlugin.toStatus(e, repository));
            }
        }
    }

    @Override
    public String getConnectorKind() {
        return WebIssuesCorePlugin.CONNECTOR_KIND;
    }

}
