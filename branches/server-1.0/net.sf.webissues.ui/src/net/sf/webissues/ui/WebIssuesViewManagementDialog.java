package net.sf.webissues.ui;

import java.util.ArrayList;
import java.util.List;

import net.sf.webissues.api.Access;
import net.sf.webissues.api.Environment;
import net.sf.webissues.api.IssueType;
import net.sf.webissues.api.User;
import net.sf.webissues.api.View;
import net.sf.webissues.api.ViewDefinition;
import net.sf.webissues.api.Views;
import net.sf.webissues.core.MonitorOperationAdapter;
import net.sf.webissues.core.WebIssuesClient;
import net.sf.webissues.core.WebIssuesCorePlugin;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;

public class WebIssuesViewManagementDialog extends ElementListSelectionDialog {
    private boolean multipleSelection;
    private Button remove;
    private Button add;
    private List<Object> chosenItems;
    private TaskRepository taskRepository;
    private Environment environment;
    private Button edit;
    private View selectedView;

    public WebIssuesViewManagementDialog(Shell parent, TaskRepository taskRepository) {
        super(parent, new ViewLabelProvider());
        this.taskRepository = taskRepository;
        this.chosenItems = new ArrayList<Object>();
        setValidator(new ISelectionStatusValidator() {

            public IStatus validate(Object[] arg0) {
                return new Status(IStatus.OK, WebIssuesUiPlugin.ID_PLUGIN, IStatus.OK, "Valid view selection", null);
            }
        });
        setTitles();
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
        List<View> v = new ArrayList<View>();
        for (IssueType type : environment.getTypes().values()) {
            Views views = type.getViews();
            v.addAll(views.values());
        }
        if (fFilteredList != null) {
            setListElements(v.toArray(new View[0]));
        } else {
            setElements(v.toArray(new View[0]));
        }
    }

    @Override
    public void setMultipleSelection(boolean multipleSelection) {
        this.multipleSelection = multipleSelection;
        super.setMultipleSelection(multipleSelection);
        if (multipleSelection) {
            setMatchEmptyString(true);
        }
        setTitles();
    }

    /**
     * Create the dialog content.
     * 
     * @param parent Parent composite
     * @return The content of the dialog
     */
    @Override
    protected Control createDialogArea(Composite parent) {

        Composite newResult = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        newResult.setLayout(layout);

        super.createDialogArea(newResult);

        // Buttons
        Composite buttons = new Composite(newResult, SWT.NONE);
        GridData gd = new GridData(SWT.CENTER, SWT.TOP, true, true);
        buttons.setLayoutData(gd);
        RowLayout rowLayout = new RowLayout(SWT.VERTICAL);
        rowLayout.pack = false;
        buttons.setLayout(rowLayout);
        add = new Button(buttons, SWT.PUSH);
        add.setText("Add");
        add.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent arg0) {
                WebIssuesViewDialog wd = new WebIssuesViewDialog(getShell(), taskRepository);
                wd.setEnvironment(environment);
                int ret = wd.open();
                if (ret == Dialog.OK) {
                    ViewEditor editor = wd.getEditor();
                    CreateViewJob job = new CreateViewJob(editor.getSelectedType(), editor.getViewName(), editor.isPublic(), editor
                                    .getDefinition());
                    job.setPriority(Job.SHORT);
                    job.setUser(true);
                    job.schedule();
                }
            }

            public void widgetDefaultSelected(SelectionEvent arg0) {
            }
        });
        remove = new Button(buttons, SWT.PUSH);
        remove.setText("Remove");
        remove.setEnabled(false);
        remove.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent arg0) {
                MessageBox mb = new MessageBox(getShell(), SWT.ICON_QUESTION | SWT.OK | SWT.CANCEL);
                mb.setText("Confirm View Removal");
                mb.setMessage("Are you sure you want to remove these " + getSelectedElements().length + " views?");
                int rc = mb.open();
                if (rc == SWT.OK) {
                    removeViews();
                }
            }

            public void widgetDefaultSelected(SelectionEvent arg0) {
            }
        });
        edit = new Button(buttons, SWT.PUSH);
        edit.setText("Edit");
        edit.setEnabled(false);
        edit.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent arg0) {
                WebIssuesViewDialog wd = new WebIssuesViewDialog(getShell(), taskRepository);
                View view = (View) getSelectedElements()[0];
                wd.setView(view);
                int ret = wd.open();
                if (ret == Dialog.OK) {
                    ViewEditor editor = wd.getEditor();
                    EditViewJob job = new EditViewJob(view, editor.getViewName(), editor.isPublic(), editor.getDefinition());
                    job.setPriority(Job.SHORT);
                    job.setUser(true);
                    job.schedule();
                }
            }

            public void widgetDefaultSelected(SelectionEvent arg0) {
            }
        });

        fFilteredList.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                selectedView = getSelectionIndex() == -1 ? null : (View) getSelectedElements()[0];
                setAvailable();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });

        return newResult;
    }

    @Override
    protected boolean validateCurrentSelection() {
        boolean ok = super.validateCurrentSelection();
        setAvailable();
        return ok;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void okPressed() {
        if (multipleSelection) {
            List chosenItems = getChosenItems();
            setResult(chosenItems);
            setSelection(chosenItems.toArray(new User[0]));
        }
        Object[] result = getSelectedElements();
        // ignore
        super.okPressed();
    }

    @SuppressWarnings("unchecked")
    public List getChosenItems() {
        return chosenItems;
    }

    private void setTitles() {
        setMessage(multipleSelection ? "Choose views ..." : "Choose view");
        setTitle(multipleSelection ? "Choose Views" : "Choose View");
    }

    private void setAvailable() {
        boolean admin = environment.getOwnerUser().getAccess().equals(Access.ADMIN);
        add.setEnabled(admin);
        remove.setEnabled(getSelectedElements().length > 0 && admin);
        edit.setEnabled(getSelectedElements().length == 1 && admin);
    }

    private void removeViews() {
        Job deleteJob = new DeleteJob(getSelectedElements());
        deleteJob.setPriority(Job.SHORT);
        deleteJob.setUser(true);
        deleteJob.schedule();
    }

    private void doRebuild() {
        getShell().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                rebuild();
            }
        });
    }

    private void rebuild() {
        setEnvironment(environment);
    }

    class DeleteJob extends Job {
        private Object[] selected;

        public DeleteJob(Object[] selected) {
            super("Removing views");
            this.selected = selected;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            monitor.beginTask("Deleting views", selected.length);
            try {
                for (Object o : selected) {
                    View view = (View) o;
                    view.delete(new MonitorOperationAdapter(monitor));
                    monitor.worked(1);
                }
            } catch (Exception e) {
                Status status = new Status(IStatus.ERROR, WebIssuesUiPlugin.ID_PLUGIN, e.getLocalizedMessage(), e);
                return status;
            } finally {
                monitor.done();
                doRebuild();
            }
            return Status.OK_STATUS;
        }

    }

    class CreateViewJob extends Job {

        private String viewName;
        private boolean publicView;
        private ViewDefinition definition;
        private IssueType type;

        public CreateViewJob(IssueType type, String viewName, boolean publicView, ViewDefinition definition) {
            super("Creating view");
            this.viewName = viewName;
            this.type = type;
            this.publicView = publicView;
            this.definition = definition;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            monitor.beginTask("Creating views", 1);
            try {
                type.getViews().createView(viewName, publicView, definition, new MonitorOperationAdapter(monitor));
                monitor.worked(1);
            } catch (Exception e) {
                Status status = new Status(IStatus.ERROR, WebIssuesUiPlugin.ID_PLUGIN, e.getLocalizedMessage(), e);
                return status;
            } finally {
                monitor.done();
                doRebuild();
            }
            return Status.OK_STATUS;
        }

    }

    class EditViewJob extends Job {

        private String newViewName;
        private boolean publicView;
        private ViewDefinition definition;
        private View view;

        public EditViewJob(View view, String newViewName, boolean publicView, ViewDefinition definition) {
            super("Editing view");
            this.newViewName = newViewName;
            this.view = view;
            this.publicView = publicView;
            this.definition = definition;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            try {
                monitor.beginTask("Update views", 3);
                if (!view.getName().equals(newViewName)) {
                    view.rename(new MonitorOperationAdapter(monitor), newViewName);
                }
                monitor.worked(1);
                view.publish(publicView, new MonitorOperationAdapter(monitor));
                monitor.worked(1);
                definition.update(new MonitorOperationAdapter(monitor));
                monitor.worked(1);
            } catch (Exception e) {
                Status status = new Status(IStatus.ERROR, WebIssuesUiPlugin.ID_PLUGIN, e.getLocalizedMessage(), e);
                return status;
            } finally {
                monitor.done();
                doRebuild();
            }
            return Status.OK_STATUS;
        }

    }

    public View getSelectedView() {
        return selectedView;
    }
}
