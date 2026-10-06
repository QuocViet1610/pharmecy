package com.medilink.checkup.web;

import org.springframework.http.HttpStatus;

import java.util.Map;

/** Lỗi nghiệp vụ. {@code code} để client xử lý, {@code chiTiet} mang dữ liệu giúp người dùng sửa ngay. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Map<String, Object> chiTiet;

    private ApiException(HttpStatus status, String code, String message, Map<String, Object> chiTiet) {
        super(message);
        this.status = status;
        this.code = code;
        this.chiTiet = chiTiet == null ? Map.of() : chiTiet;
    }

    public static ApiException badRequest(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message, null);
    }

    public static ApiException notFound(String code, String message) {
        return new ApiException(HttpStatus.NOT_FOUND, code, message, null);
    }

    public static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message, null);
    }

    public static ApiException conflict(String code, String message, Map<String, Object> chiTiet) {
        return new ApiException(HttpStatus.CONFLICT, code, message, chiTiet);
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public Map<String, Object> getChiTiet() { return chiTiet; }
}
