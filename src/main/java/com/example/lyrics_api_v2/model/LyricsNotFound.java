package com.example.lyrics_api_v2.model;

public class LyricsNotFound {
    private String message;
    private String respone;

    public LyricsNotFound(String message) {
        this.message = message;
        this.respone = "404 Not Found";
    }

    public String getMessage() {
        return message;
    }
    public String getRespone() {
        return respone;
    }
}
