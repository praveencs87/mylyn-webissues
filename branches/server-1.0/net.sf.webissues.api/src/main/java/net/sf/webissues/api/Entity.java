package net.sf.webissues.api;

/**
 * All entities in WebIssues have a numeric ID and should implement this
 * interface
 */
public interface Entity {

    /**
     * Get the ID of the entity.
     * 
     * @return ID
     */
    int getId();
}
