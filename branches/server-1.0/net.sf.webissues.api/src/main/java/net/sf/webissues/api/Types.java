package net.sf.webissues.api;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;

public class Types extends HashMap<Integer, Type> implements Serializable {

    private static final long serialVersionUID = 156091835846857151L;
    private final Environment environment;

    protected Types(Environment environment) {
        super();
        this.environment = environment;
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
        Map<Integer, Type> typeMap = new HashMap<Integer, Type>();
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
                        Type type = typeMap.get(typeId);
                        if (type == null) {
                            throw new Error("Expected type before attribute");
                        }
                        type.put(attributeId, new Attribute(attributeId, response.get(3), response.get(4)));
                    } else if (response.get(0).equals("T")) {
                        int typeId = Integer.parseInt(response.get(1));
                        Type type = new Type(this, typeId, response.get(2));
                        typeMap.put(typeId, type);
                    } else {
                        Client.LOG.warn("Unexpected response \"" + response + "\"");
                    }
                }
                clear();
                for (Type type : typeMap.values()) {
                    put(type.getId(), type);
                }
            } finally {
                method.releaseConnection();
            }
        }

    }

    /**
     * Convenience to get an {@link Attribute} given its ID. All {@link Type}s
     * contained in this list will be searched.
     * 
     * @param attributeId attribute ID
     * @return attribute or <code>null</code> if no such attribute exists
     */
    public Attribute getAttribute(int attributeId) {
        for (Type type : this.values()) {
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
     * Get a {@link Type} given its name, or <code>null</code> if no such type
     * exists.
     * 
     * @param name type name
     * @return type
     */
    public Type getByName(String name) {
        for (Type type : values()) {
            if (type.getName().equals(name)) {
                return type;
            }
        }
        return null;
    }
}
