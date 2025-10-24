package com.devcollab.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

/**
 * ✅ Lớp phản hồi chung cho tất cả REST API trong hệ thống DevCollab.
 * Dùng để chuẩn hóa cấu trúc JSON trả về từ backend → frontend.
 * 
 * Ví dụ JSON trả về:
 * {
 * "status": "success",
 * "message": "Tạo project thành công",
 * "data": { "id": 1, "name": "DevCollab" },
 * "code": 200,
 * "timestamp": "2025-10-09T23:20:00"
 * }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private String status;
    private String message;
    private T data;
    private Integer code; 
    private LocalDateTime timestamp;

    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponse(String status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.code = 200;
        this.timestamp = LocalDateTime.now();
    }
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("success", message, data);
    }

    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>("success", message, null);
    }
    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> res = new ApiResponse<>("error", message, null);
        res.setCode(400);
        return res;
    }

    public static <T> ApiResponse<T> error(String message, int code) {
        ApiResponse<T> res = new ApiResponse<>("error", message, null);
        res.setCode(code);
        return res;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("success", null, data);
    }
    
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ApiResponse{" +
                "status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", code=" + code +
                ", timestamp=" + timestamp +
                '}';
    }
}
