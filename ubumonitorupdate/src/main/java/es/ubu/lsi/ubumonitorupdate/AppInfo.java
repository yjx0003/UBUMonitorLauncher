package es.ubu.lsi.ubumonitorupdate;

public class AppInfo {

	public static final String CONFIGURATION_FILE = "configuration.json";
	public static final String DEFAULT_VERSION_DIR = "./versions/";
	public static final String DEFAULT_CHECK_URL = "https://raw.githubusercontent.com/yjx0003/UBUMonitor/master/autoUpdateConfig.json";

	private AppInfo() {
		throw new UnsupportedOperationException();
	}
}
