package net.sf.webissues.api;

import java.io.Serializable;
import java.util.HashMap;

public class Type extends HashMap<Integer, Attribute> implements Serializable {
    private static final long serialVersionUID = -2967172058163844285L;

    private int id;
    private String name;
    private final Types types;

    protected Type(Types types, int id, String name) {
        super();
        this.types = types;
        this.id = id;
        this.name = name;
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
        return "Type [id=" + id + ", name=" + name + ",attributes=" + super.toString() + "]";
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
