package org.webissues.api;

import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.httpclient.HttpException;
import org.webissues.api.Attribute.AttributeType;

public class ViewDefinition extends ArrayList<Condition> implements Serializable {

    private static final long serialVersionUID = 8767016925155729108L;

    private List<Attribute> columns = new ArrayList<Attribute>();
    private boolean sortAscending;
    private Attribute sortAttribute;
    private View view;

    public ViewDefinition(View view) {
        this.view = view;
    }

    public ViewDefinition(View view, String definition, IssueType type) throws ParseException {
        this.view = view;
        List<String> args = Util.parseLine(definition, ' ', '"');
        sortAscending = true;
        if (!args.get(0).equals("VIEW")) {
            throw new ParseException("Expected VIEW as first argument.", 0);
        }
        for (int i = 1; i < args.size(); i++) {
            String arg = args.get(i);
            List<String> els = Util.parseLine(arg, '=', '\"');
            String value = els.get(1);
            if (els.get(0).equals("columns")) {
                parseColumns(type, value);
            } else if (els.get(0).equals("filters")) {
                parseFilters(type, value);
            } else if (els.get(0).equals("sort-column")) {
                sortAttribute = type.get(viewAttributeIdToAttributeId(Integer.parseInt(value)));
            } else if (els.get(0).equals("sort-desc")) {
                sortAscending = value.equals("0");
            }
        }
    }

    public View getView() {
        return view;
    }

    public void setSortColumn(Attribute sortAttribute) {
        this.sortAttribute = sortAttribute;
    }

    public void addColumn(Attribute attribute) {
        columns.add(attribute);
    }

    public void removeColumn(Attribute attribute) {
        columns.remove(attribute);
    }

    public Collection<Attribute> getAttributes() {
        return columns;
    }

    public Attribute getSortAttribute() {
        return sortAttribute;
    }

    public void setSortAscending(boolean sortAscending) {
        this.sortAscending = sortAscending;
    }

    public boolean isSortAscending() {
        return sortAscending;
    }

    @Override
    public String toString() {
        return "ViewDefinition [columns=" + columns + ", sortAttribute=" + sortAttribute + ", toString()=" + super.toString() + "]";
    }

    private void parseColumns(IssueType type, String value) {
        for (String col : Util.parseLine(value, ',', '"')) {
            Attribute attr = getAttribute(type, col);
            if (attr != null) {
                columns.add(attr);
            } else {
                Client.LOG.warn("Unknown attribute " + col);
            }
        }
    }

    private void parseFilters(IssueType type, String filter) throws ParseException {
        List<String> fels = Util.parseLine(filter.substring(1, filter.length() - 1), ',', '"');
        int x = 1;
        for (String fel : fels) {
            List<String> fargs = Util.parseLine(fel, ' ', '"');
            ConditionType conditionType = ConditionType.valueOf(fargs.get(0));
            Attribute conditionAttribute = null;
            String conditionValue = null;
            for (int j = 1; j < fargs.size(); j++) {
                List<String> gels = Util.parseLine(fargs.get(j), '=', '\"');
                String attributeId = gels.get(1);
                if (gels.get(0).equals("column")) {
                    conditionAttribute = getAttribute(type, attributeId);
                    if (conditionAttribute == null) {
                        Client.LOG.warn("Unknown attribute with ID " + attributeId + " at index " + x);
                    }
                } else if (gels.get(0).equals("value")) {
                    conditionValue = attributeId;
                }
            }
            if (conditionAttribute != null) {
                if (conditionAttribute.getAttributeType().equals(AttributeType.DATETIME)) {
                    conditionValue = getDateValue(conditionAttribute.isDateOnly(), conditionValue);
                }
                Condition c = new Condition(conditionType, conditionAttribute, conditionValue);
                add(c);
            } else {
                Client.LOG.warn("No attribute for " + fels);
            }
            x++;
        }
    }

    private Attribute getAttribute(IssueType type, String attributeId) {
        return type.get(viewAttributeIdToAttributeId(Integer.parseInt(attributeId)));
    }

    private int viewAttributeIdToAttributeId(int id) {
        if (id >= 1000) {
            id -= 1000;
        } else {
            id = IssueType.NAME_ATTR_ID + id;
        }
        return id;
    }

    private String getDateValue(boolean dateOnly, String issueAttributeValue) throws ParseException {
        DateFormat fmt = new SimpleDateFormat(dateOnly ? Client.DATEONLY_FORMAT : Client.DATETIME_FORMAT);
        return String.valueOf(fmt.parse(issueAttributeValue).getTime() / 1000);
    }

    public String toDefinitionString() {
        StringBuilder bui = new StringBuilder();
        bui.append("VIEW columns=\"");
        List<Attribute> actualColumns = new ArrayList<Attribute>(columns);
        if(actualColumns.size() == 0 && view != null && view.getType() != null) {
            actualColumns.addAll(view.getType().values());
        }
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                bui.append(",");
            }
            bui.append(getViewAttributeId(columns.get(i).getId()));
        }
        bui.append("\" filters={");
        for (int i = 0; i < size(); i++) {
            if (i > 0) {
                bui.append(",");
            }
            bui.append("\"");
            Condition cond = get(i);
            bui.append(cond.getType().name());
            bui.append(" column=");
            bui.append(getViewAttributeId(cond.getAttribute().getId()));
            bui.append(" value=\\\"");
            bui.append(cond.getValue());
            bui.append("\\\"\"");
        }
        bui.append("} sort-column=");
        if (sortAttribute == null) {
            bui.append(getViewAttributeId(actualColumns.get(0).getId()));
        } else {
            bui.append(getViewAttributeId(sortAttribute.getId()));
        }
        bui.append(" sort-desc=");
        bui.append(sortAscending ? 0 : 1);
        return bui.toString();
    }

    public void update(Operation operation) throws HttpException, IOException, ProtocolException {
        final Client client = view.getType().getTypes().getEnvironment().getClient();
        client.doCall(new Call<Boolean>() {
            public Boolean call() throws HttpException, IOException, ProtocolException {
                client.doCommand("MODIFY VIEW " + view.getId() + " '" + Util.escape(toDefinitionString()) + "'");
                view.setDefinition(ViewDefinition.this);
                return true;
            }
        }, operation);

    }

    private int getViewAttributeId(int attrId) {
        return attrId >= IssueType.NAME_ATTR_ID ? attrId - IssueType.NAME_ATTR_ID : attrId + 1000;
    }

    public void setView(View view) {
        this.view = view;
    }
}
