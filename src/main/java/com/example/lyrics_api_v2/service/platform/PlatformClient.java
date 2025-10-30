package com.example.lyrics_api_v2.service.platform;

import org.springframework.http.ResponseEntity;

public interface PlatformClient {
    ResponseEntity<?> fetchLyrics(String title, String artist, String langCode);
}
