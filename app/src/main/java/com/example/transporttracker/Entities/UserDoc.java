package com.example.transporttracker.Entities;

// The User document as stored in firebase.
public class UserDoc {
    private String userId;
    private String eMail;
    private String displayName;
    private long accountCreatedAt;

    public UserDoc(String userId, String eMail, String displayName, long accountCreatedAt) {
        this.userId = userId;
        this.eMail = eMail;
        this.displayName = displayName;
        this.accountCreatedAt = accountCreatedAt;
    }

    public String getUserId() {
        return userId;
    }

    public String getEMail() { return eMail; }

    public String getDisplayName() {
        return displayName;
    }

    public long getAccountCreatedAt() {
        return accountCreatedAt;
    }
}
