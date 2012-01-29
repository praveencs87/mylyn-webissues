package net.sf.webissues.ui.wizard;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringTokenizer;

import net.sf.webissues.core.WebIssuesClientManager;
import net.sf.webissues.core.WebIssuesCorePlugin;
import net.sf.webissues.ui.WebIssuesUiPlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositorySettingsPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class WebIssuesRepositorySettingsPage extends AbstractRepositorySettingsPage {

    private static final String TITLE = "WebIssues Repository Settings";
    private static final String DESCRIPTION = "http://myserver/webissues/";
    private Text status;
    private Text statusAttributeName;
    private Text dueDateAttributeName;
    private Text estimateAttributeName;

    public WebIssuesRepositorySettingsPage(TaskRepository taskRepository) {
        super(TITLE, DESCRIPTION, taskRepository);
        setNeedsProxy(true);
        setNeedsHttpAuth(false);
        setNeedsTimeZone(true);
    }

    @Override
    protected void createAdditionalControls(Composite parent) {
        Label l = new Label(parent, SWT.NONE);
        l.setText("Complete when status is:");
        status = new Text(parent, SWT.BORDER | SWT.FILL);
        status.setLayoutData(new GridData (SWT.FILL, SWT.CENTER, true, false));
        status.setToolTipText("Comma separated list of status names that signal an Issue is 'Completed'. This is not case sensitive.");
        String statusList = repository == null ? null : repository.getProperty("completedStatusList");
        status.setText(statusList == null || statusList.trim().length() == 0 ? "Closed" : statusList);
        statusAttributeName = attributeField(parent, "Status Attribute Name:", "The name of the WebIssues attribute that is used for status.", "Status", "statusAttributeName");
        dueDateAttributeName = attributeField(parent, "Due Date Attribute Name:", "The name of the WebIssues attribute that is used for due date.", "Due Date", "dueDateAttributeName");
        estimateAttributeName = attributeField(parent, "Estimate Attribute Name:", "The name of the WebIssues attribute that is used for estimated hours.", "Work Hours", "estimateAttributeName");
    }
    
    private Text attributeField(Composite parent, String label, String toolTip, String defaultValue, String name) {
        Label l = new Label(parent, SWT.NONE);
        l.setText(label);
        Text field = new Text(parent, SWT.BORDER | SWT.FILL);
        field.setLayoutData(new GridData (SWT.FILL, SWT.CENTER, true, false));
        field.setToolTipText(toolTip);
        String val = repository == null ? null : repository.getProperty(name);
        field.setText(val == null || val.trim().length() == 0 ? defaultValue : val);
        return field;
    }

    @Override
    public void applyTo(TaskRepository repository) {
        WebIssuesClientManager clientManager = WebIssuesCorePlugin.getDefault().getConnector().getClientManager();
        clientManager.removeClient(repository);
        StringBuilder bui  = new StringBuilder();
        StringTokenizer t = new StringTokenizer(status.getText(), ", ");
        while(t.hasMoreTokens()) {
            if(bui.length() > 0) {
                bui.append(",");
            }
            bui.append(t.nextToken());
        }
        repository.setProperty("completedStatusList", bui.toString());
        repository.setProperty("statusAttributeName", statusAttributeName.getText());
        repository.setProperty("dueDateAttributeName", dueDateAttributeName.getText());
        repository.setProperty("estimateAttributeName", estimateAttributeName.getText());
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
