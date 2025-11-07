package com.example.lyrics_api_v2.controller;

import com.example.lyrics_api_v2.model.ApiResponse;
import com.example.lyrics_api_v2.model.ApiVersion;
import com.example.lyrics_api_v2.service.LyricsService;
import org.springframework.http.HttpStatus;
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
            @RequestParam(required = false) String trackId,
            @RequestParam(required = false) String artist,
            @RequestParam(required = false) String translate
    ) {
        ResponseEntity<?> lyricsResponse = lyricsService.getLyrics(platform, title, trackId, artist, translate);
        Object body = lyricsResponse.getBody();
        HttpStatus status = (HttpStatus) lyricsResponse.getStatusCode();
        ApiVersion version = new ApiVersion();
        ApiResponse<Object> apiResponse = new ApiResponse<>(body, version);

        return new ResponseEntity<>(apiResponse, status);
    }
}
