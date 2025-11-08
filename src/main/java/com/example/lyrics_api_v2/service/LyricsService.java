package com.example.lyrics_api_v2.service;

import com.example.lyrics_api_v2.service.platform.Musixmatch;
import com.example.lyrics_api_v2.service.platform.YouTube;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class LyricsService {

    private final Musixmatch musixmatch;
    private final YouTube youtube;

    public LyricsService(Musixmatch musixmatch, YouTube youtube) {
        this.musixmatch = musixmatch;
        this.youtube = youtube;
    }

    public ResponseEntity<?> getLyrics(String platform, String trackId, String title, String artist, String translate) throws Exception {
        switch (platform.toLowerCase()) {
            case "musixmatch":
                return musixmatch.fetchLyrics(trackId, title, artist, translate);
            case "youtube":
                return youtube.fetchLyrics(trackId, title, artist, translate);
            default:
                throw new IllegalArgumentException("Unsupported platform: " + platform);
        }
    }
}
