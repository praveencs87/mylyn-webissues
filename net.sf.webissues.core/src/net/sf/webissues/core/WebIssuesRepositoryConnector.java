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
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.webissues.api.Access;
import net.sf.webissues.api.Attribute;
import net.sf.webissues.api.Client;
import net.sf.webissues.api.Condition;
import net.sf.webissues.api.Folder;
import net.sf.webissues.api.Issue;
import net.sf.webissues.api.Project;
import net.sf.webissues.api.ProtocolException;
import net.sf.webissues.api.Type;
import net.sf.webissues.api.Util;

import org.apache.commons.httpclient.HttpException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.commons.net.Policy;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.RepositoryStatus;
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

    final static Logger LOG = Logger.getLogger(WebIssuesRepositoryConnector.class.getName());

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
        if (url != null) {
            String cmdStr = "command=";
            int index = url.indexOf(cmdStr);
            try {
                if (index == -1) {
                    StringTokenizer t = new StringTokenizer(url, "?&");
                    while (t.hasMoreTokens()) {
                        String v = t.nextToken();
                        if (v.startsWith("issue=")) {
                            return v.substring(6);
                        }
                    }
                    // 1.0-alpha+
                } else {
                    String cmd = URLDecoder.decode(url.substring(index + cmdStr.length()), "UTF-8");
                    return cmd.substring(cmd.indexOf(' ') + 1, cmd.lastIndexOf(' '));
                }
            } catch (UnsupportedEncodingException e) {
                throw new Error(e);
            }
        }
        return null;
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
        // As from 1.0-alpha
        return "client/index.php?issue=" + taskId;

        // try {
        // }
        // catch(Exception e1) {
        // // Assume old format
        // try {
        // return "?command=" + URLEncoder.encode("GET DETAILS " + taskId +
        // " 0", "UTF-8");
        // } catch (UnsupportedEncodingException e) {
        // throw new Error(e);
        // }
        // }
    }

    @Override
    public IStatus performQuery(TaskRepository repository, IRepositoryQuery query, TaskDataCollector resultCollector,
                                ISynchronizationSession session, IProgressMonitor monitor) {
        List<Throwable> errors = new ArrayList<Throwable>();
        monitor.beginTask("Querying repository", IProgressMonitor.UNKNOWN);
        try {
            WebIssuesClient client;
            Map<String, ITask> taskById = null;
            int queries = 0;
            try {
                client = getClientManager().getClient(repository, monitor);
            } catch (IOException ioe) {
                LOG.log(Level.SEVERE, "IO Error.", ioe);
                return WebIssuesCorePlugin.toStatus(ioe, repository);
            } catch (ProtocolException e) {
                LOG.log(Level.SEVERE, "Protocol Error.", e);
                return WebIssuesCorePlugin.toStatus(e, repository);
            }

            // Let the query run on each project, only reporting errors at the
            // end
            for (Project project : client.getEnvironment().getProjects().values()) {
                for (Folder folder : project.values()) {
                    try {
                        queries++;
                        WebIssuesFilterQueryAdapter search = new WebIssuesFilterQueryAdapter(query, client.getEnvironment());
                        if (folder.getType().equals(search.getType())) {
                            taskById = doFolder(repository, resultCollector, session, monitor, client, search, taskById, folder);
                        } else {
                            LOG.warning("    " + folder + " is not of type " + search.getType());
                        }
                    } catch (IOException ioe) {
                        LOG.log(Level.SEVERE, "IO Error.", ioe);
                        return WebIssuesCorePlugin.toStatus(ioe, repository);
                    } catch (ProtocolException e) {
                        LOG.log(Level.SEVERE, "Protocol Error.", e);
                        errors.add(e);
                    } catch (CoreException e) {
                        LOG.log(Level.SEVERE, "Core Error.", e);
                        errors.add(e);
                    }
                }
            }

            if (errors.size() == 0) {
                return Status.OK_STATUS;
            } else if (errors.size() == 1) {
                return WebIssuesCorePlugin.toStatus(errors.get(0), repository);
            } else if (errors.size() == queries) {
                return WebIssuesCorePlugin.toStatus(new IOException("All queries failed."), repository);
            } else {
                return new RepositoryStatus(repository.getRepositoryUrl(), IStatus.WARNING, WebIssuesCorePlugin.ID_PLUGIN,
                                RepositoryStatus.ERROR_REPOSITORY, errors.size() + " out of " + queries + " failed.");
            }
        } finally {
            monitor.done();
        }
    }

    private Map<String, ITask> doFolder(TaskRepository repository, TaskDataCollector resultCollector,
                                        ISynchronizationSession session, IProgressMonitor monitor, WebIssuesClient client,
                                        WebIssuesFilterQueryAdapter search, Map<String, ITask> taskById, Folder folder)
                    throws HttpException, IOException, ProtocolException, CoreException {
        Collection<? extends Issue> folderIssues = client.getFolderIssues(folder, 0, monitor);
        for (Issue issue : folderIssues) {
            boolean matches = true;
            for (Condition condition : search.getAllConditions()) {
                String val = condition.getValue();
                Attribute attr = condition.getAttribute();
                try {
                    String issueAttributeValue = null;
                    switch (attr.getId()) {
                        case Type.PROJECT_ATTR_ID:
                            issueAttributeValue = issue.getFolder().getProject().getName();
                            break;
                        case Type.FOLDER_ATTR_ID:
                            issueAttributeValue = issue.getFolder().getName();
                            break;
                        case Type.NAME_ATTR_ID:
                            issueAttributeValue = issue.getName();
                            break;
                        case Type.CREATED_DATE_ATTR_ID:
                            issueAttributeValue = String.valueOf((issue.getCreatedDate().getTimeInMillis() / 1000));
                            break;
                        case Type.MODIFIED_DATE_ATTR_ID:
                            issueAttributeValue = String.valueOf((issue.getModifiedDate().getTimeInMillis() / 1000));
                            break;
                        case Type.CREATED_BY_ATTR_ID:
                            issueAttributeValue = issue.getCreatedUser().getLogin();
                            break;
                        case Type.MODIFIED_BY_ATTR_ID:
                            issueAttributeValue = issue.getCreatedUser().getLogin();
                            break;
                        default:
                            issueAttributeValue = issue.get(attr);
                    }
                    if (issueAttributeValue == null) {
                        issueAttributeValue = "";
                    }
                    matches = match(issue, matches, condition, val, issueAttributeValue);
                } catch (NumberFormatException nfe) {
                    nfe.printStackTrace(System.out);
                    matches = false;
                }
                if (!matches) {
                    break;
                }
            }

            if (matches) {
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
            }
        }
        return taskById;
    }

    private String getDateValue(boolean dateOnly, String issueAttributeValue) throws ParseException {
        DateFormat fmt = new SimpleDateFormat(dateOnly ? Client.DATEONLY_FORMAT : Client.DATETIME_FORMAT);
        return String.valueOf(fmt.parse(issueAttributeValue).getTime() / 1000);
    }

    private boolean match(Issue issue, boolean matches, Condition condition, String val, String issueAttributeValue) {
        val = val.toLowerCase();
        issueAttributeValue = issueAttributeValue.toLowerCase();
        switch (condition.getType()) {
            case BEG:
                matches = issueAttributeValue.startsWith(val);
                break;
            case END:
                matches = issueAttributeValue.endsWith(val);
                break;
            case CON:
                matches = issueAttributeValue.contains(val);
                break;
            case EQ:
                matches = issueAttributeValue.equals(val);
                break;
            case NEQ:
                matches = !issueAttributeValue.equals(val);
                break;
            case GT:
                matches = parseDouble(issueAttributeValue, condition) > parseDouble(val, condition);
                break;
            case GTE:
                matches = parseDouble(issueAttributeValue, condition) >= parseDouble(val, condition);
                break;
            case LT:
                matches = parseDouble(issueAttributeValue, condition) > parseDouble(val, condition);
                break;
            case LTE:
                matches = parseDouble(issueAttributeValue, condition) >= parseDouble(val, condition);
                break;
            case IN:
                matches = Arrays.asList(val.split(":")).contains(issueAttributeValue);
                break;
        }
        return matches;
    }

    private double parseDouble(String val, Condition condition) {
        return Double.parseDouble(val);
    }

    @Override
    public void postSynchronization(ISynchronizationSession event, IProgressMonitor monitor) throws CoreException {
        // try {
        // monitor.beginTask("", 1);
        // if
        // ((Util.isNullOrBlank(event.getTaskRepository().getSynchronizationTimeStamp())
        // || event.isFullSynchronization())
        // && event.getStatus() == null) {
        // event.getTaskRepository().setSynchronizationTimeStamp(getSynchronizationStamp(event));
        // }
        // } finally {
        // monitor.done();
        // }
    }

    // private String getSynchronizationStamp(ISynchronizationSession event) {
    // Calendar mostRecent =
    // Util.parseDateTimeToCalendar(event.getTaskRepository().getSynchronizationTimeStamp());
    // for (ITask task : event.getChangedTasks()) {
    // Calendar taskModifiedDate = Util.toCalendar(task.getModificationDate());
    // if (taskModifiedDate != null && taskModifiedDate.after(mostRecent)) {
    // mostRecent = taskModifiedDate;
    // }
    // }
    // return mostRecent == null ? Calendar.getInstance() : mostRecent;
    // }

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
            LOG.fine("Last sync stamp " + synchronizationStamp);
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
            } catch (NumberFormatException nfe) {
                LOG.warning("Invalid stamp vale of " + synchronizationStamp + ", defaulting to 0");
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

            LOG.fine("Setting sync stamp to " + stampValue);
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
            repository.setProperty("protocolVersion", client.getEnvironment().getVersion());
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
            String pv = taskRepository.getProperty("protocolVersion");
            if (pv != null && pv.startsWith("0.")) {
                task.setUrl(Util.concatenateUri(taskRepository.getRepositoryUrl(),
                    "?" + "command=" + URLEncoder.encode("GET DETAILS " + task.getTaskId() + " 0", "UTF-8")));
            } else {
                task.setUrl(Util.concatenateUri(taskRepository.getRepositoryUrl(), "/client/index.php?issue=" + task.getTaskId()));
            }
            Date date = task.getModificationDate();
            task.setAttribute(TASK_KEY_UPDATE_DATE, (date != null) ? Util.formatTimestamp(date) + "" : null);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    @Override
    public boolean canDeleteTask(TaskRepository repository, ITask task) {
        try {
            WebIssuesClient client = getClientManager().getClient(repository, new NullProgressMonitor());
            if (client.getEnvironment().getOwnerUser().getAccess().equals(Access.ADMIN)) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public IStatus deleteTask(TaskRepository repository, ITask task, IProgressMonitor monitor) throws CoreException {
        try {
            WebIssuesClient client = getClientManager().getClient(repository, monitor);
            client.deleteTask(task, monitor);
        } catch (Exception e) {
            Status status = new Status(IStatus.ERROR, WebIssuesCorePlugin.ID_PLUGIN, e.getLocalizedMessage(), e);
            throw new CoreException(status);
        }
        return Status.OK_STATUS;
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