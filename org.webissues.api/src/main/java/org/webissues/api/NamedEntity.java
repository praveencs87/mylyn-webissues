package org.webissues.api;

/**
 * Many WebIssues entities have a 'name'. All objects that support this should
 * implement this interface.
 */
public interface NamedEntity extends Entity {

    /**
     * Get the entity name.
     * 
     * @return name
     */
    String getName();

}
