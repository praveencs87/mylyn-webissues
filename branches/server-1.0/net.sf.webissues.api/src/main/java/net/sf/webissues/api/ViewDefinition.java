package net.sf.webissues.api;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.sf.webissues.api.Attribute.AttributeType;

public class ViewDefinition extends ArrayList<Condition> implements Serializable {

    private static final long serialVersionUID = 8767016925155729108L;


    private List<Attribute> columns = new ArrayList<Attribute>();
    private int sortColumn;

    public ViewDefinition(String definition, Type type) throws ParseException {
        List<String> args = Util.parseLine(definition, ' ', '"');
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
                sortColumn = Integer.parseInt(value);
            }
        }
    }

    public Collection<Attribute> getAttributes() {
        return columns;
    }

    public Attribute getSortAttribute() {
        return columns.get(sortColumn);
    }

    public int getSortColumn() {
        return sortColumn;
    }

    @Override
    public String toString() {
        return "ViewDefinition [columns=" + columns + ", sortColumn=" + sortColumn + ", toString()=" + super.toString() + "]";
    }

    private void parseColumns(Type type, String value) {
        for (String col : Util.parseLine(value, ',', '"')) {
            Attribute attr = getAttribute(type, col);
            if (attr != null) {
                columns.add(attr);
            }
            else {
                Client.LOG.warn("Unknown attribute " + col);
            }
        }
    }

    private void parseFilters(Type type, String filter) throws ParseException {
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
                if(conditionAttribute.getAttributeType().equals(AttributeType.DATETIME)) {
                    conditionValue = getDateValue(conditionAttribute.isDateOnly(), conditionValue);
                }                
                Condition c = new Condition(conditionType, conditionAttribute, conditionValue);
                add(c);
            }
            else {
                Client.LOG.warn("No attribute for " +fels);
            }
            x++;
        }
    }

    private Attribute getAttribute(Type type, String attributeId) {
        int id = Integer.parseInt(attributeId);
        if(id >= 1000) {
            id -= 1000;
        }
        else {
            id = Type.NAME_ATTR_ID + id;
        }
        return type.get(id);
    }

    private String getDateValue(boolean dateOnly, String issueAttributeValue) throws ParseException {
        DateFormat fmt = new SimpleDateFormat(dateOnly ? Client.DATEONLY_FORMAT : Client.DATETIME_FORMAT);
        return String.valueOf(fmt.parse(issueAttributeValue).getTime() / 1000);
    }
}
