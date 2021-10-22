package fr.cirad.security.backup;

public interface IBackgroundProcess {
	/**
	 * @return The current state of the process log
	 */
	public String getLog();
	
	/**
	 * @return A custom status message for the process, or null if not applicable
	 */
	public String getStatusMessage();
	
	/**
	 * @return The status of the underlying process
	 */
	public ProcessStatus getStatus();
	
	/**
	 * @return Whether or not the process can be aborted
	 */
	public boolean isAbortable();
	
	/**
	 * Abort the process
	 */
	public void abort();
}
