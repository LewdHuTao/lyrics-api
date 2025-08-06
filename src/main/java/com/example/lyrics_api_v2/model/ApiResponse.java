package com.example.lyrics_api_v2.model;

public class ApiResponse<T> {
    private T data;
    private T metadata;

    public ApiResponse(T data, T metadata) {
        this.data = data;
        this.metadata = metadata;
    }

    public T getMetadata() { return metadata; }
    public T getData() { return data; }
}
