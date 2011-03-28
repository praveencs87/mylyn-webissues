package net.sf.webissues.core;

import java.util.EnumSet;

import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskAttributeMapper;

public class WebIssuesAttributeMapper extends TaskAttributeMapper {

    public enum Flag {
        READ_ONLY, ATTRIBUTE, PEOPLE
    };

    public static final EnumSet<Flag> NO_FLAGS = EnumSet.noneOf(Flag.class);

    public WebIssuesAttributeMapper(TaskRepository taskRepository) {
        super(taskRepository);
    }

    public void updateChange(WebIssuesChange taskChange, TaskAttribute attribute) {
        WebIssuesChangeMapper.createFrom(attribute).applyTo(taskChange);
    }
}
