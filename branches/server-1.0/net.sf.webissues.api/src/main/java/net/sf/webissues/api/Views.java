package net.sf.webissues.api;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;

public class Views extends HashMap<Integer, View> implements Serializable {

    private static final long serialVersionUID = 156091835846857151L;
    private final Environment environment;
    private IssueType type;

    protected Views(Environment environment, IssueType folderType) {
        super();
        this.environment = environment;
        this.type = folderType;
    }
    
    /**
     * Get the folder type this view is attached to.
     * 
     * @return folder type
     */
    public IssueType getType() {
        return type;
    }

    /**
     * Get the environment this list type belongs to.
     * 
     * @return environment
     */
    public Environment getEnvironment() {
        return environment;
    }

    /**
     * Get a {@link View} given its name, or <code>null</code> if no such type
     * exists.
     * 
     * @param name view name
     * @return view
     */
    public View getByName(String name) {
        for (View view : values()) {
            if (view.getName().equals(name)) {
                return view;
            }
        }
        return null;
    }

    /**
     * Add a view.
     * 
     * @param view view to add
     */
    public void add(View view) {
        put(view.getId(), view);        
    }

    public View createView(final String viewName, final boolean publicView, final ViewDefinition definition,
                           final Operation operation) throws HttpException, IOException, ProtocolException {
        return environment.getClient().doCall(new Call<View>() {
            public View call() throws HttpException, IOException, ProtocolException {
                operation.beginJob("Creating user", 1);
                try {

                    Client client = environment.getClient();
//                    public function addView( $typeId, $name, $definition, $isPublic )
                    HttpMethod method = client.doCommand("ADD VIEW " + getType().getId() + " '" +  Util.escape(viewName) + "' '" + Util.escape(definition.toDefinitionString()) + "' " + ( publicView ? 1 : 0));
                    try {
                        List<List<String>> response = client.readResponse(method.getResponseBodyAsStream());
                        View u  = new View(getType(), Integer.parseInt(response.get(0).get(1)), viewName);
                        definition.setView(u);
                        u.setPublicView(publicView);
                        u.setDefinition(definition);
                        add(u);
                        return u;
                    } finally {
                        method.releaseConnection();
                    }
                } finally {
                    operation.done();
                }
            }
        }, operation);
        
    }
}
