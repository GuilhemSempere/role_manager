package fr.cirad.manager.dump;

public abstract class AbstractProcess {
    protected String processID;
    protected String module;
    protected ProcessStatus status;
    protected String statusMessage;

    public String getProcessID() { return processID; }
    public String getModule() { return module; }
    public ProcessStatus getStatus() { return status; }
    public String getStatusMessage() { return statusMessage; }
}


