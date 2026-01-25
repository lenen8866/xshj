package com.sda.books.reader.entity;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class UpdateDio {

    @SerializedName("platform")
    private String platform;
    @SerializedName("channel")
    private String channel;
    @SerializedName("current_version")
    private String currentVersion;
    @SerializedName("latest_version")
    private String latestVersion;
    @SerializedName("update_available")
    private Boolean updateAvailable;
    @SerializedName("mandatory")
    private Boolean mandatory;
    @SerializedName("assets")
    private AssetsDTO assets;
    @SerializedName("release_notes")
    private List<String> releaseNotes;
    @SerializedName("released_at")
    private String releasedAt;
    @SerializedName("server_time")
    private String serverTime;

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public void setLatestVersion(String latestVersion) {
        this.latestVersion = latestVersion;
    }

    public Boolean getUpdateAvailable() {
        return updateAvailable;
    }

    public void setUpdateAvailable(Boolean updateAvailable) {
        this.updateAvailable = updateAvailable;
    }

    public Boolean getMandatory() {
        return mandatory;
    }

    public void setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
    }

    public AssetsDTO getAssets() {
        return assets;
    }

    public void setAssets(AssetsDTO assets) {
        this.assets = assets;
    }

    public List<String> getReleaseNotes() {
        return releaseNotes;
    }

    public void setReleaseNotes(List<String> releaseNotes) {
        this.releaseNotes = releaseNotes;
    }

    public String getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(String releasedAt) {
        this.releasedAt = releasedAt;
    }

    public String getServerTime() {
        return serverTime;
    }

    public void setServerTime(String serverTime) {
        this.serverTime = serverTime;
    }

    public static class AssetsDTO {
        @SerializedName("url")
        private String url;
        @SerializedName("size")
        private String size;
        @SerializedName("sha256")
        private String sha256;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getSize() {
            return size;
        }

        public void setSize(String size) {
            this.size = size;
        }

        public String getSha256() {
            return sha256;
        }

        public void setSha256(String sha256) {
            this.sha256 = sha256;
        }
    }
}
