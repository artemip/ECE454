package ece454.util;

import ece454.ece454;

public class Config {
	public static final int MAX_PEERS = 6;

	// Cheesy, but allows us to do a simple Status class
	public static final int MAX_FILES = 100;

    public static final String FILES_DIRECTORY = "./files";

    public static final int NUM_USABLE_CORES = Runtime.getRuntime().availableProcessors();
}
