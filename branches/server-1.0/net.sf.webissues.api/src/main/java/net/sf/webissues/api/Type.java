package net.sf.webissues.api;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;

public class Type extends HashMap<Integer, Attribute> implements Serializable {
    private static final long serialVersionUID = -2967172058163844285L;

    private int id;
    private String name;
    private final Types types;
    private Views views;

    protected Type(Types types, int id, String name) {
        super();
        this.types = types;
        this.id = id;
        this.name = name;
        views = new Views(types.getEnvironment());
    }

    /**
     * Create a new attrbiute
     * 
     * @param name name
     * @param definition definition
     * @return attribute
     * @throws IOException
     * @throws HttpException
     * @throws ProtocolException 
     */
    public Attribute createAttribute(String name, String definition) throws HttpException, IOException, ProtocolException {
        final Client client = getViews().getEnvironment().getClient();
        HttpMethod method = client.doCommand("ADD ATTRIBUTE " + getId() + " '" + Util.escape(name) + "' '" + Util.escape(definition) + "'");
        try {
            List<List<String>> response = client.readResponse(method.getResponseBodyAsStream());
            int id = Integer.parseInt(response.get(0).get(1));
            Attribute attr = new Attribute(this, id, name, definition);
            put(id, attr);
            return attr;
        } finally {
            method.releaseConnection();
        }
    }
    
    /**
     * Rename this type.
     * 
     * @throws IOException on any error
     * @throws ProtocolException 
     */
    public void rename(Operation operation, final String newName) throws IOException, ProtocolException {
        final Client client = getViews().getEnvironment().getClient();
        client.doCall(new Call<Boolean>() {
            public Boolean call() throws HttpException, IOException, ProtocolException {
                client.doCommand("RENAME TYPE " + id + " '" + Util.escape(newName) + "'");
                Type.this.name = newName;
                return true;
            }
        }, operation);
    }
    
    /**
     * Delete this type.
     * 
     * @throws IOException on any error
     * @throws ProtocolException 
     */
    public void delete(Operation operation) throws IOException, ProtocolException {
        final Client client = getViews().getEnvironment().getClient();
        client.doCall(new Call<Boolean>() {
            public Boolean call() throws HttpException, IOException, ProtocolException {
                client.doCommand("DELETE TYPE " + id);
                getTypes().remove(Type.this.getId());
                return true;
            }
        }, operation);
    }
    
    /**
     * Get the list of views for this type.
     * 
     * @retur views
     */
    public Views getViews() {
        return views;
    }

    /**
     * Get the parent list of types.
     * 
     * @return parent type list
     */
    public Types getTypes() {
        return types;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Type [id=" + id + ", name=" + name + ", views=" + views + ", toString()=" + super.toString()
                        + "]";
    }

    public Attribute getByName(String name) {
        for (Attribute attr : values()) {
            if (attr.getName().equals(name)) {
                return attr;
            }
        }
        return null;
    }
}
