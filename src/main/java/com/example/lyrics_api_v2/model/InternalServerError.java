package com.example.lyrics_api_v2.model;

public class InternalServerError {
    private String message;
    private String respone;

    public InternalServerError(String message) {
        this.message = message;
        this.respone = "500 Internal Server Error";
    }

    public String getMessage() {
        return message;
    }
    public String getRespone() {
        return respone;
    }
}
