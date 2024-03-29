package net.sf.webissues.ui.editor;

import java.util.ArrayList;
import java.util.Set;

import net.sf.webissues.core.WebIssuesAttribute;
import net.sf.webissues.core.WebIssuesClient;
import net.sf.webissues.core.WebIssuesCorePlugin;
import net.sf.webissues.core.WebIssuesTaskDataHandler;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.mylyn.internal.tasks.ui.editors.TaskEditorActionPart;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskDataModel;
import org.eclipse.mylyn.tasks.core.data.TaskDataModelEvent;
import org.eclipse.mylyn.tasks.core.data.TaskDataModelListener;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPage;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPart;
import org.eclipse.mylyn.tasks.ui.editors.AttributeEditorFactory;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditor;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditorInput;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditorPartDescriptor;
import org.webissues.api.Folder;
import org.webissues.api.IEnvironment;
import org.webissues.api.IssueType;
import org.webissues.api.Project;

public class WebIssuesTaskEditorPage extends AbstractTaskEditorPage {

    public static final String ID_PART_HISTORY = "net.sf.webissues.ui.editor.history"; //$NON-NLS-1$

    public WebIssuesTaskEditorPage(TaskEditor editor) {
        super(editor, WebIssuesCorePlugin.CONNECTOR_KIND);
    }

    @Override
    public void selectionChanged(SelectionChangedEvent event) {
    }

    @Override
    public void refreshFormContent() {
        if (getManagedForm().getForm().isDisposed()) {
            return;
        }
        try {
            super.refreshFormContent();
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    @Override
    protected AttributeEditorFactory createAttributeEditorFactory() {
        return new WebIssuesAttributeEditorFactory(getModel(), getTaskRepository());
    }

    @Override
    protected void createParts() {
        super.createParts();
    }

    protected TaskDataModel createModel(TaskEditorInput input) throws CoreException {
        final TaskDataModel model = super.createModel(input);
        TaskDataModelListener listener = new TaskDataModelListener() {
            @Override
            public void attributeChanged(TaskDataModelEvent evt) {
                if (evt.getTaskAttribute().getId().equals(WebIssuesAttribute.PROJECT.getTaskKey())) {
                    try {
                        WebIssuesCorePlugin.getDefault().getPluginPreferences()
                                        .setValue("lastProjectUsedForNewTask", Integer.parseInt(evt.getTaskAttribute().getValue()));
                        boolean existingTask = model.getTask().getTaskKey() != null;
                        WebIssuesClient client = WebIssuesCorePlugin.getDefault().getConnector().getClientManager()
                                        .getClient(getTaskRepository(), new NullProgressMonitor());
                        TaskAttribute attribute = model.getTaskData().getRoot()
                                        .getAttribute(WebIssuesAttribute.FOLDER.getTaskKey());
                        IssueType currentType = null;
                        IEnvironment environment = client.getEnvironment();
                        if (existingTask) {
                            currentType = environment
                                            .getProjects()
                                            .getFolder(
                                                Integer.parseInt(model.getTaskData().getRoot()
                                                                .getAttribute(WebIssuesAttribute.FOLDER.getTaskKey()).getValue()))
                                            .getType();
                        }
                        WebIssuesTaskDataHandler.rebuildFolders(environment, evt.getTaskAttribute(), model.getTaskData(),
                            attribute, currentType);
                        model.attributeChanged(attribute);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        model.addModelListener(listener);
        return model;
    }

    @Override
    protected Set<TaskEditorPartDescriptor> createPartDescriptors() {
        Set<TaskEditorPartDescriptor> descriptors = super.createPartDescriptors();

        // Remove the default parts
        removePart(descriptors, ID_PART_PEOPLE);
        removePart(descriptors, ID_PART_ACTIONS);

        // Add the replacement parts
        descriptors.add(new TaskEditorPartDescriptor(ID_PART_PEOPLE) {
            @Override
            public AbstractTaskEditorPart createPart() {
                return new WebIssuesPeoplePart();
            }
        }.setPath(PATH_PEOPLE));
        descriptors.add(new TaskEditorPartDescriptor(ID_PART_ACTIONS) {
            @Override
            public AbstractTaskEditorPart createPart() {
                return new WebIssuesActionPart();
            }
        }.setPath(PATH_ACTIONS));

        // Add new parts
        descriptors.add(new TaskEditorPartDescriptor(ID_PART_HISTORY) {
            @Override
            public AbstractTaskEditorPart createPart() {
                return new WebIssuesHistoryPart();
            }
        }.setPath(PATH_ATTACHMENTS));
        return descriptors;
    }

    private void removePart(Set<TaskEditorPartDescriptor> descriptors, String id) {
        for (TaskEditorPartDescriptor taskEditorPartDescriptor : new ArrayList<TaskEditorPartDescriptor>(descriptors)) {
            if (taskEditorPartDescriptor.getId().equals(id)) {
                descriptors.remove(taskEditorPartDescriptor);
                break;
            }
        }
    }
}
