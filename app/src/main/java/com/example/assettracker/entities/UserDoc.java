package com.example.assettracker.entities;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;

// The User document as stored in firebase.
public class UserDoc {
    private String userUid;
    private String eMail;
    private String displayName;
    @ServerTimestamp
    private Timestamp accountCreatedAt; // firestore timestamp
    private ArrayList<String> authorizedUsers = new ArrayList<>();

    public UserDoc(String userUId, String eMail, String displayName) {
        this.userUid = userUId;
        this.eMail = eMail;
        this.displayName = displayName;
    }

    public String getUserUid() {
        return userUid;
    }

    public String getEMail() { return eMail; }

    public String getDisplayName() { return displayName; }

    public Timestamp getAccountCreatedAt() { return accountCreatedAt; }

    public ArrayList<String> getAuthorizedUsers() { return authorizedUsers; }

    public void setAuthorizedUsers(ArrayList<String> authorizedUsers) {
        this.authorizedUsers = authorizedUsers;
    }
}
