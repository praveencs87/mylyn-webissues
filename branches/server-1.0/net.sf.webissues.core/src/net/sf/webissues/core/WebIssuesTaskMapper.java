/*******************************************************************************
 * Copyright (c) 2006, 2008 Steffen Pingel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package net.sf.webissues.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITask.PriorityLevel;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskMapper;

/**
 * @author Steffen Pingel
 */
public class WebIssuesTaskMapper extends TaskMapper {


    public WebIssuesTaskMapper(TaskData taskData) {
        super(taskData);
    }

    //
    @Override
    public boolean hasChanges(ITask task) {
        boolean changed = hasChanges(task.getModificationDate(), TaskAttribute.DATE_MODIFICATION);
        return changed;
    }

    private boolean hasChanges(Object value, String attributeKey) {
        boolean hasChanges = doHasChanges(value, attributeKey);
        return hasChanges;
    }

    private boolean doHasChanges(Object value, String attributeKey) {
        TaskData taskData = getTaskData();
        TaskAttribute attribute = taskData.getRoot().getMappedAttribute(attributeKey);
        if (attribute != null) {
            if (TaskAttribute.TYPE_BOOLEAN.equals(attribute.getMetaData().getType())) {
                return areNotEquals(value, taskData.getAttributeMapper().getBooleanValue(attribute));
            } else if (TaskAttribute.TYPE_DATE.equals(attribute.getMetaData().getType())) {
                Date dateValue = taskData.getAttributeMapper().getDateValue(attribute);
                return areNotEquals(value, dateValue);
            } else if (TaskAttribute.TYPE_INTEGER.equals(attribute.getMetaData().getType())) {
                return areNotEquals(value, taskData.getAttributeMapper().getIntegerValue(attribute));
            } else if (TaskAttribute.PRIORITY.equals(attributeKey)) {
                PriorityLevel priorityLevel = getPriorityLevel();
                return areNotEquals(value, (priorityLevel != null) ? priorityLevel.toString() : getPriority());
            } else if (TaskAttribute.TASK_KIND.equals(attributeKey)) {
                return areNotEquals(value, getTaskKind());
            } else {
                return areNotEquals(value, taskData.getAttributeMapper().getValue(attribute));
            }
        }
        return false;
    }

    //
    private boolean areNotEquals(Object existingProperty, Object newProperty) {
        boolean changed = (existingProperty != null) ? !existingProperty.equals(newProperty) : newProperty != null;
        return changed;
    }

    @Override
    public PriorityLevel getPriorityLevel() {
        TaskData taskData = getTaskData();
        TaskAttribute attribute = taskData.getRoot().getMappedAttribute(WebIssuesAttribute.PRIORITY.getTaskKey());
        if(attribute == null) {
            return PriorityLevel.getDefault();
        }
        List<String> options = new ArrayList<String>(attribute.getOptions().values());
        int range = options.size();
        int mylynRange = PriorityLevel.values().length;
        float factor = (float) mylynRange / (float) range;
        int indexOf = options.indexOf(attribute.getValue());
        int mylynLevel = (int) Math.round(((float) indexOf * factor));
        return PriorityLevel.fromLevel(mylynLevel);
    }

}
