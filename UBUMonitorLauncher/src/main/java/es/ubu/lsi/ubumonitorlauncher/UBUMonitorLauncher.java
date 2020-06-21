package es.ubu.lsi.ubumonitorlauncher;

/**
 * UBUMonitorUpdate launcher: This just starts UBUMonitorUpdateMain because name
 * consistent as other java applications.
 */
public class UBUMonitorLauncher {
	public static void main(String[] args) {
		System.setProperty("logfile.name", AppInfo.LOGGER_FILE_APPENDER);
		Loader.initialize(args);
	}

}
