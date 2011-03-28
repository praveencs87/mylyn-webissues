package net.sf.webissues.ui;

import java.util.ArrayList;
import java.util.List;

import net.sf.webissues.core.WebIssuesCorePlugin;
import net.sf.webissues.ui.wizard.WebIssuesQueryPage;
import net.sf.webissues.ui.wizard.WebIssuesRepositorySettingsPage;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITaskMapping;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.AbstractRepositoryConnectorUi;
import org.eclipse.mylyn.tasks.ui.LegendElement;
import org.eclipse.mylyn.tasks.ui.wizards.ITaskRepositoryPage;
import org.eclipse.mylyn.tasks.ui.wizards.ITaskSearchPage;
import org.eclipse.mylyn.tasks.ui.wizards.NewTaskWizard;
import org.eclipse.mylyn.tasks.ui.wizards.RepositoryQueryWizard;

public class WebIssuesConnectorUi extends AbstractRepositoryConnectorUi {

    @SuppressWarnings("restriction")
    public WebIssuesConnectorUi() {
        org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin.getDefault().addSearchHandler(new WebIssuesSearchHandler());
    }

    @Override
    public String getTaskKindLabel(ITask repositoryTask) {
        return "Issue";
    }

    @Override
    public ITaskRepositoryPage getSettingsPage(TaskRepository taskRepository) {
        return new WebIssuesRepositorySettingsPage(taskRepository);
    }

    @Override
    public ITaskSearchPage getSearchPage(TaskRepository repository, IStructuredSelection selection) {
        return new WebIssuesQueryPage(repository);
    }

    @Override
    public boolean hasSearchPage() {
        return true;
    }

    @Override
    public IWizard getNewTaskWizard(TaskRepository repository, ITaskMapping selection) {
        return new NewTaskWizard(repository, selection);
    }

    @Override
    public IWizard getQueryWizard(TaskRepository repository, IRepositoryQuery query) {
        RepositoryQueryWizard wizard = new RepositoryQueryWizard(repository);
        wizard.addPage(new WebIssuesQueryPage(repository, query));
        return wizard;
    }

    @Override
    public String getConnectorKind() {
        return WebIssuesCorePlugin.CONNECTOR_KIND;
    }

    @Override
    public ImageDescriptor getTaskKindOverlay(ITask task) {
        String kind = task.getTaskKind();
        if (kind.equalsIgnoreCase("features")) {
            return WebIssuesImages.OVERLAY_FEATURES;
        } else if (kind.equalsIgnoreCase("bugs")) {
            return WebIssuesImages.OVERLAY_BUGS;
        }
        return super.getTaskKindOverlay(task);
    }

    @Override
    public List<LegendElement> getLegendElements() {
        List<LegendElement> legendItems = new ArrayList<LegendElement>();
        legendItems.add(LegendElement.createTask("Bugs", WebIssuesImages.OVERLAY_BUGS));
        legendItems.add(LegendElement.createTask("Features", WebIssuesImages.OVERLAY_FEATURES));
        return legendItems;
    }

}
