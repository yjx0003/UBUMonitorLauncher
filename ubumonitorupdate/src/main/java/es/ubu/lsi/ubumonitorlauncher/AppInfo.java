package es.ubu.lsi.ubumonitorlauncher;

public class AppInfo {

	public static final String VERSION = "0.1-stable";
	public static final String APPLICATION_NAME = "UBUMonitor Launcher";
	private static final String APPLICATION_NAME_WITH_VERSION = APPLICATION_NAME + " " + VERSION;

	public static final String DEFAULT_VERSION_DIR = "versions/";
	public static final String CONFIGURATION_FILE = DEFAULT_VERSION_DIR + "configuration.json";
	public static final String DEFAULT_CHECK_URL = "https://raw.githubusercontent.com/yjx0003/UBUMonitor/master/autoUpdateConfig.json";

	public static final String ASK_AGAIN = "askAgain";

	public static final String LOGGER_FILE_APPENDER = "log/" + APPLICATION_NAME_WITH_VERSION + ".log";
	public static final String PATTERN_FILE = "^UBUMonitor.+.jar$";

	private AppInfo() {
		throw new UnsupportedOperationException();
	}
}
