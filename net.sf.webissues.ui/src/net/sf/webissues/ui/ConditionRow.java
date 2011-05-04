/**
 * 
 */
package net.sf.webissues.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;


import org.eclipse.mylyn.internal.provisional.commons.ui.CommonImages;
import org.eclipse.mylyn.internal.provisional.commons.ui.DatePicker;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.webissues.api.Attribute;
import org.webissues.api.Condition;
import org.webissues.api.ConditionType;
import org.webissues.api.Folder;
import org.webissues.api.IEnvironment;
import org.webissues.api.IssueType;
import org.webissues.api.Project;
import org.webissues.api.User;
import org.webissues.api.Util;

@SuppressWarnings("restriction")
public class ConditionRow {

    private Composite value;
    private final org.webissues.api.IssueType type;
    private Combo attributeCombo;
    private Combo conditionCombo;
    private GridData comboGridData;
    private GridData valueGridData;
    private GridData buttonGridData;
    private Condition condition;
    private List<ConditionRow> rows;

    public ConditionRow(org.webissues.api.IssueType type, Composite parent, Condition condition, List<ConditionRow> rows, boolean forView) {
        this.rows = rows;
        this.type = type;

        this.condition = condition;

        // Attribute type
        attributeCombo = new Combo(parent, SWT.NONE);
        if (!forView) {
            for (Attribute attribute : type.values()) {
                addAttr(condition, attribute);
            }
        }
        else {
            addAttr(condition, type.get(IssueType.PROJECT_ATTR_ID));
            addAttr(condition, type.get(IssueType.FOLDER_ATTR_ID));
        }
        comboGridData = new GridData(SWT.NONE, SWT.NONE, false, false);
        comboGridData.widthHint = 120;
        comboGridData.horizontalSpan = 1;
        comboGridData.verticalAlignment = SWT.TOP;
        attributeCombo.setLayoutData(comboGridData);
        attributeCombo.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                ConditionRow.this.condition.setAttribute(null);
                ConditionRow.this.condition.setValue(null);
                buildConditions();
                rebuildValue();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                rebuildValue();
            }
        });

        // Condition
        conditionCombo = new Combo(parent, SWT.NONE);
        comboGridData = new GridData(SWT.NONE, SWT.NONE, false, false);
        comboGridData.widthHint = 128;
        comboGridData.horizontalSpan = 1;
        comboGridData.verticalAlignment = SWT.TOP;
        conditionCombo.setLayoutData(comboGridData);
        conditionCombo.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                ConditionRow.this.condition.setType(getSelectedConditionType());
                rebuildValue();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });
        buildConditions();

        // Value
        value = new Composite(parent, SWT.BORDER_DASH);
        value.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        GridLayout gridLayout = new GridLayout(1, true);
        gridLayout.marginHeight = 0;
        value.setLayout(gridLayout);
        valueGridData = new GridData(SWT.FILL, SWT.NONE, true, false);
        valueGridData.verticalAlignment = SWT.TOP;
        valueGridData.horizontalSpan = 2;
        value.setLayoutData(valueGridData);

        // Delete

        final Button removeButton = new Button(parent, SWT.PUSH);
        removeButton.setBackground(value.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
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
                removeRow(removeButton);
            }
        });

        // Default select
        Attribute attrVal = condition.getAttribute();
        int idx = attrVal == null ? -1 : Arrays.asList(attributeCombo.getItems()).indexOf(attrVal.getName());
        if (idx != -1) {
            attributeCombo.select(idx);
            rebuildValue();
        } else {
            if (attributeCombo.getItemCount() > 0) {
                attributeCombo.select(0);
                rebuildValue();
            }
        }
    }

    private void addAttr(Condition condition, Attribute attribute) {
        attributeCombo.add(attribute.getName());
        if (attribute.equals(condition.getAttribute())) {
            attributeCombo.select(attributeCombo.getItemCount() - 1);
        }
    }

    private Attribute getSelectedAttribute() {
        int selectionIndex = attributeCombo.getSelectionIndex();
        return selectionIndex == -1 ? null : type.getByName(attributeCombo.getItem(selectionIndex));
    }

    private void buildConditions() {
        Attribute attr = getSelectedAttribute();
        conditionCombo.removeAll();
        for (ConditionType conditionType : ConditionType.values()) {
            if (attr == null || conditionType.isFor(attr)) {
                conditionCombo.add(conditionType.getLabel());
                if (conditionType.equals(condition.getType())) {
                    conditionCombo.select(conditionCombo.getItemCount() - 1);
                }
            }
        }
        if (conditionCombo.getSelectionIndex() == -1 && conditionCombo.getItemCount() > 0) {
            conditionCombo.select(0);
        }
    }

    void removeRow(final Button removeButton) {
        value.dispose();
        removeButton.dispose();
        attributeCombo.dispose();
        conditionCombo.dispose();
        rows.remove(this);
        redoLayout();
    }

    void rebuildValue() {
        for (Control control : value.getChildren()) {
            control.dispose();
        }
        GridData gd = new GridData(SWT.FILL, SWT.NONE, true, false);
        Attribute attr = getSelectedAttribute();
        if (attr != null) {
            ConditionType conditionType = getSelectedConditionType();
            condition.setAttribute(attr);
            boolean multiple = conditionType.equals(ConditionType.IN);
            if (attr.getId() == org.webissues.api.IssueType.PROJECT_ATTR_ID) {
                addProjects(gd, multiple);
            } else if (attr.getId() == org.webissues.api.IssueType.FOLDER_ATTR_ID) {
                addFolders(gd, multiple);
            } else {
                switch (attr.getAttributeType()) {
                    case ENUM:
                        addEnum(attr, gd, multiple);
                        break;
                    case NUMERIC:
                        addNumeric(attr, gd);
                        break;
                    case TEXT:
                        addText(gd);
                        break;
                    case USER:
                        if (conditionType.equals(ConditionType.IN)) {
                            addUser(gd);
                        } else {
                            addText(gd);
                        }
                        break;
                    case DATETIME:
                        addDate(attr, gd);
                        break;
                }
            }
        }
        redoLayout();
    }

    private ConditionType getSelectedConditionType() {
        int selectionIndex = conditionCombo.getSelectionIndex();
        if (selectionIndex == -1) {
            return null;
        }
        return ConditionType.fromLabel(conditionCombo.getItem(selectionIndex));
    }

    private void redoLayout() {
        if (!value.isDisposed()) {
            value.layout();
        }
        onDoLayout();
    }

    protected void onDoLayout() {
    }

    private void addText(GridData gd) {
        configureForTop();
        final Text text = new Text(value, SWT.BORDER);
        // text.setBackground(value.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        text.setLayoutData(gd);
        text.addFocusListener(new FocusListener() {
            @Override
            public void focusLost(FocusEvent arg0) {
                condition.setValue(text.getText());                
            }
            
            @Override
            public void focusGained(FocusEvent arg0) {
                // TODO Auto-generated method stub
                
            }
        });
        text.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }

            @Override
            public void widgetSelected(SelectionEvent e) {
                condition.setValue(text.getText());
            }
        });
        if (condition.getValue() != null) {
            text.setText(Util.nonNull(condition.getValue()));
        } else {
            text.setText("");
        }
    }

    private void addDate(Attribute attribute, GridData gd) {
        addDate(attribute.isDateOnly(), gd);
    }

    private void addDate(boolean dateOnly, GridData gd) {
        configureForTop();

        comboGridData.verticalAlignment = SWT.CENTER;
        valueGridData.verticalAlignment = SWT.CENTER;
        buttonGridData.verticalAlignment = SWT.CENTER;

        final DatePicker dateFrom = new DatePicker(value, SWT.NONE, "", !dateOnly, 0);
        dateFrom.setBackground(value.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        dateFrom.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
        dateFrom.addPickerSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                writeDate(dateFrom);
            }
        });
        if (condition.getValue() != null) {
            String arg = condition.getValue();
            try {
                Calendar fromDate = Util.parseTimestampInSeconds(arg);
                if (fromDate.getTimeInMillis() != 0) {
                    dateFrom.setDate(fromDate);
                }
            } catch (NumberFormatException nfe) {
                nfe.printStackTrace();
            }
        }
    }

    private void writeDate(final DatePicker dateFrom) {
        String dateFromText = dateFrom.getDate() == null ? "0" : Util.formatTimestampInSeconds(dateFrom.getDate());
        condition.setValue(dateFromText);
    }

    private void addUser(GridData gd) {
        configureForTop();
        IEnvironment environment = type.getTypes().getEnvironment();
        final WebIssuesUserSelector text = new WebIssuesUserSelector(environment.getUsers().values(), environment, value, SWT.NONE,
                        "", true, "", false);
        // text.setBackground(value.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        text.setLayoutData(gd);
        text.addPickerSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }

            @Override
            public void widgetSelected(SelectionEvent e) {
                StringBuilder bui = new StringBuilder();
                for (Object item : text.getItems()) {
                    User user = (User) item;
                    if (bui.length() > 0) {
                        bui.append(":");
                    }
                    bui.append(user.getName());
                }
                condition.setValue(bui.toString());
            }
        });
        if (condition.getValue() != null) {
            List<User> s = new ArrayList<User>();
            for (String arg : condition.getValue().split(":")) {
                User u = text.getItemForText(arg);
                if (u != null) {
                    s.add(u);
                }
            }
            text.setItems(s);
        } else {
            List<User> s = Collections.emptyList();
            text.setItems(s);
        }
    }

    private void addProjects(GridData gd, boolean multiple) {
        if (multiple) {
            final org.eclipse.swt.widgets.List itemList = addList(gd);
            for (Project project : type.getTypes().getEnvironment().getProjects().values()) {
                // Only add projects that contain folder of the selected type
                int ofThisType = 0;
                for (Folder folder : project.values()) {
                    if (folder.getType().equals(type)) {
                        ofThisType++;
                    }
                }
                if (ofThisType > 0) {
                    itemList.add(project.getName());
                }
            }
            configureItemList(itemList);
        } else {
            final org.eclipse.swt.widgets.Combo itemList = addChoice(gd);
            for (Project project : type.getTypes().getEnvironment().getProjects().values()) {
                // Only add projects that contain folder of the selected type
                int ofThisType = 0;
                for (Folder folder : project.values()) {
                    if (folder.getType().equals(type)) {
                        ofThisType++;
                    }
                }
                if (ofThisType > 0) {
                    itemList.add(project.getName());
                }
            }
            configureItemChoice(itemList);
        }
    }

    private void addFolders(GridData gd, boolean multiple) {
        if (multiple) {
            final org.eclipse.swt.widgets.List itemList = addList(gd);
            for (Project project : type.getTypes().getEnvironment().getProjects().values()) {
                // Only add folders that are of this type
                for (Folder folder : project.values()) {
                    if (folder.getType().equals(type)) {
                        itemList.add(folder.getName());
                    }
                }
            }
            configureItemList(itemList);
        } else {
            final org.eclipse.swt.widgets.Combo itemList = addChoice(gd);
            for (Project project : type.getTypes().getEnvironment().getProjects().values()) {
                // Only add folders that are of this type
                for (Folder folder : project.values()) {
                    if (folder.getType().equals(type)) {
                        itemList.add(folder.getName());
                    }
                }
            }
            configureItemChoice(itemList);
        }
    }

    private org.eclipse.swt.widgets.Combo addChoice(GridData gd) {
        configureForTop();
        final org.eclipse.swt.widgets.Combo itemChoice = new org.eclipse.swt.widgets.Combo(value, SWT.BORDER | SWT.MULTI
                        | SWT.V_SCROLL);
        itemChoice.setBackground(value.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        itemChoice.setLayoutData(gd);
        return itemChoice;
    }

    private org.eclipse.swt.widgets.List addList(GridData gd) {
        configureForTop();
        final org.eclipse.swt.widgets.List itemList = new org.eclipse.swt.widgets.List(value, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
        itemList.setBackground(value.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        gd.heightHint = 100;
        itemList.setLayoutData(gd);
        return itemList;
    }

    private void configureForTop() {
        comboGridData.verticalAlignment = SWT.TOP;
        valueGridData.verticalAlignment = SWT.TOP;
        buttonGridData.verticalAlignment = SWT.TOP;
    }

    private void configureItemList(final org.eclipse.swt.widgets.List itemList) {
        itemList.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }

            @Override
            public void widgetSelected(SelectionEvent e) {
                setValueForSelection(itemList);
            }
        });
        if (!Util.isNullOrBlank(condition.getValue())) {
            String[] names = condition.getValue().split(":");
            int[] sel = Util.getSelectedIndex(names, Arrays.asList(itemList.getItems()));
            itemList.setSelection(sel);
        } else {
            itemList.selectAll();
        }
        setValueForSelection(itemList);
    }

    private void configureItemChoice(final org.eclipse.swt.widgets.Combo itemChoice) {
        itemChoice.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }

            @Override
            public void widgetSelected(SelectionEvent e) {
                setValueForSelection(itemChoice);
            }
        });
        if (!Util.isNullOrBlank(condition.getValue())) {
            itemChoice.select(Arrays.asList(itemChoice.getItems()).indexOf(condition.getValue()));
        }
        setValueForSelection(itemChoice);
    }

    private void setValueForSelection(final org.eclipse.swt.widgets.Combo itemChoice) {
        int sel = itemChoice.getSelectionIndex();
        condition.setValue(sel == -1 ? null : itemChoice.getItem(sel));
    }

    private void setValueForSelection(final org.eclipse.swt.widgets.List itemList) {
        StringBuilder bui = new StringBuilder();
        for (int sel : itemList.getSelectionIndices()) {
            if (bui.length() > 0) {
                bui.append(":");
            }
            bui.append(itemList.getItem(sel));
        }
        condition.setValue(bui.toString());
    }

    private void addEnum(Attribute attribute, GridData gd, boolean multiple) {
        if (multiple) {
            final org.eclipse.swt.widgets.List itemList = addList(gd);
            if (!attribute.isRequired()) {
                itemList.add("");
            }
            for (String option : attribute.getOptions()) {
                itemList.add(option);
            }
            configureItemList(itemList);
        } else {
            final org.eclipse.swt.widgets.Combo itemList = addChoice(gd);
            if (!attribute.isRequired()) {
                itemList.add("");
            }
            for (String option : attribute.getOptions()) {
                itemList.add(option);
            }
            configureItemChoice(itemList);

        }
    }

    private void addNumeric(Attribute attribute, GridData gd) {
        comboGridData.verticalAlignment = SWT.CENTER;
        valueGridData.verticalAlignment = SWT.CENTER;
        buttonGridData.verticalAlignment = SWT.CENTER;
        final Spinner val = new Spinner(value, SWT.NONE);
        val.setBackground(value.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        val.setDigits(attribute.getDecimalPlaces());
        val.setMinimum((int) attribute.getMinValue());
        val.setMaximum((int) attribute.getMaxValue());
        val.setIncrement(1);
        val.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
        val.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                writeVal(val);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        String attrVal = condition.getValue();
        if (attrVal != null) {
            if (condition.getType().equals(ConditionType.IN)) {
                String[] args = attrVal.split("-");
                val.setSelection((int) Double.parseDouble(args[0]));
            } else {
                val.setSelection(Integer.parseInt(attrVal));
            }
        } else {
            val.setSelection((int) attribute.getMinValue());
            writeVal(val);
        }
    }

    private void writeVal(final Spinner val) {
        condition.setValue(String.valueOf(val.getSelection()));
    }

    public Condition getCondition() {
        return condition;
    }
}