package fr.cirad.security.dump;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import fr.cirad.security.base.IModuleManager;

@Component
public class DumpManager {
	private static final Logger LOG = Logger.getLogger(DumpManager.class);
	
	@Autowired private IModuleManager moduleManager;
	
	private Map<String, IBackgroundProcess> m_processes = new TreeMap<String, IBackgroundProcess>();
	
	public String startDumpProcess(String moduleName, String dumpName, String dumpDescription, Authentication authToken) {
		String processID = generateProcessID("dump", authToken);
		
		IBackgroundProcess process = moduleManager.startDump(moduleName, dumpName, dumpDescription);
		this.m_processes.put(processID, process);
		return processID;
	}
	
	public String startRestoreProcess(String moduleName, String backupName, boolean drop, Authentication authToken) {
		String processID = generateProcessID("restore", authToken);
		IBackgroundProcess process = moduleManager.startRestore(moduleName, backupName, drop);
		this.m_processes.put(processID, process);
		return processID;
	}
	
	public boolean abortProcess(String processID) {
		IBackgroundProcess process = m_processes.get(processID);
		if (process.isAbortable()) {
			process.abort();
			return true;
		} else {
			return false;
		}
	}
	
	public IBackgroundProcess getProcess(String processID) {
		return m_processes.get(processID);
	}
	
	public Map<String, IBackgroundProcess> getProcesses() {
		return Collections.unmodifiableMap(this.m_processes);
	}

	/**
	 * Clean old finished processes regularly
	 */
	@Scheduled(fixedRate = 86400000)
	public void cleanupFinishedProcesses() {
		for (String processID : m_processes.keySet()) {
			IBackgroundProcess process = m_processes.get(processID);
			if (process.getStatus().isFinal()) {
				m_processes.remove(processID);
			}
		}
	}
	
	private String generateProcessID(String processType, Authentication authToken) {
		return "role_manager." + processType + "." + authToken.getName() + "." + System.currentTimeMillis();
	}
}
