package net.sf.webissues.core;

import java.util.Calendar;

import org.eclipse.core.runtime.Assert;
import org.eclipse.mylyn.tasks.core.IRepositoryPerson;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;

public class WebIssuesChange {

    private IRepositoryPerson user;
    private Calendar date;
    private String oldValue;
    private String newValue;
    private String attributeName;
    private final ITask task;
    private final TaskAttribute taskAttribute;
    private final TaskRepository taskRepository;

    public WebIssuesChange(TaskRepository taskRepository, ITask task, TaskAttribute taskAttribute) {
        Assert.isNotNull(taskRepository);
        Assert.isNotNull(task);
        Assert.isNotNull(taskAttribute);
        this.taskRepository = taskRepository;
        this.task = task;
        this.taskAttribute = taskAttribute;
    }

    public IRepositoryPerson getUser() {
        return user;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public void setUser(IRepositoryPerson user) {
        this.user = user;
    }

    public Calendar getDate() {
        return date;
    }

    public void setDate(Calendar date) {
        this.date = date;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public ITask getTask() {
        return task;
    }

    public TaskAttribute getTaskAttribute() {
        return taskAttribute;
    }

    public TaskRepository getTaskRepository() {
        return taskRepository;
    }

}
