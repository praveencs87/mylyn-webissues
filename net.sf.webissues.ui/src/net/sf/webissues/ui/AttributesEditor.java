package net.sf.webissues.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.webissues.api.Condition;
import org.webissues.api.IssueType;
import org.webissues.api.View;

public class AttributesEditor {

    private Composite attributesGrid;
    private ScrolledComposite scroller;
    List<ConditionRow> rows = new ArrayList<ConditionRow>();
    private View view;
    private IssueType type;
    
    public AttributesEditor(Composite control) {
        scroller = new ScrolledComposite(control, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.horizontalSpan = 4;
        gd.heightHint = 200;
        scroller.setLayoutData(gd);
        scroller.setExpandHorizontal(true);
        scroller.setExpandVertical(true);
        Point minSize = control.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        scroller.setMinWidth(minSize.x);
        scroller.setMinHeight(minSize.y);
        scroller.setAlwaysShowScrollBars(false);

        attributesGrid = new Composite(scroller, SWT.NONE);
        attributesGrid.setBackground(scroller.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        GridLayout layout = new GridLayout();
        layout.numColumns = 5;
        attributesGrid.setLayout(layout);
        scroller.setContent(attributesGrid);
    }
    
    public void setType(IssueType type) {
        this.type  = type;
        clearRows();
    }
    
    public void setView(View view) {
        this.view = view;
        setType(view.getType());
    }

    public void addRow(Condition condition) {
        boolean viewSelected = view != null;
        ConditionRow row = new ConditionRow(type, attributesGrid, condition, rows, viewSelected) {
            @Override
            protected void onDoLayout() {
                attributesGrid.layout();
                Point minSize = attributesGrid.computeSize(SWT.DEFAULT, SWT.DEFAULT);
                scroller.setMinWidth(minSize.x);
                scroller.setMinHeight(minSize.y);
            }
        };
        attributesGrid.layout();
        rows.add(row);
    }

    public void clearRows() {
        rows.clear();
        for (Control child : attributesGrid.getChildren()) {
            child.dispose();
        }
    }

    public Collection<Condition> getConditions() {
        List<Condition> c = new ArrayList<Condition>();
        for(ConditionRow row : rows) {
            c.add(row.getCondition());
        }
        return c;
    }
}