package com.ak.essprogc.targets;

/** Operating Systems. */
public enum OperatingSystem {
	WINDOWS("win", ".bat"),
	UNIX("unix", ".sh"),
	POSIX_UNIX("posix", ".sh"),
	MAC("mac", ".sh"),
	UNKNOWN(null, null);

	public final String nickname;
	public final String scriptExt;

	OperatingSystem(String nickname, String scriptExt) {
		this.nickname = nickname;
		this.scriptExt = scriptExt;
	}
}