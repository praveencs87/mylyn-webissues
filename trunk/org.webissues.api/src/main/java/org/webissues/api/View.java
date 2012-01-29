package org.webissues.api;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.httpclient.HttpException;

public class View extends EntityMap<Alert> implements Serializable {
    private static final long serialVersionUID = -2967172058163844285L;

    private int id;
    private String name;
    private IssueType type;
    private ViewDefinition definition;
    private boolean publicView;

    public View() {
        super();
    }

    public View(IssueType type, int id, String name) {
        super();
        this.type = type;
        this.id = id;
        this.name = name;
    }

    public void setType(IssueType type) {
        this.type = type;
    }

    public IssueType getType() {
        return type;
    }

    public ViewDefinition getDefinition() {
        return definition;
    }

    public void setDefinition(ViewDefinition definition) {
        this.definition = definition;
    }

    public boolean isPublicView() {
        return publicView;
    }

    public void setPublicView(boolean publicView) {
        this.publicView = publicView;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * Rename this view.
     * 
     * @throws IOException on any error
     * @throws ProtocolException
     */
    public void rename(Operation operation, final String newName) throws IOException, ProtocolException {
        final Client client = type.getViews().getEnvironment().getClient();
        client.doCall(new Call<Boolean>() {
            public Boolean call() throws HttpException, IOException, ProtocolException {
                client.doCommand("RENAME VIEW " + id + " '" + Util.escape(newName) + "'");
                View.this.name = newName;
                return true;
            }
        }, operation);
    }

    /**
     * Delete this view.
     * 
     * @throws IOException on any error
     * @throws ProtocolException
     */
    public void delete(Operation operation) throws IOException, ProtocolException {
        final Client client = type.getViews().getEnvironment().getClient();
        client.doCall(new Call<Boolean>() {
            public Boolean call() throws HttpException, IOException, ProtocolException {
                client.doCommand("DELETE VIEW " + id);
                type.getViews().remove(View.this.getId());
                return true;
            }
        }, operation);
    }

    /**
     * Query all supplied folders for issues last modified since the provided stamp and filter them
     * according to this view. The filtering happens client side. The {@link ResultCollector#found(Issue)}
     * method will be invoked for each issue found.
     * 
     * @param folders folders to query
     * @param operation operation
     * @param stamp list issues since
     * @param collector collector
     * @throws IOException
     * @throws ProtocolException
     */
    public void query(Collection<Folder> folders, Operation operation, long stamp, ResultCollector collector) throws IOException, ProtocolException {
        for(Folder folder : folders) {
            Collection<? extends Issue> folderIssues = folder.getIssues(operation, stamp);
            for (Issue issue : folderIssues) {
                boolean matches = true;
                for (Condition condition : definition) {
                    String val = condition.getValue();
                    Attribute attr = condition.getAttribute();
                    try {
                        String issueAttributeValue = null;
                        switch (attr.getId()) {
                            case IssueType.PROJECT_ATTR_ID:
                                issueAttributeValue = issue.getFolder().getProject().getName();
                                break;
                            case IssueType.FOLDER_ATTR_ID:
                                issueAttributeValue = issue.getFolder().getName();
                                break;
                            case IssueType.NAME_ATTR_ID:
                                issueAttributeValue = issue.getName();
                                break;
                            case IssueType.CREATED_DATE_ATTR_ID:
                                issueAttributeValue = String.valueOf((issue.getCreatedDate().getTimeInMillis() / 1000));
                                break;
                            case IssueType.MODIFIED_DATE_ATTR_ID:
                                issueAttributeValue = String.valueOf((issue.getModifiedDate().getTimeInMillis() / 1000));
                                break;
                            case IssueType.CREATED_BY_ATTR_ID:
                                issueAttributeValue = issue.getCreatedUser().getLogin();
                                break;
                            case IssueType.MODIFIED_BY_ATTR_ID:
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
                if(matches) {
                    collector.found(issue);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "View [id=" + id + ", name=" + name + ", definition=" + definition + ", publicView=" + publicView + ", alerts="
                        + super.values().toString() + "]";
    }

    public void publish(final boolean publicView, Operation operation) throws HttpException, IOException, ProtocolException {
        final Client client = type.getViews().getEnvironment().getClient();
        client.doCall(new Call<Boolean>() {
            public Boolean call() throws HttpException, IOException, ProtocolException {
                client.doCommand("PUBLISH VIEW " + id + " " + (publicView ? "1" : "0"));
                View.this.publicView = publicView;
                return true;
            }
        }, operation);

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
}
