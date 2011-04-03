package net.sf.webissues.ui.editor;

import java.io.IOException;
import java.util.Arrays;

import net.sf.webissues.api.Environment;
import net.sf.webissues.api.NamedEntity;
import net.sf.webissues.api.ProtocolException;
import net.sf.webissues.core.WebIssuesClient;
import net.sf.webissues.core.WebIssuesCorePlugin;
import net.sf.webissues.ui.AbstractSelector;

import org.apache.commons.httpclient.HttpException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.mylyn.internal.tasks.ui.editors.EditorUtil;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskDataModel;
import org.eclipse.mylyn.tasks.ui.editors.AbstractAttributeEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

@SuppressWarnings("restriction")
public abstract class AbstractSelectorAttributeEditor<T extends NamedEntity> extends AbstractAttributeEditor {

    private final TaskDataModel manager;

    private AbstractSelector<T> userPicker;

    public AbstractSelectorAttributeEditor(TaskDataModel manager, final TaskAttribute taskAttribute) {
        super(manager, taskAttribute);
        this.manager = manager;
    }

    protected WebIssuesClient getClient() throws HttpException, ProtocolException, IOException {
        return WebIssuesCorePlugin.getDefault().getConnector().getClientManager().getClient(manager.getTaskRepository(),
            new NullProgressMonitor());
    }

    @Override
    public void createControl(Composite parent, FormToolkit toolkit) {
        if (!isReadOnly()) {
            try {
                WebIssuesClient client = getClient();
                Environment attributes = client.getEnvironment();
                userPicker = createPicker(manager, parent, attributes);
                toolkit.adapt(userPicker, false, false);
                setControl(userPicker);
            } catch (Exception capie) {
                capie.printStackTrace();
                createTextLabel(parent, toolkit);
            }
        } else {
            createTextLabel(parent, toolkit);
        }

    }

    private void createTextLabel(Composite parent, FormToolkit toolkit) {
        Text text = new Text(parent, SWT.FLAT | SWT.READ_ONLY);
        text.setFont(EditorUtil.TEXT_FONT);
        toolkit.adapt(text, false, false);
        text.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.FALSE);
        text.setText(getValue());
        setControl(text);
    }

    protected abstract AbstractSelector<T> createPicker(TaskDataModel manager, Composite parent, Environment attributes);

    public String getValue() {
        return getAttributeMapper().getValue(getTaskAttribute());
    }

    public String getValueLabel() {
        return getAttributeMapper().getValueLabel(getTaskAttribute());
    }

    public void setValue(String value) {
        getAttributeMapper().setValue(getTaskAttribute(), value);
        attributeChanged();
    }

    public void setValues(String[] values) {
        getAttributeMapper().setValues(getTaskAttribute(), Arrays.asList(values));
        attributeChanged();
    }
}