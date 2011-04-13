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

import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.webissues.api.Attachment;
import net.sf.webissues.api.Attribute;
import net.sf.webissues.api.Change;
import net.sf.webissues.api.Client;
import net.sf.webissues.api.Comment;
import net.sf.webissues.api.Environment;
import net.sf.webissues.api.Folder;
import net.sf.webissues.api.Issue;
import net.sf.webissues.api.IssueDetails;
import net.sf.webissues.api.Project;
import net.sf.webissues.api.Projects;
import net.sf.webissues.api.ProtocolException;
import net.sf.webissues.api.Type;
import net.sf.webissues.api.User;
import net.sf.webissues.api.Util;

import org.apache.commons.httpclient.HttpException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.mylyn.commons.net.Policy;
import org.eclipse.mylyn.tasks.core.ITaskMapping;
import org.eclipse.mylyn.tasks.core.RepositoryResponse;
import org.eclipse.mylyn.tasks.core.RepositoryResponse.ResponseKind;
import org.eclipse.mylyn.tasks.core.RepositoryStatus;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskDataHandler;
import org.eclipse.mylyn.tasks.core.data.TaskAttachmentMapper;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskAttributeMapper;
import org.eclipse.mylyn.tasks.core.data.TaskAttributeMetaData;
import org.eclipse.mylyn.tasks.core.data.TaskCommentMapper;
import org.eclipse.mylyn.tasks.core.data.TaskData;

/**
 * @author Steffen Pingel
 */
public class WebIssuesTaskDataHandler extends AbstractTaskDataHandler {

    public static final String WEBISSUES_TASK_KEY_PREFIX = "task.webissues.";
    private static final String WEBISSUES_ATTRIBUTE_KEY_PREFIX = WebIssuesTaskDataHandler.WEBISSUES_TASK_KEY_PREFIX + "attribute.";
    private static final String TASK_DATA_VERSION = "1";

    private final WebIssuesRepositoryConnector connector;

    public WebIssuesTaskDataHandler(WebIssuesRepositoryConnector connector) {
        this.connector = connector;
    }

    public TaskData getTaskData(TaskRepository repository, String taskId, IProgressMonitor monitor) throws CoreException {
        monitor = Policy.monitorFor(monitor);
        try {
            monitor.beginTask("Task Download", IProgressMonitor.UNKNOWN);
            return downloadTaskData(repository, WebIssuesRepositoryConnector.getBugId(taskId), monitor);
        } finally {
            monitor.done();
        }
    }

    TaskData downloadTaskData(TaskRepository repository, int taskId, IProgressMonitor monitor) throws CoreException {
        try {
            WebIssuesClient client = connector.getClientManager().getClient(repository, monitor);
            // client.updateAttributes(monitor, false);
            IssueDetails issue = client.getIssueDetails(taskId, monitor);
            return createTaskDataFromIssueDetails(client, repository, issue, monitor);
        } catch (OperationCanceledException e) {
            throw e;
        } catch (Exception e) {
            throw new CoreException(WebIssuesCorePlugin.toStatus(e, repository));
        }
    }

    TaskData createTaskDataFromIssueDetails(WebIssuesClient client, TaskRepository repository, IssueDetails issueDetails,
                                            IProgressMonitor monitor) throws CoreException {
        TaskData taskData = new TaskData(getAttributeMapper(repository), WebIssuesCorePlugin.CONNECTOR_KIND,
                        repository.getRepositoryUrl(), issueDetails.getIssue().getId() + "");
        taskData.setVersion(TASK_DATA_VERSION);
        try {
            createDefaultAttributes(taskData, client.getEnvironment(), issueDetails.getIssue());
            updateTaskData(updatePartialTaskData(client, repository, taskData, issueDetails.getIssue()), client, repository,
                taskData, issueDetails, monitor);
            return taskData;
        } catch (OperationCanceledException e) {
            throw e;
        } catch (Exception e) {
            throw new CoreException(WebIssuesCorePlugin.toStatus(e, repository));
        }
    }

    TaskData createTaskDataFromIssue(WebIssuesClient client, TaskRepository repository, Issue issue, IProgressMonitor monitor)
                    throws CoreException {
        TaskData taskData = new TaskData(getAttributeMapper(repository), WebIssuesCorePlugin.CONNECTOR_KIND,
                        repository.getRepositoryUrl(), String.valueOf(issue.getId()));
        taskData.setVersion(TASK_DATA_VERSION);
        try {
            createDefaultAttributes(taskData, client.getEnvironment(), issue);
            updatePartialTaskData(client, repository, taskData, issue);
            return taskData;
        } catch (OperationCanceledException e) {
            throw e;
        } catch (Exception e) {
            throw new CoreException(WebIssuesCorePlugin.toStatus(e, repository));
        }
    }

    public static Set<TaskAttribute> updatePartialTaskData(WebIssuesClient client, TaskRepository repository, TaskData data,
                                                           Issue issue) {
        Set<TaskAttribute> changedAttributes = new HashSet<TaskAttribute>();
        doString(TaskAttribute.TASK_KEY, data, changedAttributes, String.valueOf(issue.getId()));
        doString(TaskAttribute.SUMMARY, data, changedAttributes, String.valueOf(issue.getName()));

        // Priority
        TaskAttribute priorityAttribute = data.getRoot().getAttribute(TaskAttribute.PRIORITY);
        if (priorityAttribute != null) {
            doString(priorityAttribute.getId(), data, changedAttributes, issue.getAttributeValueByName("Priority"));
            changedAttributes.add(priorityAttribute);
        }

        // Users and dates
        doDateAttribute(TaskAttribute.DATE_CREATION, data, changedAttributes, issue.getCreatedDate());
        doUser(client, WebIssuesAttribute.CREATED_BY.getTaskKey(), data, changedAttributes, issue.getCreatedUser());
        doDateAttribute(TaskAttribute.DATE_MODIFICATION, data, changedAttributes, issue.getModifiedDate());
        doUser(client, WebIssuesAttribute.MODIFIED_BY.getTaskKey(), data, changedAttributes, issue.getModifiedUser());

        // Dates
        if (client.isCompletedStatus(issue.getAttributeValueByName(client.getStatusAttributeName()))) {
            doDateAttribute(TaskAttribute.DATE_COMPLETION, data, changedAttributes, issue.getModifiedDate());
        }
        if (client.getDueDateAttributeName() != null && !client.getDueDateAttributeName().equals("")) {
            try {
                Calendar date = parseDateAttribute(repository, issue, client.getDueDateAttributeName());
                if (date != null) {
                    doDateAttribute(TaskAttribute.DATE_DUE, data, changedAttributes, date);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (client.getEstimateAttributeName() != null && !client.getEstimateAttributeName().equals("")) {
        }

        TaskAttribute typeAttribute = data.getRoot().getAttribute(TaskAttribute.TASK_KIND);
        typeAttribute.setValue(issue.getFolder().getType().getName());
        changedAttributes.add(typeAttribute);

        return changedAttributes;
    }

    private static void doUser(WebIssuesClient client, String taskKey, TaskData data, Set<TaskAttribute> changedAttributes,
                               User user) {
        TaskAttribute taskAttribute = data.getRoot().getAttribute(taskKey);
        if (user != null) {
            taskAttribute.setValue(String.valueOf(user.getName()));
            changedAttributes.add(taskAttribute);
        } else if (data.isNew()) {
            taskAttribute.setValue(String.valueOf(client.getEnvironment().getOwnerUser().getName()));
            changedAttributes.add(taskAttribute);
        }
    }

    private static Calendar parseDateAttribute(TaskRepository repository, Issue issue, String attributeName) {
        Folder folder = issue.getFolder();
        Type type = folder.getType();
        Attribute attr = type.getByName(attributeName);
        if (attr != null && attr.getType().equals(Attribute.AttributeType.DATETIME)) {
            DateFormat fmt = new SimpleDateFormat(attr.isDateOnly() ? Client.DATEONLY_FORMAT : Client.DATETIME_FORMAT);
            Date d;
            try {
                String val = issue.getAttributeValueByName(attributeName);
                d = fmt.parse(val);
            } catch (ParseException e) {
                return null;
            }
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            return c;
        }
        return null;
    }

    public static Set<TaskAttribute> updateTaskData(Set<TaskAttribute> changedAttributes, WebIssuesClient client,
                                                    TaskRepository repository, TaskData data, IssueDetails issue,
                                                    IProgressMonitor monitor) throws HttpException, ProtocolException, IOException {
        doDateAttribute(TaskAttribute.DATE_CREATION, data, changedAttributes, issue.getIssue().getCreatedDate());
        doDateAttribute(TaskAttribute.DATE_MODIFICATION, data, changedAttributes, issue.getIssue().getModifiedDate());

        Environment environment = WebIssuesCorePlugin.getDefault().getConnector().getClientManager().getClient(repository, monitor)
                        .getEnvironment();
        Folder folder = issue.getIssue().getFolder();
        Type type = folder.getType();

        // Set the project and folder attributes
        TaskAttribute projectAttribute = data.getRoot().getAttribute(WebIssuesAttribute.PROJECT.getTaskKey());
        TaskAttribute folderAttribute = data.getRoot().getAttribute(WebIssuesAttribute.FOLDER.getTaskKey());
        projectAttribute.setValue(String.valueOf(folder.getProject().getId()));
        rebuildFolders(environment, projectAttribute, data, folderAttribute, issue.getIssue().getFolder().getType());
        folderAttribute.setValue(String.valueOf(folder.getId()));

        // Remove all webissues attributes exception project and folder
        for (Iterator<TaskAttribute> attributeIterator = data.getRoot().getAttributes().values().iterator(); attributeIterator
                        .hasNext();) {
            TaskAttribute attr = attributeIterator.next();
            if (attr.getId().startsWith(WEBISSUES_ATTRIBUTE_KEY_PREFIX)) {
                attributeIterator.remove();
            }
        }

        // We might already have a priority attribute
        TaskAttribute priorityAttribute = data.getRoot().getAttribute(TaskAttribute.PRIORITY);

        for (Attribute attr : type.values()) {
            if (!attr.isBuiltIn() && ( priorityAttribute == null || !attr.getName().equals("Priority"))) {
                String defaultValue = attr.getDefaultValue();
                String attributeId = WEBISSUES_ATTRIBUTE_KEY_PREFIX + attr.getId();
                TaskAttribute taskAttr = data.getRoot().createAttribute(attributeId);
                TaskAttributeMetaData metaData = taskAttr.getMetaData();
                metaData.setKind(TaskAttribute.KIND_DEFAULT);
                switch (attr.getAttributeType()) {
                    case DATETIME:
                        if (attr.isDateOnly()) {
                            metaData.setType(TaskAttribute.TYPE_DATE);
                        } else {
                            metaData.setType(TaskAttribute.TYPE_DATETIME);
                        }
                        break;
                    case TEXT:
                        metaData.setType(TaskAttribute.TYPE_SHORT_TEXT);
                        break;
                    case NUMERIC:
                        metaData.setType(TaskAttribute.TYPE_SHORT_TEXT);
                        break;
                    case USER:
                        metaData.setType(TaskAttribute.TYPE_PERSON);
                        metaData.setKind(TaskAttribute.KIND_PEOPLE);
                        metaData.putValue("membersOnly", String.valueOf(attr.isMembersOnly()));
                        break;
                    case ENUM:
                        metaData.setType(TaskAttribute.TYPE_SINGLE_SELECT);
                        taskAttr.clearOptions();
                        if (!attr.isRequired()) {
                            taskAttr.putOption("", "");
                        }
                        for (String option : attr.getOptions()) {
                            taskAttr.putOption(option, option);
                        }
                        break;
                }

                // Set as default if nothing set
                String value = issue.getIssue().get(attr);
                if (Util.isNullOrBlank(value)) {
                    value = defaultValue;
                }

                // Format the value if it is numeric
                if (attr.getType().equals(Attribute.AttributeType.NUMERIC) && attr.getDecimalPlaces() > 0) {
                    DecimalFormat fmt = new DecimalFormat();
                    fmt.setMinimumFractionDigits(attr.getDecimalPlaces());
                    fmt.setMaximumFractionDigits(attr.getDecimalPlaces());
                    taskAttr.setValue(fmt.format(Util.isNullOrBlank(value) ? 0d : Double.parseDouble(value)));
                } else if (attr.getType().equals(Attribute.AttributeType.DATETIME)) {
                    DateFormat fmt = new SimpleDateFormat(attr.isDateOnly() ? Client.DATEONLY_FORMAT : Client.DATETIME_FORMAT);
                    try {
                        taskAttr.setValue(value == null ? "" : String.valueOf(fmt.parse(value).getTime()));
                    } catch (ParseException e) {
                        taskAttr.setValue("");
                    }
                } else {
                    taskAttr.setValue(Util.nonNull(value));
                }

                metaData.setLabel(attr.getName());
            }
        }

        // Comments
        Collection<Comment> comments = issue.getComments();
        if (comments != null) {
            int count = 1;
            for (Comment comment : comments) {
                String plainText = comment.getText();
                if (Util.isNullOrBlank(plainText)) {
                    continue;
                }
                TaskCommentMapper mapper = new TaskCommentMapper();
                User owner = comment.getCreatedUser();
                if (owner != null) {
                    mapper.setAuthor(repository.createPerson(owner.getName()));
                }
                mapper.setCreationDate(comment.getCreatedDate().getTime());
                mapper.setText(plainText);
                mapper.setNumber(count);
                TaskAttribute attribute = data.getRoot().createAttribute(TaskAttribute.PREFIX_COMMENT + count);
                mapper.applyTo(attribute);
                count++;
            }
        }

        Collection<Attachment> attachments = issue.getAttachments();
        int count = 1;
        for (Attachment attachment : attachments) {
            TaskAttachmentMapper mapper = new TaskAttachmentMapper();
            mapper.setAuthor(repository.createPerson(attachment.getCreatedUser().getName()));
            mapper.setDescription(attachment.getDescription());
            mapper.setCreationDate(attachment.getCreatedDate().getTime());
            mapper.setAttachmentId(String.valueOf(attachment.getId()));
            mapper.setFileName(attachment.getName());
            mapper.setUrl(Util.concatenateUri(repository.getRepositoryUrl(), "attachments", String.valueOf(attachment.getId())));
            mapper.setLength(Long.valueOf(attachment.getSize()));
            TaskAttribute attribute = data.getRoot().createAttribute(TaskAttribute.PREFIX_ATTACHMENT + count);
            mapper.applyTo(attribute);
            count++;
        }

        count = 1;
        for (Change change : issue.getChanges()) {
            WebIssuesChangeMapper mapper = new WebIssuesChangeMapper();
            mapper.setChangeId(String.valueOf(count));
            mapper.setUser(repository.createPerson(change.getModifiedUser().getName()));
            mapper.setAttributeName(change.getAttribute() == null ? "Name" : change.getAttribute().getName());
            mapper.setType(change.getType());
            mapper.setOldValue(change.getOldValue());
            mapper.setNewValue(change.getNewValue());
            mapper.setDate(change.getModifiedDate());
            TaskAttribute attribute = data.getRoot().createAttribute(WebIssuesChangeMapper.PREFIX_CHANGE + String.valueOf(count));
            mapper.applyTo(attribute);
            count++;
        }

        return changedAttributes;
    }

    private static void doString(String key, TaskData data, Set<TaskAttribute> changedAttributes, Object val) {
        TaskAttribute taskAttribute = data.getRoot().getAttribute(key);
        if (taskAttribute != null) {
            if (val != null) {
                taskAttribute.setValue(val.toString());
            } else {
                taskAttribute.clearAttributes();
            }
            changedAttributes.add(taskAttribute);
        }
    }

    private static void doDateAttribute(String key, TaskData data, Set<TaskAttribute> changedAttributes, Calendar date) {
        if (date != null) {
            TaskAttribute taskAttribute = data.getRoot().getAttribute(key);
            taskAttribute.setValue(Util.formatTimestamp(date));
            changedAttributes.add(taskAttribute);
        }
    }

    public static void createDefaultAttributes(TaskData data, Environment environment, Issue issue) {
        boolean existingTask = issue != null;
        data.setVersion(TASK_DATA_VERSION);
        createAttribute(data, WebIssuesAttribute.ID);
        createAttribute(data, WebIssuesAttribute.SUMMARY);
        
        boolean readOnly = existingTask;
        List<Project> projects = new ArrayList<Project>(); 
        if (existingTask && !environment.getVersion().startsWith("0.")) {
            /*
             * 1.0 allows moving to a different project (i.e. folder, as long as
             * the type is the same). So ony add projects that are of the same type
             */
            for (Project p : environment.getProjects().values()) {
                boolean hasFolderOfSameType = false;
                for (Folder f : p.values()) {
                    if (f.getType().equals(issue.getFolder().getType())) {
                        hasFolderOfSameType = true;
                        break;
                    }
                }
                if (hasFolderOfSameType) {
                    projects.add(p);
                }
            }
            readOnly = projects.size() < 2;
        } 
        else {
            projects = new ArrayList<Project>(environment.getProjects().values());
        }
        TaskAttribute projectAttr = createAttribute(data, WebIssuesAttribute.PROJECT, (Project[])projects.toArray(new Project[0]));
        projectAttr.getMetaData().setReadOnly(readOnly);

        TaskAttribute folderAttr = createAttribute(data, WebIssuesAttribute.FOLDER);
        // 1.0 allows moving to a different folder
        rebuildFolders(environment, projectAttr, data, folderAttr, issue == null ? null : issue.getFolder().getType());
        folderAttr.getMetaData().setReadOnly(existingTask && environment.getVersion().startsWith("0."));
        if (existingTask) {
            createAttribute(data, WebIssuesAttribute.TYPE);
            createAttribute(data, WebIssuesAttribute.CREATED_BY);
            createAttribute(data, WebIssuesAttribute.MODIFIED_BY);
            createAttribute(data, WebIssuesAttribute.CREATED_DATE);
            createAttribute(data, WebIssuesAttribute.MODIFIED_DATE);
            createAttribute(data, WebIssuesAttribute.COMPLETION_DATE);
            createAttribute(data, WebIssuesAttribute.DUE_DATE);

            // Special handling for any existing Priority attribute
            Attribute attribute = issue.getFolder().getType().getByName("Priority");
            if (attribute != null) {
                List<Integer> values = new ArrayList<Integer>();
                for (int i = (int) attribute.getMinValue(); i <= attribute.getMaxValue(); i++) {
                    values.add(i);
                }
                createAttribute(data, WebIssuesAttribute.PRIORITY, values.toArray());
            }
        }

        data.getRoot().createAttribute(TaskAttribute.COMMENT_NEW).getMetaData().setType(TaskAttribute.TYPE_LONG_RICH_TEXT)
                        .setReadOnly(false);

    }

    private static TaskAttribute createAttribute(TaskData data, WebIssuesAttribute webissuesAttribute) {
        TaskAttribute attr = data.getRoot().createAttribute(webissuesAttribute.getTaskKey());
        TaskAttributeMetaData metaData = attr.getMetaData();
        metaData.setType(webissuesAttribute.getType());
        metaData.setKind(webissuesAttribute.getKind());
        metaData.setLabel(webissuesAttribute.toString());
        metaData.setReadOnly(webissuesAttribute.isReadOnly());
        return attr;
    }

    public static void rebuildFolders(Environment environment, TaskAttribute projectAttribute, TaskData data, TaskAttribute attr,
                                      Type currentType) {
        String value = projectAttribute.getValue();
        attr.clearOptions();
        if (!Util.isNullOrBlank(value)) {
            Project project = environment.getProjects().get(Integer.parseInt(value));
            if (project != null) {
                attr.clearOptions();
                for (Folder folder : project.values()) {
                    if (environment.getVersion().startsWith("0.") || folder.getType().equals(currentType)) {
                        String key = String.valueOf(folder.getId());
                        attr.putOption(key, folder.getName() + " (" + folder.getType().getName() + ")");
                        if (attr.getOptions().size() == 1) {
                            attr.setValue(key);
                        }
                    }
                }
            }
        }
    }

    private static TaskAttribute createAttribute(TaskData data, WebIssuesAttribute webissuesAttribute, boolean allowEmtpy,
                                                 Object... values) {
        return createAttributeWithOptions(data, webissuesAttribute, allowEmtpy, values);
    }

    private static TaskAttribute createAttributeWithOptions(TaskData data, WebIssuesAttribute webissuesAttribute,
                                                            boolean allowEmtpy, Object[] values) {
        TaskAttribute attr = createAttribute(data, webissuesAttribute);
        if (values != null && values.length > 0) {
            if (allowEmtpy) {
                attr.putOption("", "");
            }
            for (Object value : values) {
                String id = value.toString();
                String val = id;
                if (value instanceof User) {
                    id = ((User) value).getLogin();
                    val = ((User) value).getName();
                } else if (value instanceof Project) {
                    id = String.valueOf(((Project) value).getId());
                    val = ((Project) value).getName();
                } else if (value instanceof Folder) {
                    id = String.valueOf(((Folder) value).getId());
                    val = ((Folder) value).getName();
                }
                attr.putOption(id, val);
                if (attr.getOptions().size() == 1) {
                    attr.setValue(id);
                }
            }
        } else {
            attr.getMetaData().setReadOnly(true);
        }
        return attr;
    }

    private static TaskAttribute createAttribute(TaskData data, WebIssuesAttribute webissuesAttribute, Object... values) {
        return createAttribute(data, webissuesAttribute, false, values);
    }

    @Override
    public RepositoryResponse postTaskData(TaskRepository repository, TaskData taskData, Set<TaskAttribute> oldAttributes,
                                           IProgressMonitor monitor) throws CoreException {
        try {
            WebIssuesClient client = connector.getClientManager().getClient(repository, monitor);
            Folder folder = getFolder(taskData, client);
            if (taskData.isNew()) {
                Issue issue = new Issue(taskData.getRoot().getAttribute(TaskAttribute.SUMMARY).getValue(), client.getEnvironment()
                                .getOwnerUser(), folder);
                try {
                    int id = client.createIssue(issue, monitor);
                    String newComment = getNewComment(taskData);
                    if (!Util.isNullOrBlank(newComment)) {
                        IssueDetails details = client.getIssueDetails(id, monitor);
                        addComment(monitor, client, newComment, details);
                    }
                    return new RepositoryResponse(ResponseKind.TASK_CREATED, id + "");
                } catch (ProtocolException pe) {
                    if (pe.getCode() == ProtocolException.INVALID_STRING) {
                        throw new CoreException(new RepositoryStatus(RepositoryStatus.ERROR, WebIssuesCorePlugin.ID_PLUGIN,
                                        pe.getCode(), "Invalid issue summary. Maximum length of 80"));
                    } else {
                        throw pe;
                    }
                }
            } else {
                String newName = null;
                int newFolderId = -1;
                Map<Attribute, String> newServerAttributeValues = new HashMap<Attribute, String>();
                for (TaskAttribute oldAttribute : oldAttributes) {
                    TaskAttribute attribute = taskData.getRoot().getAttribute(oldAttribute.getId());

                    // Store the value
                    if (oldAttribute.getId().equals(WebIssuesAttribute.PROJECT.getTaskKey())) {
                        // Ignore the project change as the folder should have changed too, which is the Id we need
                    } 
                    else if (oldAttribute.getId().equals(WebIssuesAttribute.FOLDER.getTaskKey())) {
                        newFolderId = Integer.parseInt(attribute.getValue());
                        // Ignore the project change as the folder should have changed too, which is the Id we need
                    } 
                    
                    if (oldAttribute.getId().startsWith(WEBISSUES_ATTRIBUTE_KEY_PREFIX) && !attribute.getMetaData().isReadOnly()) {
                        String value = attribute.getValue();
                        String serverAttributeId = attribute.getId().substring(WEBISSUES_ATTRIBUTE_KEY_PREFIX.length());
                        Attribute key = folder.getType().get(Integer.parseInt(serverAttributeId));
                        if (key == null) {
                            throw new Error("Invalid serverAttributeId " + serverAttributeId);
                        }
                        validateAttribute(repository, value, key);
                        newServerAttributeValues.put(key, value);
                    } else if (oldAttribute.getId().equals(TaskAttribute.SUMMARY)) {
                        newName = attribute.getValue();
                    } else if (oldAttribute.getId().equals(TaskAttribute.PRIORITY)) {
                        Attribute key = folder.getType().getByName("Priority");
                        validateAttribute(repository, attribute.getValue(), key);
                        newServerAttributeValues.put(key, attribute.getValue());
                    }
                }
                int issueId = Integer.parseInt(taskData.getTaskId());
                
                // Move the issue first if need be
                if(newFolderId != -1) {
                    client.moveIssue(issueId, newFolderId, monitor);
                }

                // Update issue
                client.updateIssue(issueId, newName, newServerAttributeValues, monitor);

                // Add a comment if needed
                String newComment = getNewComment(taskData);
                if (!Util.isNullOrBlank(newComment)) {
                    IssueDetails details = client.getIssueDetails(issueId, monitor);
                    addComment(monitor, client, newComment, details);
                }
                return new RepositoryResponse(ResponseKind.TASK_UPDATED, taskData.getTaskId() + "");
            }
        } catch (OperationCanceledException e) {
            throw e;
        } catch (CoreException ce) {
            throw ce;
        } catch (Exception e) {
            e.printStackTrace();
            throw new CoreException(WebIssuesCorePlugin.toStatus(e, repository));
        }
    }

    private void addComment(IProgressMonitor monitor, WebIssuesClient client, String newComment, IssueDetails details)
                    throws IOException, CoreException {
        try {
            client.addComment(new Comment(details, newComment, client.getEnvironment().getOwnerUser()), monitor);
        } catch (ProtocolException pe) {
            throw new CoreException(new RepositoryStatus(RepositoryStatus.ERROR, WebIssuesCorePlugin.ID_PLUGIN, pe.getCode(),
                            "Invalid comment text."));
        }
    }

    private void validateAttribute(TaskRepository repository, String value, Attribute key) throws CoreException {
        if (key.isRequired() && Util.isNullOrBlank(value)) {
            throw new CoreException(RepositoryStatus.createStatus(repository.getRepositoryUrl(), IStatus.ERROR,
                WebIssuesCorePlugin.ID_PLUGIN, key.getName() + " is a required attribute."));
        }
        if (!Util.isNullOrBlank(value)) {
            if (key.getType().equals(Attribute.AttributeType.NUMERIC)) {
                if (key.getDecimalPlaces() > 0) {
                    try {
                        double i = Double.parseDouble(value);
                        int idx = value.indexOf('.');
                        if (idx != -1) {
                            if ((value.length() - idx - 1) > key.getDecimalPlaces()) {
                                throw new NumberFormatException();
                            }
                        }
                        if (i < key.getMinValue() || i > key.getMaxValue()) {
                            throw new NumberFormatException();
                        }
                    } catch (NumberFormatException nfe) {
                        throw new CoreException(RepositoryStatus.createStatus(
                            repository.getRepositoryUrl(),
                            IStatus.ERROR,
                            WebIssuesCorePlugin.ID_PLUGIN,
                            key.getName() + " must be a floating point number between " + key.getMinValue() + " and "
                                            + key.getMaxValue() + " with a maximum of " + key.getDecimalPlaces()
                                            + " decimal places"));
                    }

                } else {
                    try {
                        long i = Long.parseLong(value);
                        if (i < key.getMinValue() || i > key.getMaxValue()) {
                            throw new NumberFormatException();
                        }
                    } catch (NumberFormatException nfe) {
                        throw new CoreException(
                                        RepositoryStatus.createStatus(repository.getRepositoryUrl(), IStatus.ERROR,
                                            WebIssuesCorePlugin.ID_PLUGIN, key.getName() + " must be an integer number between "
                                                            + key.getMinValue() + " and " + key.getMaxValue()));
                    }
                }
            }
            if (key.getType().equals(Attribute.AttributeType.TEXT)) {
                if (value.length() > key.getMaxLength()) {
                    throw new CoreException(RepositoryStatus.createStatus(repository.getRepositoryUrl(), IStatus.ERROR,
                        WebIssuesCorePlugin.ID_PLUGIN, key.getName() + " exceeds the maximum length of " + key.getMaxLength()));

                }
            }
        }
    }

    public static Folder getFolder(TaskData taskData, WebIssuesClient client) {
        return client.getEnvironment()
                        .getProjects()
                        .getFolder(
                            Integer.parseInt(taskData.getRoot().getAttribute(WebIssuesAttribute.FOLDER.getTaskKey()).getValue()));
    }

    private String getNewComment(TaskData taskData) {
        String newComment = "";
        TaskAttribute newCommentAttribute = taskData.getRoot().getMappedAttribute(TaskAttribute.COMMENT_NEW);
        if (newCommentAttribute != null) {
            newComment = newCommentAttribute.getValue();
        }
        return newComment;
    }

    @Override
    public boolean initializeTaskData(TaskRepository repository, TaskData data, ITaskMapping initializationData,
                                      IProgressMonitor monitor) throws CoreException {
        try {
            WebIssuesClient client = connector.getClientManager().getClient(repository, monitor);
            createDefaultAttributes(data, client.getEnvironment(), null);
            return true;
        } catch (OperationCanceledException e) {
            throw e;
        } catch (Exception e) {
            throw new CoreException(WebIssuesCorePlugin.toStatus(e, repository));
        }
    }

    @Override
    public TaskAttributeMapper getAttributeMapper(TaskRepository taskRepository) {
        return new WebIssuesAttributeMapper(taskRepository);
    }

}
