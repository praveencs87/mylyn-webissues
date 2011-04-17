/**
 * 
 */
package net.sf.webissues.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sf.webissues.api.Attribute;
import net.sf.webissues.api.IssueType;

import org.eclipse.mylyn.internal.provisional.commons.ui.CommonImages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

@SuppressWarnings("restriction")
public class ColumnRow {

    private Combo attributeCombo;
    private Attribute selectedAttribute;
    private GridData comboGridData;
    private GridData buttonGridData;
    private List<ColumnRow> rows;
    private Button downButton;
    private Button upButton;
    private IssueType type;
    private List<Attribute> attributes = new ArrayList<Attribute>();
    private Button removeButton;

    public ColumnRow(IssueType type, Attribute selectedAttribute, Composite parent, final List<ColumnRow> rows,
                     List<Attribute> attributes) {
        this.rows = rows;
        this.type = type;
        this.selectedAttribute = selectedAttribute;

        // Attribute type
        attributeCombo = new Combo(parent, SWT.READ_ONLY);
        List<Attribute> attrList = attributes == null ? new ArrayList<Attribute>(type.values()) : attributes;
        for (Attribute attribute : attrList) {
            if (attribute.isForViewFilter()) {
                addAttr(selectedAttribute, attribute);
            }
        }
        comboGridData = new GridData(GridData.FILL_HORIZONTAL);
        comboGridData.horizontalSpan = 1;
        comboGridData.verticalAlignment = SWT.TOP;
        attributeCombo.setLayoutData(comboGridData);
        attributeCombo.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                attributeSelected();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                attributeSelected();
            }
        });

        createMiddleColumns(parent);

        // Up
        upButton = new Button(parent, SWT.PUSH);
        upButton.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        upButton.setEnabled(rows.size() > 0);
        buttonGridData = new GridData(SWT.NONE, SWT.NONE, false, false);
        buttonGridData.widthHint = 28;
        buttonGridData.horizontalSpan = 1;
        buttonGridData.verticalAlignment = SWT.TOP;
        upButton.setImage(CommonImages.getImage(WebIssuesImages.UP));
        upButton.setToolTipText("Move up");
        upButton.setLayoutData(buttonGridData);
        upButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
            }
        });

        // Down
        downButton = new Button(parent, SWT.PUSH);
        downButton.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        buttonGridData = new GridData(SWT.NONE, SWT.NONE, false, false);
        buttonGridData.widthHint = 28;
        buttonGridData.horizontalSpan = 1;
        buttonGridData.verticalAlignment = SWT.TOP;
        downButton.setEnabled(rows.size() > 0);
        downButton.setImage(CommonImages.getImage(WebIssuesImages.DOWN));
        downButton.setToolTipText("Move down");
        downButton.setLayoutData(buttonGridData);
        downButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
            }
        });

        // Delete
        removeButton = new Button(parent, SWT.PUSH);
        removeButton.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        buttonGridData = new GridData(SWT.NONE, SWT.NONE, false, false);
        buttonGridData.widthHint = 28;
        buttonGridData.horizontalSpan = 1;
        buttonGridData.verticalAlignment = SWT.TOP;
        removeButton.setImage(CommonImages.getImage(WebIssuesImages.DELETE));
        removeButton.setToolTipText("Remove condition");
        removeButton.setLayoutData(buttonGridData);
        removeButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                removeRow();
            }
        });

        // Default select
        int idx = selectedAttribute == null ? -1 : Arrays.asList(attributeCombo.getItems()).indexOf(selectedAttribute.getName());
        if (idx != -1) {
            attributeCombo.select(idx);
            attributeSelected();
        } else {
            if (attributeCombo.getItemCount() > 0) {
                attributeCombo.select(0);
                attributeSelected();
            }
        }
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public void setAvailable() {
        boolean fixedPosition = selectedAttribute != null && selectedAttribute.isFixedPosition();
        attributeCombo.setEnabled(!fixedPosition);
        removeButton.setEnabled(!fixedPosition);
        Attribute attrAbove = rows.indexOf(this) > 0 ? rows.get(rows.indexOf(this) - 1).getSelectedAttribute() : null;
        boolean aboveIsFixed = attrAbove != null && attrAbove.isFixedPosition();
        upButton.setEnabled(rows.indexOf(this) > 0 && !fixedPosition && !aboveIsFixed);
        downButton.setEnabled(rows.indexOf(this) < rows.size() - 1 && !fixedPosition);

        List<Attribute> fullList = new ArrayList<Attribute>();
        Attribute selectedAttribute = getSelectedAttribute();
        if (selectedAttribute != null) {
            fullList.add(selectedAttribute);
        }

        // Remove any attributes that are selected in other rows
        for (ColumnRow row : rows) {
            if (row != this) {
                Attribute selAttr = row.getSelectedAttribute();
                if (selAttr != null) {
                    fullList.add(selAttr);
                    int idx = attributes.indexOf(selAttr);
                    if (idx != -1) {
                        attributes.remove(idx);
                        attributeCombo.remove(idx);
                    }
                }
            }
        }

        // Add any attributes that aren't selected in any other row
        for (Attribute attr : type.values()) {
            if (attr.isForViewFilter() && !fullList.contains(attr) && !attributes.contains(attr)) {
                addAttr(selectedAttribute, attr);
            }
        }
    }

    public final Attribute getSelectedAttribute() {
        return selectedAttribute;
    }

    protected final void removeRow() {
        removeButton.dispose();
        attributeCombo.dispose();
        downButton.dispose();
        upButton.dispose();
        rows.remove(this);
        redoLayout();
    }

    protected void redoLayout() {
        onDoLayout();
    }

    protected void onDoLayout() {
    }

    protected void configureForTop() {
        comboGridData.verticalAlignment = SWT.TOP;
        buttonGridData.verticalAlignment = SWT.TOP;
    }

    protected void createMiddleColumns(Composite parent) {
    }

    protected void attributeSelected() {
        selectedAttribute = doGetSelectedAttribute();
        redoLayout();
    }

    protected final void addAttr(Attribute selectedAttribute, Attribute attribute) {
        attributeCombo.add(attribute.getName());
        attributes.add(attribute);
        if (attribute.equals(selectedAttribute)) {
            attributeCombo.select(attributeCombo.getItemCount() - 1);
        }
    }

    private Attribute doGetSelectedAttribute() {
        int selectionIndex = attributeCombo.getSelectionIndex();
        return selectionIndex == -1 ? null : type.getByName(attributeCombo.getItem(selectionIndex));
    }

}