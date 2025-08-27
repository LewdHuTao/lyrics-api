package com.example.lyrics_api_v2.model;

public class MusixmatchToken {
    private String message;
    private String response;
    private int code;

    public MusixmatchToken(String message, String response, int code) {
        this.message = message;
        this.response = response;
        this.code = code;
    }

    public String getMessage() {
        return message;
    }
    public String getRespone() {
        return response;
    }
    public int getCode() {
        return code;
    }
}
