package com.example.lyrics_api_v2.controller;

import com.example.lyrics_api_v2.model.LyricsNotFound;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class LyricsNotFoundController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<LyricsNotFound> handleError(HttpServletRequest request) {
        return ResponseEntity.status(404).body(new LyricsNotFound());
    }
}
