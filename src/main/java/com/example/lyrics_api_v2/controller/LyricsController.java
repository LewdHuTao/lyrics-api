package com.example.lyrics_api_v2.controller;

import com.example.lyrics_api_v2.model.ApiResponse;
import com.example.lyrics_api_v2.model.ApiVersion;
import com.example.lyrics_api_v2.model.Lyrics;
import com.example.lyrics_api_v2.model.LyricsNotFound;
import com.example.lyrics_api_v2.service.LyricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/lyrics")
public class LyricsController {

    private final LyricsService lyricsService;

    public LyricsController(LyricsService lyricsService) {
        this.lyricsService = lyricsService;
    }

    @GetMapping
    public ResponseEntity<?> getLyrics(
            @RequestParam String platform,
            @RequestParam String title,
            @RequestParam(required = false) String artist
    ) {
        Lyrics lyrics = lyricsService.getLyrics(platform, title, artist);
        ApiVersion version = new ApiVersion();

        if (lyrics == null) {
            LyricsNotFound error = new LyricsNotFound();
            return ResponseEntity.status(404).body(new ApiResponse<>(error, version));
        }

        return ResponseEntity.ok(new ApiResponse<>(lyrics, version));
    }
}
