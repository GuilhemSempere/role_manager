package fr.cirad.manager.dump;

public class ImportProcess extends AbstractProcess {
    private String processID;
    private String module;
    private ProcessStatus status;
    private String statusMessage;

    public ImportProcess(String processID, String module) {
        this.processID = processID;
        this.module = module;
        this.status = ProcessStatus.IDLE;
        this.statusMessage = "Import process initialized";
    }

    @Override
    public String getProcessID() {
        return processID;
    }

    @Override
    public String getModule() {
        return module;
    }

    @Override
    public ProcessStatus getStatus() {
        return status;
    }

    @Override
    public String getStatusMessage() {
        return statusMessage;
    }

    @Override
    public void abort() {
        // Implémentez ici la logique pour interrompre le processus
        this.status = ProcessStatus.INTERRUPTED;
        this.statusMessage = "Import process aborted";
    }

    public void start() {
        this.status = ProcessStatus.RUNNING;
        this.statusMessage = "Import started";
    }

    public void complete() {
        this.status = ProcessStatus.SUCCESS;
        this.statusMessage = "Import completed successfully";
    }

    public void fail(String errorMessage) {
        this.status = ProcessStatus.ERROR;
        this.statusMessage = "Import failed: " + errorMessage;
    }
}
