package fr.cirad.security.dump;

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
	 * @return The module the process is operating on
	 */
	public String getModule();
	
	/**
	 * @return The status of the underlying process
	 */
	public ProcessStatus getStatus();
	
	/**
	 * @return Whether or not the process can be aborted
	 */
	public boolean isAbortable();
	
	/**
	 * @return A warning to give before attempting abort, may be null
	 */
	public String getAbortWarning();
	
	/**
	 * Abort the process
	 */
	public void abort();
}
