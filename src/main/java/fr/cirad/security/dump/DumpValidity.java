package fr.cirad.security.dump;

public enum DumpValidity {
	/** The dump is valid and more recent than the last modification */
	VALID(3),
	
	/** The dump is older than the last modification */
	OUTDATED(2),
	
	/** An older dump has been restored, this one is thus in a different and probably undesirable state */
	UNWANTED(1),
	
	/** No existing dump */
	NONE(0);
	
	
	public int validity;
	
	private DumpValidity(int level) {
		validity = level;
	}
}
