package net.sf.webissues.core;


import org.eclipse.core.runtime.IProgressMonitor;
import org.webissues.api.Operation;

public class MonitorOperationAdapter implements Operation {

    private IProgressMonitor monitor;

    public MonitorOperationAdapter(IProgressMonitor monitor) {
        this.monitor = monitor;
    }

    public boolean isCancelled() {
        return monitor != null && monitor.isCanceled();
    }

    @Override
    public void beginJob(String name, int size) {
        monitor.beginTask(name, size);
    }

    @Override
    public void done() {
        monitor.done();
    }

    @Override
    public boolean isCanceled() {
        return monitor.isCanceled();
    }

    @Override
    public void progressed(int value) {
        monitor.worked(value);
    }

    @Override
    public void setCanceled(boolean cancelled) {
        monitor.setCanceled(cancelled);
    }

    @Override
    public void setName(String name) {
        monitor.setTaskName(name);
    }

}