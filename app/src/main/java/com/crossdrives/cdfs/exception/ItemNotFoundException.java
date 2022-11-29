package com.crossdrives.cdfs.exception;

public class ItemNotFoundException extends RuntimeException{
    public ItemNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
