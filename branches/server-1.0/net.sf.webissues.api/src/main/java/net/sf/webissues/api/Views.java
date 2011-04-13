package net.sf.webissues.api;

import java.io.Serializable;
import java.util.HashMap;

public class Views extends HashMap<Integer, View> implements Serializable {

    private static final long serialVersionUID = 156091835846857151L;
    private final Environment environment;
    private Type type;

    protected Views(Environment environment, Type folderType) {
        super();
        this.environment = environment;
        this.type = folderType;
    }
    
    /**
     * Get the folder type this view is attached to.
     * 
     * @return folder type
     */
    public Type getType() {
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
}
