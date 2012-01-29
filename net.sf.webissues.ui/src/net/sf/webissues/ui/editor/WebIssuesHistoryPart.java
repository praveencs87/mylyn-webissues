package net.sf.webissues.ui.editor;

import java.util.ArrayList;
import java.util.List;

import net.sf.webissues.core.WebIssuesAttributeMapper;
import net.sf.webissues.core.WebIssuesChange;
import net.sf.webissues.core.WebIssuesChangeMapper;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.mylyn.internal.tasks.ui.editors.EditorUtil;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

@SuppressWarnings("restriction")
public class WebIssuesHistoryPart extends AbstractTaskEditorPart {

    private final String[] historyColumns = { "Type", "User", "Attribute", "Old Value", "New Value", "Date" };
    private final int[] attachmentsColumnWidths = { 100, 120, 110, 160, 160, 180 };
    private List<TaskAttribute> changes;
    private boolean hasIncoming;
    private Composite historyComposite;

    public WebIssuesHistoryPart() {
        setPartName("History");
    }

    private void createHistoryTable(FormToolkit toolkit, final Composite attachmentsComposite) {
        Table historyTable = toolkit.createTable(attachmentsComposite, SWT.MULTI | SWT.FULL_SELECTION);
        historyTable.setLinesVisible(true);
        historyTable.setHeaderVisible(true);
        historyTable.setLayout(new GridLayout());
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).hint(500, SWT.DEFAULT).applyTo(historyTable);
        historyTable.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);

        for (int i = 0; i < historyColumns.length; i++) {
            TableColumn column = new TableColumn(historyTable, SWT.LEFT, i);
            column.setText(historyColumns[i]);
            column.setWidth(attachmentsColumnWidths[i]);
        }

        TableViewer historyViewer = new TableViewer(historyTable);
        historyViewer.setUseHashlookup(true);
        historyViewer.setColumnProperties(historyColumns);

        List<WebIssuesChange> historyList = new ArrayList<WebIssuesChange>(changes.size());
        for (TaskAttribute attribute : changes) {
            WebIssuesChange taskAttachment = new WebIssuesChange(getModel().getTaskRepository(), getModel().getTask(), attribute);
            ((WebIssuesAttributeMapper) getTaskData().getAttributeMapper()).updateChange(taskAttachment, attribute);
            historyList.add(taskAttachment);
        }
        historyViewer.setContentProvider(new ArrayContentProvider());
        historyViewer.setLabelProvider(new HistoryTableLabelProvider(getModel(), getTaskEditorPage().getAttributeEditorToolkit()));
        historyViewer.addSelectionChangedListener(getTaskEditorPage());
        historyViewer.setInput(historyList.toArray());
    }

    @Override
    public void createControl(Composite parent, final FormToolkit toolkit) {
        initialize();

        final Section section = createSection(parent, toolkit, hasIncoming);
        section.setText(getPartName() + " (" + changes.size() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        if (hasIncoming) {
            expandSection(toolkit, section);
        } else {
            section.addExpansionListener(new ExpansionAdapter() {
                @Override
                public void expansionStateChanged(ExpansionEvent event) {
                    if (historyComposite == null) {
                        expandSection(toolkit, section);
                        getTaskEditorPage().reflow();
                    }
                }
            });
        }
        setSection(toolkit, section);
    }

    private void expandSection(FormToolkit toolkit, Section section) {
        historyComposite = toolkit.createComposite(section);
        historyComposite.setLayout(EditorUtil.createSectionClientLayout());
        historyComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

        getTaskEditorPage().registerDefaultDropListener(section);

        if (changes.size() > 0) {
            createHistoryTable(toolkit, historyComposite);
        } else {
            Label label = toolkit.createLabel(historyComposite, "No History");
            getTaskEditorPage().registerDefaultDropListener(label);
        }

        toolkit.paintBordersFor(historyComposite);
        section.setClient(historyComposite);
    }

    private void initialize() {
        changes = getTaskData().getAttributeMapper().getAttributesByType(getTaskData(), WebIssuesChangeMapper.TYPE_CHANGE);
        for (TaskAttribute attachmentAttribute : changes) {
            if (getModel().hasIncomingChanges(attachmentAttribute)) {
                hasIncoming = true;
                break;
            }
        }
    }

}
