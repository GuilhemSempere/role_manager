package fr.cirad.manager;

public abstract class AbstractProcess implements IBackgroundProcess {
    protected String processID;
    protected String module;
    protected ProcessStatus status;
    protected String statusMessage;

    @Override
    public String getProcessID() { return processID; }

    @Override
    public String getModule() { return module; }

    @Override
    public ProcessStatus getStatus() { return status; }

    @Override
    public String getStatusMessage() { return statusMessage; }

    @Override
    public boolean isAbortable() {
        return true;
    }

    @Override
    public void abort() {
        this.status = ProcessStatus.INTERRUPTED;
        this.statusMessage = "Process aborted";
    }

    public void setID(String processID) {
		this.processID = processID;
	}
}