package net.sf.webissues.core;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import net.sf.webissues.core.WebIssuesAttributeMapper.Flag;

import org.eclipse.mylyn.tasks.core.data.TaskAttribute;

public enum WebIssuesAttribute {
    /**
     * Id
     */
    ID("ID:", TaskAttribute.TASK_KEY, TaskAttribute.TYPE_SHORT_TEXT, Flag.PEOPLE),
    /**
     * Created by
     */
    CREATED_BY("Created by:", TaskAttribute.USER_REPORTER, TaskAttribute.TYPE_PERSON, Flag.PEOPLE, Flag.READ_ONLY),
    /**
     * Created date
     */
    CREATED_DATE("Created date:", TaskAttribute.DATE_CREATION, TaskAttribute.TYPE_DATE, Flag.READ_ONLY),
    /**
     * Modified by
     */
    MODIFIED_BY("Modified by:", WebIssuesTaskDataHandler.WEBISSUES_TASK_KEY_PREFIX + "modifiedBy", TaskAttribute.TYPE_PERSON,
                    Flag.PEOPLE, Flag.READ_ONLY),
    /**
     * Project
     */
    PROJECT("Project:", WebIssuesTaskDataHandler.WEBISSUES_TASK_KEY_PREFIX + "project", TaskAttribute.TYPE_SINGLE_SELECT,
                    Flag.ATTRIBUTE),
    /**
     * Folder
     */
    FOLDER("Folder:", WebIssuesTaskDataHandler.WEBISSUES_TASK_KEY_PREFIX + "folder", TaskAttribute.TYPE_SINGLE_SELECT,
                    Flag.ATTRIBUTE),
    /**
     * Modified date
     */
    MODIFIED_DATE("Modified date:", TaskAttribute.DATE_MODIFICATION, TaskAttribute.TYPE_DATE, Flag.READ_ONLY),
    /**
     * Completion date
     */
    COMPLETION_DATE("Completion date:", TaskAttribute.DATE_COMPLETION, TaskAttribute.TYPE_DATE, Flag.READ_ONLY),
    /**
     * Due date
     */
    DUE_DATE("Due date:", TaskAttribute.DATE_DUE, TaskAttribute.TYPE_DATE, Flag.READ_ONLY),
    /**
     * Type
     */
    TYPE("Type:", TaskAttribute.TASK_KIND, TaskAttribute.TYPE_SHORT_TEXT, Flag.ATTRIBUTE, Flag.READ_ONLY),
    /**
     * Summary
     */
    SUMMARY("Summary:", TaskAttribute.SUMMARY, TaskAttribute.TYPE_SHORT_RICH_TEXT),
    /**
     * Summary
     */
    PRIORITY("Priority:", TaskAttribute.PRIORITY, TaskAttribute.TYPE_SINGLE_SELECT);

    static Map<String, WebIssuesAttribute> attributeByTracKey = new HashMap<String, WebIssuesAttribute>();
    static Map<String, String> tracKeyByTaskKey = new HashMap<String, String>();

    private final String prettyName;
    private final String taskKey;
    private final String type;
    private EnumSet<Flag> flags;

    public static WebIssuesAttribute getByTaskKey(String taskKey) {
        for (WebIssuesAttribute attribute : values()) {
            if (taskKey.equals(attribute.getTaskKey())) {
                return attribute;
            }
        }
        return null;
    }

    WebIssuesAttribute(String prettyName, String taskKey, String type, Flag firstFlag, Flag... moreFlags) {
        this.taskKey = taskKey;
        this.prettyName = prettyName;
        this.type = type;
        if (firstFlag == null) {
            this.flags = WebIssuesAttributeMapper.NO_FLAGS;
        } else {
            this.flags = EnumSet.of(firstFlag, moreFlags);
        }
    }

    WebIssuesAttribute(String prettyName, String taskKey, String type) {
        this(prettyName, taskKey, type, null);
    }

    public String getTaskKey() {
        return taskKey;
    }

    public String getKind() {
        if (flags.contains(Flag.ATTRIBUTE)) {
            return TaskAttribute.KIND_DEFAULT;
        } else if (flags.contains(Flag.PEOPLE)) {
            return TaskAttribute.KIND_PEOPLE;
        }
        return null;
    }

    public String getType() {
        return type;
    }

    public boolean isReadOnly() {
        return flags.contains(Flag.READ_ONLY);
    }

    @Override
    public String toString() {
        return prettyName;
    }

}
