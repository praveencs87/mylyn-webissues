package net.sf.webissues.ui;

import net.sf.webissues.core.WebIssuesCorePlugin;

import org.eclipse.mylyn.tasks.ui.TaskRepositoryLocationUiFactory;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class WebIssuesUiPlugin extends AbstractUIPlugin {

    public static final String ID_PLUGIN = "net.sf.webissues.uiW";

    private static WebIssuesUiPlugin plugin;

    public WebIssuesUiPlugin() {
        plugin = this;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        WebIssuesCorePlugin.getDefault().getConnector().setTaskRepositoryLocationFactory(new TaskRepositoryLocationUiFactory());
        TasksUi.getRepositoryManager().addListener(WebIssuesCorePlugin.getDefault().getConnector().getClientManager());

    }

    @Override
    public void stop(BundleContext context) throws Exception {
        TasksUi.getRepositoryManager().removeListener(WebIssuesCorePlugin.getDefault().getConnector().getClientManager());

        plugin = null;
        super.stop(context);
    }

    public static WebIssuesUiPlugin getDefault() {
        return plugin;
    }
}
