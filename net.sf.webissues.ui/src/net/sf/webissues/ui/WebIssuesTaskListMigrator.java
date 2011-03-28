package net.sf.webissues.ui;

import java.util.HashSet;
import java.util.Set;

import net.sf.webissues.core.WebIssuesCorePlugin;

import org.eclipse.mylyn.tasks.core.AbstractTaskListMigrator;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.ITask;
import org.w3c.dom.Element;


public class WebIssuesTaskListMigrator extends AbstractTaskListMigrator {

	private static final String KEY_WEBISSUES = "WebIssues";
	private static final String KEY_WEBISSUES_TASK = KEY_WEBISSUES + KEY_TASK;
	private static final String KEY_WEBISSUES_QUERY = KEY_WEBISSUES + KEY_QUERY;

	@Override
	public String getConnectorKind() {
		return WebIssuesCorePlugin.CONNECTOR_KIND;
	}

	@Override
	public String getTaskElementName() {
		return KEY_WEBISSUES_TASK;
	}

	@Override
	public Set<String> getQueryElementNames() {
		Set<String> names = new HashSet<String>();
		names.add(KEY_WEBISSUES_QUERY);
		return names;
	}

	@Override
	public void migrateQuery(IRepositoryQuery query, Element element) {
		// nothing to do
	}

	@Override
	public void migrateTask(ITask task, Element element) {
	}

}
