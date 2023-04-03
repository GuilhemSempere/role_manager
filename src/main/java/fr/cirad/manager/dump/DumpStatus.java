package fr.cirad.manager.dump;

public enum DumpStatus {
	/** The dump is valid and more recent than the last modification */
	VALID(4),

	/** The dump is older than the last modification */
	OUTDATED(3),

	/** An older dump has been restored, this one is thus in a different and probably undesirable state */
	DIVERGED(2),

	/** The module is busy (a dump/restore process is underway) */
	BUSY(1),

	/** No existing dump */
	NONE(0),

	/** No existing dump */
	UNSUPPORTED(-1);

	public int validity;

	private DumpStatus(int level) {
		validity = level;
	}
}
