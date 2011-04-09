package net.sf.webissues.ui.editor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import net.sf.webissues.api.Environment;
import net.sf.webissues.api.Folder;
import net.sf.webissues.api.User;
import net.sf.webissues.api.Util;
import net.sf.webissues.core.WebIssuesClient;
import net.sf.webissues.core.WebIssuesCorePlugin;
import net.sf.webissues.core.WebIssuesTaskDataHandler;
import net.sf.webissues.ui.AbstractSelector;
import net.sf.webissues.ui.WebIssuesUserSelector;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.mylyn.internal.tasks.ui.editors.EditorUtil;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskDataModel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

@SuppressWarnings("restriction")
public class WebIssuesPersonAttributeEditor extends AbstractSelectorAttributeEditor<User> {
    final static Logger LOG = Logger.getLogger(WebIssuesPersonAttributeEditor.class.getName());

    public WebIssuesPersonAttributeEditor(TaskDataModel manager, TaskAttribute taskAttribute) {
        super(manager, taskAttribute);
        // ignore
    }

    @Override
    protected AbstractSelector<User> createPicker(TaskDataModel manager, Composite parent, Environment attributes) {
        String person = getValue();
        List<User> users = null;
        Environment environment = null;
        try {
            WebIssuesClient client = WebIssuesCorePlugin.getDefault().getConnector().getClientManager().getClient(
                manager.getTaskRepository(), new NullProgressMonitor());
            environment = client.getEnvironment();
            Folder folder = WebIssuesTaskDataHandler.getFolder(manager.getTaskData(), client);
            TaskAttribute attr = getTaskAttribute();
            if (attr.getMetaData().getValue("membersOnly").equals("true")) {
                users = new ArrayList<User>(client.getEnvironment().getMembersOf(folder.getProject()));
            } else {
                users = new ArrayList<User>(client.getEnvironment().getUsers().values());
            }
        } catch (Exception e) {
            LOG.warning("Could not get user list.");
            e.printStackTrace();
            users = new ArrayList<User>();
        }
        final WebIssuesUserSelector userPicker = new WebIssuesUserSelector(users, environment, parent, SWT.FLAT, person, false,
                        getTaskAttribute().getId(), true);
        userPicker.setFont(EditorUtil.TEXT_FONT);
        if (!Util.isNullOrBlank(person)) {
            User userByLogin = attributes.getUsers().getUserByName(person);
            if (userByLogin == null) {
                LOG.severe("User with login '" + person + "' does not exist");
            } else {
                userPicker.setItem(userByLogin);
            }
        }
        userPicker.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
        userPicker.addPickerSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Collection<User> components = userPicker.getItems();
                String[] sel = new String[components.size()];
                Iterator<User> it = components.iterator();
                for (int i = 0; i < sel.length; i++) {
                    sel[i] = String.valueOf(it.next().getName());
                }
                setValues(sel);
            }
        });
        return userPicker;
    }
}