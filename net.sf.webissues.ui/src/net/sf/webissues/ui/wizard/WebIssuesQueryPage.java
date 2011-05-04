/*******************************************************************************
 * Copyright (c) 2006, 2008 Steffen Pingel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package net.sf.webissues.ui.wizard;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.webissues.core.WebIssuesClient;
import net.sf.webissues.core.WebIssuesClientManager;
import net.sf.webissues.core.WebIssuesCorePlugin;
import net.sf.webissues.core.WebIssuesFilterQueryAdapter;
import net.sf.webissues.ui.ConditionRow;
import net.sf.webissues.ui.WebIssuesImages;
import net.sf.webissues.ui.WebIssuesUiPlugin;
import net.sf.webissues.ui.WebIssuesViewManagementDialog;

import org.apache.commons.httpclient.HttpException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.mylyn.internal.provisional.commons.ui.CommonImages;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositoryQueryPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;
import org.webissues.api.Access;
import org.webissues.api.Condition;
import org.webissues.api.ConditionType;
import org.webissues.api.IEnvironment;
import org.webissues.api.IssueType;
import org.webissues.api.IssueTypes;
import org.webissues.api.ProtocolException;
import org.webissues.api.Util;
import org.webissues.api.View;
import org.webissues.api.Views;

/**
 * Evolved from trac search page.
 * 
 * @author Steffen Pingel
 */
public class WebIssuesQueryPage extends AbstractRepositoryQueryPage {
    final static Logger LOG = Logger.getLogger(WebIssuesQueryPage.class.getName());

    private static final String TITLE = "Enter query parameters";
    private static final String DESCRIPTION = "If attributes are blank or stale press the Update button.";
    private static final String TITLE_QUERY_TITLE = "Query Title:";

    protected final static String PAGE_NAME = "WebIssuesSearchPage"; //$NON-NLS-1$
    private static final String SEARCH_URL_ID = PAGE_NAME + ".SEARCHURL";

    private Text titleText;
    private Text textField;
    private Button updateButton;
    private boolean firstTime = true;
    private Button searchComments;
    private Composite attributesGrid;
    private ScrolledComposite scroller;
    List<ConditionRow> rows = new ArrayList<ConditionRow>();
    private Combo type;
    private IssueTypes types;
    private Combo viewCombo;
    private Views views;
    private Button addButton;
    private Label viewLabel;
    private Composite control;
    private Button manageViews;

    public WebIssuesQueryPage(TaskRepository repository, IRepositoryQuery query) {
        super(TITLE, repository, query);
        setTitle(TITLE);
        setDescription(DESCRIPTION);
    }

    public WebIssuesQueryPage(TaskRepository repository) {
        this(repository, null);
    }

    public void createControl(Composite parent) {
        control = new Composite(parent, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_BOTH);
        control.setLayoutData(gd);
        GridLayout layout = new GridLayout(2, false);
        if (inSearchContainer()) {
            layout.marginWidth = 0;
            layout.marginHeight = 0;
        }
        gd.horizontalSpan = 1;
        control.setLayout(layout);

        createTitleGroup(control);
        createFolderTree(control);
        createOptionsGroup(control);

        if (getQuery() != null) {
            titleText.setText(getQuery().getSummary());
        }

        setControl(control);
        computeAvailableActions();
    }

    @Override
    public boolean canFlipToNextPage() {
        return false;
    }

    private synchronized void restoreWidgetValues(WebIssuesFilterQueryAdapter search) {
        updateClient(false);
        type.select(type.indexOf(search.getType().getName()));
        if (search.getView() != null) {
            for (int i = 0; i < viewCombo.getItemCount(); i++) {
                if (viewCombo.getItem(i).equals(search.getView().getName())) {
                    viewCombo.select(i);
                    break;
                }
            }
        }
        for (Condition condition : search.getConditions()) {
            addRow(condition);
        }
    }

    private void createTitleGroup(Composite control) {
        if (inSearchContainer()) {
            return;
        }

        Label titleLabel = new Label(control, SWT.NONE);
        titleLabel.setText(TITLE_QUERY_TITLE);

        titleText = new Text(control, SWT.BORDER);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        gd.horizontalSpan = 3;
        titleText.setLayoutData(gd);
        titleText.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {
                // ignore
            }

            public void keyReleased(KeyEvent e) {
                getContainer().updateButtons();
            }
        });
    }

    protected void createFolderTree(final Composite control) {
        Composite group = new Group(control, SWT.NONE);
        group.setLayout(new GridLayout(3, false));
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 2;
        group.setLayoutData(gd);

        // Type
        Label l = new Label(group, SWT.NONE);
        l.setText("Folder Type:");
        type = new Combo(group, 0);
        type.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                rebuildAttributes();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        type.setLayoutData(gd);

        // View
        viewLabel = new Label(group, SWT.NONE);
        viewLabel.setText("View:");
        viewCombo = new Combo(group, SWT.READ_ONLY);
        viewCombo.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                clearRows();
                computeAvailableActions();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                computeAvailableActions();
            }
        });
        manageViews = new Button(group, SWT.BORDER);
        manageViews.setImage(CommonImages.getImage(WebIssuesImages.MANAGE));
        manageViews.setToolTipText("Manage server provided views");
        manageViews.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                try {
                    WebIssuesViewManagementDialog wd = new WebIssuesViewManagementDialog(control.getShell(), getTaskRepository());
                    wd.setEnvironment(getEnvironment());
                    if (wd.open() == Dialog.OK) {
                        rebuildViewList();
                        View view = wd.getSelectedView();
                        if (view != null) {
                            for (int i = 0; i < viewCombo.getItemCount(); i++) {
                                if (viewCombo.getItem(i).equals(view.getName())) {
                                    viewCombo.select(i);
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    setErrorMessage(e.getMessage());
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
            }
        });

        gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 1;
        viewCombo.setLayoutData(gd);
    }

    protected void computeAvailableActions() {
        // addButton.setEnabled(getSelectedView() == null);
    }

    public IssueType getSelectedType() {
        int sel = type.getSelectionIndex();
        return sel < 0 ? null : types.getByName(type.getItem(sel));
    }

    private View getSelectedView() {
        int selectionIndex = viewCombo.getSelectionIndex();
        return selectionIndex == -1 || views == null ? null : views.getByName(viewCombo.getItem(selectionIndex));
    }

    private void rebuildTypeList() {
        type.removeAll();
        if (types != null) {
            for (org.webissues.api.IssueType issueType : types.values()) {
                type.add(issueType.getName());
                if (type.getItemCount() == 1) {
                    type.select(0);
                }
            }
        }
        rebuildViewList();
    }

    private void rebuildViewList() {
        viewCombo.removeAll();
        viewCombo.add("Custom query ...");
        viewCombo.select(0);
        org.webissues.api.IssueType type = getSelectedType();
        IEnvironment environment = type == null ? null : type.getTypes().getEnvironment();
        views = type == null || type.getViews() == null ? null : type.getViews();
        if (views != null && !environment.getVersion().startsWith("0.")) {
            for (org.webissues.api.View view : views.values()) {
                viewCombo.add(view.getName());
            }
            viewCombo.setEnabled(true);
            viewLabel.setEnabled(true);
            manageViews.setEnabled(environment.getOwnerUser().getAccess().equals(Access.ADMIN));

            viewCombo.setToolTipText("View defined or server or custom query");
        } else {
            viewCombo.setToolTipText("Views not support by server");
            viewCombo.setEnabled(false);
            viewLabel.setEnabled(false);
            manageViews.setEnabled(false);

        }
        control.layout();
    }

    void rebuildAttributes() {
        rebuildViewList();
        clearRows();
    }

    private void clearRows() {
        rows.clear();
        for (Control child : attributesGrid.getChildren()) {
            child.dispose();
        }
    }

    protected Control createTextGroup(Composite control) {

        Composite group = new Group(control, SWT.NONE);
        // group.setText("Ticket Attributes");
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        group.setLayout(layout);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        group.setLayoutData(gd);

        Label label = new Label(group, SWT.LEFT);
        label.setText("Text:");

        GridData gd3 = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        textField = new Text(group, SWT.BORDER);
        textField.setLayoutData(gd3);

        label = new Label(group, SWT.LEFT);
        label.setText("Search in:");
        Composite opts = new Composite(group, SWT.NONE);
        GridData gd2 = new GridData();
        gd2.horizontalIndent = 10;
        opts.setLayoutData(gd2);
        opts.setLayout(new RowLayout());

        searchComments = new Button(opts, SWT.CHECK);
        searchComments.setText("Comments");
        searchComments.setSelection(true);

        return group;
    }

    protected Control createOptionsGroup(Composite control) {
        Group group = new Group(control, SWT.NONE);
        // group.setText("Ticket Attributes");
        GridLayout layout = new GridLayout();
        layout.numColumns = 4;
        group.setLayout(layout);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.horizontalSpan = 4;
        group.setLayoutData(gd);

        createAttributes(group);
        createUpdateButton(group);

        return group;
    }

    protected Control createAttributes(Composite control) {
        scroller = new ScrolledComposite(control, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.horizontalSpan = 4;
        gd.heightHint = 200;
        scroller.setLayoutData(gd);
        scroller.setExpandHorizontal(true);
        scroller.setExpandVertical(true);
        Point minSize = control.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        scroller.setMinWidth(minSize.x);
        scroller.setMinHeight(minSize.y);
        scroller.setAlwaysShowScrollBars(false);

        attributesGrid = new Composite(scroller, SWT.NONE);
        attributesGrid.setBackground(scroller.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        GridLayout layout = new GridLayout();
        layout.numColumns = 5;
        attributesGrid.setLayout(layout);

        scroller.setContent(attributesGrid);

        return attributesGrid;
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
                if (getTaskRepository() != null) {
                    updateClient(true);
                } else {
                    MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "Update Attributes Failed",
                        "No repository available, please add one using the Task Repositories view.");
                }
            }
        });

        new Label(group, SWT.NONE);

        addButton = new Button(group, SWT.PUSH);
        addButton.setText("Add");
        addButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                addRow(new Condition(ConditionType.EQ, null, null));
            }
        });

        return group;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (getSearchContainer() != null) {
            getSearchContainer().setPerformActionEnabled(true);
        }
        if (visible && firstTime) {
            Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                    if (getControl() != null && !getControl().isDisposed()) {
                        initializePage();
                        firstTime = false;
                    }
                }
            });
        }
    }

    private void initializePage() {
        boolean restored = false;
        IRepositoryQuery query = getQuery();
        try {
            IEnvironment environment = getEnvironment();
            if (query != null) {
                WebIssuesFilterQueryAdapter search = new WebIssuesFilterQueryAdapter(query, environment);
                restoreWidgetValues(search);
                restored = true;
            } else {
                if (inSearchContainer()) {
                    restored |= restoreWidgetValues();
                }
                if (!restored) {
                    restoreWidgetValues(new WebIssuesFilterQueryAdapter(environment));
                }
            }
        } catch (Exception e) {
            setErrorMessage(e.getMessage());
            e.printStackTrace();
        }
    }

    private void addRow(Condition condition) {
        boolean viewSelected = getSelectedView() != null;
        ConditionRow row = new ConditionRow(getSelectedType(), attributesGrid, condition, rows, viewSelected) {
            @Override
            protected void onDoLayout() {
                attributesGrid.layout();
                Point minSize = attributesGrid.computeSize(SWT.DEFAULT, SWT.DEFAULT);
                scroller.setMinWidth(minSize.x);
                scroller.setMinHeight(minSize.y);
            }
        };
        attributesGrid.layout();
        rows.add(row);
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

            if (getContainer() != null) {
                getContainer().run(true, true, runnable);
            } else if (getSearchContainer() != null) {
                getSearchContainer().getRunnableContext().run(true, true, runnable);
            } else {
                IProgressService service = PlatformUI.getWorkbench().getProgressService();
                service.busyCursorWhile(runnable);
            }
        } catch (InvocationTargetException e) {
            LOG.log(Level.SEVERE, "Failed to get client for query.", e);
            setErrorMessage(WebIssuesCorePlugin.toStatus(e.getCause(), getTaskRepository()).getMessage());
            return;
        } catch (InterruptedException e) {
            return;
        }
    }

    @Override
    public boolean isPageComplete() {
        if (titleText != null && titleText.getText().length() > 0) {
            return true;
        }
        return false;
    }

    public String getQueryUrl(String repsitoryUrl) {
        WebIssuesFilterQueryAdapter search = getSearch();
        StringBuilder sb = new StringBuilder();
        sb.append(Util.concatenateUri(repsitoryUrl, "search"));
        sb.append("?");
        sb.append(search.toQueryString());
        return sb.toString();
    }

    private WebIssuesFilterQueryAdapter getSearch() {

        // Search
        WebIssuesFilterQueryAdapter search = new WebIssuesFilterQueryAdapter();
        org.webissues.api.IssueType selectedType = getSelectedType();
        search.setType(selectedType);
        View view = getSelectedView();
        if (view != null) {
            search.setView(view);
        }
        for (ConditionRow row : rows) {
            if (row.getCondition() != null) {
                search.addCondition(row.getCondition());
            }
        }
        return search;
    }

    @Override
    public boolean performSearch() {
        if (inSearchContainer()) {
            saveState();
        }

        return super.performSearch();
    }

    @Override
    public IDialogSettings getDialogSettings() {
        IDialogSettings settings = WebIssuesUiPlugin.getDefault().getDialogSettings();
        IDialogSettings dialogSettings = settings.getSection(PAGE_NAME);
        if (dialogSettings == null) {
            dialogSettings = settings.addNewSection(PAGE_NAME);
        }
        return dialogSettings;
    }

    private boolean restoreWidgetValues() {
        IDialogSettings settings = getDialogSettings();
        String repoId = "." + getTaskRepository().getRepositoryUrl();
        String searchUrl = settings.get(SEARCH_URL_ID + repoId);
        if (searchUrl != null) {
            try {
                restoreWidgetValues(new WebIssuesFilterQueryAdapter(new URL(searchUrl), getEnvironment()));
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public void saveState() {
        String repoId = "." + getTaskRepository().getRepositoryUrl();
        IDialogSettings settings = getDialogSettings();
        settings.put(SEARCH_URL_ID + repoId, getSearch().toQueryString());
    }

    @Override
    public String getQueryTitle() {
        return (titleText != null) ? titleText.getText() : null;
    }

    @Override
    public void applyTo(IRepositoryQuery query) {
        query.setUrl(getQueryUrl(getTaskRepository().getRepositoryUrl()));
        query.setSummary(getQueryTitle());
    }

    private IEnvironment getEnvironment() throws HttpException, ProtocolException, IOException {
        return getClient().getEnvironment();
    }

    private WebIssuesClient getClient() throws ProtocolException, HttpException, IOException {
        return getClientManager().getClient(getTaskRepository(), new NullProgressMonitor());
    }

    private WebIssuesClientManager getClientManager() {
        return WebIssuesCorePlugin.getDefault().getConnector().getClientManager();
    }
}
