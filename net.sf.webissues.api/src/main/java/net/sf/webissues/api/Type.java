package net.sf.webissues.api;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import net.sf.webissues.api.Attribute.AttributeType;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;

public class Type extends HashMap<Integer, Attribute> implements Serializable {
    private static final long serialVersionUID = -2967172058163844285L;

    private int id;
    private String name;
    private final Types types;
    private Views views;

    public final static int PROJECT_ATTR_ID = 2147482645;
    public final static int FOLDER_ATTR_ID = 2147482646; 
    public final static int NAME_ATTR_ID = 2147482647; 
    public final static int ID_ATTR_ID = 2147482648;
    public final static int CREATED_DATE_ATTR_ID = 2147482649;
    public final static int CREATED_BY_ATTR_ID = 2147482650;
    public final static int MODIFIED_DATE_ATTR_ID = 2147482651;
    public final static int MODIFIED_BY_ATTR_ID = 2147482652;

    protected Type(Types types, int id, String name) {
        super();
        this.types = types;
        this.id = id;
        this.name = name;
        views = new Views(types.getEnvironment(), this);
        addAttribute(new Attribute(this, PROJECT_ATTR_ID, "Project", AttributeType.ENUM, true));
        addAttribute(new Attribute(this, FOLDER_ATTR_ID, "Folder", AttributeType.ENUM, true));
        addAttribute(new Attribute(this, NAME_ATTR_ID, "Name", AttributeType.TEXT, true));
        addAttribute(new Attribute(this, ID_ATTR_ID, "ID", AttributeType.NUMERIC, true));
        Attribute createdAttr = new Attribute(this, CREATED_DATE_ATTR_ID, "Created Date", AttributeType.DATETIME, true);
        createdAttr.setDateOnly(true);
        addAttribute(createdAttr);
        addAttribute(new Attribute(this, CREATED_BY_ATTR_ID, "Created By", AttributeType.USER, true));
        Attribute modifiedAttr = new Attribute(this, MODIFIED_DATE_ATTR_ID, "Modified Date", AttributeType.DATETIME, true);
        modifiedAttr.setDateOnly(true);
        addAttribute(modifiedAttr);
        addAttribute(new Attribute(this, MODIFIED_BY_ATTR_ID, "Modified By", AttributeType.USER, true));
    }
    
    public void addAttribute(Attribute attr) {
        put(attr.getId(), attr);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        Type other = (Type) obj;
        if (id != other.id)
            return false;
        return true;
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
            Attribute attr = new Attribute(this, id, name, definition, false);
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
