/**
 * 
 */
package net.sf.webissues.ui.wizard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import net.sf.webissues.api.Attribute;
import net.sf.webissues.api.Environment;
import net.sf.webissues.api.Folder;
import net.sf.webissues.api.Project;
import net.sf.webissues.api.User;
import net.sf.webissues.api.Util;
import net.sf.webissues.core.WebIssuesFilterCondition;
import net.sf.webissues.core.WebIssuesFilterCondition.Type;
import net.sf.webissues.ui.WebIssuesImages;
import net.sf.webissues.ui.WebIssuesUserSelector;

import org.eclipse.mylyn.internal.provisional.commons.ui.CommonImages;
import org.eclipse.mylyn.internal.provisional.commons.ui.DatePicker;
import org.eclipse.swt.SWT;
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

@SuppressWarnings("restriction")
class AttributeRow {

    private Composite value;
    private final net.sf.webissues.api.Type type;
    private Combo attributeCombo;
    private GridData comboGridData;
    private GridData valueGridData;
    private GridData buttonGridData;
    private WebIssuesFilterCondition condition;
    private List<AttributeRow> rows;

    AttributeRow(net.sf.webissues.api.Type type, Composite parent, WebIssuesFilterCondition condition, List<AttributeRow> rows) {
        this.rows = rows;
        this.type = type;

        this.condition = condition;

        // Attribute type
        attributeCombo = new Combo(parent, SWT.NONE);
        attributeCombo.add(WebIssuesFilterCondition.PROJECT);
        attributeCombo.add(WebIssuesFilterCondition.FOLDER);
        attributeCombo.add(WebIssuesFilterCondition.NAME);
        attributeCombo.add(WebIssuesFilterCondition.DATE_CREATED);
        attributeCombo.add(WebIssuesFilterCondition.USER_CREATED);
        attributeCombo.add(WebIssuesFilterCondition.DATE_MODIFIED);
        attributeCombo.add(WebIssuesFilterCondition.USER_MODIFIED);
        for (Attribute attribute : type.values()) {
            attributeCombo.add(attribute.getName());
        }
        comboGridData = new GridData(SWT.NONE, SWT.NONE, false, false);
        comboGridData.widthHint = 120;
        comboGridData.horizontalSpan = 1;
        comboGridData.verticalAlignment = SWT.TOP;
        attributeCombo.setLayoutData(comboGridData);
        attributeCombo.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                AttributeRow.this.condition.setName(null);
                AttributeRow.this.condition.setValue(null);
                rebuildValue();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                rebuildValue();
            }
        });

        // Value
        value = new Composite(parent, SWT.NONE);
        value.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        value.setLayout(new GridLayout(1, true));
        valueGridData = new GridData(SWT.FILL, SWT.NONE, true, false);
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
        String attrVal = condition.getName();
        int idx = attrVal == null ? -1 : Arrays.asList(attributeCombo.getItems()).indexOf(attrVal);
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

    void removeRow(final Button removeButton) {
        value.dispose();
        removeButton.dispose();
        attributeCombo.dispose();
        rows.remove(this);
        redoLayout();
    }

    void rebuildValue() {
        for (Control control : value.getChildren()) {
            control.dispose();
        }
        GridData gd = new GridData(SWT.FILL, SWT.NONE, true, false);
        String item = attributeCombo.getItem(attributeCombo.getSelectionIndex());
        condition.setName(item);
        if (item.equals(WebIssuesFilterCondition.PROJECT)) {
            addProjects(gd);
        } else if (item.equals(WebIssuesFilterCondition.FOLDER)) {
            addFolders(gd);
        } else if (item.equals(WebIssuesFilterCondition.NAME)) {
            addText(gd);
        } else if (item.equals(WebIssuesFilterCondition.DATE_CREATED) || item.equals(WebIssuesFilterCondition.DATE_MODIFIED)) {
            addDate(false, gd);
        } else if (item.equals(WebIssuesFilterCondition.USER_CREATED) || item.equals(WebIssuesFilterCondition.USER_MODIFIED)) {
            addUser(gd);
        } else {
            Attribute attribute = type.getByName(item);
            if (attribute != null) {
                switch (attribute.getAttributeType()) {
                    case ENUM:
                        addEnum(attribute, gd);
                        break;
                    case NUMERIC:
                        addNumeric(attribute, gd);
                        break;
                    case TEXT:
                        addText(gd);
                        break;
                    case USER:
                        addUser(gd);
                        break;
                    case DATETIME:
                        addDate(attribute, gd);
                        break;
                }
            }
        }
        redoLayout();
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
        condition.setType(Type.IS_EQUAL_TO);
        configureForTop();
        final Text text = new Text(value, SWT.BORDER);
        // text.setBackground(value.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        text.setLayoutData(gd);
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
        condition.setType(Type.IS_IN);
        configureForTop();

        comboGridData.verticalAlignment = SWT.CENTER;
        valueGridData.verticalAlignment = SWT.CENTER;
        buttonGridData.verticalAlignment = SWT.CENTER;
        Composite row = new Composite(value, SWT.NONE);
        row.setBackground(value.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        row.setLayout(new GridLayout(2, true));
        row.setLayoutData(gd);

        final DatePicker dateFrom = new DatePicker(row, SWT.NONE, "", !dateOnly, 0);
        dateFrom.setBackground(value.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        dateFrom.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
        final DatePicker dateTo = new DatePicker(row, SWT.NONE, "", !dateOnly, 23);
        dateTo.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
        dateTo.setBackground(value.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        dateFrom.addPickerSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                writeDate(dateFrom, dateTo);
            }
        });
        dateTo.addPickerSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                writeDate(dateFrom, dateTo);
            }
        });
        if (condition.getValue() != null) {
            String[] args = condition.getValue().split("-");
            if (args.length > 0) {
                Calendar fromDate = Util.parseTimestampInSeconds(args[0]);
                if (fromDate.getTimeInMillis() != 0) {
                    dateFrom.setDate(fromDate);
                }
                if (args.length > 1) {
                    Calendar toDate = Util.parseTimestampInSeconds(args[1]);
                    long timeInMillis = toDate.getTimeInMillis();
                    long maxValue = Long.MAX_VALUE;
                    if (timeInMillis < maxValue - 1000) {
                        dateTo.setDate(toDate);
                    }
                }
            }
        }
    }

    private void writeDate(final DatePicker dateFrom, final DatePicker dateTo) {
        String dateFromText = dateFrom.getDate() == null ? "0" : Util.formatTimestampInSeconds(dateFrom.getDate());
        String dateToText = dateTo.getDate() == null ? String.valueOf(Long.MAX_VALUE / 1000) : Util.formatTimestampInSeconds(dateTo
                        .getDate());
        String dateRangeString = dateFromText + "-" + dateToText;
        condition.setValue(dateRangeString);
    }

    private void addUser(GridData gd) {
        condition.setType(Type.IS_IN);
        configureForTop();
        Environment environment = type.getTypes().getEnvironment();
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

    private void addProjects(GridData gd) {
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
    }

    private void addFolders(GridData gd) {
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
    }

    private org.eclipse.swt.widgets.List addList(GridData gd) {
        condition.setType(Type.IS_IN);
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

    private void addEnum(Attribute attribute, GridData gd) {
        final org.eclipse.swt.widgets.List itemList = addList(gd);
        if (!attribute.isRequired()) {
            itemList.add("");
        }
        for (String option : attribute.getOptions()) {
            itemList.add(option);
        }
        configureItemList(itemList);
    }

    private void addNumeric(Attribute attribute, GridData gd) {
        comboGridData.verticalAlignment = SWT.CENTER;
        valueGridData.verticalAlignment = SWT.CENTER;
        buttonGridData.verticalAlignment = SWT.CENTER;
        Composite row = new Composite(value, SWT.NONE);
        row.setBackground(value.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        row.setLayout(new GridLayout(3, true));
        row.setLayoutData(gd);
        final Spinner min = new Spinner(row, SWT.NONE);
        min.setBackground(value.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        min.setDigits(attribute.getDecimalPlaces());
        min.setMinimum((int) attribute.getMinValue());
        min.setMaximum((int) attribute.getMaxValue());
        min.setIncrement(1);
        min.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
        final Button range = new Button(row, SWT.CHECK);
        range.setBackground(value.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        final Spinner max = new Spinner(row, SWT.NONE);
        max.setBackground(value.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        min.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (!range.getSelection()) {
                    max.setSelection(min.getSelection());
                    condition.setValue(String.valueOf(min.getSelection()));
                } else {
                    writeRange(min, max);
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });
        max.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (range.getSelection()) {
                    writeRange(min, max);
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                // widgetSelected(e);
            }
        });
        range.setText("Range");
        max.setDigits(attribute.getDecimalPlaces());
        max.setMinimum((int) attribute.getMinValue());
        max.setMaximum((int) attribute.getMaxValue());
        max.setIncrement(1);
        max.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
        range.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                max.setEnabled(range.getSelection());
                if (!range.getSelection()) {
                    max.setSelection(min.getSelection());
                    condition.setType(Type.IS_EQUAL_TO);
                    condition.setValue(String.valueOf(min.getSelection()));
                } else {
                    condition.setType(Type.IS_IN);
                    writeRange(min, max);
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                // widgetSelected(e);
            }
        });

        String attrVal = condition.getValue();
        if (attrVal != null) {
            if (condition.getType().equals(Type.IS_IN)) {
                String[] args = attrVal.split("-");
                max.setSelection((int) Double.parseDouble(args[1]));
                min.setSelection((int) Double.parseDouble(args[0]));
            } else {
                max.setSelection(min.getSelection());
                min.setSelection(Integer.parseInt(attrVal));
            }
            range.setSelection(condition.getType().equals(Type.IS_IN));
        } else {
            range.setSelection(true);
            min.setSelection((int) attribute.getMinValue());
            max.setSelection((int) attribute.getMaxValue());
            condition.setType(Type.IS_IN);
            writeRange(min, max);
        }
    }

    private void writeRange(final Spinner min, final Spinner max) {
        condition.setValue(String.valueOf(min.getSelection()) + "-" + String.valueOf(max.getSelection()));
    }

    public WebIssuesFilterCondition getCondition() {
        return condition;
    }
}