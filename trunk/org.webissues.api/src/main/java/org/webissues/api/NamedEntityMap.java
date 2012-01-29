package org.webissues.api;

import java.io.Serializable;

/**
 * Abstract implementation for lists of {@link Entity}.
 * 
 * @param <T> entity
 */
public abstract class NamedEntityMap<T extends NamedEntity> extends EntityMap<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Get a {@link T} given its name, or <code>null</code> if no such type
     * exists.
     * 
     * @param name view name
     * @return view
     */
    public T getByName(String name) {
        for (T entity : values()) {
            if (entity.getName().equals(name)) {
                return entity;
            }
        }
        return null;
    }

}
