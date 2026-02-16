package org.kuxa.telegrambotplatform.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * DTO for processing Webhooks from Radarr.
 * API Documentation: https://radarr.video/docs/api/#/Webhook
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RadarrWebhookDTO {

    @JsonProperty("eventType")
    private String eventType; // "Grab", "Download", "Rename", "MovieDelete", "Test"

    @JsonProperty("movie")
    private MovieInfo movie;

    @JsonProperty("remoteMovie")
    private RemoteMovieInfo remoteMovie;

    @JsonProperty("release")
    private ReleaseInfo release;

    @JsonProperty("downloadId")
    private String downloadId;

    @JsonProperty("isUpgrade")
    private Boolean isUpgrade;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MovieInfo {
        @JsonProperty("id")
        private Integer id;

        @JsonProperty("title")
        private String title;

        @JsonProperty("year")
        private Integer year;

        @JsonProperty("releaseDate")
        private String releaseDate;

        @JsonProperty("folderPath")
        private String folderPath;

        @JsonProperty("tmdbId")
        private Integer tmdbId;

        @JsonProperty("imdbId")
        private String imdbId;

        @JsonProperty("overview")
        private String overview;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RemoteMovieInfo {
        @JsonProperty("tmdbId")
        private Integer tmdbId;

        @JsonProperty("imdbId")
        private String imdbId;

        @JsonProperty("title")
        private String title;

        @JsonProperty("year")
        private Integer year;
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

    public String getMovieTitle() {
        if (movie != null) {
            return String.format("%s (%d)", movie.getTitle(), movie.getYear());
        }
        if (remoteMovie != null) {
            return String.format("%s (%d)", remoteMovie.getTitle(), remoteMovie.getYear());
        }
        return "Unknown Movie";
    }

    public String getQualityProfile() {
        return release != null ? release.getQuality() : "Unknown";
    }

    public String getReleaseGroupName() {
        return release != null && release.getReleaseGroup() != null
            ? release.getReleaseGroup()
            : "Unknown Group";
    }
}
