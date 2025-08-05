package com.example.lyrics_api_v2.service;

import com.example.lyrics_api_v2.model.Lyrics;
import com.example.lyrics_api_v2.service.platform.Musixmatch;
import org.springframework.stereotype.Service;

@Service
public class LyricsService {

    private final Musixmatch musixmatch;

    public LyricsService(Musixmatch musixmatch) {
        this.musixmatch = musixmatch;
    }

    public Lyrics getLyrics(String platform, String title, String artist) {
        switch (platform.toLowerCase()) {
            case "musixmatch":
                return musixmatch.fetchLyrics(title, artist);
            default:
                throw new IllegalArgumentException("Unsupported platform: " + platform);
        }
    }

    // Optional fallback logic (for future platforms like Genius, YouTube)
    public Lyrics getLyricsWithFallback(String title, String artist) {
        try {
            return musixmatch.fetchLyrics(title, artist);
        } catch (RuntimeException ex) {
            // Future: try Genius, YouTube here
            throw ex; // For now, just rethrow
        }
    }
}
