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

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import net.sf.webissues.api.Attribute;
import net.sf.webissues.api.Client;
import net.sf.webissues.api.Folder;
import net.sf.webissues.api.Issue;
import net.sf.webissues.api.Project;
import net.sf.webissues.api.ProtocolException;
import net.sf.webissues.api.Util;

import org.apache.commons.httpclient.HttpException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.commons.net.Policy;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.TaskRepositoryLocationFactory;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskDataCollector;
import org.eclipse.mylyn.tasks.core.data.TaskMapper;
import org.eclipse.mylyn.tasks.core.sync.ISynchronizationSession;

/**
 * @author Steffen Pingel
 */
public class WebIssuesRepositoryConnector extends AbstractRepositoryConnector {

    private final static String CLIENT_LABEL = "WebIssues";
    public static final String TASK_KEY_UPDATE_DATE = "UpdateDate";

    public static int getBugId(String taskId) throws CoreException {
        try {
            return Integer.parseInt(taskId);
        } catch (NumberFormatException e) {
            throw new CoreException(new Status(IStatus.ERROR, WebIssuesCorePlugin.ID_PLUGIN, IStatus.OK, "Invalid ticket id: "
                            + taskId, e));
        }
    }

    static List<String> getAttributeValues(TaskData data, String attributeId) {
        TaskAttribute attribute = data.getRoot().getMappedAttribute(attributeId);
        if (attribute != null) {
            return attribute.getValues();
        } else {
            return Collections.emptyList();
        }
    }

    static String getAttributeValue(TaskData data, String attributeId) {
        TaskAttribute attribute = data.getRoot().getMappedAttribute(attributeId);
        if (attribute != null) {
            return attribute.getValue();
        } else {
            return "";
        }
    }

    private final WebIssuesAttachmentHandler attachmentHandler = new WebIssuesAttachmentHandler(this);

    private WebIssuesClientManager clientManager;

    private File repositoryConfigurationCacheFile;

    private final WebIssuesTaskDataHandler taskDataHandler = new WebIssuesTaskDataHandler(this);
    private TaskRepositoryLocationFactory taskRepositoryLocationFactory = new TaskRepositoryLocationFactory();

    public WebIssuesRepositoryConnector() {
        if (WebIssuesCorePlugin.getDefault() != null) {
            WebIssuesCorePlugin.getDefault().setConnector(this);
            IPath path = WebIssuesCorePlugin.getDefault().getCachePath();
            this.repositoryConfigurationCacheFile = path.toFile();
        }
    }

    public WebIssuesRepositoryConnector(File repositoryConfigurationCacheFile) {
        this.repositoryConfigurationCacheFile = repositoryConfigurationCacheFile;
    }

    @Override
    public boolean canCreateNewTask(TaskRepository repository) {
        return true;
    }

    @Override
    public boolean canCreateTaskFromKey(TaskRepository repository) {
        return true;
    }

    @Override
    public boolean canSynchronizeTask(TaskRepository taskRepository, ITask task) {
        return true;
    }

    @Override
    public WebIssuesAttachmentHandler getTaskAttachmentHandler() {
        return attachmentHandler;
    }

    public synchronized WebIssuesClientManager getClientManager() {
        if (clientManager == null) {
            clientManager = new WebIssuesClientManager(repositoryConfigurationCacheFile, taskRepositoryLocationFactory);
        }
        return clientManager;
    }

    @Override
    public String getConnectorKind() {
        return WebIssuesCorePlugin.CONNECTOR_KIND;
    }

    @Override
    public String getLabel() {
        return CLIENT_LABEL;
    }

    @Override
    public TaskData getTaskData(TaskRepository repository, String taskId, IProgressMonitor monitor) throws CoreException {
        return taskDataHandler.getTaskData(repository, taskId, monitor);
    }

    @Override
    public WebIssuesTaskDataHandler getTaskDataHandler() {
        return taskDataHandler;
    }

    @Override
    public String getRepositoryUrlFromTaskUrl(String url) {
        if (url == null) {
            return null;
        }
        int index = url.lastIndexOf("/");
        return index == -1 ? null : url.substring(0, index);
    }

    @Override
    public String getTaskIdFromTaskUrl(String url) {
        if (url == null) {
            return null;
        }
        String cmdStr = "command=";
        int index = url.indexOf(cmdStr);
        try {
            String cmd = URLDecoder.decode(index == -1 ? null : url.substring(index + cmdStr.length()), "UTF-8");
            return cmd.substring(cmd.indexOf(' ') + 1, cmd.lastIndexOf(' '));
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }

    @Override
    public String getTaskIdPrefix() {
        return "#";
    }

    public TaskRepositoryLocationFactory getTaskRepositoryLocationFactory() {
        return taskRepositoryLocationFactory;
    }

    @Override
    public String getTaskUrl(String repositoryUrl, String taskId) {
        try {
            return "?command=" + URLEncoder.encode("GET DETAILS " + taskId + " 0", "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }

    @Override
    public IStatus performQuery(TaskRepository repository, IRepositoryQuery query, TaskDataCollector resultCollector,
                                ISynchronizationSession session, IProgressMonitor monitor) {
        try {
            monitor.beginTask("Querying repository", IProgressMonitor.UNKNOWN);
            WebIssuesClient client;
            client = getClientManager().getClient(repository, monitor);
            WebIssuesFilterQueryAdapter search = new WebIssuesFilterQueryAdapter(query, client.getEnvironment());
            Map<String, ITask> taskById = null;
            for (Project project : client.getEnvironment().getProjects().values()) {
                for (Folder folder : project.values()) {
                    if (folder.getType().equals(search.getType())) {
                        taskById = doFolder(repository, resultCollector, session, monitor, client, search, taskById, folder);
                    } else {
                        System.out.println("    " + folder + " is not of type " + search.getType());
                    }
                }
            }
            return Status.OK_STATUS;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return WebIssuesCorePlugin.toStatus(ioe, repository);
        } catch (CoreException e) {
            e.printStackTrace();
            return WebIssuesCorePlugin.toStatus(e, repository);
        } catch (ProtocolException e) {
            e.printStackTrace();
            return WebIssuesCorePlugin.toStatus(e, repository);
        } finally {
            monitor.done();
        }
    }

    private Map<String, ITask> doFolder(TaskRepository repository, TaskDataCollector resultCollector,
                                        ISynchronizationSession session, IProgressMonitor monitor, WebIssuesClient client,
                                        WebIssuesFilterQueryAdapter search, Map<String, ITask> taskById, Folder folder)
                    throws HttpException, IOException, ProtocolException, CoreException {
        Collection<? extends Issue> folderIssues = client.getFolderIssues(folder, 0, monitor);
        System.out.println("Folder " + folder + " has + " + folderIssues.size() + " issues");
        for (Issue issue : folderIssues) {
            boolean matches = true;
            for (WebIssuesFilterCondition condition : search.getConditions()) {
                String val = condition.getValue();
                String name = condition.getName();
                if (name.equals(WebIssuesFilterCondition.PROJECT)) {
                    matches = match(issue, matches, condition, val, issue.getFolder().getProject().getName());
                } else if (name.equals(WebIssuesFilterCondition.FOLDER)) {
                    matches = match(issue, matches, condition, val, issue.getFolder().getName());
                } else if (name.equals(WebIssuesFilterCondition.NAME)) {
                    matches = match(issue, matches, condition, val, issue.getName());
                } else if (name.equals(WebIssuesFilterCondition.DATE_CREATED)) {
                    matches = match(issue, matches, condition, val, String
                                    .valueOf((issue.getCreatedDate().getTimeInMillis() / 1000)));
                } else if (name.equals(WebIssuesFilterCondition.DATE_MODIFIED)) {
                    matches = match(issue, matches, condition, val, String
                                    .valueOf((issue.getModifiedDate().getTimeInMillis() / 1000)));
                } else if (name.equals(WebIssuesFilterCondition.USER_CREATED)) {
                    matches = match(issue, matches, condition, val, issue.getCreatedUser().getLogin());
                } else if (name.equals(WebIssuesFilterCondition.USER_MODIFIED)) {
                    matches = match(issue, matches, condition, val, issue.getModifiedUser().getLogin());
                } else {
                    Attribute key = search.getType().getByName(name);
                    if (key == null) {
                        System.err.println("No attribute '" + name + "'");
                    } else {
                        String issueAttributeValue = Util.nonNull(issue.get(key));
                        try {
                            if (key.getType().equals(Attribute.Type.DATETIME)) {
                                issueAttributeValue = getDateValue(key.isDateOnly(), issueAttributeValue);
                            }
                            matches = match(issue, matches, condition, val, issueAttributeValue);
                        } catch (ParseException e) {
                            matches = false;
                        }
                    }
                    if (condition.isNegate()) {
                        matches = !matches;
                    }
                }
                if (!matches) {
                    break;
                }
            }

            if (matches) {
                System.out.println("    " + issue.getId() + " matches");
                TaskData taskData = taskDataHandler.createTaskDataFromIssue(client, repository, issue, monitor);
                taskData.setPartial(true);

                if (session != null && !session.isFullSynchronization()) {
                    if (taskById == null) {
                        taskById = new HashMap<String, ITask>();
                        for (ITask task : session.getTasks()) {
                            taskById.put(task.getTaskId(), task);
                        }
                    }
                    ITask task = taskById.get(String.valueOf(issue.getId()));
                    if (task != null && hasTaskChanged(repository, task, taskData)) {
                        session.markStale(task);
                    }
                }
                resultCollector.accept(taskData);
            } else {
                System.out.println("    " + issue.getId() + " does not match");
            }
        }
        return taskById;
    }

    private String getDateValue(boolean dateOnly, String issueAttributeValue) throws ParseException {
        DateFormat fmt = new SimpleDateFormat(dateOnly ? Client.DATEONLY_FORMAT : Client.DATETIME_FORMAT);
        return String.valueOf(fmt.parse(issueAttributeValue).getTime() / 1000);
    }

    private boolean match(Issue issue, boolean matches, WebIssuesFilterCondition condition, String val, String issueAttributeValue) {
        switch (condition.getType()) {
            case IS_EMPTY:
                matches = Util.isNullOrBlank(issueAttributeValue);
            case IS_EQUAL_TO:
                matches = issueAttributeValue.equals(val);
                break;
            case IS_IN:
                if (val.indexOf("-") != -1) {
                    // Range
                    String[] args = val.split("-");
                    double start = Double.parseDouble(args[0]);
                    double end = Double.parseDouble(args[1]);
                    try {
                        double thisVal = Double.parseDouble(issueAttributeValue);
                        matches = thisVal >= start && thisVal <= end;
                    } catch (NumberFormatException nfe) {
                        matches = false;
                    }
                } else {
                    // Enum
                    matches = Arrays.asList(val.split(":")).contains(issueAttributeValue);
                }
        }
        return matches;
    }

    @Override
    public void postSynchronization(ISynchronizationSession event, IProgressMonitor monitor) throws CoreException {
//        try {
//            monitor.beginTask("", 1);
//            if ((Util.isNullOrBlank(event.getTaskRepository().getSynchronizationTimeStamp()) || event.isFullSynchronization())
//                            && event.getStatus() == null) {
//                event.getTaskRepository().setSynchronizationTimeStamp(getSynchronizationStamp(event));
//            }
//        } finally {
//            monitor.done();
//        }
    }

//    private String getSynchronizationStamp(ISynchronizationSession event) {
//        Calendar mostRecent = Util.parseDateTimeToCalendar(event.getTaskRepository().getSynchronizationTimeStamp());
//        for (ITask task : event.getChangedTasks()) {
//            Calendar taskModifiedDate = Util.toCalendar(task.getModificationDate());
//            if (taskModifiedDate != null && taskModifiedDate.after(mostRecent)) {
//                mostRecent = taskModifiedDate;
//            }
//        }
//        return mostRecent == null ? Calendar.getInstance() : mostRecent;
//    }

    @Override
    public void preSynchronization(ISynchronizationSession session, IProgressMonitor monitor) throws CoreException {
        monitor = Policy.monitorFor(monitor);
        monitor.beginTask("Getting changed tasks", IProgressMonitor.UNKNOWN);
        session.setNeedsPerformQueries(true);
        if (!session.isFullSynchronization()) {
            return;
        }

        TaskRepository repository = session.getTaskRepository();
        try {
            Calendar now = Calendar.getInstance(TimeZone.getTimeZone(repository.getTimeZoneId()));
            String synchronizationStamp = repository.getSynchronizationTimeStamp();
            System.out.println("Last sync stamp " + synchronizationStamp);
            if (Util.isNullOrBlank(synchronizationStamp)) {
                for (ITask task : session.getTasks()) {
                    session.markStale(task);
                }
                repository.setSynchronizationTimeStamp("0");
                return;
            }

            WebIssuesClient client = getClientManager().getClient(repository, monitor);
            long stampValue = 0;
            try {
                stampValue = Long.parseLong(synchronizationStamp);
            } catch(NumberFormatException nfe) {
                System.err.println("WARNING: Invalid stamp vale of " + synchronizationStamp + ", defaulting to 0");
            }
            List<Issue> issues = new ArrayList<Issue>(client.findIssues(stampValue, monitor));
            if (issues.isEmpty()) {
                // repository is unchanged
                session.setNeedsPerformQueries(false);
                return;
            }

            // Map the tasks
            HashMap<String, ITask> taskById = new HashMap<String, ITask>();
            for (ITask task : session.getTasks()) {
                taskById.put(task.getTaskId(), task);
            }

            // 
            boolean stale = false;
            for (Issue issue : issues) {
                ITask task = taskById.get(String.valueOf(issue.getId()));
                stampValue = Math.max(stampValue, issue.getStamp());
                if (task != null) {
                    if (issue.getModifiedDate() == null || issue.getModifiedDate().getTime().after(task.getModificationDate())) {
                        stale = true;
                        session.markStale(task);
                    }
                }
            }

            System.out.println("Setting sync stamp to " + stampValue);
            repository.setSynchronizationTimeStamp(String.valueOf(stampValue));
            if (!stale) {
                session.setNeedsPerformQueries(false);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new CoreException(WebIssuesCorePlugin.toStatus(e, repository));
        } finally {
            monitor.done();
        }
    }

    public synchronized void setTaskRepositoryLocationFactory(TaskRepositoryLocationFactory taskRepositoryLocationFactory) {
        this.taskRepositoryLocationFactory = taskRepositoryLocationFactory;
        if (this.clientManager != null) {
            clientManager.setTaskRepositoryLocationFactory(taskRepositoryLocationFactory);
        }
    }

    @Override
    public boolean hasLocalCompletionState(TaskRepository taskRepository, ITask task) {
        // TODO Auto-generated method stub
        return super.hasLocalCompletionState(taskRepository, task);
    }

    public void stop() {
        if (clientManager != null) {
            clientManager.writeCache();
        }
    }

    @Override
    public void updateRepositoryConfiguration(TaskRepository repository, IProgressMonitor monitor) throws CoreException {
        try {
            WebIssuesClient client = getClientManager().getClient(repository, monitor);
            client.updateAttributes(monitor, true);
        } catch (Exception e) {
            throw new CoreException(WebIssuesCorePlugin.toStatus(e, repository));
        }
    }

    @Override
    public void updateTaskFromTaskData(TaskRepository taskRepository, ITask task, TaskData taskData) {
        TaskMapper mapper = getTaskMapping(taskData);
        mapper.applyTo(task);
        try {
            task.setUrl(Util.concatenateUri(taskRepository.getRepositoryUrl(), "?" + "command="
                            + URLEncoder.encode("GET DETAILS " + task.getTaskId() + " 0", "UTF-8")));
            Date date = task.getModificationDate();
            task.setAttribute(TASK_KEY_UPDATE_DATE, (date != null) ? Util.formatTimestamp(date) + "" : null);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    @Override
    public boolean hasTaskChanged(TaskRepository taskRepository, ITask task, TaskData taskData) {
        TaskMapper mapper = getTaskMapping(taskData);
        if (taskData.isPartial()) {
            boolean changes = mapper.hasChanges(task);
            if (changes) {
                return true;
            }
        } else {
            Calendar repositoryDate = Util.toCalendar(mapper.getModificationDate());
            Calendar localDate = Util.parseTimestamp(task.getAttribute(TASK_KEY_UPDATE_DATE));
            if (repositoryDate != null && !repositoryDate.equals(localDate)) {
                return true;
            }
        }
        return false;
    }

    public TaskMapper getTaskMapping(TaskData taskData) {
        return new WebIssuesTaskMapper(taskData);
    }

}