package net.sf.webissues.ui;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import net.sf.webissues.api.Attribute;
import net.sf.webissues.api.Condition;
import net.sf.webissues.api.ConditionType;
import net.sf.webissues.api.Environment;
import net.sf.webissues.api.IssueType;
import net.sf.webissues.api.IssueTypes;
import net.sf.webissues.api.ProtocolException;
import net.sf.webissues.api.View;
import net.sf.webissues.api.ViewDefinition;
import net.sf.webissues.core.WebIssuesClient;
import net.sf.webissues.core.WebIssuesClientManager;
import net.sf.webissues.core.WebIssuesCorePlugin;

import org.apache.commons.httpclient.HttpException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

public class ViewEditor {
    private Combo type;
    private Text nameText;
    private AttributesEditor attributesEditor;
    private ColumnsEditor columnsEditor;
    private Button updateButton;
    private TaskRepository taskRepository;
    private IssueTypes types;
    private Combo sortColumn;
    private Button ascending;
    private Button descending;
    private Button publicViewCheckbox;
    private IssueType selectedType;
    private String name;
    private boolean publicView;
    private boolean ascendingSort;
    private Attribute sortAttribute;
    private View editingView;
    private Label typeLabel;

    public ViewEditor(Composite parent, TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
        Composite control = new Composite(parent, SWT.NONE);
        control.setLayoutData(new GridData(GridData.FILL_BOTH));
        GridLayout layout = new GridLayout(2, false);
        control.setLayout(layout);
        createMain(control);
        createTabs(control);
        createUpdateButton(control);
        rebuildTypeList();
        validateName(nameText.getText());
    }

    public void setView(View view) {
        type.setEnabled(false);
        typeLabel.setEnabled(false);
        columnsEditor.clearRows();
        editingView = view;
        setEnvironment(view.getType().getViews().getEnvironment());
        name = view.getName();
        publicView = view.isPublicView();
        selectedType = view.getType();
        nameText.setText(name);
        publicViewCheckbox.setSelection(publicView);
        for (int i = 0; i < type.getItemCount(); i++) {
            if (type.getItem(i).equals(selectedType.getName())) {
                type.select(i);
            }
        }
        ViewDefinition definition = view.getDefinition();
        for (Attribute attr : definition.getAttributes()) {
            columnsEditor.addRow(attr);
        }
        sortAttribute = definition.getSortAttribute();
        ascendingSort = definition.isSortAscending();
        for (int i = 0; i < sortColumn.getItemCount(); i++) {
            if (sortColumn.getItem(i).equals(sortAttribute.getName())) {
                sortColumn.select(i);
            }
        }
        ascending.setSelection(ascendingSort);
        descending.setSelection(!ascendingSort);
        validateName(name);
    }

    public void setEnvironment(Environment environment) {
        types = environment.getTypes();
        rebuildTypeList();
    }

    public IssueType getSelectedType() {
        return selectedType;
    }

    public IssueType doGetSelectedType() {
        int sel = type.getSelectionIndex();
        return sel < 0 ? null : types.getByName(type.getItem(sel));
    }

    public boolean isPublic() {
        return publicView;
    }

    public String getViewName() {
        return name;
    }

    public Attribute getSortAttribute() {
        return sortAttribute;
    }

    public boolean isSortAscending() {
        return ascendingSort;
    }

    public ViewDefinition getDefinition() {
        ViewDefinition def = new ViewDefinition(editingView);
        for (Attribute attr : columnsEditor.getUsedAttributes()) {
            def.addColumn(attr);
        }
        def.addAll(attributesEditor.getConditions());
        def.setSortColumn(getSortAttribute());
        def.setSortAscending(isSortAscending());
        return def;
    }

    //

    protected void handleUpdateError(Throwable e) {
    }

    protected void createTabs(Composite control) {
        TabFolder folder = new TabFolder(control, SWT.NONE);

        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 3;
        folder.setLayoutData(gd);

        // Filter
        TabItem filter = new TabItem(folder, SWT.NONE);
        filter.setText("Filter");
        filter.setControl(createFilterGroup(folder));

        // Columns
        TabItem column = new TabItem(folder, SWT.NONE);
        column.setText("Columns");
        column.setControl(createColumnsGroup(folder));
    }

    protected void doRun(IRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException {
        IProgressService service = PlatformUI.getWorkbench().getProgressService();
        service.busyCursorWhile(runnable);
    }

    protected void validateName(String name) {
    }

    protected void createMain(final Composite control) {
        Composite group = new Group(control, SWT.NONE);
        group.setLayout(new GridLayout(3, false));
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.grabExcessVerticalSpace = false;
        gd.horizontalSpan = 2;
        group.setLayoutData(gd);

        Label titleLabel = new Label(group, SWT.NONE);
        titleLabel.setText("Name:");

        nameText = new Text(group, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        gd.horizontalSpan = 2;
        nameText.setLayoutData(gd);
        nameText.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {
                name = nameText.getText();
                validateName(nameText.getText());
                // ignore
            }

            public void keyReleased(KeyEvent e) {
                name = nameText.getText();
                validateName(nameText.getText());
            }
        });

        // Type
        typeLabel = new Label(group, SWT.NONE);
        typeLabel.setText("Folder Type:");
        type = new Combo(group, SWT.READ_ONLY);
        type.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                selectedType = doGetSelectedType();
                attributesEditor.setType(selectedType);
                columnsEditor.setType(selectedType);
                columnsEditor.addRow(selectedType.get(IssueType.ID_ATTR_ID));
                columnsEditor.addRow(selectedType.get(IssueType.NAME_ATTR_ID));
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        type.setLayoutData(gd);

        // Type
        publicViewCheckbox = new Button(group, SWT.CHECK);
        publicViewCheckbox.setText("Public &view");
        publicViewCheckbox.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                publicView = publicViewCheckbox.getSelection();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                publicView = publicViewCheckbox.getSelection();
            }
        });
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalIndent = 8;
        gd.verticalIndent = 8;
        gd.horizontalSpan = 2;
        publicViewCheckbox.setLayoutData(gd);
    }

    protected Control createUpdateButton(final Composite control) {
        Composite group = new Composite(control, SWT.NONE);
        GridLayout layout = new GridLayout(4, false);
        group.setLayout(layout);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        updateButton = new Button(group, SWT.PUSH);
        updateButton.setText("Update Attributes from Repository");
        updateButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (taskRepository != null) {
                    updateClient(true);
                } else {
                    MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "Update Attributes Failed",
                        "No repository available, please add one using the Task Repositories view.");
                }
            }
        });

        new Label(group, SWT.NONE);

        return group;
    }

    protected Control createFilterGroup(Composite control) {
        Composite group = new Composite(control, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 4;
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.horizontalSpan = 4;
        group.setLayoutData(gd);
        group.setLayout(layout);

        // Add button
        Button addButton = new Button(group, SWT.PUSH);
        addButton.setText("Add");
        addButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                attributesEditor.addRow(new Condition(ConditionType.EQ, null, null));
            }
        });
        gd = new GridData();
        gd.horizontalSpan = 3;
        addButton.setLayoutData(gd);

        createFilter(group);
        return group;
    }

    protected Control createSortingGroup(Composite control) {
        Composite group = new Composite(control, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        group.setLayout(layout);
        GridData gd = new GridData(GridData.FILL_VERTICAL);
        gd.horizontalIndent = 8;
        gd.horizontalSpan = 1;
        group.setLayoutData(gd);

        Label l = new Label(group, SWT.NONE);
        l.setText("Sort column");
        l.setLayoutData(new GridData());

        sortColumn = new Combo(group, SWT.READ_ONLY);
        sortColumn.setLayoutData(new GridData());
        sortColumn.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                sortAttribute = doGetSortAttribute();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                sortAttribute = doGetSortAttribute();
            }
        });

        ascending = new Button(group, SWT.RADIO);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalIndent = 16;
        gd.horizontalSpan = 2;
        ascending.setText("Ascending order");
        ascending.setLayoutData(gd);

        descending = new Button(group, SWT.RADIO);
        descending.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                ascendingSort = !descending.getSelection();

            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                ascendingSort = !descending.getSelection();
            }
        });
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalIndent = 16;
        gd.horizontalSpan = 2;
        descending.setText("Descending order");
        descending.setLayoutData(gd);
        descending.setSelection(true);

        return group;
    }

    protected Control createColumnsGroup(Composite control) {
        Composite group = new Composite(control, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 3;
        group.setLayoutData(gd);
        GridLayout layout = new GridLayout();
        layout.numColumns = 4;
        group.setLayout(layout);

        createColumns(group);
        createSortingGroup(group);

        return group;
    }

    //

    private void buildSortList() {
        int idx = sortColumn.getSelectionIndex();
        String val = idx == -1 ? null : sortColumn.getItem(idx);
        sortColumn.removeAll();
        for (Attribute attr : columnsEditor.getUsedAttributes()) {
            sortColumn.add(attr.getName());
            if (attr.getName().equals(val) || (val == null && sortColumn.getItemCount() == 1)) {
                sortColumn.select(sortColumn.getItemCount() - 1);
            }
        }
        if (sortColumn.getSelectionIndex() == -1 && sortColumn.getItemCount() > 0) {
            sortColumn.select(0);
        }
        sortAttribute = doGetSortAttribute();
    }

    private void rebuildTypeList() {
        type.removeAll();
        if (types != null) {
            for (net.sf.webissues.api.IssueType issueType : types.values()) {
                type.add(issueType.getName());
                if (type.getItemCount() == 1) {
                    type.select(0);
                    selectedType = issueType;
                    attributesEditor.setType(issueType);
                    columnsEditor.setType(issueType);
                    if (editingView == null) {
                        columnsEditor.addRow(selectedType.get(IssueType.ID_ATTR_ID));
                        columnsEditor.addRow(selectedType.get(IssueType.NAME_ATTR_ID));
                    }
                }
            }
        }
    }

    protected void createFilter(Composite control) {
        attributesEditor = new AttributesEditor(control);
    }

    protected void createColumns(Composite control) {
        columnsEditor = new ColumnsEditor(control) {
            public void setAvailable() {
                super.setAvailable();
                buildSortList();
            }
        };
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 3;
        gd.heightHint = 200;
        columnsEditor.setLayoutData(gd);
    }

    private Attribute doGetSortAttribute() {
        int idx = sortColumn.getSelectionIndex();
        IssueType type = getSelectedType();
        return idx == -1 || type == null ? null : type.getByName(sortColumn.getItem(idx));
    }

    private WebIssuesClient getClient() throws ProtocolException, HttpException, IOException {
        return getClient(new NullProgressMonitor());
    }

    private WebIssuesClient getClient(IProgressMonitor monitor) throws ProtocolException, HttpException, IOException {
        return getClientManager().getClient(taskRepository, monitor);
    }

    private WebIssuesClientManager getClientManager() {
        return WebIssuesCorePlugin.getDefault().getConnector().getClientManager();
    }

    private void updateClient(final boolean force) {
        try {
            IRunnableWithProgress runnable = new IRunnableWithProgress() {
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        final WebIssuesClient client = getClient();
                        try {
                            client.updateAttributes(monitor, force);
                        } finally {
                            types = client.getEnvironment().getTypes();
                        }
                        PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
                            public void run() {
                                rebuildTypeList();
                            }
                        });
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    }
                }
            };

            doRun(runnable);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            handleUpdateError(e.getCause());
            return;
        } catch (InterruptedException e) {
            return;
        }
    }

    public View getEditingView() {
        return editingView;
    }
}