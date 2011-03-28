package net.sf.webissues.ui;

import net.sf.webissues.core.WebIssuesCorePlugin;

import org.eclipse.mylyn.internal.tasks.core.AbstractSearchHandler;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskData;

@SuppressWarnings("restriction")
public class WebIssuesSearchHandler extends AbstractSearchHandler {

    @Override
    public String getConnectorKind() {
        return WebIssuesCorePlugin.CONNECTOR_KIND;
    }

    @Override
    public boolean queryForText(TaskRepository taskRepository, IRepositoryQuery query, TaskData taskData, String searchString) {
        return false;
    }

}
