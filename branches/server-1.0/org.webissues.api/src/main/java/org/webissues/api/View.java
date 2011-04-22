package org.webissues.api;

import java.io.IOException;
import java.io.Serializable;

import org.apache.commons.httpclient.HttpException;

public class View extends EntityMap<Alert> implements Serializable {
    private static final long serialVersionUID = -2967172058163844285L;


    private int id;
    private String name;
    private IssueType type;
    private ViewDefinition definition;
    private boolean publicView;

    protected View(IssueType type, int id, String name) {
        super();
        this.type = type;
        this.id = id;
        this.name = name;
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
}
