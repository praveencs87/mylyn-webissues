package net.sf.webissues.ui;

import net.sf.webissues.core.WebIssuesCorePlugin;

import org.eclipse.mylyn.internal.tasks.ui.views.TaskRepositoriesView;
import org.eclipse.mylyn.tasks.ui.TaskRepositoryLocationUiFactory;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class WebIssuesUiPlugin extends AbstractUIPlugin {

    public static final String ID_PLUGIN = "net.sf.webissues.uiW";

    private static WebIssuesUiPlugin plugin;

    private IPartListener2 partListener;

    public WebIssuesUiPlugin() {
        plugin = this;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        WebIssuesCorePlugin.getDefault().getConnector().setTaskRepositoryLocationFactory(new TaskRepositoryLocationUiFactory());
        TasksUi.getRepositoryManager().addListener(WebIssuesCorePlugin.getDefault().getConnector().getClientManager());

        partListener = new IPartListener2() {

            @Override
            public void partVisible(IWorkbenchPartReference partRef) {
                // TODO Auto-generated method stub

            }

            @Override
            public void partOpened(IWorkbenchPartReference partRef) {
                configureViewReference(partRef);
            }

            @Override
            public void partInputChanged(IWorkbenchPartReference partRef) {
                // TODO Auto-generated method stub

            }

            @Override
            public void partHidden(IWorkbenchPartReference partRef) {
                // TODO Auto-generated method stub

            }

            @Override
            public void partDeactivated(IWorkbenchPartReference partRef) {
                // TODO Auto-generated method stub

            }

            @Override
            public void partClosed(IWorkbenchPartReference partRef) {
                // TODO Auto-generated method stub

            }

            @Override
            public void partBroughtToTop(IWorkbenchPartReference partRef) {
                // TODO Auto-generated method stub

            }

            @Override
            public void partActivated(IWorkbenchPartReference partRef) {
                // TODO Auto-generated method stub

            }
        };

        // Listen for new pages being created
        getWorkbench().getActiveWorkbenchWindow().addPageListener(new IPageListener() {

            @Override
            public void pageOpened(IWorkbenchPage page) {
                newPage(page);
            }

            @Override
            public void pageClosed(IWorkbenchPage page) {
                // TODO Auto-generated method stub

            }

            @Override
            public void pageActivated(IWorkbenchPage page) {
                removePage(page);

            }
        });
        
        // Add currently active pages
        for(IWorkbenchPage page : getWorkbench().getActiveWorkbenchWindow().getPages()) {
            newPage(page);
        }
    }

    private void removePage(IWorkbenchPage page) {
        page.addPartListener(partListener);
    }

    private void newPage(IWorkbenchPage page) {
        page.addPartListener(partListener);
        
        // Set up any task views already visible
        for(IViewReference view: page.getViewReferences()) {
            configureViewReference(view);
                
        }
    }

    private void configureViewReference(IWorkbenchPartReference view) {
        if(view instanceof IViewReference) {
        IViewPart viewPart = ((IViewReference)view).getView(false);
        if(viewPart != null && viewPart instanceof TaskRepositoriesView) {
            TaskRepositoriesView tpc = (TaskRepositoriesView)viewPart;
            configureView(tpc);
        }
        }
    }

    private void configureView(TaskRepositoriesView tpc) {
        
        Menu menu = tpc.getViewer().getControl().getMenu();
        menu.addMenuListener(new MenuListener() {
            
            @Override
            public void menuShown(MenuEvent e) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void menuHidden(MenuEvent e) {
                // TODO Auto-generated method stub
                
            }
        });

//                MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
//                menuMgr.setRemoveAllWhenShown(true);
//                menuMgr.addMenuListener(new IMenuListener() {
//                    public void menuAboutToShow(IMenuManager manager) {
//                        TaskRepositoriesView.this.fillContextMenu(manager);
//                    }
//                });
//                Menu menu = menuMgr.createContextMenu(viewer.getControl());
//                viewer.getControl().setMenu(menu);
//                getSite().registerContextMenu(menuMgr, viewer);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        TasksUi.getRepositoryManager().removeListener(WebIssuesCorePlugin.getDefault().getConnector().getClientManager());

        plugin = null;
        super.stop(context);
    }

    public static WebIssuesUiPlugin getDefault() {
        return plugin;
    }
}
