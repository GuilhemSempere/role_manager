package fr.cirad.manager.dump;

import java.util.Date;

public class DumpMetadata {

	/** Name of the dump */
	private String name;
	
	/** Date of creation of the dump */
	private Date creationDate;

	/** Dump filesize in Mb*/
    private float fileSizeMb;
    
	/** Description of the dump */
	private String description;
	
	/** Implementation-specific identifier to the dump (typically a file path) */
	private String identifier;
	
	/** Validity of the dump */
	private DumpValidity validity;
	

	public DumpMetadata(String sIdentifier, String sName, Date creation, float nFileSizeMb, String sDescription, DumpValidity dumpValidity) {
		identifier = sIdentifier;
		name = sName;
		creationDate = creation;
		fileSizeMb = nFileSizeMb;
		description = sDescription;
		validity = dumpValidity;
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

	public float getFileSizeMb() {
        return fileSizeMb;
    }

    public void setFileSizeMb(float fileSizeMb) {
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
