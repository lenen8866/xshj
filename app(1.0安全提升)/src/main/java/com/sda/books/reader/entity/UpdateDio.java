package com.sda.books.reader.entity;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;

public class UpdateDio {

    @JSONField(name = "platform")
    private String platform;
    @JSONField(name = "channel")
    private String channel;
    @JSONField(name = "current_version")
    private String currentVersion;
    @JSONField(name = "latest_version")
    private String latestVersion;
    @JSONField(name = "update_available")
    private Boolean updateAvailable;
    @JSONField(name = "mandatory")
    private Boolean mandatory;
    @JSONField(name = "assets")
    private AssetsDTO assets;
    @JSONField(name = "release_notes")
    private List<String> releaseNotes;
    @JSONField(name = "released_at")
    private String releasedAt;
    @JSONField(name = "server_time")
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
        @JSONField(name = "url")
        private String url;
        @JSONField(name = "size")
        private String size;
        @JSONField(name = "sha256")
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
