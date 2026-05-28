package com.btl.n8.Exception;

/**
 * Ném ra khi người dùng chưa đăng nhập hoặc không có quyền thực hiện thao tác.
 */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }
}