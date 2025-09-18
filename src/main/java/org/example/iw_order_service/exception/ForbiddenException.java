package org.example.iw_order_service.exception;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String s) {
        super(s);
    }
}
