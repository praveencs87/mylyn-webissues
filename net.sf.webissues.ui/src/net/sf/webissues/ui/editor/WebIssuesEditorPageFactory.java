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

package net.sf.webissues.ui.editor;

import net.sf.webissues.core.WebIssuesCorePlugin;

import org.eclipse.mylyn.internal.provisional.commons.ui.CommonImages;
import org.eclipse.mylyn.tasks.ui.ITasksUiConstants;
import org.eclipse.mylyn.tasks.ui.TasksUiImages;
import org.eclipse.mylyn.tasks.ui.TasksUiUtil;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPageFactory;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditor;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditorInput;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.forms.editor.FormPage;

/**
 * @author Steffen Pingel
 */
@SuppressWarnings("restriction")
public class WebIssuesEditorPageFactory extends AbstractTaskEditorPageFactory {

    @Override
    public boolean canCreatePageFor(TaskEditorInput input) {
        if (input.getTask().getConnectorKind().equals(WebIssuesCorePlugin.CONNECTOR_KIND)) {
            return true;
        } else if (TasksUiUtil.isOutgoingNewTask(input.getTask(), WebIssuesCorePlugin.CONNECTOR_KIND)) {
            return true;
        }
        return false;
    }

    @Override
    public FormPage createPage(TaskEditor parentEditor) {
        TaskEditorInput input = parentEditor.getTaskEditorInput();
        if (TasksUiUtil.isOutgoingNewTask(input.getTask(), WebIssuesCorePlugin.CONNECTOR_KIND)) {
            return new WebIssuesTaskEditorPage(parentEditor);
        } else {
            return new WebIssuesTaskEditorPage(parentEditor);
        }
    }

    @Override
    public String[] getConflictingIds(TaskEditorInput input) {
        if (!input.getTask().getConnectorKind().equals(WebIssuesCorePlugin.CONNECTOR_KIND)) {
            return new String[] { ITasksUiConstants.ID_PAGE_PLANNING };
        }
        return null;
    }

    @Override
    public Image getPageImage() {
        return CommonImages.getImage(TasksUiImages.REPOSITORY_SMALL);
    }

    @Override
    public String getPageText() {
        return "WebIssues";
    }

    @Override
    public int getPriority() {
        return PRIORITY_TASK;
    }

}
