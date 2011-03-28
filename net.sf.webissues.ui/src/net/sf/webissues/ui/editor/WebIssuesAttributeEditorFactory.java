package net.sf.webissues.ui.editor;

import org.eclipse.core.runtime.Assert;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskDataModel;
import org.eclipse.mylyn.tasks.ui.editors.AbstractAttributeEditor;
import org.eclipse.mylyn.tasks.ui.editors.AttributeEditorFactory;

public class WebIssuesAttributeEditorFactory extends AttributeEditorFactory {

    private final TaskDataModel model;

    public WebIssuesAttributeEditorFactory(TaskDataModel model, TaskRepository taskRepository) {
        super(model, taskRepository);
        this.model = model;
    }

    @Override
    public AbstractAttributeEditor createEditor(String type, TaskAttribute taskAttribute) {
        Assert.isNotNull(type);
        if (taskAttribute.getMetaData().getType().equals(TaskAttribute.TYPE_INTEGER)) {
            return new WebIssuesNumericAttributeEditor(model, taskAttribute);
        } else if (TaskAttribute.TYPE_PERSON.equals(type)) {
            return new WebIssuesPersonAttributeEditor(model, taskAttribute);
        }  else {
            return super.createEditor(type, taskAttribute);
        }
    }

}
