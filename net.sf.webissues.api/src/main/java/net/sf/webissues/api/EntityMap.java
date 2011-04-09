package net.sf.webissues.api;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * Abstract implementation for lists of {@link Entity}.
 * 
 * @param <T> entity
 */
public abstract class EntityMap<T extends Entity>  {

    private Map<Integer, T> entities = new TreeMap<Integer, T>();

    /**
     * Add an entity to this list.
     * 
     * @param entity entity to add
     */
    public void add(T entity) {
        entities.put(entity.getId(), entity);
    }

    /**
     * Remove an entity from this list.
     * 
     * @param entity entity to from
     */
    public void remove(T entity) {
        entities.remove(entity.getId());
    }

    /**
     * Get an entity given its ID.
     * 
     * @param id ID of entity
     * @return entity or <code>null</code> if no such entity exists.
     */
    public T get(int id) {
        return entities.get(id);
    }

    /**
     * Remove all entities from this list.
     */
    public void clear() {
        entities.clear();
    }

    /**
     * Get the number of entities in the map
     * 
     * @return size
     */
    public int size() {
        return entities.size();
    }

    /**
     * Get all of the entities in this list
     * 
     * @return entities
     */
    public Collection<T> values() {
        return entities.values();
    }

}
