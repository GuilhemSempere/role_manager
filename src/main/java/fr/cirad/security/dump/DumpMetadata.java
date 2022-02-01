package fr.cirad.security.dump;

import java.util.Date;

public class DumpMetadata {
	/** Name of the dumped module */
	private String module;
	
	/** Name of the dump */
	private String name;
	
	/** Date of creation of the dump */
	private Date creationDate;

	/** Dump filesize in Mb*/
    private int fileSizeMb;
    
	/** Description of the dump */
	private String description;
	
	/** Implementation-specific identifier to the dump (typically a file path) */
	private String identifier;
	
	/** Validity of the dump */
	private DumpValidity validity;
	

	public DumpMetadata(String sIdentifier, String sModule, String sName, Date creation, int nFileSizeMb, String sDescription, DumpValidity dumpValidity) {
		identifier = sIdentifier;
		module = sModule;
		name = sName;
		creationDate = creation;
		fileSizeMb = nFileSizeMb;
		description = sDescription;
		validity = dumpValidity;
	}

	public String getModule() {
		return module;
	}

	public void setModule(String module) {
		this.module = module;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public int getFileSizeMb() {
        return fileSizeMb;
    }

    public void setFileSizeMb(int fileSizeMb) {
        this.fileSizeMb = fileSizeMb;
    }

    public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	
	public DumpValidity getValidity() {
		return validity;
	}

	public void setValidity(DumpValidity validity) {
		this.validity = validity;
	}
}
