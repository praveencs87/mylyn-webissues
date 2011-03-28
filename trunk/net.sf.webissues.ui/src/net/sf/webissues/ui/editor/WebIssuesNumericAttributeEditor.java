package net.sf.webissues.ui.editor;

import org.eclipse.mylyn.internal.tasks.ui.editors.TextAttributeEditor;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskDataModel;

@SuppressWarnings("restriction")
public class WebIssuesNumericAttributeEditor extends TextAttributeEditor {

    public WebIssuesNumericAttributeEditor(TaskDataModel manager, TaskAttribute taskAttribute) {
        super(manager, taskAttribute);
    }

}
