package org.kuxa.telegrambotplatform.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * DTO for processing Webhooks from Sonarr.
 * API Documentation: https://sonarr.tv/docs/api/#/Webhook
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SonarrWebhookDTO {

    @JsonProperty("eventType")
    private String eventType; // "Grab", "Download", "EpisodeFileDelete", "SeriesDelete", "Test"

    @JsonProperty("series")
    private SeriesInfo series;

    @JsonProperty("episodes")
    private List<EpisodeInfo> episodes;

    @JsonProperty("release")
    private ReleaseInfo release;

    @JsonProperty("downloadId")
    private String downloadId;

    @JsonProperty("isUpgrade")
    private Boolean isUpgrade;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SeriesInfo {
        @JsonProperty("id")
        private Integer id;

        @JsonProperty("title")
        private String title;

        @JsonProperty("path")
        private String path;

        @JsonProperty("tvdbId")
        private Integer tvdbId;

        @JsonProperty("tvMazeId")
        private Integer tvMazeId;

        @JsonProperty("imdbId")
        private String imdbId;

        @JsonProperty("type")
        private String type;

        @JsonProperty("year")
        private Integer year;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EpisodeInfo {
        @JsonProperty("id")
        private Integer id;

        @JsonProperty("episodeNumber")
        private Integer episodeNumber;

        @JsonProperty("seasonNumber")
        private Integer seasonNumber;

        @JsonProperty("title")
        private String title;

        @JsonProperty("airDate")
        private String airDate;

        @JsonProperty("airDateUtc")
        private String airDateUtc;

        @JsonProperty("quality")
        private String quality;

        @JsonProperty("qualityVersion")
        private Integer qualityVersion;

        @JsonProperty("releaseGroup")
        private String releaseGroup;

        @JsonProperty("sceneName")
        private String sceneName;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReleaseInfo {
        @JsonProperty("quality")
        private String quality;

        @JsonProperty("qualityVersion")
        private Integer qualityVersion;

        @JsonProperty("releaseGroup")
        private String releaseGroup;

        @JsonProperty("releaseTitle")
        private String releaseTitle;

        @JsonProperty("indexer")
        private String indexer;

        @JsonProperty("size")
        private Long size;
    }

    // Helper methods
    public boolean isGrabEvent() {
        return "Grab".equalsIgnoreCase(eventType);
    }

    public boolean isDownloadEvent() {
        return "Download".equalsIgnoreCase(eventType);
    }

    public boolean isTestEvent() {
        return "Test".equalsIgnoreCase(eventType);
    }

    public String getSeriesTitle() {
        return series != null ? series.getTitle() : "Unknown Series";
    }

    public String getEpisodesDescription() {
        if (episodes == null || episodes.isEmpty()) {
            return "Unknown Episode";
        }

        if (episodes.size() == 1) {
            EpisodeInfo ep = episodes.get(0);
            return String.format("S%02dE%02d - %s",
                ep.getSeasonNumber(), ep.getEpisodeNumber(), ep.getTitle());
        }

        // Multiple episodes (batch)
        return String.format("%d episodes", episodes.size());
    }

    public String getQualityProfile() {
        if (release != null && release.getQuality() != null) {
            return release.getQuality();
        }
        if (episodes != null && !episodes.isEmpty() && episodes.get(0).getQuality() != null) {
            return episodes.get(0).getQuality();
        }
        return "Unknown";
    }

    public String getReleaseGroupName() {
        if (release != null && release.getReleaseGroup() != null) {
            return release.getReleaseGroup();
        }
        if (episodes != null && !episodes.isEmpty() && episodes.get(0).getReleaseGroup() != null) {
            return episodes.get(0).getReleaseGroup();
        }
        return "Unknown Group";
    }
}
