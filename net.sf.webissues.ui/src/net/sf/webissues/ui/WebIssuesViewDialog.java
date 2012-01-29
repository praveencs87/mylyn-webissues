package net.sf.webissues.ui;


import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.SelectionStatusDialog;
import org.webissues.api.IEnvironment;
import org.webissues.api.View;

public class WebIssuesViewDialog extends SelectionStatusDialog {

    private ViewEditor editor;
    private TaskRepository taskRepository;
    private IEnvironment environment;
    private View view;

    public WebIssuesViewDialog(Shell parent, TaskRepository taskRepository) {
        super(parent);
        this.taskRepository = taskRepository;
    }

    public ViewEditor getEditor() {
        return editor;
    }

    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(this.view == null ? "New View" : "Edit View " + view.getName());
        shell.setSize(640, 520);
    }

    protected Control createDialogArea(Composite parent) {
        Composite dialogParent = (Composite) super.createDialogArea(parent);
        editor = new ViewEditor(dialogParent, taskRepository) {
            @Override
            protected void validateName(String name) {
                if (name.length() == 0) {
                    updateStatus(new Status(Status.ERROR, WebIssuesUiPlugin.ID_PLUGIN, "Name is too short."));
                } else if (editor.getSelectedType() != null && editor.getSelectedType().getViews().getByName(name) != null) {
                    if (editor.getEditingView() == null || !editor.getEditingView().getName().equals(name)) {
                        updateStatus(new Status(Status.ERROR, WebIssuesUiPlugin.ID_PLUGIN, "A view named " + name
                                        + " already exists."));
                    }
                    else {
                        updateStatus(Status.OK_STATUS);
                    }
                } else {
                    updateStatus(Status.OK_STATUS);
                }
            }
        };
        if (environment != null) {
            editor.setEnvironment(environment);
        }
        if (view != null) {
            editor.setView(view);
        }
        return dialogParent;
    }

    public void setEnvironment(IEnvironment environment) {
        this.environment = environment;
        if (editor != null) {
            editor.setEnvironment(environment);
        }
    }

    public void setView(View view) {
        this.environment = view.getType().getTypes().getEnvironment();
        this.view = view;
    }

    @Override
    protected void computeResult() {
    }
}
