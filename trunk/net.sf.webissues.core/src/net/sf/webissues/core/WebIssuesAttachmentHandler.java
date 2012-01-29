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

import java.io.InputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.mylyn.commons.net.Policy;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskAttachmentHandler;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskAttachmentSource;
import org.eclipse.mylyn.tasks.core.data.TaskAttachmentMapper;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.webissues.api.Attachment;
import org.webissues.api.Comment;
import org.webissues.api.IEnvironment;
import org.webissues.api.Issue;
import org.webissues.api.IssueDetails;
import org.webissues.api.User;
import org.webissues.api.Util;

/**
 * @author Steffen Pingel
 */
public class WebIssuesAttachmentHandler extends AbstractTaskAttachmentHandler {

    private final WebIssuesRepositoryConnector connector;

    public WebIssuesAttachmentHandler(WebIssuesRepositoryConnector connector) {
        this.connector = connector;
    }

    @Override
    public InputStream getContent(TaskRepository repository, ITask task, TaskAttribute attachmentAttribute, IProgressMonitor monitor)
                    throws CoreException {
        try {
            WebIssuesClient client = connector.getClientManager().getClient(repository, monitor);
            int attachmentId = Integer.parseInt(attachmentAttribute.getValue());
            return client.getAttachmentData(attachmentId, monitor);
        } catch (OperationCanceledException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new CoreException(WebIssuesCorePlugin.toStatus(e, repository));
        }
    }

    @Override
    public void postContent(TaskRepository repository, ITask task, AbstractTaskAttachmentSource source, String comment,
                            TaskAttribute attachmentAttribute, IProgressMonitor monitor) throws CoreException {
        String filename = source.getName();
        String description = source.getDescription();
        String user = null;
        long length = source.getLength();

        if (attachmentAttribute != null) {
            TaskAttachmentMapper mapper = TaskAttachmentMapper.createFrom(attachmentAttribute);
            if (mapper.getFileName() != null) {
                filename = mapper.getFileName();
            }
            if (mapper.getDescription() != null) {
                description = mapper.getDescription();
            }
            if (mapper.getLength() != null) {
                length = mapper.getLength();
            }
            if (mapper.getAuthor() != null) {
                user = mapper.getAuthor().getName();
            }
        }
        if (description == null) {
            description = "";
        }

        monitor = Policy.monitorFor(monitor);
        try {
            monitor.beginTask("Uploading attachment", IProgressMonitor.UNKNOWN);
            try {
                WebIssuesClient client = connector.getClientManager().getClient(repository, monitor);
                IssueDetails issueDetails = client.getIssueDetails(Integer.parseInt(task.getTaskId()), monitor);
                Issue issue = issueDetails.getIssue();
                IEnvironment environment = client.getEnvironment();
                User owner = user == null ? environment.getOwnerUser() : environment.getUsers().getByLogin(user);
                Attachment attachment = new Attachment(issue, owner, filename, description, length);
                InputStream inputStream = source.createInputStream(monitor);
                client.putAttachmentData(Integer.parseInt(task.getTaskId()), attachment, inputStream, attachment.getSize(),
                    "application/octet-stream", monitor);
                if (!Util.isNullOrBlank(comment)) {
                    client.addComment(new Comment(issue, comment, owner), monitor);
                }
            } catch (OperationCanceledException e) {
                throw e;
            } catch (Exception e) {
                throw new CoreException(WebIssuesCorePlugin.toStatus(e, repository));
            }
        } finally {
            monitor.done();
        }
    }

    @Override
    public boolean canGetContent(TaskRepository repository, ITask task) {
        return true;
    }

    @Override
    public boolean canPostContent(TaskRepository repository, ITask task) {
        return true;
    }

}
