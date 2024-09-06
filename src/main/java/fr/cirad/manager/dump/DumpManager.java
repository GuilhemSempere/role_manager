package fr.cirad.manager.dump;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.cirad.manager.AbstractProcess;
import fr.cirad.manager.IModuleManager;

@Component
@EnableScheduling
public class DumpManager {
	private static final Logger LOG = Logger.getLogger(DumpManager.class);
	
	@Autowired private IModuleManager moduleManager;
	
	private Map<String, AbstractProcess> m_processes = new TreeMap<String, AbstractProcess>();
	
	public String startDumpProcess(String moduleName, String dumpName, String dumpDescription, String userName) {
		String processID = generateProcessID("dump", userName);
		AbstractProcess process = moduleManager.startDump(moduleName, dumpName, dumpDescription);
		process.setID(processID);
		this.m_processes.put(processID, process);
		return processID;
	}
	
	public String startRestoreProcess(String moduleName, String backupName, boolean drop, String userName) {
		String processID = generateProcessID("restore", userName);
		AbstractProcess process = moduleManager.startRestore(moduleName, backupName, drop);
		process.setID(processID);
		this.m_processes.put(processID, process);
		return processID;
	}
	
	public boolean abortProcess(String processID) {
		AbstractProcess process = m_processes.get(processID);
		if (process.isAbortable()) {
			process.abort();
			return true;
		} else {
			return false;
		}
	}
	
	public AbstractProcess getProcess(String processID) {
		return m_processes.get(processID);
	}
	
	public Map<String, AbstractProcess> getProcesses() {
		return Collections.unmodifiableMap(this.m_processes);
	}

	/**
	 * Clean old finished processes regularly
	 */
	@Scheduled(fixedRate = 86400000)
	public void cleanupFinishedProcesses() {
		for (String processID : m_processes.keySet()) {
			AbstractProcess process = m_processes.get(processID);
			if (process.getStatus().isFinal()) {
				m_processes.remove(processID);
			}
		}
	}
	
	private String generateProcessID(String processType, String userName) {
		return processType + "::" + userName + "::" + System.currentTimeMillis();
	}
}
