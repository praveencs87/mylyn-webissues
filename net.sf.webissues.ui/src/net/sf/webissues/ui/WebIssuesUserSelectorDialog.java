package net.sf.webissues.ui;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.FilteredList;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.webissues.api.Access;
import org.webissues.api.User;

public class WebIssuesUserSelectorDialog extends ElementListSelectionDialog {
    private boolean multipleSelection;
    private FilteredList list;
    private Button removeAll;
    private Button remove;
    private Button add;
    private User anyone;
    private List<Object> chosenItems;
    private User me;

    public WebIssuesUserSelectorDialog(User me, Shell parent, Collection<User> users, boolean includeAnyone) {
        super(parent, new UserLabelProvider(me));
        this.me = me;
        this.chosenItems = new ArrayList<Object>();
        setValidator(new ISelectionStatusValidator() {

            public IStatus validate(Object[] arg0) {
                return new Status(IStatus.OK, WebIssuesUiPlugin.ID_PLUGIN, IStatus.OK, "Valid user selection", null);
            }
        });
        setTitles();
        List<User> owners = new ArrayList<User>(users);
        if (includeAnyone) {
            anyone = new User(me.getEnvironment(), "", "All", Access.NONE);
            owners.add(0, anyone);
        }
        setElements(owners.toArray(new User[0]));
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
    @SuppressWarnings("unchecked")
    @Override
    protected Control createDialogArea(Composite parent) {

        if (!multipleSelection) {
            return super.createDialogArea(parent);
        }

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
        add.setEnabled(false);
        add.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent arg0) {
                chosenItems.addAll(Arrays.asList(getSelectedElements()));
                list.setElements(chosenItems.toArray());
            }

            public void widgetDefaultSelected(SelectionEvent arg0) {
            }
        });
        remove = new Button(buttons, SWT.PUSH);
        remove.setText("Remove");
        remove.setEnabled(false);
        remove.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent arg0) {
                chosenItems.removeAll(Arrays.asList(list.getSelection()));
                list.setElements(chosenItems.toArray());
            }

            public void widgetDefaultSelected(SelectionEvent arg0) {
            }
        });
        removeAll = new Button(buttons, SWT.PUSH);
        removeAll.setEnabled(false);
        removeAll.setText("Remove All");
        removeAll.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent arg0) {
                chosenItems.clear();
                list.setElements(chosenItems.toArray());
            }

            public void widgetDefaultSelected(SelectionEvent arg0) {
            }
        });

        // Selected list
        list = new FilteredList(newResult, SWT.BORDER | SWT.V_SCROLL | SWT.MULTI | SWT.SEARCH, new UserLabelProvider(me), true,
                        false, true);
        list.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent arg0) {
                updateOkState();
            }

            public void widgetDefaultSelected(SelectionEvent arg0) {
            }
        });
        chosenItems = getInitialElementSelections();
        if (chosenItems != null && chosenItems.size() > 0) {
            list.setElements(chosenItems.toArray());
        } else {
            list.setElements(new User[0]);
        }
        gd = new GridData(SWT.RIGHT, SWT.FILL, true, true);
        gd.widthHint = 200;
        gd.heightHint = 400;
        list.setLayoutData(gd);

        return newResult;
    }

    @Override
    protected boolean validateCurrentSelection() {
        // ignore
        boolean ok = super.validateCurrentSelection();

        // TODO a bit hacky, only way found so far to be informed when list is
        // finished loading
        if (ok && list != null) {
            add.setEnabled(true);
            remove.setEnabled(true);
            removeAll.setEnabled(true);
        }
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
        if (anyone != null) {
            if (Arrays.asList(result).contains(anyone)) {
                setSelection(new Object[0]);
            }
        }
        // ignore
        super.okPressed();
    }

    @SuppressWarnings("unchecked")
    public List getChosenItems() {
        return chosenItems;
    }

    private void setTitles() {
        setMessage(multipleSelection ? "Choose people ..." : "Choose person");
        setTitle(multipleSelection ? "Choose People" : "Choose Person");
    }
}
