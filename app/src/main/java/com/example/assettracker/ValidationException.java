package com.example.assettracker;

import androidx.annotation.NonNull;

/**
 * Exception that indicates a developer error because an assumption is not valid.
 */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }

    public static void ensureTrue(@NonNull Boolean condition, @NonNull String exceptionMsgIfFalse) {
        if (!condition) {
            throw new ValidationException(exceptionMsgIfFalse);
        }
    }
}
