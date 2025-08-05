package com.example.lyrics_api_v2.model;

public class LyricsNotFound {
    private String message;
    private String respone;
    private int code;

    public LyricsNotFound() {
        this.message = "No lyrics were found.";
        this.respone = "404 - Not Found";
        this.code = 404;
    }

    public String getMessage() {
        return message;
    }

    public String getRespone() {
        return respone;
    }

    public int getCode() {
        return code;
    }
}
