package net.sf.webissues.api;

import java.io.IOException;
import java.io.Serializable;

import org.apache.commons.httpclient.HttpException;

public class View extends EntityMap<Alert> implements Serializable {
    private static final long serialVersionUID = -2967172058163844285L;


    private int id;
    private String name;
    private Views views;
    private ViewDefinition definition;
    private boolean userView;

    protected View(Views views, int id, String name) {
        super();
        this.views = views;
        this.id = id;
        this.name = name;
    }

    /**
     * Get the parent list of views.
     * 
     * @return parent view list
     */
    public Views getTypes() {
        return views;
    }

    public Views getViews() {
        return views;
    }

    public void setViews(Views views) {
        this.views = views;
    }

    public ViewDefinition getDefinition() {
        return definition;
    }

    public void setDefinition(ViewDefinition definition) {
        this.definition = definition;
    }

    public boolean isUserView() {
        return userView;
    }

    public void setUserView(boolean userView) {
        this.userView = userView;
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
        final Client client = getViews().getEnvironment().getClient();
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
        final Client client = getViews().getEnvironment().getClient();
        client.doCall(new Call<Boolean>() {
            public Boolean call() throws HttpException, IOException, ProtocolException {
                client.doCommand("DELETE VIEW " + id);
                getViews().remove(View.this);
                return true;
            }
        }, operation);
    }

    @Override
    public String toString() {
        return "View [id=" + id + ", name=" + name + ", definition=" + definition + ", userView=" + userView + ", alerts="
                        + super.values().toString() + "]";
    }
    /*
     * 
     * $query = 'SELECT view_id, type_id, view_name, view_def, ( CASE WHEN
     * user_id IS NULL THEN 1 ELSE 0 END ) AS is_public' . ' FROM {views}' . '
     * WHERE user_id = %d OR user_id IS NULL';
     */
}
