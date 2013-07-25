package ece454.util;

import java.io.File;

public class Config {
    public static final int MAX_PEERS = 6;

    public static final String USER_HOME_DIRECTORY = System.getProperty("user.home");
    public static final String FILES_DIRECTORY_NAME = "dist-files";
    public static final File FILES_DIRECTORY = new File(USER_HOME_DIRECTORY, FILES_DIRECTORY_NAME);

    public static final int NUM_USABLE_CORES = Runtime.getRuntime().availableProcessors();
}
