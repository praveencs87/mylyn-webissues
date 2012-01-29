package net.sf.webissues.ui.actions;

import net.sf.webissues.ui.WebIssuesViewManagementDialog;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.mylyn.internal.tasks.ui.actions.AbstractTaskRepositoryAction;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class ManageViewsActions extends AbstractTaskRepositoryAction implements IWorkbenchWindowActionDelegate {

    public ManageViewsActions() {
        super("A");
    }

    @Override
    public void init(IWorkbenchWindow arg0) {
    }

    @SuppressWarnings("restriction")
    public void run(IAction action) {
        final TaskRepository r = getTaskRepository(getStructuredSelection());
        WebIssuesViewManagementDialog wd = new WebIssuesViewManagementDialog(null, r);
        int dialogResponse = wd.open();
    }

    @Override
    public void selectionChanged(IAction arg0, ISelection arg1) {
    }

    @Override
    public void dispose() {
    }
}
