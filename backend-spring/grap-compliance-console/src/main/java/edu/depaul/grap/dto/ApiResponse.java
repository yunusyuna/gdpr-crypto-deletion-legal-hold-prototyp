package edu.depaul.grap.dto;

public class ApiResponse<T> {
    public boolean ok;
    public T data;
    public String error;
    public String details;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.ok = true;
        r.data = data;
        return r;
    }

    public static <T> ApiResponse<T> err(String error, String details) {
        ApiResponse<T> r = new ApiResponse<>();
        r.ok = false;
        r.error = error;
        r.details = details;
        return r;
    }
}
