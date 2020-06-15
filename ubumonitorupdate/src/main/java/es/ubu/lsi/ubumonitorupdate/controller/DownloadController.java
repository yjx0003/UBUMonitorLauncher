package es.ubu.lsi.ubumonitorupdate.controller;

import java.io.File;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.ubu.lsi.ubumonitorupdate.Loader;
import es.ubu.lsi.ubumonitorupdate.configuration.ConfigHelper;
import es.ubu.lsi.ubumonitorupdate.connection.Connection;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;

public class DownloadController {
	private static final Logger LOGGER = LoggerFactory.getLogger(DownloadController.class);
	@FXML
	private Label label;

	@FXML
	private ProgressIndicator progress;

	private Loader loader;
	private Pattern patternFile;
	private ZonedDateTime lastChecked;
	private String checkUrl;
	private String versionDir;
	private Set<String> versionsFiles;
	private ResourceBundle resourceBundle;

	public void init(Loader loader, String checkUrl, ZonedDateTime lastChecked, String versionDir) {
		this.loader = loader;
		this.resourceBundle = loader.getResourceBundle();
		this.lastChecked = lastChecked;
		this.checkUrl = checkUrl;
		this.versionDir = versionDir;
		String[] versionDirectory = new File(versionDir).list();
		versionsFiles = versionDirectory == null ? Collections.emptySet()
				: new HashSet<>(Arrays.asList(versionDirectory));

		createGetDownloadUrlService().start();
	}

	private Service<List<String>> createGetDownloadUrlService() {
		Service<List<String>> service = new Service<List<String>>() {

			@Override
			protected Task<List<String>> createTask() {
				return new Task<List<String>>() {

					@Override
					protected List<String> call() throws Exception {
						String url = null;

						// Check in the online configuration file what server download the files, GitHub
						// or own server
						LOGGER.info("Connecting to {}", checkUrl);
						try (Response response = Connection.getResponse(checkUrl)) {
							JSONObject jsonObject = new JSONObject(response.body()
									.string());
							url = jsonObject.getString("checkUrl");

							patternFile = Pattern.compile(jsonObject.getString("pattern"));

						}
						return getDownloadFile(url, patternFile, lastChecked, versionsFiles);

					}

				};
			}
		};
		service.setOnSucceeded(e -> {
			
			ConfigHelper.setProperty("lastUpdateCheck", ZonedDateTime.now(ZoneOffset.UTC));
			List<String> list = service.getValue();
			if (!list.isEmpty() && (loader.destFolderIsEmpty() || userConfirmation(list.get(1), list.get(2)))) {
				
				new File(versionDir).mkdirs();
				loader.getStage()
						.show();
				LOGGER.info("Downloading version {} in {}", list.get(0), list.get(1));
				createDownloadService(list.get(0), versionDir + list.get(1)).start();

			} else {
				onFinalize();
			}
		});
		service.setOnFailed(e -> onFinalize(e.getSource()
				.getException()));
		return service;
	}

	private boolean userConfirmation(String version, String body) {
		if (ConfigHelper.getProperty("askAgain", true)) {
			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/DownloadConfirmation.fxml"),
					resourceBundle);
			try {
				fxmlLoader.load();
			} catch (IOException e) {
				return false;
			}
			DownloadConfirmationController downloadConfirmationController = fxmlLoader.getController();
			boolean isUserConfirmed = downloadConfirmationController.isUserConfirmed(version, body);
			ConfigHelper.setProperty("askAgain", downloadConfirmationController.askAgain());
			ConfigHelper.setProperty("lastChoiceDownload", isUserConfirmed);
			LOGGER.info("User selected yes in confirmation download {}", isUserConfirmed);
			LOGGER.info("Ask again {}", downloadConfirmationController.askAgain());
			return isUserConfirmed;
		}
		return ConfigHelper.getProperty("lastChoiceDownload", true);

	}

	public List<String> getDownloadFile(String url, Pattern patternFile, ZonedDateTime lastChecked,
			Set<String> versionsFiles) throws IOException {

		try (Response response = Connection.getResponse(url)) {
			JSONObject jsonObject = new JSONObject(response.body()
					.string());

			if (!jsonObject.optBoolean("prerelease", false)) {
				JSONArray jsonArray = jsonObject.getJSONArray("assets");
				for (int i = 0; i < jsonArray.length(); ++i) {
					JSONObject asset = jsonArray.getJSONObject(i);
					String fileName = asset.getString("name");
					if (patternFile.matcher(fileName)
							.matches() && !versionsFiles.contains(fileName)
							&& (ZonedDateTime.parse(asset.getString("updated_at"))
									.isAfter(lastChecked) || versionsFiles.isEmpty())) {
						return Arrays.asList(asset.getString("browser_download_url"), fileName,
								jsonObject.getString("body"));
					}

				}
			}
		}
		return Collections.emptyList();
	}

	public Pattern getPatternFile() {
		return patternFile;
	}

	/**
	 * Download content of the url and save in file dest
	 * 
	 * @param downloadUrl file to download
	 * @param fileDest    path where save the file
	 * @return null
	 */
	private Service<Void> createDownloadService(String downloadUrl, String fileDest) {
		Service<Void> service = new Service<Void>() {

			@Override
			protected Task<Void> createTask() {
				return new Task<Void>() {

					@Override
					protected Void call() throws Exception {

						try (Response response = Connection.getResponse(downloadUrl);
								BufferedSink sink = Okio.buffer(Okio.sink(new File(fileDest)))) {
							sink.writeAll(response.body()
									.source());

						}

						return null;
					}
				};
			}

		};
		service.setOnSucceeded(e -> {
			ConfigHelper.setProperty("applicationPath", fileDest);
			onFinalize();
		});

		service.setOnFailed(e -> onFinalize(e.getSource()
				.getException()));

		return service;
	}

	private void onFinalize(Throwable exception) {
		LOGGER.error("Error:", exception);
		onFinalize();
	}

	private void onFinalize() {
		ConfigHelper.save();
		loader.executeFile();
		loader.close();
	}

}
