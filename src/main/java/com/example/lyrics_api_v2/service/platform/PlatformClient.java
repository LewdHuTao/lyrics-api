package com.example.lyrics_api_v2.service.platform;

import com.example.lyrics_api_v2.model.Lyrics;

public interface PlatformClient {
    Lyrics fetchLyrics(String title, String artist);
}
