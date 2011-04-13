package net.sf.webissues.core;

import java.util.Calendar;

import net.sf.webissues.api.AbstractChange.Type;
import net.sf.webissues.api.Util;

import org.eclipse.core.runtime.Assert;
import org.eclipse.mylyn.tasks.core.IRepositoryPerson;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskAttributeMapper;
import org.eclipse.mylyn.tasks.core.data.TaskData;

public class WebIssuesChangeMapper {

    public static final String CHANGE_TYPE = "task.change.type"; //$NON-NLS-1$
    public static final String CHANGE_USER = "task.change.user"; //$NON-NLS-1$
    public static final String CHANGE_ATTRIBUTE = "task.change.attribute"; //$NON-NLS-1$
    public static final String CHANGE_OLD_VALUE = "task.change.oldValue"; //$NON-NLS-1$
    public static final String CHANGE_NEW_VALUE = "task.change.newValue"; //$NON-NLS-1$
    public static final String CHANGE_DATE = "task.change.date"; //$NON-NLS-1$

    public static final String TYPE_CHANGE = "change"; //$NON-NLS-1$
    public static final String PREFIX_CHANGE = "task.change-"; //$NON-NLS-1$;

    private IRepositoryPerson user;

    private String attributeName;
    private String oldValue;
    private Calendar date;
    private String newValue;
    private String changeId;
    private Type type;

    public String getChangeId() {
        return changeId;
    }

    public void setChangeId(String changeId) {
        this.changeId = changeId;
    }

    public void setUser(IRepositoryPerson user) {
        this.user = user;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public void setDate(Calendar date) {
        this.date = date;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public IRepositoryPerson getUser() {
        return user;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public Calendar getDate() {
        return date;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public static WebIssuesChangeMapper createFrom(TaskAttribute taskAttribute) {
        Assert.isNotNull(taskAttribute);
        TaskAttributeMapper mapper = taskAttribute.getTaskData().getAttributeMapper();
        WebIssuesChangeMapper change = new WebIssuesChangeMapper();
        change.setChangeId(mapper.getValue(taskAttribute));
        TaskAttribute child = taskAttribute.getMappedAttribute(CHANGE_USER);
        if (child != null) {
            change.setUser(mapper.getRepositoryPerson(child));
        }
        child = taskAttribute.getMappedAttribute(CHANGE_TYPE);
        if (child != null) {
            change.setType(Type.valueOf(mapper.getValue(child)));
        }
        child = taskAttribute.getMappedAttribute(CHANGE_ATTRIBUTE);
        if (child != null) {
            change.setAttributeName(mapper.getValue(child));
        }
        child = taskAttribute.getMappedAttribute(CHANGE_OLD_VALUE);
        if (child != null) {
            change.setOldValue(mapper.getValue(child));
        }
        child = taskAttribute.getMappedAttribute(CHANGE_NEW_VALUE);
        if (child != null) {
            change.setNewValue(mapper.getValue(child));
        }
        child = taskAttribute.getMappedAttribute(CHANGE_DATE);
        if (child != null) {
            change.setDate(Util.toCalendar(mapper.getDateValue(child)));
        }
        return change;
    }

    public void applyTo(TaskAttribute taskAttribute) {
        Assert.isNotNull(taskAttribute);
        TaskData taskData = taskAttribute.getTaskData();
        TaskAttributeMapper mapper = taskData.getAttributeMapper();
        taskAttribute.getMetaData().defaults().setType(TYPE_CHANGE);
        if (getChangeId() != null) {
            mapper.setValue(taskAttribute, getChangeId());
        }
        if (getUser() != null) {
            TaskAttribute child = taskAttribute.createMappedAttribute(CHANGE_USER);
            child.getMetaData().defaults().setType(TaskAttribute.TYPE_PERSON);
            mapper.setRepositoryPerson(child, getUser());
        }
        if (getType() != null) {
            TaskAttribute child = taskAttribute.createMappedAttribute(CHANGE_TYPE);
            child.getMetaData().defaults().setType(TaskAttribute.TYPE_SHORT_TEXT);
            mapper.setValue(child, getType().name());
        }
        if (getAttributeName() != null) {
            TaskAttribute child = taskAttribute.createMappedAttribute(CHANGE_ATTRIBUTE);
            child.getMetaData().defaults().setType(TaskAttribute.TYPE_SHORT_TEXT);
            mapper.setValue(child, getAttributeName());
        }
        if (getDate() != null) {
            TaskAttribute child = taskAttribute.createMappedAttribute(CHANGE_DATE);
            child.getMetaData().defaults().setType(TaskAttribute.TYPE_DATE);
            mapper.setDateValue(child, getDate().getTime());
        }
        if (getOldValue() != null) {
            TaskAttribute child = taskAttribute.createMappedAttribute(CHANGE_OLD_VALUE);
            child.getMetaData().defaults().setType(TaskAttribute.TYPE_SHORT_TEXT);
            mapper.setValue(child, getOldValue());
        }
        if (getNewValue() != null) {
            TaskAttribute child = taskAttribute.createMappedAttribute(CHANGE_NEW_VALUE);
            child.getMetaData().defaults().setType(TaskAttribute.TYPE_SHORT_TEXT);
            mapper.setValue(child, getNewValue());
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof WebIssuesChangeMapper)) {
            return false;
        }
        WebIssuesChangeMapper other = (WebIssuesChangeMapper) obj;
        if ((other.changeId != null && this.changeId != null) && !other.changeId.equals(this.changeId)) {
            return false;
        }
        if ((other.type != null && this.type != null) && !other.type.equals(this.type)) {
            return false;
        }
        if ((other.attributeName != null && this.attributeName != null) && !(other.attributeName.equals(this.attributeName))) {
            return false;
        }
        if ((other.oldValue != null && this.oldValue != null) && !(other.oldValue.equals(this.oldValue))) {
            return false;
        }
        if ((other.newValue != null && this.newValue != null) && !(other.newValue.equals(this.newValue))) {
            return false;
        }
        if ((other.date != null && this.date != null) && !(other.date.equals(this.date))) {
            return false;
        }
        return true;
    }

    public void applyTo(WebIssuesChange taskChange) {
        Assert.isNotNull(taskChange);
        if (getUser() != null) {
            taskChange.setUser(getUser());
        }
        if (getAttributeName() != null) {
            taskChange.setAttributeName(getAttributeName());
        }
        if (getType() != null) {
            taskChange.setType(getType());
        }
        if (getOldValue() != null) {
            taskChange.setOldValue(getOldValue());
        }
        if (getNewValue() != null) {
            taskChange.setNewValue(getNewValue());
        }
        if (getDate() != null) {
            taskChange.setDate(getDate());
        }

    }

}
