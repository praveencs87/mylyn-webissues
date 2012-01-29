package net.sf.webissues.ui.actions;

import net.sf.webissues.core.MonitorOperationAdapter;
import net.sf.webissues.core.WebIssuesClientManager;
import net.sf.webissues.core.WebIssuesCorePlugin;
import net.sf.webissues.ui.WebIssuesUiPlugin;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.mylyn.internal.tasks.core.AbstractTask;
import org.eclipse.mylyn.internal.tasks.ui.views.TaskListView;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.actions.ActionDelegate;

public class DeleteIssueAction extends ActionDelegate implements IWorkbenchWindowActionDelegate {

    @Override
    public void init(IWorkbenchWindow arg0) {
    }

    @SuppressWarnings("restriction")
    public void run(IAction action) {
        AbstractTask task = TaskListView.getFromActivePerspective().getSelectedTask();
        TaskRepository r = TasksUi.getRepositoryManager().getRepository(task.getConnectorKind(), task.getRepositoryUrl());
        Job deleteJob = new DeleteJob(r, Integer.parseInt(task.getTaskId()));
        deleteJob.setPriority(Job.SHORT);
        deleteJob.setUser(true);
        deleteJob.schedule();
    }

    class DeleteJob extends Job {

        private WebIssuesClientManager clientManager;
        private int issueId;
        private TaskRepository taskRepository;

        public DeleteJob(TaskRepository taskRepository, int issueId) {
            super("Deleting task");
            this.issueId = issueId;
            this.taskRepository = taskRepository;
            clientManager = WebIssuesCorePlugin.getDefault().getConnector().getClientManager();
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            monitor.beginTask("Deleting task " + issueId, IProgressMonitor.UNKNOWN);
            try {
                clientManager.getClient(taskRepository, monitor).getIssueDetails(issueId, monitor).getIssue()
                                .delete(new MonitorOperationAdapter(monitor));
            } catch (Exception e) {
                Status status = new Status(IStatus.ERROR, WebIssuesUiPlugin.ID_PLUGIN, e.getLocalizedMessage(), e);
                return status;
            }
            monitor.done();
            return Status.OK_STATUS;
        }

    }
}
