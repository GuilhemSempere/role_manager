package fr.cirad.manager.dump;

public class ImportProcess extends AbstractProcess {
    public ImportProcess(String processID, String module) {
        this.processID = processID;
        this.module = module;
        this.status = ProcessStatus.IDLE;  // Par défaut
    }

    public void start() {
        this.status = ProcessStatus.RUNNING;
        // Logique pour démarrer le processus d'import
    }

    public void complete() {
        this.status = ProcessStatus.SUCCESS;
        // Logique pour compléter le processus d'import
    }

    public void fail(String errorMessage) {
        this.status = ProcessStatus.ERROR;
        this.statusMessage = errorMessage;
        // Logique en cas d'échec du processus d'import
    }
}
