package org.example.iw_order_service.exception;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(String s) {
        super(s);
    }
}
