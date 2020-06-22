package es.ubu.lsi.ubumonitorlauncher.model;

import java.time.ZonedDateTime;

public class DownloadInfo {

	private String releaseName;
	private String fileName;
	private String releaseDescription;
	private String downloadUrl;
	private ZonedDateTime updatedAt;

	public String getReleaseName() {
		return releaseName;
	}

	public String getFileName() {
		return fileName;
	}

	public String getReleaseDescription() {
		return releaseDescription;
	}

	public String getDownloadUrl() {
		return downloadUrl;
	}

	public ZonedDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setReleaseName(String releaseName) {
		this.releaseName = releaseName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public void setReleaseDescription(String releaseDescription) {
		this.releaseDescription = releaseDescription;
	}

	public void setDownloadUrl(String downloadUrl) {
		this.downloadUrl = downloadUrl;
	}

	public void setUpdatedAt(ZonedDateTime uploadedAt) {
		this.updatedAt = uploadedAt;
	}

	@Override
	public String toString() {
		return "DownloadInfo [releaseName=" + releaseName + ", fileName=" + fileName + ", releaseDescription="
				+ releaseDescription + ", downloadUrl=" + downloadUrl + ", updatedAt=" + updatedAt + "]";
	}
	
}
