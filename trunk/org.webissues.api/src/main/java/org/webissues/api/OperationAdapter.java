package org.webissues.api;

/**
 * Adapter implementation of an {@link Operation}. Sub classes can
 * override the bits they need (if any).
 */
public class OperationAdapter implements Operation {

    private boolean cancelled;

    public void beginJob(String name, int size) {
    }

    public void done() {
    }

    public boolean isCanceled() {
        return cancelled;
    }

    public void progressed(int value) {
    }

    public void setCanceled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public void setName(String name) {
    }

}
