package fr.cirad.manager.dump;

public enum ProcessStatus {
	INEXISTENT("inexistent"),
	IDLE("idle"), RUNNING("running"),
	SUCCESS("success"),
	ERROR("error"), INTERRUPTED("interrupted");
	
	public final String label;
	
	private ProcessStatus(String label) {
		this.label = label;
	}
	
	public boolean isFinal() {
		return !(this.equals(IDLE) || this.equals(RUNNING));
	}
}
