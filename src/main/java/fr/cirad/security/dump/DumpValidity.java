package fr.cirad.security.dump;

public enum DumpValidity {
	/** The dump is valid and more recent than the last modification */
	VALID,
	
	/** The dump is older than the last modification */
	OUTDATED,
	
	/** An older dump has been restored, this one is thus in a different and probably undesirable state */
	DIVERGENT,
}
