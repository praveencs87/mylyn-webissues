package org.webissues.api;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;

public class IssueTypes extends HashMap<Integer, IssueType> implements Serializable {

    private static final long serialVersionUID = 156091835846857151L;
    private final IEnvironment environment;

    protected IssueTypes(IEnvironment environment) {
        super();
        this.environment = environment;
    }

    /**
     * Get the environment this list type belongs to.
     * 
     * @return environment
     */
    public IEnvironment getEnvironment() {
        return environment;
    }

    /**
     * Reload all types and attributes from the server.
     * 
     * @param operation operation call back
     * @throws HttpException on any HTTP error
     * @throws IOException on any I/O error
     * @throws ProtocolException on any protocol error
     */
    public void reload(final Operation operation) throws HttpException, IOException, ProtocolException {
        environment.getClient().doCall(new Call<Object>() {
            public Object call() throws HttpException, IOException, ProtocolException {
                operation.beginJob("Reloading types", 1);
                try {
                    doReload(operation);
                } finally {
                    operation.done();
                }
                return null;
            }
        }, operation);
    }

    protected void doReload(final Operation operation) throws HttpException, IOException, ProtocolException {
        Client client = environment.getClient();
        Map<Integer, IssueType> typeMap = new HashMap<Integer, IssueType>();
        for (int i = 0; i < 2; i++) {
            HttpMethod method = client.doCommand("LIST TYPES");
            try {
                for (List<String> response : client.readResponse(method.getResponseBodyAsStream())) {
                    if (operation.isCanceled()) {
                        throw new ProtocolException(ProtocolException.CANCELLED);
                    }
                    if (response.get(0).equals("A")) {
                        int attributeId = Integer.parseInt(response.get(1));
                        int typeId = Integer.parseInt(response.get(2));
                        IssueType type = typeMap.get(typeId);
                        if (type == null) {
                            throw new Error("Expected type before attribute");
                        }
                        type.put(attributeId, new Attribute(type, attributeId, response.get(3), response.get(4), false));
                    } else if (response.get(0).equals("T")) {
                        int typeId = Integer.parseInt(response.get(1));
                        IssueType type = new IssueType(this, typeId, response.get(2));
                        typeMap.put(typeId, type);
                    } else if (response.get(0).equals("V")) {
                        int viewId = Integer.parseInt(response.get(1));
                        int typeId = Integer.parseInt(response.get(2));
                        String viewName = response.get(3);
                        String definition = response.get(4);
                        boolean publicView = response.get(5).equals("1");
                        IssueType type = typeMap.get(typeId);
                        View view = new View(type, viewId, viewName);
                        ViewDefinition def;
                        try {
                            def = new ViewDefinition(view, definition, type);
                            view.setDefinition(def);
                            view.setPublicView(publicView);
                            type.getViews().add(view);
                        } catch (ParseException e) {
                            Client.LOG.error("Could not parse view definition '" + definition + "'.", e);
                        }
                    } else {
                        Client.LOG.warn("Unexpected response \"" + response + "\"");
                    }
                }
                clear();
                for (IssueType type : typeMap.values()) {
                    put(type.getId(), type);
                }
            } finally {
                method.releaseConnection();
            }
        }

    }

    /**
     * Convenience to get an {@link Attribute} given its ID. All {@link IssueType}s
     * contained in this list will be searched.
     * 
     * @param attributeId attribute ID
     * @return attribute or <code>null</code> if no such attribute exists
     */
    public Attribute getAttribute(int attributeId) {
        for (IssueType type : this.values()) {
            if (type.containsKey(attributeId)) {
                return type.get(attributeId);
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "Types [types=" + super.toString() + "]";
    }

    /**
     * Get a {@link IssueType} given its name, or <code>null</code> if no such type
     * exists.
     * 
     * @param name type name
     * @return type
     */
    public IssueType getByName(String name) {
        for (IssueType type : values()) {
            if (type.getName().equals(name)) {
                return type;
            }
        }
        return null;
    }
}
