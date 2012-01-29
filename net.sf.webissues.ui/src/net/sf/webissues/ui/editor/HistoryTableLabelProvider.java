package net.sf.webissues.ui.editor;

import java.text.DateFormat;

import net.sf.webissues.core.WebIssuesChange;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.mylyn.tasks.core.data.TaskDataModel;
import org.eclipse.mylyn.tasks.ui.editors.AttributeEditorToolkit;
import org.eclipse.swt.graphics.Color;

public class HistoryTableLabelProvider extends ColumnLabelProvider {

    private final TaskDataModel model;

    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);

    private final AttributeEditorToolkit attributeEditorToolkit;

    public HistoryTableLabelProvider(TaskDataModel model, AttributeEditorToolkit attributeEditorToolkit) {
        this.model = model;
        this.attributeEditorToolkit = attributeEditorToolkit;
    }

    public String getColumnText(Object element, int columnIndex) {
        WebIssuesChange change = (WebIssuesChange) element;
        switch (columnIndex) {
            case 0:
                return change.getType() == null ? "" : change.getType().getLabel();
            case 1:
                return (change.getUser() != null) ? change.getUser().toString() : ""; //$NON-NLS-1$
            case 2:
                return change.getAttributeName();
            case 3:
                return change.getOldValue();
            case 4:
                return change.getNewValue();
            case 5:
                return dateFormat.format(change.getDate().getTime()); //$NON-NLS-1$
        }
        return "unrecognized column"; //$NON-NLS-1$
    }

    @Override
    public void addListener(ILabelProviderListener listener) {
    }

    @Override
    public void dispose() {
    }

    @Override
    public boolean isLabelProperty(Object element, String property) {
        return false;
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {
    }

    @Override
    public void update(ViewerCell cell) {
        Object element = cell.getElement();
        cell.setText(getColumnText(element, cell.getColumnIndex()));
        cell.setBackground(getBackground(element));
        cell.setForeground(getForeground(element));
        cell.setFont(getFont(element));
    }

    @Override
    public Color getBackground(Object element) {
        WebIssuesChange change = (WebIssuesChange) element;
        if (model.hasIncomingChanges(change.getTaskAttribute())) {
            return attributeEditorToolkit.getColorIncoming();
        } else {
            return null;
        }
    }
}