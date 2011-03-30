package net.sf.webissues.api;

/**
 * All operations that require a connection to the server require than an
 * instance of this callback be provided as well. It may be used to set the
 * current progress of an operation, and allow the user to cancel long running
 * operations.
 */
public interface Operation {

    /**
     * Start a new operation.
     * 
     * @param name name of operation
     * @param size size of operation in units
     */
    void beginJob(String name, int size);

    /**
     * Operation is complete.
     */
    void done();

    /**
     * Get if the operation should be cancelled.
     * 
     * @return cancelled
     */
    boolean isCanceled();

    /**
     * Set whether to cancel the operation.
     * 
     * @param cancelled cancelled
     */
    void setCanceled(boolean cancelled);

    /**
     * Set the name of this operation.
     * 
     * @param name
     */
    void setName(String name);

    /**
     * The operation has progressed.
     * 
     * @param value number of work units done
     */
    void progressed(int value);
}
