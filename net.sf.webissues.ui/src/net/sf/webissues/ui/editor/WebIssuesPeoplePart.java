/*******************************************************************************
 * Copyright (c) 2004, 2008 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package net.sf.webissues.ui.editor;

import net.sf.webissues.core.WebIssuesAttribute;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.ui.editors.AbstractAttributeEditor;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * @author Rob Elves
 */
public class WebIssuesPeoplePart extends AbstractTaskEditorPart {

    private static final int COLUMN_MARGIN = 5;

    public WebIssuesPeoplePart() {
        setPartName("People");
    }

    private void addAttribute(Composite composite, FormToolkit toolkit, TaskAttribute attribute) {
        AbstractAttributeEditor editor = createAttributeEditor(attribute);
        if (editor != null) {
            editor.createLabelControl(composite, toolkit);
            GridDataFactory.defaultsFor(editor.getLabelControl()).indent(COLUMN_MARGIN, 0).applyTo(editor.getLabelControl());
            editor.createControl(composite, toolkit);
            getTaskEditorPage().getAttributeEditorToolkit().adapt(editor);
            GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.TOP).hint(130, SWT.DEFAULT).applyTo(
                editor.getControl());
        }
    }

    @Override
    public void createControl(Composite parent, FormToolkit toolkit) {
        Section section = createSection(parent, toolkit, true);
        Composite peopleComposite = toolkit.createComposite(section);
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 5;
        peopleComposite.setLayout(layout);

        for(TaskAttribute attr : getTaskData().getRoot().getAttributes().values()) {
            if(attr.getMetaData().getType().equals(TaskAttribute.TYPE_PERSON)) {
                addAttribute(peopleComposite, toolkit, attr);
            }
        }

        toolkit.paintBordersFor(peopleComposite);
        section.setClient(peopleComposite);
        setSection(toolkit, section);
    }

    protected void addSelfToCC(Composite composite) {

        TaskRepository repository = this.getTaskEditorPage().getTaskRepository();

        if (repository.getUserName() == null) {
            return;
        }

        TaskAttribute root = getTaskData().getRoot();
        TaskAttribute modifier = root.getMappedAttribute(WebIssuesAttribute.MODIFIED_BY.getTaskKey());
        if (modifier != null && modifier.getValue().indexOf(repository.getUserName()) != -1) {
            return;
        }

        TaskAttribute creator = root.getMappedAttribute(WebIssuesAttribute.CREATED_BY.getTaskKey());
        if (creator != null && creator.getValue().indexOf(repository.getUserName()) != -1) {
            return;
        }

    }
}