package com.example.assettracker.Entities;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;

import java.util.ArrayList;

// The User document as stored in firebase.
public class UserDoc {
    private String userUid;
    private String eMail;
    private String displayName;
    private Timestamp accountCreatedAt; // firestore timestamp
    private ArrayList<DocumentReference> permissionGivenTo = new ArrayList<>();

    public UserDoc(String userUId, String eMail, String displayName) {
        this.userUid = userUId;
        this.eMail = eMail;
        this.displayName = displayName;
        this.accountCreatedAt = Timestamp.now();
    }

    public String getUserUid() {
        return userUid;
    }

    public String getEMail() { return eMail; }

    public String getDisplayName() {
        return displayName;
    }

    public Timestamp getAccountCreatedAt() {
        return accountCreatedAt;
    }

    public ArrayList<DocumentReference> getPermissionGivenTo() {
        return permissionGivenTo;
    }

    public void setPermissionGivenTo(ArrayList<DocumentReference> permissionGivenTo) {
        this.permissionGivenTo = permissionGivenTo;
    }
}
