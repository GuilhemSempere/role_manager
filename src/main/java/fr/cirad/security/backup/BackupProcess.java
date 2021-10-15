package fr.cirad.security.backup;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;

import fr.cirad.security.base.IModuleManager;


public class BackupProcess {
	
	public enum BackupStatus {
		IDLE("idle"), RUNNING("running"),
		SUCCESS("success"),
		ERROR("error"), INTERRUPTED("interrupted");
		
		public final String label;
		
		private BackupStatus(String label) {
			this.label = label;
		}
	}
	
	private static final Logger LOG = Logger.getLogger(BackupProcess.class);
	
	private static final String dumpManagementPath = "/WEB-INF/dump_management";
	private static final String backupDestinationFolder = dumpManagementPath + "/backups";
	private static final String dumpCommand = dumpManagementPath + "/dbDump.sh";
	private static final String restoreCommand = dumpManagementPath + "/dbRestore.sh";
	
	@Autowired private IModuleManager moduleManager;
	
	private String processID;
	private String moduleName;
	private Authentication authToken;
	private Process subprocess = null;
	
	private List<String> log;
	private BackupStatus status;
	private String statusMessage;
	
	public BackupProcess(String processID, String moduleName, Authentication authToken) {
		this.processID = processID;
		this.moduleName = moduleName;
		this.authToken = authToken;
		
		this.log = new ArrayList<String>();
		this.status = BackupStatus.IDLE;
		this.statusMessage = null;
	}
	
	public void startDump() {
		(new Thread() {
			public void run() {
				status = BackupStatus.RUNNING;
				for (int i = 0; i < 50; i++) {
					String logLine = "This is the log line no. " + i;
					log.add(logLine);
					
					try {
						Thread.sleep((long) (Math.random() * 1000));
					} catch (InterruptedException e) {
						status = BackupStatus.INTERRUPTED;
						statusMessage = e.toString();
						return;
					}
				}
				status = BackupStatus.SUCCESS;
			}
		}).start();
	}
	
	public void startRestore() {
		(new Thread() {
			public void run() {
				for (int i = 0; i < 50; i++) {
					String logLine = "This is the log line no. " + i;
					log.add(logLine);
					
					try {
						Thread.sleep((long) (Math.random() * 1000));
					} catch (InterruptedException e) {
						status = BackupStatus.INTERRUPTED;
						statusMessage = e.toString();
						return;
					}
				}
				status = BackupStatus.SUCCESS;
			}
		}).start();
	}
	
	public List<String> getLog(){
		return this.log;
	}
	
	public BackupStatus getStatus() {
		return this.status;
	}
	
	public String getStatusMessage() {
		return this.statusMessage;
	}
	

	public String getProcessID() {
		return processID;
	}

	public void setProcessID(String processID) {
		this.processID = processID;
	}

	public String getModuleName() {
		return moduleName;
	}

	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}

	public Authentication getAuthToken() {
		return authToken;
	}

	public void setAuthToken(Authentication authToken) {
		this.authToken = authToken;
	}
	
	
}
