package com.example.lyrics_api_v2.service;

import com.example.lyrics_api_v2.model.Lyrics;
import com.example.lyrics_api_v2.service.platform.Musixmatch;
import com.example.lyrics_api_v2.service.platform.YouTube;
import org.springframework.stereotype.Service;

@Service
public class LyricsService {

    private final Musixmatch musixmatch;
    private final YouTube youtube;

    public LyricsService(Musixmatch musixmatch, YouTube youtube) {
        this.musixmatch = musixmatch;
        this.youtube = youtube;
    }

    public Lyrics getLyrics(String platform, String title, String artist) {
        switch (platform.toLowerCase()) {
            case "musixmatch":
                return musixmatch.fetchLyrics(title, artist);
            case "youtube":
                return youtube.fetchLyrics(title, artist);
            default:
                throw new IllegalArgumentException("Unsupported platform: " + platform);
        }
    }
}
