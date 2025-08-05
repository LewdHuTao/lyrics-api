package com.example.lyrics_api_v2.model;

public class Lyrics {
    private String artistName;
    private String trackName;
    private String trackId;
    private String searchEngine;
    private String artworkUrl;
    private String lyrics;

    public Lyrics(String artistName, String trackName, String trackId, String searchEngine, String artworkUrl, String lyrics) {
        this.artistName = artistName;
        this.trackName = trackName;
        this.trackId = trackId;
        this.searchEngine = searchEngine;
        this.artworkUrl = artworkUrl;
        this.lyrics = lyrics;
    }

    public String getArtistName() {
        return artistName;
    }

    public String getTrackName() {
        return trackName;
    }

    public String getTrackId() {
        return trackId;
    }

    public String getSearchEngine() {
        return searchEngine;
    }

    public String getArtworkUrl() {
        return artworkUrl;
    }

    public String getLyrics() {
        return lyrics;
    }
}
