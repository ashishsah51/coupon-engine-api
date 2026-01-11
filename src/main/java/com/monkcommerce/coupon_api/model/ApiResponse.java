package com.monkcommerce.coupon_api.model;


import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private String error;
    private LocalDateTime timestamp;

    public ApiResponse(T data) {
        this.success = true;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponse(String error) {
        this.success = false;
        this.error = error;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and setters
    public boolean isSuccess() { return success; }
    public T getData() { return data; }
    public String getError() { return error; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
