package fr.cirad.security.backup;

public enum ProcessStatus {
	INEXISTANT("inexistant"),
	IDLE("idle"), RUNNING("running"),
	SUCCESS("success"),
	ERROR("error"), INTERRUPTED("interrupted");
	
	public final String label;
	
	private ProcessStatus(String label) {
		this.label = label;
	}
}
