package com.crossdrives.cdfs.exception;

public class MissingDriveClientException extends RuntimeException{
    public MissingDriveClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
