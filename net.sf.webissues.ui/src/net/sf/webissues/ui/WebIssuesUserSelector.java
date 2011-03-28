package net.sf.webissues.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.sf.webissues.api.Environment;
import net.sf.webissues.api.User;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

public class WebIssuesUserSelector extends AbstractSelector<User> {

    private Collection<User> users;
    private boolean includeAnyone;

    public WebIssuesUserSelector(Collection<User> users, Environment environment, Composite parent, int style, String initialText,
                                 boolean allowMultiple, String fieldName, boolean includeAnyone) {
        super(environment, parent, style, initialText, allowMultiple);
        this.users = users;
        this.includeAnyone = includeAnyone;
    }

    @Override
    public User getItemForText(String text) {
        return getEnvironment().getUsers().getUserByName(text);
    }

    @Override
    protected void showPicker(Shell shell) {
        Object[] newComponents = null;
        WebIssuesUserSelectorDialog dialog = new WebIssuesUserSelectorDialog(getEnvironment().getOwnerUser(), shell, users,
                        includeAnyone);
        List<User> items = new ArrayList<User>(getItems());
        User[] initialSel = items.toArray(new User[0]);
        dialog.setInitialSelections(initialSel);
        dialog.setMultipleSelection(isAllowMultiple());
        int dialogResponse = dialog.open();
        if (dialog.getResult() != null) {
            newComponents = dialog.getResult();
        } else {
            newComponents = null;
        }
        componentSelected(dialogResponse == Window.CANCEL, newComponents);

    }

}
