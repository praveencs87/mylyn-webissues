package net.sf.webissues.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.sf.webissues.api.Attribute;
import net.sf.webissues.api.IssueType;
import net.sf.webissues.api.View;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class ColumnsEditor extends ScrolledComposite {

    private Composite attributesGrid;
    List<ColumnRow> rows = new ArrayList<ColumnRow>();
    private View view;
    private IssueType type;
    private Button addColumnButton;

    public ColumnsEditor(Composite control) {
        super(control, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        setExpandHorizontal(true);
        setExpandVertical(true);
        Point minSize = control.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        setMinWidth(minSize.x);
        setMinHeight(minSize.y);
        setAlwaysShowScrollBars(false);

        attributesGrid = new Composite(this, SWT.NONE);
        attributesGrid.setBackground(getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        GridLayout layout = new GridLayout();
        layout.numColumns = 4;
        attributesGrid.setLayout(layout);
        setContent(attributesGrid);
    }

    public void setType(IssueType type) {
        this.type = type;
        clearRows();
    }

    public void setView(View view) {
        this.view = view;
        setType(view.getType());
    }

    public void addRow(Attribute attribute) {
        if (addColumnButton != null) {
            addColumnButton.dispose();
        }
        List<Attribute> attrs = new ArrayList<Attribute>(type.getViewFilterAttributes());
        for (ColumnRow row : rows) {
            Attribute selectedAttribute = row.getSelectedAttribute();
            attrs.remove(selectedAttribute);
        }
        ColumnRow row = new ColumnRow(type, attribute, attributesGrid, rows, attrs) {
            @Override
            protected void onDoLayout() {
                ColumnsEditor.this.setAvailable();
                recomputeBounds();

            }
        };
        rows.add(row);
        addAddButton();
        recomputeBounds();
        setAvailable();
    }

    private void recomputeBounds() {
        attributesGrid.layout();
        Point minSize = attributesGrid.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        setMinWidth(minSize.x);
        setMinHeight(minSize.y);
    }

    public void scrollToCenter() {
        int origin = Math.max(0, addColumnButton.getBounds().y - (getClientArea().height / 2));
        System.out.println(">>" + origin);
        setOrigin(0, origin);
    }

    private void addAddButton() {
        addColumnButton = new Button(attributesGrid, SWT.PUSH);
        addColumnButton.setText("Add");
        addColumnButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                Collection<Attribute> unused = getUnusedAttributes();
                if (unused.size() > 0) {
                    addRow(unused.iterator().next());
                    scrollToCenter();
                }
            }
        });
        GridData gd = new GridData();
        gd.horizontalSpan = 4;
        addColumnButton.setLayoutData(gd);
        layout();
    }

    public void setAvailable() {
        if (addColumnButton != null && !addColumnButton.isDisposed()) {
            addColumnButton.setEnabled(getUnusedAttributes().size() > 0);
        }
        for (ColumnRow crow : rows) {
            crow.setAvailable();
        }
    }

    public void clearRows() {
        rows.clear();
        for (Control child : attributesGrid.getChildren()) {
            child.dispose();
        }
        addAddButton();
    }

    public Collection<Attribute> getUsedAttributes() {
        List<Attribute> a = new ArrayList<Attribute>();
        for (ColumnRow row : rows) {
            Attribute attr = row.getSelectedAttribute();
            if (attr != null) {
                a.add(attr);
            }
        }
        return a;
    }

    public Collection<Attribute> getUnusedAttributes() {
        List<Attribute> usedAttributes = new ArrayList<Attribute>(getUsedAttributes());
        List<Attribute> a = new ArrayList<Attribute>();
        for (Attribute attr : type.values()) {
            if (attr.isForViewFilter() && !usedAttributes.contains(attr)) {
                a.add(attr);
            }
        }
        return a;
    }
}