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

import net.sf.webissues.api.Condition;
import net.sf.webissues.api.ConditionType;
import net.sf.webissues.api.Environment;
import net.sf.webissues.api.ProtocolException;
import net.sf.webissues.api.Types;
import net.sf.webissues.api.Util;
import net.sf.webissues.api.View;
import net.sf.webissues.api.Views;
import net.sf.webissues.core.WebIssuesClient;
import net.sf.webissues.core.WebIssuesClientManager;
import net.sf.webissues.core.WebIssuesCorePlugin;
import net.sf.webissues.core.WebIssuesFilterQueryAdapter;
import net.sf.webissues.ui.WebIssuesUiPlugin;

import org.apache.commons.httpclient.HttpException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
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
    List<AttributeRow> rows = new ArrayList<AttributeRow>();
    private Combo type;
    private Types types;
    private Combo viewCombo;
    private Views views;

    private Button addButton;

    public WebIssuesQueryPage(TaskRepository repository, IRepositoryQuery query) {
        super(TITLE, repository, query);
        setTitle(TITLE);
        setDescription(DESCRIPTION);
    }

    public WebIssuesQueryPage(TaskRepository repository) {
        this(repository, null);
    }

    public void createControl(Composite parent) {
        Composite control = new Composite(parent, SWT.NONE);
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
        // createTextGroup(control);
        createOptionsGroup(control);

        if (getQuery() != null) {
            titleText.setText(getQuery().getSummary());
        }

        rebuildTypeList();
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
        // textField.setText(Util.nonNull(search.getSearchText()));
        // newSearchComments = search.isSearchComments();
        // searchComments.setSelection(newSearchComments);
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

    protected void createFolderTree(Composite control) {
        Composite group = new Group(control, SWT.NONE);
        group.setLayout(new GridLayout(2, false));
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

        // View
        l = new Label(group, SWT.NONE);
        l.setText("Preset View:");
        viewCombo = new Combo(group, 0);
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

        gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 1;
        viewCombo.setLayoutData(gd);
    }
    
    void computeAvailableActions() {
//        addButton.setEnabled(getSelectedView() == null);
    }

    net.sf.webissues.api.Type getSelectedType() {
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
            for (net.sf.webissues.api.Type issueType : types.values()) {
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
        viewCombo.add("Custom client side query ...");
        viewCombo.select(0);
        net.sf.webissues.api.Type type = getSelectedType();
        views = type == null ? null : type.getViews();
        if (views != null) {
            for (net.sf.webissues.api.View view : views.values()) {
                viewCombo.add(view.getName());
            }
        }
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
            Environment environment = getEnvironment();
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
        AttributeRow row = new AttributeRow(getSelectedType(), attributesGrid, condition, rows, getSelectedView() != null) {
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
                        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
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
        net.sf.webissues.api.Type selectedType = getSelectedType();
        search.setType(selectedType);
        View view = getSelectedView();
        if (view != null) {
            search.setView(view);
        }
        for (AttributeRow row : rows) {
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

    private Environment getEnvironment() throws HttpException, ProtocolException, IOException {
        return getClient().getEnvironment();
    }

    private WebIssuesClient getClient() throws ProtocolException, HttpException, IOException {
        return getClientManager().getClient(getTaskRepository(), new NullProgressMonitor());
    }

    private WebIssuesClientManager getClientManager() {
        return WebIssuesCorePlugin.getDefault().getConnector().getClientManager();
    }
}
