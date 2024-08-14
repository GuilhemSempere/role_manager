package fr.cirad.manager.dump;

public interface IProcess {
    String getProcessID();
    String getModule();
    ProcessStatus getStatus();
    String getStatusMessage();
    boolean isAbortable();
    void abort();
}