package com.example.assettracker.Entities;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Exclude;

public class LogMessageDoc {
    private Timestamp time;
    private String level;
    private String message;
    public static final String LEVEL_ERROR = "ERROR";
    public static final String LEVEL_INFO = "INFO";

    public LogMessageDoc(String level, String message) {
        if (level != LEVEL_ERROR && level != LEVEL_INFO) {
            throw new IllegalArgumentException("level");
        }
        this.time = Timestamp.now();
        this.level = level;
        this.message = message;
    }

    public String getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public Timestamp getTime() {
        return time;
    }

    @Exclude
    public Boolean isError() {
        return level == LEVEL_ERROR;
    }
}
