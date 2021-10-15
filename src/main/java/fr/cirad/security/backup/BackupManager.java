package fr.cirad.security.backup;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import fr.cirad.security.base.IModuleManager;

@Component
public class BackupManager {
	private static final Logger LOG = Logger.getLogger(BackupManager.class);
	
	@Autowired private IModuleManager moduleManager;
	
	private Map<String, BackupProcess> m_processes = new TreeMap<String, BackupProcess>();
	
	public String startDumpProcess(String moduleName, Authentication authToken) {
		String processID = generateProcessID("dump", authToken);
		BackupProcess process = new BackupProcess(processID, moduleName, authToken);
		this.m_processes.put(processID, process);
		process.startDump();
		return processID;
	}
	
	public String startRestoreProcess(String moduleName, Authentication authToken) {
		String processID = generateProcessID("restore", authToken);
		BackupProcess process = new BackupProcess(processID, moduleName, authToken);
		this.m_processes.put(processID, process);
		process.startRestore();
		return processID;
	}
	
	public BackupProcess getProcess(String processID) {
		return m_processes.get(processID);
	}
	
	private String generateProcessID(String processType, Authentication authToken) {
		return "role_manager." + processType + "." + authToken.getName() + "." + System.currentTimeMillis();
	}
}
